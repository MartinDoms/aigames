package com.guesshole.services;

import com.guesshole.entities.*;
import com.guesshole.repositories.PlayerRepository;
import com.guesshole.websocket.messages.outgoing.GameStateMessage;
import com.guesshole.websocket.messages.outgoing.GameStateMessageBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service for building GameStateMessage objects with appropriate data
 * based on the current state of the game
 */
@Service
public class GameStateMessageService {
    private static final Logger log = LoggerFactory.getLogger(GameStateMessageService.class);

    private final PlayerRepository playerRepository;
    private final RoundService roundService;
    private final GuessService guessService;
    private final LobbyService lobbyService;

    public GameStateMessageService(
            PlayerRepository playerRepository,
            RoundService roundService,
            GuessService guessService, LobbyService lobbyService) {
        this.playerRepository = playerRepository;
        this.roundService = roundService;
        this.guessService = guessService;
        this.lobbyService = lobbyService;
    }

    /**
     * Build a GameStateMessage with all available information for the current game state
     * @param lobbyId The lobby ID
     * @param state The current game state
     * @param gameInstanceId The game instance ID (may be null for LOBBY state)
     * @param currentRound The current round (may be null)
     * @return A Mono containing the populated GameStateMessage
     */
    public Mono<GameStateMessage> buildGameStateMessage(UUID lobbyId, String state, UUID gameInstanceId, Round currentRound) {
        log.debug("Building GameStateMessage for state: {}, lobby: {}, gameInstance: {}", state, lobbyId, gameInstanceId);

        // Initial builder with basic information
        GameStateMessageBuilder builder = GameStateMessageBuilder.builder()
                .state(state)
                .gameInstanceId(gameInstanceId);

        // Always start with the freshest lobby information
        return lobbyService.findById(lobbyId)
                .flatMap(lobby -> {
                    // Build message based on state
                    switch (state) {
                        case GameStateService.STATE_LOBBY:
                            // In LOBBY state, we only need basic information
                            return Mono.just(builder.build());

                        case GameStateService.STATE_GAME_IN_PROGRESS:
                            return buildGameInProgressMessage(lobby, gameInstanceId, currentRound, builder);

                        case GameStateService.STATE_ROUND_SCOREBOARD:
                            return buildRoundScoreboardMessage(lobby, gameInstanceId, currentRound, builder);

                        default:
                            log.warn("Unknown game state: {}", state);
                            return Mono.just(builder.build());
                    }
                });
    }

    /**
     * Build message for GAME_IN_PROGRESS state
     */
    private Mono<GameStateMessage> buildGameInProgressMessage(Lobby lobby, UUID gameInstanceId, Round currentRound, GameStateMessageBuilder builder) {
        if (currentRound != null) {
            builder
                    .currentRound(currentRound)
                    .roundId(currentRound.getId())
                    .roundOrder(currentRound.getRoundOrder());
        }

        // Get total rounds for this game instance
        return getTotalRoundsCount(gameInstanceId)
                .flatMap(totalRounds -> {
                    builder.totalRounds(totalRounds);

                    // If this is not the first round, include previous player scores
                    if (currentRound != null && currentRound.getRoundOrder() > 0) {
                        return fetchPlayerScores(lobby.getId(), gameInstanceId, currentRound)
                                .collectList()
                                .map(playerScores -> {
                                    builder.playerScores(playerScores);

                                    // Check if this is the last round
                                    if (currentRound.getRoundOrder() >= totalRounds - 1) {
                                        builder.lastRound(true);
                                    }

                                    builder.gameConfiguration(lobby.getGameConfiguration());
                                    return builder.build();
                                });
                    } else {
                        // First round - no player scores yet
                        builder.gameConfiguration(lobby.getGameConfiguration());
                        return Mono.just(builder.build());
                    }
                });
    }

