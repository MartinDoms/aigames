package com.guesshole.websocket;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.guesshole.repositories.PlayerRepository;
import com.guesshole.services.GameStateService;
import com.guesshole.websocket.services.WebSocketSessionService;
import com.guesshole.websocket.messages.outgoing.HeartbeatMessage;
import com.guesshole.websocket.messages.outgoing.PlayersUpdateMessage;
import com.guesshole.services.LobbyService;
import com.guesshole.websocket.services.MessageHandlerRegistry;
import com.guesshole.services.PlayerSessionTracker;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketMessage;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.UUID;

/**
 * Handler for WebSocket connections to game lobbies.
 * Now includes session tracking for disconnection/reconnection management.
 */
@Component
public class LobbyWebSocketHandler implements WebSocketHandler {
    private static final Logger log = LoggerFactory.getLogger(LobbyWebSocketHandler.class);
    private static final Duration HEARTBEAT_INTERVAL = Duration.ofSeconds(30);

    private final ObjectMapper objectMapper;
    private final PlayerRepository playerRepository;
    private final WebSocketSessionService sessionHandler;
    private final MessageHandlerRegistry messageHandlerRegistry;
    private final LobbyService lobbyService;
    private final PlayerSessionTracker playerSessionTracker;
    private final GameStateService gameStateService;

    public LobbyWebSocketHandler(
            ObjectMapper objectMapper,
            PlayerRepository playerRepository,
            WebSocketSessionService sessionHandler,
            MessageHandlerRegistry messageHandlerRegistry,
            LobbyService lobbyService,
            PlayerSessionTracker playerSessionTracker,
            GameStateService gameStateService
    ) {
        this.objectMapper = objectMapper;
        this.playerRepository = playerRepository;
        this.sessionHandler = sessionHandler;
        this.messageHandlerRegistry = messageHandlerRegistry;
        this.lobbyService = lobbyService;
        this.playerSessionTracker = playerSessionTracker;
        this.gameStateService = gameStateService;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        UUID lobbyId = extractLobbyId(session);
        log.info("New WebSocket connection: session={}, lobbyId={}", session.getId(), lobbyId);

        // Register session with session handler
        sessionHandler.registerSession(lobbyId, session);

        // Sequence of operations
        return Mono.when(
                // 1. Send initial players list and game state
                sendInitialData(session, lobbyId),
                // 2. Send heartbeats and process incoming messages in parallel
                Mono.zip(
                        session.send(createHeartbeatFlux(session)),
                        processIncomingMessages(session, lobbyId)
                )
        ).doFinally(signal -> {
            UUID playerId = sessionHandler.getPlayerIdForSession(session);
            log.info("WebSocket session completed: {}, playerId={}", session.getId(), playerId);

            // Unregister session
            sessionHandler.unregisterSession(lobbyId, session);

            // Handle player disconnection if we have a player ID
            if (playerId != null) {
                playerSessionTracker.handleDisconnection(playerId);
            }
        });
    }

    /**
     * Extract the lobby ID from the WebSocket session path
     */
    private UUID extractLobbyId(WebSocketSession session) {
        String path = session.getHandshakeInfo().getUri().getPath();
        String lobbyIdStr = path.substring(path.lastIndexOf('/') + 1);
        return UUID.fromString(lobbyIdStr);
    }

    /**
     * Send the initial data to the new client:
     * - List of players in the lobby
     * - Current game state
     */
    private Mono<Void> sendInitialData(WebSocketSession session, UUID lobbyId) {
        // Send players list
        Mono<Void> sendPlayers = playerRepository.findByLobbyId(lobbyId)
                .collectList()
                .flatMap(players -> {
                    PlayersUpdateMessage message = new PlayersUpdateMessage(players);
                    return lobbyService.sendToSession(session, message);
                })
                .onErrorResume(e -> {
                    log.error("Error sending initial players list", e);
                    return Mono.empty();
                });

        // Send game state
        Mono<Void> sendGameState = gameStateService.sendGameStateToClient(lobbyId, session)
                .onErrorResume(e -> {
                    log.error("Error sending initial game state", e);
                    return Mono.empty();
                });

        // Execute both operations and return when both complete
        return Mono.when(sendPlayers, sendGameState);
    }

    /**
     * Create a flux that sends periodic heartbeat messages
     */
    private Flux<WebSocketMessage> createHeartbeatFlux(WebSocketSession session) {
        return Flux.interval(HEARTBEAT_INTERVAL)
                .map(tick -> {
                    try {
                        String json = objectMapper.writeValueAsString(new HeartbeatMessage());
                        return session.textMessage(json);
                    } catch (Exception e) {
                        log.error("Error creating heartbeat", e);
                        return session.textMessage("{\"type\":\"HEARTBEAT\"}");
                    }
                });
    }

    /**
     * Process incoming messages from the client
     */
    private Mono<Void> processIncomingMessages(WebSocketSession session, UUID lobbyId) {
        return session.receive()
                .flatMap(message -> handleIncomingMessage(message, session, lobbyId))
                .onErrorResume(e -> {
                    log.error("WebSocket session error: {}", e.getMessage(), e);
                    return Mono.empty();
                })
                .then();
    }

    /**
     * Handle a single incoming message
     */
    private Mono<Void> handleIncomingMessage(WebSocketMessage message, WebSocketSession session, UUID lobbyId) {
        try {
            String payload = message.getPayloadAsText();
            log.debug("Processing message: {}", payload);

            return messageHandlerRegistry.handleMessage(payload, session, lobbyId);
        } catch (Exception e) {
            log.error("Error processing WebSocket message", e);
            return Mono.empty();
        }
    }
}