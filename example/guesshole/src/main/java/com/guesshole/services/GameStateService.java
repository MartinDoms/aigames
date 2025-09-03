package com.guesshole.services;

import reactor.core.Disposable;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import com.guesshole.entities.*;
import com.guesshole.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.util.stream.Collectors;

/**
 * Service for managing game state
 */
@Service
public class GameStateService {
    private static final Logger log = LoggerFactory.getLogger(GameStateService.class);

    private final GameStateRepository gameStateRepository;
    private final LobbyService lobbyService;
    private final PlayerRepository playerRepository;
    private final GuessRepository guessRepository;
    private final RoundService roundService;
    private final GameInstanceService gameInstanceService;
    private final GameStateMessageService gameStateMessageService;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;

    private final Map<UUID, Disposable> roundTimers = new ConcurrentHashMap<>();
    private final Map<UUID, Disposable> roundAdvanceTimers = new ConcurrentHashMap<>();

    // Valid game states
    public static final String STATE_LOBBY = "LOBBY";
    public static final String STATE_GAME_IN_PROGRESS = "GAME_IN_PROGRESS";
    public static final String STATE_ROUND_SCOREBOARD = "ROUND_SCOREBOARD";


    public GameStateService(GameStateRepository gameStateRepository,
                            LobbyService lobbyService,
                            PlayerRepository playerRepository,
                            GuessRepository guessRepository,
                            RoundService roundService,
                            GameInstanceService gameInstanceService,
                            GameStateMessageService gameStateMessageService,
                            R2dbcEntityTemplate r2dbcEntityTemplate) {
        this.gameStateRepository = gameStateRepository;
        this.lobbyService = lobbyService;
        this.playerRepository = playerRepository;
        this.guessRepository = guessRepository;
        this.roundService = roundService;
        this.gameInstanceService = gameInstanceService;
        this.gameStateMessageService = gameStateMessageService;
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
    }

    /**
     * Change the game state of a lobby and broadcast to all clients
     */
    public Mono<Void> changeGameState(UUID lobbyId, String newState, UUID gameInstanceId, Round currentRound) {
        log.info("Changing game state for lobby {} to {}", lobbyId, newState);

        UUID roundId = currentRound != null ? currentRound.getId() : null;

        return gameStateRepository.findByLobbyId(lobbyId)
                .defaultIfEmpty(new GameState(lobbyId))
                .flatMap(gameState -> {
                    // Update or create game state with new values
                    gameState.setLobbyState(newState);
                    gameState.setRoundId(roundId);
                    gameState.setGameInstanceId(gameInstanceId);

                    log.info("Saving game state for lobby {}: state={}, roundId={}, gameInstanceId={}",
                            lobbyId, newState, roundId, gameInstanceId);

                    return gameStateRepository.save(gameState);
                })
                .flatMap(updatedGameState -> {
                    // Fetch the full round data if there's a round ID
                    if (updatedGameState.getRoundId() != null && (currentRound == null || !updatedGameState.getRoundId().equals(currentRound.getId()))) {
                        return roundService.findById(updatedGameState.getRoundId())
                                .map(fetchedRound -> {
                                    // Create a client-safe version of the round if needed
                                    if (STATE_GAME_IN_PROGRESS.equals(updatedGameState.getLobbyState())) {
                                        return fetchedRound.getClientSafeRound();
                                    }
                                    return fetchedRound;
                                })
                                .defaultIfEmpty(currentRound)
                                .flatMap(round -> broadcastGameState(lobbyId, updatedGameState.getLobbyState(),
                                        updatedGameState.getGameInstanceId(), round));
                    } else {
                        // Use the round provided in the method arguments
                        return broadcastGameState(lobbyId, updatedGameState.getLobbyState(),
                                updatedGameState.getGameInstanceId(), currentRound);
                    }
                })
                .onErrorResume(e -> {
                    log.error("Error changing game state for lobby {}: {}", lobbyId, e.getMessage(), e);
                    return Mono.empty();
                });
    }

    /**
     * Broadcast the current game state to all clients in a lobby
     */
    public Mono<Void> broadcastGameState(UUID lobbyId, String state, UUID gameInstanceId, Round currentRound) {
        return gameStateMessageService.buildGameStateMessage(lobbyId, state, gameInstanceId, currentRound)
                .flatMap(message -> lobbyService.broadcastToLobby(lobbyId, message));
    }

