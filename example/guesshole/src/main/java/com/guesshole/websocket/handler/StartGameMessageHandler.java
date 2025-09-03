package com.guesshole.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guesshole.repositories.PlayerRepository;
import com.guesshole.websocket.messages.incoming.StartGameMessage;
import com.guesshole.services.GameStateService;
import com.guesshole.services.LobbyService;
import com.guesshole.websocket.services.WebSocketSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for START_GAME messages.
 */
@Component
public class StartGameMessageHandler implements MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(StartGameMessageHandler.class);
    private static final String MESSAGE_TYPE = "START_GAME";

    private final ObjectMapper objectMapper;
    private final WebSocketSessionService sessionService;
    private final PlayerRepository playerRepository;
    private final LobbyService lobbyService;
    private final GameStateService gameStateService;

    public StartGameMessageHandler(
            ObjectMapper objectMapper,
            WebSocketSessionService sessionService,
            PlayerRepository playerRepository,
            LobbyService lobbyService,
            GameStateService gameStateService
    ) {
        this.objectMapper = objectMapper;
        this.sessionService = sessionService;
        this.playerRepository = playerRepository;
        this.lobbyService = lobbyService;
        this.gameStateService = gameStateService;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public Mono<Void> handle(JsonNode payload, WebSocketSession session, UUID lobbyId) {
        log.info("Received START_GAME message from session {} in lobby {}", session.getId(), lobbyId);

        // Get the player ID associated with this session
        UUID playerId = sessionService.getPlayerIdForSession(session);
        if (playerId == null) {
            log.warn("START_GAME message received from session with no player ID");
            return Mono.empty();
        }

        try {
            // Parse the message with game settings
            StartGameMessage message = objectMapper.treeToValue(payload, StartGameMessage.class);
            log.info("Processing start game request: rounds={}, roundLength={}, geoType={}, session={}",
                    message.getRounds(), message.getRoundLength(), message.getGeoType(), session.getId());

            // Verify that the player is the host
            return playerRepository.findById(playerId)
                    .flatMap(player -> {
                        if (!player.isHost()) {
                            log.warn("Non-host player {} attempted to start game in lobby {}", playerId, lobbyId);
                            return Mono.empty();
                        }

                        log.info("Host player {} started game in lobby {} with settings: rounds={}, roundLength={}, geoType={}",
                                playerId, lobbyId, message.getRounds(), message.getRoundLength(), message.getGeoType());

                        // Change the game state in the database with the provided settings
                        return gameStateService.startGame(lobbyId, message.getRounds(), message.getRoundLength(), message.getGeoType());
                    })
                    .onErrorResume(e -> {
                        log.error("Error handling START_GAME message", e);
                        return Mono.empty();
                    });
        } catch (Exception e) {
            log.error("Error processing START_GAME message", e);
            return Mono.error(e);
        }
    }
}