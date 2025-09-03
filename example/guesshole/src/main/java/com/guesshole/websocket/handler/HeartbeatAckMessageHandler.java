package com.guesshole.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.UUID; /**
 * Handler for HEARTBEAT_ACK messages
 */
@Component
public class HeartbeatAckMessageHandler implements MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(HeartbeatAckMessageHandler.class);
    private static final String MESSAGE_TYPE = "HEARTBEAT_ACK";

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public Mono<Void> handle(JsonNode payload, WebSocketSession session, UUID lobbyId) {
        log.debug("Received heartbeat acknowledgment from session: {}", session.getId());
        return Mono.empty();
    }
}
