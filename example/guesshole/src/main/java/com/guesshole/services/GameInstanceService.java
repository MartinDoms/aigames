package com.guesshole.services;

import com.guesshole.entities.*;
import com.guesshole.repositories.PlayerRepository;
import com.guesshole.repositories.RoundTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.r2dbc.core.R2dbcEntityTemplate;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class GameInstanceService {
    private static final Logger log = LoggerFactory.getLogger(GameInstanceService.class);
    private final PlayerRepository playerRepository;
    private final R2dbcEntityTemplate r2dbcEntityTemplate;
    private final RoundTemplateService roundTemplateService;

    public GameInstanceService(
            RoundTemplateService roundTemplateService,
            PlayerRepository playerRepository,
            R2dbcEntityTemplate r2dbcEntityTemplate
    ) {
        this.roundTemplateService = roundTemplateService;
        this.playerRepository = playerRepository;
        this.r2dbcEntityTemplate = r2dbcEntityTemplate;
    }

    public Mono<GameInstance> createGameInstance(int numRounds, int roundLength, String geoType) {
        log.info("Creating new game instance with settings: rounds={}, roundLength={}, geoType={}",
                numRounds, roundLength, geoType);

        // Create the game instance with UUID
        GameInstance gameInstance = new GameInstance();
        gameInstance.setId(UUID.randomUUID());
        gameInstance.setGameType(GameType.CITY_GUESSER);

        log.info("Inserting game instance with ID: {}", gameInstance.getId());

        // Use insert operation explicitly
        return r2dbcEntityTemplate.insert(GameInstance.class)
                .using(gameInstance)
                .doOnNext(savedInstance -> log.info("Game instance saved with ID: {}", savedInstance.getId()))
                .flatMap(savedInstance -> {
                    // Select random round templates from the database based on region filter
                    Flux<RoundTemplate> templatesFlux = roundTemplateService.getRandomTemplates(numRounds);

                    // Inside the createGameInstance method in GameInstanceService.java
                    return templatesFlux
                            .index() // Add index for round ordering
                            .map(tuple -> {
                                RoundTemplate template = tuple.getT2();
                                int roundOrder = tuple.getT1().intValue();

                                // Calculate a random start time that:
                                // 1. Skips the first 5 minutes (300 seconds)
                                // 2. Ensures the video doesn't end before the round ends
                                int minStartTime = 300; // Skip first 5 minutes (300 seconds)
                                int videoLength = template.getVideoLength() != null ? template.getVideoLength() : 0;

                                // Make sure we have enough video to play for the round duration
                                int maxPossibleStartTime = videoLength - roundLength;

                                // If video is too short or we don't have length info, use template start time as fallback
                                int startTime = template.getStartTime();

                                if (videoLength > 0 && maxPossibleStartTime > minStartTime) {
                                    // Generate random start time between min and max
                                    startTime = minStartTime + (int)(Math.random() * (maxPossibleStartTime - minStartTime));
                                    log.debug("Generated random start time {} for video {} (length: {}s, round: {}s)",
                                            startTime, template.getYoutubeVideoId(), videoLength, roundLength);
                                } else {
                                    log.warn("Using template start time {}s for video {} - insufficient length info or video too short",
                                            startTime, template.getYoutubeVideoId());
                                }

                                // Create a round from the template with the calculated start time
                                Round round = new Round(
                                        roundOrder,
                                        template.getYoutubeVideoId(),
                                        startTime,
                                        roundLength,
                                        template.getLatitude(),
                                        template.getLongitude(),
                                        template.getLocationPointId()
                                );

                                round.setId(UUID.randomUUID());
                                round.setGameInstanceId(savedInstance.getId());

                                return round;
                            })
                            .collectList()
                            .flatMap(generatedRounds -> {
                                log.info("Inserting {} rounds for game instance: {}", generatedRounds.size(), savedInstance.getId());

                                if (generatedRounds.isEmpty()) {
                                    log.warn("No round templates found in the database for geoType: {}. Cannot create rounds for game instance: {}",
                                            geoType, savedInstance.getId());
                                    return Mono.just(savedInstance); // Return instance without rounds if no templates exist
                                }

                                @SuppressWarnings("unchecked")
                                Mono<Round>[] roundInsertions = generatedRounds.stream()
                                        .map(round -> r2dbcEntityTemplate.insert(Round.class).using(round))
                                        .toArray(Mono[]::new);

                                return Mono.when(roundInsertions)
                                        .then(Mono.just(savedInstance));
                            })
                            .flatMap(instance -> {
                                // Fetch rounds to attach to the instance
                                return r2dbcEntityTemplate.select(Round.class)
                                        .matching(org.springframework.data.relational.core.query.Query.query(
                                                org.springframework.data.relational.core.query.Criteria.where("game_instance_id").is(instance.getId())
                                        ))
                                        .all()
                                        .collectList()
                                        .map(fetchedRounds -> {
                                            instance.setRounds(fetchedRounds);
                                            return instance;
                                        });
                            });
                });
    }

    /**
     * Associate all players in a lobby with a game instance
     */
    public Mono<Void> associateLobbyPlayersWithGameInstance(UUID lobbyId, GameInstance gameInstance) {
        return playerRepository.findByLobbyId(lobbyId)
                .flatMap(player -> {
                    log.info("Associating player {} with game instance {}", player.getName(), gameInstance.getId());

                    // Create the join entity with UUID
                    GameInstancePlayer association = new GameInstancePlayer(gameInstance.getId(), player.getId());
                    association.setId(UUID.randomUUID());

                    // Use explicit insert
                    return r2dbcEntityTemplate.insert(GameInstancePlayer.class)
                            .using(association)
                            .doOnNext(saved -> log.info("Created association with ID: {}", saved.getId()))
                            .onErrorResume(e -> {
                                log.error("Error associating player {} with game: {}", player.getId(), e.getMessage());
                                return Mono.empty();
                            });
                })
                .then();
    }

}