    /**
     * Build message for ROUND_SCOREBOARD state
     */
    private Mono<GameStateMessage> buildRoundScoreboardMessage(Lobby lobby, UUID gameInstanceId, Round currentRound, GameStateMessageBuilder builder) {
        if (currentRound == null) {
            log.warn("Round scoreboard requested but no current round provided");
            return Mono.just(builder.build());
        }

        builder.currentRound(currentRound)
                .roundId(currentRound.getId())
                .roundOrder(currentRound.getRoundOrder());

        return getTotalRoundsCount(gameInstanceId)
                .flatMap(totalRounds -> {
                    builder.totalRounds(totalRounds);

                    // Check if this is the last round
                    if (currentRound.getRoundOrder() >= totalRounds - 1) {
                        builder.lastRound(true);
                    }

                    // Fetch player scores for this round
                    return fetchPlayerScores(lobby.getId(), gameInstanceId, currentRound)
                            .collectList()
                            .map(playerScores -> {
                                builder.playerScores(playerScores);
                                builder.gameConfiguration(lobby.getGameConfiguration());
                                return builder.build();
                            });
                });
    }

    /**
     * Get the total number of rounds for a game instance
     */
    private Mono<Integer> getTotalRoundsCount(UUID gameInstanceId) {
        if (gameInstanceId == null) {
            return Mono.just(0);
        }

        return roundService.findByGameInstanceId(gameInstanceId)
                .collectList()
                .map(List::size);
    }

    /**
     * Fetch player scores for the current round and game instance
     */
    private Flux<GameStateMessage.PlayerScore> fetchPlayerScores(UUID lobbyId, UUID gameInstanceId, Round currentRound) {
        // TODO avoid this n+1 by fetching all player and guess data in one query
        return playerRepository.findByLobbyId(lobbyId)
                .flatMap(player -> {
                    // Get player's current round guess
                    Mono<Guess> currentGuess = guessService.findPlayerGuessForRound(player.getId(), currentRound.getId());
                    // TODO make a proper default guess (defaultIfEmpty)

                    // Get all player's guesses in this game instance for total score
                    Mono<List<Guess>> allPlayerGuesses = guessService.findPlayerGuessesForGameInstance(gameInstanceId, player.getId())
                            .collectList();

                    return Mono.zip(currentGuess, allPlayerGuesses, (guess, allGuesses) -> {
                        // Calculate total score
                        int totalScore = allGuesses.stream()
                                .mapToInt(g -> g.getScore() != null ? g.getScore() : 0)
                                .sum();

                        List<ScoreMultiplier> multipliers = guess.getScoreMultipliers();

                        return new GameStateMessage.PlayerScore(
                                player, guess, totalScore
                        );
                    });
                })
                // Sort by total score descending
                .sort((ps1, ps2) -> Integer.compare(ps2.getTotalScore(), ps1.getTotalScore()));
    }

    /**
     * Calculate final scores for all players across all rounds
     */
    private List<GameStateMessage.PlayerScore> calculateFinalScores(List<Player> players, List<Guess> allGuesses) {
        // Group guesses by player ID
        var guessesByPlayer = allGuesses.stream()
                .collect(Collectors.groupingBy(Guess::getPlayerId));

        return players.stream()
                .map(player -> {
                    var playerGuesses = guessesByPlayer.getOrDefault(player.getId(), Collections.emptyList());

                    // Calculate total score
                    int totalScore = playerGuesses.stream()
                            .mapToInt(g -> g.getScore() != null ? g.getScore() : 0)
                            .sum();

                    // Get last round score and distance
                    int roundBaseScore = 0;
                    int roundScore = 0;
                    double roundDistance = 0.0;
                    List<ScoreMultiplier> multipliers = List.of();

                    Guess lastGuess = null;
                    if (!playerGuesses.isEmpty()) {
                        // Find the most recent guess
                        lastGuess = playerGuesses.stream()
                                .max(Comparator.comparing(Guess::getTimestamp))
                                .orElse(null);
                    }

                    return new GameStateMessage.PlayerScore(
                            player, lastGuess, totalScore
                    );
                })
                .sorted((ps1, ps2) -> Integer.compare(ps2.getTotalScore(), ps1.getTotalScore()))
                .collect(Collectors.toList());
    }
}