    /**
     * Send the current game state to a specific client
     */
    public Mono<Void> sendGameStateToClient(UUID lobbyId, WebSocketSession session) {
        return gameStateRepository.findByLobbyId(lobbyId)
                .switchIfEmpty(Mono.defer(() -> {
                    // Create a new GameState if none exists - this happens on a new lobby
                    GameState newGameState = new GameState(lobbyId);
                    return gameStateRepository.save(newGameState);
                }))
                .flatMap(gameState -> {
                    String state = gameState.getLobbyState();
                    UUID gameInstanceId = gameState.getGameInstanceId();
                    UUID roundId = gameState.getRoundId();

                    // If we have a round ID, fetch the round first
                    Mono<Round> roundMono = roundId != null
                            ? roundService.findById(roundId)
                            .map(round -> {
                                // Create a client-safe version of the round if needed
                                if (STATE_GAME_IN_PROGRESS.equals(state)) {
                                    return round.getClientSafeRound();
                                }
                                return round;
                            })
                            : Mono.empty();

                    return roundMono
                            .flatMap(round -> gameStateMessageService.buildGameStateMessage(
                                    lobbyId, state, gameInstanceId, round
                            ))
                            .switchIfEmpty(gameStateMessageService.buildGameStateMessage(
                                    lobbyId, state, gameInstanceId, null
                            ));
                })
                .flatMap(message -> lobbyService.sendToSession(session, message));
    }

    /**
     * Start the game for a lobby
     */
    public Mono<Void> startGame(UUID lobbyId, int rounds, int roundLength, String geoType) {
        log.info("Starting game for lobby {} with settings: rounds={}, roundLength={}, geoType={}",
                lobbyId, rounds, roundLength, geoType);

        GameConfiguration gameConfiguration = new GameConfiguration(GameType.CITY_GUESSER, rounds, roundLength, geoType);

        return lobbyService.setLobbyGameConfiguration(lobbyId, gameConfiguration)
                .flatMap(lobby -> gameInstanceService.createGameInstance(rounds, roundLength, geoType)
                        .flatMap(gameInstance -> gameInstanceService.associateLobbyPlayersWithGameInstance(lobbyId, gameInstance)
                                .thenReturn(gameInstance))
                        .flatMap(gameInstance -> {
                            log.info("Starting game with instance ID: {}", gameInstance.getId());

                            // Get the first round for the game
                            Round firstRound = gameInstance.getRounds().get(0);
                            // Set the round length from game settings
                            firstRound.setDurationSeconds(roundLength);

                            // Create a client-safe version of the round (without lat/long)
                            Round clientSafeRound = firstRound.getClientSafeRound();

                            // Start the timer for the first round
                            startRoundTimer(lobbyId, firstRound);

                            // Change the game state and broadcast with the game instance ID and first round
                            return changeGameState(lobbyId, STATE_GAME_IN_PROGRESS, gameInstance.getId(), clientSafeRound);
                        }))
                .onErrorResume(e -> {
                    log.error("Error starting game for lobby {}: {}", lobbyId, e.getMessage(), e);
                    return Mono.empty();
                });

    }

    /**
     * Reset the game to lobby state
     */
    public Mono<Void> resetToLobby(UUID lobbyId) {
        // Cancel any running round timer
        cancelRoundTimer(lobbyId);

        // TODO we will need to store scoreboard timers and store them here too

        // Change game state to LOBBY
        return changeGameState(lobbyId, STATE_LOBBY, null, null);
    }

    /**
     * Start a timer for the current round
     */
    private void startRoundTimer(UUID lobbyId, Round round) {
        log.info("Starting timer for round {} in lobby {} with duration {} seconds",
                round.getId(), lobbyId, round.getDurationSeconds());

        // Cancel any existing timer for this lobby
        cancelRoundTimer(lobbyId);

        // Let's fetch the round fresh, because sometimes it's not fully populated with data
        roundService.findById(round.getId())
                .doOnNext(freshRound -> {
                    Disposable disposable = Mono.delay(Duration.ofSeconds(freshRound.getDurationSeconds()))
                            .flatMap(ignored -> handleRoundEnd(lobbyId, freshRound))
                            .doOnSubscribe(sub -> log.info("Round timer started for lobby {}", lobbyId))
                            .doOnError(error -> log.error("Error in round timer: {}", error.getMessage(), error))
                            .subscribe();

                    // Store the timer reference
                    roundTimers.put(lobbyId, disposable);
                }).subscribe();
    }

    private void cancelRoundTimer(UUID lobbyId) {
        Disposable existingTimer = roundTimers.remove(lobbyId);
        if (existingTimer != null && !existingTimer.isDisposed()) {
            log.info("Cancelling existing round timer for lobby {}", lobbyId);
            existingTimer.dispose();
        }
    }

