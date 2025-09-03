package com.guesshole.services;

import com.guesshole.entities.Guess;
import com.guesshole.entities.LocationPoint;
import com.guesshole.entities.Player;
import com.guesshole.entities.Round;
import com.guesshole.entities.ScoreMultiplier;
import com.guesshole.repositories.GuessRepository;
import com.guesshole.repositories.LocationPointRepository;
import com.guesshole.repositories.PlayerRepository;
import com.guesshole.repositories.ScoreMultiplierRepository;
import com.guesshole.utils.GeographicDistanceCalculator;
import com.guesshole.utils.ScoreCalculator;
import com.guesshole.websocket.messages.outgoing.GuessResultMessage;
import com.guesshole.websocket.services.WebSocketSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.env.Environment;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GuessService {
    private static final Logger log = LoggerFactory.getLogger(GuessService.class);

    private final GuessRepository guessRepository;
    private final RoundService roundService;
    private final PlayerRepository playerRepository;
    private final ScoreMultiplierRepository scoreMultiplierRepository;
    private final LocationPointRepository locationPointRepository;
    private final LocationService locationService;
    private final WebSocketSessionService sessionService;
    private final LobbyService lobbyService;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final Environment environment;

    public GuessService(
            GuessRepository guessRepository,
            RoundService roundService,
            PlayerRepository playerRepository,
            ScoreMultiplierRepository scoreMultiplierRepository,
            LocationPointRepository locationPointRepository,
            LocationService locationService,
            WebSocketSessionService sessionService,
            LobbyService lobbyService,
            R2dbcEntityTemplate r2dbcEntityTemplate,
            Environment environment) {
        this.guessRepository = guessRepository;
        this.roundService = roundService;
        this.playerRepository = playerRepository;
        this.scoreMultiplierRepository = scoreMultiplierRepository;
        this.locationPointRepository = locationPointRepository;
        this.locationService = locationService;
        this.sessionService = sessionService;
        this.lobbyService = lobbyService;
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
        this.environment = environment;
    }

    /**
     * Process a guess from a player
     *
     * @param playerId The ID of the player making the guess
     * @param roundId The ID of the round
     * @param latitude The guessed latitude
     * @param longitude The guessed longitude
     * @param roundDuration The total duration of the round in seconds
     * @param guessTime The time elapsed from round start to guess in seconds
     * @param session The WebSocket session
     * @return A Mono that completes when the guess has been processed
     */
    public Mono<Void> processGuess(
            UUID playerId,
            UUID roundId,
            BigDecimal latitude,
            BigDecimal longitude,
            Integer roundDuration,
            Integer guessTime,
            WebSocketSession session) {

        log.info("Processing guess from player {} for round {}: ({}, {}), made at {} seconds of {} second round",
                playerId, roundId, latitude, longitude, guessTime, roundDuration);

        // Find the player to get their name
        Mono<Player> playerMono = playerRepository.findById(playerId);

        // Find the round to get the actual location and game instance ID
        Mono<Round> roundMono = roundService.findById(roundId);

        // Find or create the location point for the guessed coordinates
        Mono<LocationPoint> locationPointMono = locationService.createAndSaveLocationPoint(
                longitude.doubleValue(), latitude.doubleValue());

        return Mono.zip(playerMono, roundMono, locationPointMono)
                .flatMap(tuple -> {
                    Player player = tuple.getT1();
                    Round round = tuple.getT2();
                    LocationPoint guessLocationPoint = tuple.getT3();

                    // Check if this is a solo game
                    return checkSoloGame(player.getLobbyId())
                            .flatMap(isSoloGame -> {
                                // Calculate distance
                                double distanceKm = calculateDistance(latitude, longitude, round);

                                // Process remaining logic
                                return processGuessLogic(player, round, guessLocationPoint, distanceKm,
                                        roundDuration, guessTime, isSoloGame, session);
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error processing guess from player {}: {}", playerId, e.getMessage(), e);
                    return Mono.empty();
                });
    }

    /**
     * Check if this is a solo game by counting players in the lobby
     */
    private Mono<Boolean> checkSoloGame(UUID lobbyId) {
        return playerRepository.findByLobbyId(lobbyId)
                .collectList()
                .map(players -> players.size() <= 1);
    }

    /**
     * Calculate the distance between the guess and the actual location
     */
    private double calculateDistance(BigDecimal latitude, BigDecimal longitude, Round round) {
        double distanceKm = GeographicDistanceCalculator.calculateDistanceInKilometers(
                latitude, longitude,
                round.getLatitude(), round.getLongitude());

        log.info("Distance between guess and actual location: {} km", distanceKm);
        return distanceKm;
    }

    /**
     * Process the main guess logic after initial data is gathered
     */
    private Mono<Void> processGuessLogic(
            Player player,
            Round round,
            LocationPoint guessLocationPoint,
            double distanceKm,
            Integer roundDuration,
            Integer guessTime,
            boolean isSoloGame,
            WebSocketSession session) {

        Mono<Boolean> isFirstGuessMono = guessRepository.countByRoundId(round.getId())
                .map(count -> count == 0);

        return isFirstGuessMono.flatMap(isFirstGuess ->
                // Get location data from the target location
                locationService.getLocationById(round.getLocationPointId())
                        .flatMap(targetLocation ->
                                processScoreAndSaveGuess(
                                        player,
                                        round,
                                        guessLocationPoint,
                                        targetLocation,
                                        distanceKm,
                                        isFirstGuess,
                                        guessTime,
                                        roundDuration,
                                        isSoloGame,
                                        session
                                )
                        )
        );
    }

    /**
     * Calculate the base score for a guess
     */
    private int calculateBaseScore(double distanceKm, Integer roundDuration, Integer guessTime) {
        return ScoreCalculator.calculateBaseScore(
                distanceKm,
                roundDuration,
                guessTime,
                environment.getProperty("app.game-round.maxDistanceThreshold", Double.class, 5000.0),
                environment.getProperty("app.game-round.minDistanceThreshold", Double.class, 10.0),
                environment.getProperty("app.game-round.distanceScoreMultiplier", Double.class, 1.0),
                environment.getProperty("app.game-round.timeScoreMultiplier", Double.class, 1.0),
                environment.getProperty("app.game-round.graceTimeThreshold", Double.class, 5.0)
        );
    }

    /**
     * Process score multipliers and save the guess
     */
    private Mono<Void> processScoreAndSaveGuess(
            Player player,
            Round round,
            LocationPoint guessLocationPoint,
            LocationPoint targetLocation,
            double distanceKm,
            boolean isFirstGuess,
            Integer guessTime,
            Integer roundDuration,
            boolean isSoloGame,
            WebSocketSession session) {


        // Calculate base score with isSoloGame parameter
        int baseScore = calculateBaseScore(distanceKm, roundDuration, guessTime);

        // Calculate multipliers
        List<ScoreMultiplier> scoreMultipliers = ScoreCalculator.calculateScoreMultipliers(
                distanceKm,
                guessTime,
                isFirstGuess,
                guessLocationPoint,
                targetLocation,
                isSoloGame
        );

        // Calculate final score with multipliers
        int finalScore = ScoreCalculator.calculateFinalScore(baseScore, scoreMultipliers);

        // Create guess entity with locationPointId
        Guess guess = new Guess(
                player.getId(),
                round.getId(),
                round.getGameInstanceId(),
                new BigDecimal(guessLocationPoint.getLatitude()),
                new BigDecimal(guessLocationPoint.getLongitude()),
                guessLocationPoint.getId(),
                distanceKm,
                baseScore,
                finalScore,
                scoreMultipliers,
                roundDuration,
                guessTime
        );

        return saveGuessAndNotifyPlayers(guess, player, round, guessLocationPoint, session);
    }

    /**
     * Save the guess and related data, then notify players
     */
    private Mono<Void> saveGuessAndNotifyPlayers(
            Guess guess,
            Player player,
            Round round,
            LocationPoint guessLocationPoint,
            WebSocketSession session) {

        // Use R2dbcEntityTemplate for explicit INSERT operation
        return r2dbcEntityTemplate.insert(Guess.class)
                .using(guess)
                .flatMap(savedGuess -> {
                    log.info("Saved guess with ID: {} and LocationPoint ID: {}",
                            savedGuess.getId(), savedGuess.getLocationPointId());

                    // Set the location point on the saved guess
                    savedGuess.setLocationPoint(guessLocationPoint);

                    return saveScoreMultipliers(savedGuess)
                            .then(sendGuessResults(savedGuess, player, round, guessLocationPoint, session));
                });
    }

    /**
     * Save all score multipliers for a guess
     */
    private Mono<Void> saveScoreMultipliers(Guess savedGuess) {
        // Create database entries for each multiplier
        List<Mono<ScoreMultiplier>> persistedMultipliers = new ArrayList<>();

        for (ScoreMultiplier multiplier : savedGuess.getScoreMultipliers()) {
            // Set the guess ID reference on each multiplier
            multiplier.setGuessId(savedGuess.getId());

            // Insert each multiplier into the database
            persistedMultipliers.add(r2dbcEntityTemplate.insert(ScoreMultiplier.class)
                    .using(multiplier)
                    .doOnSuccess(sm -> log.info("Saved score multiplier {} for guess {}",
                            sm.getId(), savedGuess.getId())));
        }

        // Wait for all multipliers to be persisted
        return Mono.when(persistedMultipliers);
    }

    /**
     * Send guess results to players
     */
    private Mono<Void> sendGuessResults(
            Guess savedGuess,
            Player player,
            Round round,
            LocationPoint guessLocationPoint,
            WebSocketSession session) {

        UUID lobbyId = player.getLobbyId();

        // Include location info in the result message
        GuessResultMessage resultMessage = new GuessResultMessage(
                savedGuess.getId(),
                round.getId(),
                player,
                guessLocationPoint,
                round.getLocationPoint(),
                savedGuess.getDistanceKm(),
                savedGuess.getBaseScore(),
                savedGuess.getScore(),
                savedGuess.getScoreMultipliers()
        );

        // Also fetch all previous guesses for this round to send to the player
        Mono<Void> sendPreviousGuesses = sendPreviousGuessesToPlayer(round.getId(), player.getId(), session);

        // Send the result back to the guessing player
        Mono<Void> sendResult = lobbyService.sendToSession(session, resultMessage);

        Mono<Void> broadcastGuess = broadcastGuessToPlayersWhoGuessed(
                lobbyId, round.getId(), player.getId(), resultMessage);

        // Execute all operations
        return Mono.when(sendResult, broadcastGuess, sendPreviousGuesses);
    }

    /**
     * Broadcast a player's guess only to other players who have already submitted their own guesses
     */
    private Mono<Void> broadcastGuessToPlayersWhoGuessed(
            UUID lobbyId,
            UUID roundId,
            UUID currentPlayerId,
            GuessResultMessage message) {

        log.info("Broadcasting guess from player {} to other players who have already guessed", currentPlayerId);

        // Find all guesses for this round to determine which players have already guessed
        return guessRepository.findByRoundId(roundId)
                .collectList()
                .flatMap(guesses -> {
                    // Create a set of player IDs who have already guessed
                    Set<UUID> playersWhoGuessed = guesses.stream()
                            .map(Guess::getPlayerId)
                            .collect(Collectors.toSet());

                    log.info("Found {} players who have already guessed for round {}",
                            playersWhoGuessed.size(), roundId);

                    // Filter out the current player (don't send to self)
                    playersWhoGuessed.remove(currentPlayerId);

                    if (playersWhoGuessed.isEmpty()) {
                        log.info("No other players have guessed yet - skipping broadcast");
                        return Mono.empty();
                    }

                    // Get all active sessions for this lobby
                    List<WebSocketSession> lobbySessions = sessionService.getLobbySessions(lobbyId);
                    if (lobbySessions.isEmpty()) {
                        log.info("No active sessions found for lobby {} - skipping broadcast", lobbyId);
                        return Mono.empty();
                    }

                    // For each session, check if it belongs to a player who has already guessed
                    List<Mono<Void>> sendOperations = new ArrayList<>();

                    for (WebSocketSession playerSession : lobbySessions) {
                        // Get the player ID for this session
                        UUID sessionPlayerId = sessionService.getPlayerIdForSession(playerSession);

                        if (sessionPlayerId != null &&
                                playersWhoGuessed.contains(sessionPlayerId) &&
                                playerSession.isOpen()) {

                            log.info("Sending guess info to player {} who already guessed", sessionPlayerId);
                            sendOperations.add(lobbyService.sendToSession(playerSession, message));
                        }
                    }

                    if (sendOperations.isEmpty()) {
                        log.info("No sessions found for players who have already guessed");
                        return Mono.empty();
                    }

                    // Execute all send operations in parallel
                    return Mono.when(sendOperations);
                });
    }

    /**
     * Send previous guesses for a round to a player
     */
    private Mono<Void> sendPreviousGuessesToPlayer(UUID roundId, UUID currentPlayerId, WebSocketSession session) {
        log.info("Fetching previous guesses for round {} to send to player {}", roundId, currentPlayerId);

        // Find the guesses for this round, excluding the current player's guess
        return guessRepository.findByRoundId(roundId)
                .filter(guess -> !guess.getPlayerId().equals(currentPlayerId)) // Exclude the current player's guess
                .flatMap(guess -> {
                    // Load location point for each guess
                    Mono<LocationPoint> locationPointMono = locationPointRepository.findById(guess.getLocationPointId());

                    return Mono.zip(
                            playerRepository.findById(guess.getPlayerId()),
                            roundService.findById(roundId),
                            locationPointMono
                    ).map(tuple -> {
                        Player player = tuple.getT1();
                        Round round = tuple.getT2();
                        LocationPoint guessLocationPoint = tuple.getT3();

                        guess.setLocationPoint(guessLocationPoint);

                        // Create a player guess message with location info
                        return new GuessResultMessage(
                                guess.getId(),
                                roundId,
                                player,
                                guessLocationPoint,
                                round.getLocationPoint(),
                                guess.getDistanceKm(),
                                guess.getBaseScore(),
                                guess.getScore(),
                                guess.getScoreMultipliers()
                        );
                    });
                })
                .flatMap(message -> lobbyService.sendToSession(session, message))
                .then();
    }

    /**
     * Find a player's guess for a specific round, with associated LocationPoint data
     */
    public Mono<Guess> findPlayerGuessForRound(UUID playerId, UUID roundId) {
        return guessRepository.findByPlayerIdAndRoundId(playerId, roundId)
                .flatMap(guess -> {
                    if (guess == null) {
                        return Mono.empty();
                    }

                    Mono<List<ScoreMultiplier>> multipliersMono =
                            scoreMultiplierRepository.findByGuessId(guess.getId()).collectList();

                    Mono<LocationPoint> locationPointMono = guess.getLocationPointId() != null ?
                            locationPointRepository.findById(guess.getLocationPointId()) : Mono.empty();

                    return Mono.zip(multipliersMono, locationPointMono)
                            .map(tuple -> {
                                List<ScoreMultiplier> multipliers = tuple.getT1();
                                LocationPoint locationPoint = tuple.getT2();

                                guess.setScoreMultipliers(multipliers);
                                if (locationPoint != null) {
                                    guess.setLocationPoint(locationPoint);
                                }
                                return guess;
                            });
                });
    }

    /**
     * Find all guesses for a player in a game instance, with LocationPoint data
     */
    public Flux<Guess> findPlayerGuessesForGameInstance(UUID gameInstanceId, UUID playerId) {
        return guessRepository.findByGameInstanceIdAndPlayerId(gameInstanceId, playerId)
                .flatMap(this::loadGuessRelations);
    }

    /**
     * Find all guesses for a game instance, with LocationPoint data
     */
    public Flux<Guess> findAllGuessesForGameInstance(UUID gameInstanceId) {
        return guessRepository.findByGameInstanceId(gameInstanceId)
                .flatMap(this::loadGuessRelations);
    }

    /**
     * Helper method to load related data for a guess
     */
    private Mono<Guess> loadGuessRelations(Guess guess) {
        Mono<List<ScoreMultiplier>> multipliersMono =
                scoreMultiplierRepository.findByGuessId(guess.getId()).collectList();

        Mono<LocationPoint> locationPointMono = guess.getLocationPointId() != null ?
                locationPointRepository.findById(guess.getLocationPointId()) : Mono.empty();

        return Mono.zip(multipliersMono, locationPointMono)
                .map(tuple -> {
                    List<ScoreMultiplier> multipliers = tuple.getT1();
                    LocationPoint locationPoint = tuple.getT2();

                    guess.setScoreMultipliers(multipliers);
                    if (locationPoint != null) {
                        guess.setLocationPoint(locationPoint);
                    }
                    return guess;
                });
    }
}