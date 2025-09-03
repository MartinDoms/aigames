package com.guesshole.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.guesshole.repositories.PlayerRepository;
import com.guesshole.services.GameStateService;
import com.guesshole.websocket.services.WebSocketSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for RETURN_TO_LOBBY messages.
 * Resets the game state back to lobby mode.
 */
@Component
public class ReturnToLobbyMessageHandler implements MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(ReturnToLobbyMessageHandler.class);
    private static final String MESSAGE_TYPE = "RETURN_TO_LOBBY";

    private final WebSocketSessionService sessionService;
    private final PlayerRepository playerRepository;
    private final GameStateService gameStateService;

    public ReturnToLobbyMessageHandler(
            WebSocketSessionService sessionService,
            PlayerRepository playerRepository,
            GameStateService gameStateService
    ) {
        this.sessionService = sessionService;
        this.playerRepository = playerRepository;
        this.gameStateService = gameStateService;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public Mono<Void> handle(JsonNode message, WebSocketSession session, UUID lobbyId) {
        log.info("Handling RETURN_TO_LOBBY request for lobby: {}", lobbyId);

        // Get the player ID for this session
        UUID playerId = sessionService.getPlayerIdForSession(session);
        if (playerId == null) {
            log.warn("Session has no associated player ID, cannot return to lobby");
            return Mono.empty();
        }

        // Verify the player is the host
        return playerRepository.findById(playerId)
                .flatMap(player -> {
                    if (!player.isHost()) {
                        log.warn("Non-host player {} attempted to return game to lobby", playerId);
                        return Mono.empty();
                    }

                    log.info("Host player {} returning game to lobby state", playerId);

                    // Reset the game state to LOBBY
                    return gameStateService.resetToLobby(lobbyId);
                })
                .onErrorResume(e -> {
                    log.error("Error handling return to lobby request: {}", e.getMessage(), e);
                    return Mono.empty();
                });
    }
}