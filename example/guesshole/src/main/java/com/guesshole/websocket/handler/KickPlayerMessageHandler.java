package com.guesshole.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guesshole.services.KickPlayerService;
import com.guesshole.websocket.messages.incoming.KickPlayerMessage;
import com.guesshole.websocket.services.WebSocketSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for KICK_PLAYER messages
 */
@Component
public class KickPlayerMessageHandler implements MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(KickPlayerMessageHandler.class);
    private static final String MESSAGE_TYPE = "KICK_PLAYER";

    private final ObjectMapper objectMapper;
    private final WebSocketSessionService sessionService;
    private final KickPlayerService kickPlayerService;

    public KickPlayerMessageHandler(
            ObjectMapper objectMapper,
            WebSocketSessionService sessionService,
            KickPlayerService kickPlayerService) {
        this.objectMapper = objectMapper;
        this.sessionService = sessionService;
        this.kickPlayerService = kickPlayerService;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public Mono<Void> handle(JsonNode payload, WebSocketSession session, UUID lobbyId) {
        try {
            KickPlayerMessage message = objectMapper.treeToValue(payload, KickPlayerMessage.class);
            UUID playerIdToKick = message.getPlayerId();

            if (playerIdToKick == null) {
                log.warn("Received kick player message without player ID");
                return Mono.empty();
            }

            // Get the ID of the player sending the request (the host)
            UUID hostPlayerId = sessionService.getPlayerIdForSession(session);
            if (hostPlayerId == null) {
                log.error("Player not associated with session");
                return Mono.empty();
            }

            log.info("Processing kick request from player {} for player {}", hostPlayerId, playerIdToKick);

            // Verify the requester is the host and can kick the target player
            return kickPlayerService.kickPlayer(hostPlayerId, playerIdToKick, lobbyId)
                    .onErrorResume(e -> {
                        log.error("Error kicking player: {}", e.getMessage(), e);
                        return Mono.empty();
                    });
        } catch (Exception e) {
            log.error("Error processing KICK_PLAYER message", e);
            return Mono.error(e);
        }
    }
}