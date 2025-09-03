package com.guesshole.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guesshole.websocket.messages.incoming.PlayerReconnectMessage;
import com.guesshole.services.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for PLAYER_RECONNECT messages
 */
@Component
public class PlayerReconnectMessageHandler implements MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(PlayerReconnectMessageHandler.class);
    private static final String MESSAGE_TYPE = "PLAYER_RECONNECT";

    private final ObjectMapper objectMapper;
    private final PlayerService playerService;

    public PlayerReconnectMessageHandler(ObjectMapper objectMapper, PlayerService playerService) {
        this.objectMapper = objectMapper;
        this.playerService = playerService;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public Mono<Void> handle(JsonNode payload, WebSocketSession session, UUID lobbyId) {
        try {
            PlayerReconnectMessage message = objectMapper.treeToValue(payload, PlayerReconnectMessage.class);
            UUID playerId = message.getPlayerId();

            if (playerId == null) {
                log.warn("Received reconnect message without player ID");
                return Mono.empty();
            }

            log.info("Processing player reconnect: playerId={}, session={}", playerId, session.getId());
            return playerService.handlePlayerReconnection(playerId, session, lobbyId);
        } catch (Exception e) {
            log.error("Error processing PLAYER_RECONNECT message", e);
            return Mono.error(e);
        }
    }
}