    /**
     * Handle the end of a round
     */
    public Mono<Void> handleRoundEnd(UUID lobbyId, Round round) {
        log.info("Handling round end for round {} in lobby {}", round.getId(), lobbyId);

        // Check if this is the last round and get the next round if available
        return r2dbcEntityTemplate.select(Round.class)
                .matching(org.springframework.data.relational.core.query.Query.query(
                        org.springframework.data.relational.core.query.Criteria.where("game_instance_id").is(round.getGameInstanceId())
                ))
                .all()
                .collectList()
                .flatMap(rounds -> {
                    int totalRounds = rounds.size();
                    boolean isLastRound = round.getRoundOrder() >= totalRounds - 1;

                    log.info("Round {}/{} ended. Is last round: {}",
                            round.getRoundOrder() + 1, totalRounds, isLastRound);

                    // First change game state in the database
                    return changeGameState(lobbyId, STATE_ROUND_SCOREBOARD, round.getGameInstanceId(), round);
                })
                .onErrorResume(e -> {
                    log.error("Error handling round end: {}", e.getMessage(), e);
                    return Mono.<Void>empty(); // Explicitly typed as Void
                });
    }

    /**
     * Check if all active players in a lobby have submitted guesses for a round
     * If all have submitted, end the round early
     *
     * @param lobbyId The lobby ID
     * @param round The current round
     * @return A Mono that completes when the check is done
     */
    public Mono<Void> checkAllPlayersGuessed(UUID lobbyId, Round round) {
        log.info("Checking if all players in lobby {} have submitted guesses for round {}", lobbyId, round.getId());

        // Get all active players in the lobby
        return playerRepository.findByLobbyId(lobbyId)
                .collectList()
                .flatMap(activePlayers -> {
                    if (activePlayers.isEmpty()) {
                        log.warn("No active players found in lobby {}", lobbyId);
                        return Mono.empty();
                    }

                    log.info("Found {} active players in lobby {}", activePlayers.size(), lobbyId);

                    // Get all guesses for this round
                    return guessRepository.findByRoundId(round.getId())
                            .collectList()
                            .flatMap(guesses -> {
                                // Create a set of player IDs who have guessed
                                Set<UUID> playerGuesses = guesses.stream()
                                        .map(Guess::getPlayerId)
                                        .collect(Collectors.toSet());

                                log.info("Found {} guesses for round {}", playerGuesses.size(), round.getId());

                                // Check if all active players have guessed
                                boolean allPlayersGuessed = true;
                                for (Player activePlayer : activePlayers) {
                                    if (!playerGuesses.contains(activePlayer.getId())) {
                                        allPlayersGuessed = false;
                                        log.info("Player {} has not submitted a guess yet", activePlayer.getId());
                                        break;
                                    }
                                }

                                if (allPlayersGuessed && activePlayers.size() > 0) {
                                    log.info("All {} active players have submitted guesses for round {}. Ending round early.",
                                            activePlayers.size(), round.getId());

                                    // Cancel the current round timer
                                    cancelRoundTimer(lobbyId);

                                    // Handle the round end using the existing method
                                    return handleRoundEnd(lobbyId, round);
                                } else {
                                    log.info("Not all players have submitted guesses for round {} yet.", round.getId());
                                    return Mono.empty();
                                }
                            });
                });
    }

    /**
     * Advance to the next round
     */
    public Mono<Void> advanceToNextRound(UUID lobbyId, Round currentRound) {
        log.info("Advancing to next round after round {} in game {}", currentRound.getRoundOrder(), currentRound.getGameInstanceId());

        int nextRoundOrder = currentRound.getRoundOrder() + 1;

        // Find the next round
        return r2dbcEntityTemplate.select(Round.class)
                .matching(org.springframework.data.relational.core.query.Query.query(
                        org.springframework.data.relational.core.query.Criteria.where("game_instance_id").is(currentRound.getGameInstanceId())
                                .and("round_order").is(nextRoundOrder)
                ))
                .one()
                .flatMap(nextRound -> {
                    log.info("Found next round: {}", nextRound.getId());

                    // Create a client-safe version of the round
                    Round clientSafeRound = nextRound.getClientSafeRound();

                    var existingTimer = roundAdvanceTimers.remove(lobbyId);
                    if (existingTimer != null && !existingTimer.isDisposed()) {
                        log.info("Cancelling existing round advance timer for lobby {}", lobbyId);
                        existingTimer.dispose();
                    }

                    // Update the game state with the new round
                    return changeGameState(lobbyId, STATE_GAME_IN_PROGRESS, currentRound.getGameInstanceId(), clientSafeRound)
                            .then(Mono.fromRunnable(() -> {
                                // Start the timer for the next round
                                startRoundTimer(lobbyId, nextRound);
                            }))
                            .then(); // Ensure we return Mono<Void>
                })
                .onErrorResume(e -> {
                    log.error("Error advancing to next round: {}", e.getMessage(), e);
                    var existingTimer = roundAdvanceTimers.remove(lobbyId);
                    if (existingTimer != null && !existingTimer.isDisposed()) {
                        log.info("Cancelling existing round advance timer for lobby {}", lobbyId);
                        existingTimer.dispose();
                    }
                    return Mono.<Void>empty(); // Explicitly typed as Void
                })
                .then(); // Final then() to ensure Mono<Void> return type
    }
}