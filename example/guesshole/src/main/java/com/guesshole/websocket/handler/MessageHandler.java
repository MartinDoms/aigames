package com.guesshole.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Interface for WebSocket message handlers.
 * Each message type should have its own handler implementation.
 */
public interface MessageHandler {

    /**
     * Get the message type this handler supports
     */
    String getMessageType();

    /**
     * Handle a WebSocket message
     *
     * @param message The parsed JSON message
     * @param session The WebSocket session
     * @param lobbyId The lobby ID
     * @return A Mono that completes when the message has been handled
     */
    Mono<Void> handle(JsonNode message, WebSocketSession session, UUID lobbyId);
}