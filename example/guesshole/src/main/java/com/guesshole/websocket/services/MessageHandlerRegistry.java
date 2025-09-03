package com.guesshole.websocket.services;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guesshole.websocket.handler.MessageHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Registry of message handlers.
 * Follows Open/Closed Principle by allowing new message types to be added without modifying existing code.
 */
@Service
public class MessageHandlerRegistry {
    private static final Logger log = LoggerFactory.getLogger(MessageHandlerRegistry.class);

    private final ObjectMapper objectMapper;
    private final Map<String, MessageHandler> handlers = new HashMap<>();

    public MessageHandlerRegistry(ObjectMapper objectMapper, List<MessageHandler> messageHandlers) {
        this.objectMapper = objectMapper;

        // Register all message handlers
        for (MessageHandler handler : messageHandlers) {
            handlers.put(handler.getMessageType(), handler);
            log.info("Registered message handler for type: {}", handler.getMessageType());
        }
    }

    /**
     * Handle an incoming WebSocket message
     */
    public Mono<Void> handleMessage(String payload, WebSocketSession session, UUID lobbyId) {
        try {
            // Parse the message to get its type
            JsonNode rootNode = objectMapper.readTree(payload);
            String messageType = rootNode.path("type").asText("");

            // Find the appropriate handler
            MessageHandler handler = handlers.get(messageType);
            if (handler != null) {
                return handler.handle(rootNode, session, lobbyId)
                        .onErrorResume(e -> {
                            log.error("Error handling message of type {}", messageType, e);
                            return Mono.empty();
                        });
            } else {
                log.warn("Received unknown message type: {}", messageType);
                return Mono.empty();
            }
        } catch (Exception e) {
            log.error("Error parsing message payload", e);
            return Mono.empty();
        }
    }
}