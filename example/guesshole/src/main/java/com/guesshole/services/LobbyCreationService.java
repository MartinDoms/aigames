package com.guesshole.services;

import com.guesshole.entities.GameState;
import com.guesshole.entities.Lobby;
import com.guesshole.repositories.GameStateRepository;
import com.guesshole.repositories.LobbyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Service to handle lobby creation with associated game state
 */
@Service
public class LobbyCreationService {
    private static final Logger log = LoggerFactory.getLogger(LobbyCreationService.class);

    private final LobbyRepository lobbyRepository;
    private final GameStateRepository gameStateRepository;

    public LobbyCreationService(LobbyRepository lobbyRepository, GameStateRepository gameStateRepository) {
        this.lobbyRepository = lobbyRepository;
        this.gameStateRepository = gameStateRepository;
    }

    /**
     * Create a new lobby with an initial game state
     */
    public Mono<Lobby> createLobby(String name, String privacy) {
        // Create a new lobby with UUID
        Lobby lobby = new Lobby(name, privacy);
        lobby.setId(UUID.randomUUID());

        log.info("Creating new lobby: {}", lobby);

        return lobbyRepository.save(lobby)
                .flatMap(savedLobby -> {
                    // Create initial game state
                    GameState gameState = new GameState(savedLobby.getId());
                    gameState.setLobbyState("LOBBY");

                    log.info("Creating initial game state for lobby: {}", savedLobby.getId());

                    return gameStateRepository.save(gameState)
                            .thenReturn(savedLobby);
                })
                .doOnSuccess(savedLobby -> log.info("Successfully created lobby with ID: {}", savedLobby.getId()))
                .doOnError(error -> log.error("Error creating lobby: {}", error.getMessage()));
    }
}