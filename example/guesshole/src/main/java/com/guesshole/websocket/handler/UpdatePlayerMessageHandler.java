package com.guesshole.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guesshole.websocket.messages.incoming.UpdatePlayerMessage;
import com.guesshole.services.PlayerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.UUID; /**
 * Handler for UPDATE_PLAYER messages
 */
@Component
public class UpdatePlayerMessageHandler implements MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(UpdatePlayerMessageHandler.class);
    private static final String MESSAGE_TYPE = "UPDATE_PLAYER";

    private final ObjectMapper objectMapper;
    private final PlayerService playerService;

    public UpdatePlayerMessageHandler(ObjectMapper objectMapper, PlayerService playerService) {
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
            UpdatePlayerMessage message = objectMapper.treeToValue(payload, UpdatePlayerMessage.class);
            log.info("Processing player update: playerId={}, name={}, avatar={}, session={}",
                    message.getPlayerId(), message.getName(), message.getAvatar(), session.getId());

            return playerService.handlePlayerUpdate(message, session, lobbyId);
        } catch (Exception e) {
            log.error("Error processing UPDATE_PLAYER message", e);
            return Mono.error(e);
        }
    }
}
