package com.guesshole.websocket.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages WebSocket sessions for game lobbies.
 * Follows Single Responsibility Principle by focusing only on session management.
 */
@Service
public class WebSocketSessionService {
    private static final Logger log = LoggerFactory.getLogger(WebSocketSessionService.class);

    // Store active sessions by lobby ID
    private final Map<UUID, Map<String, WebSocketSession>> lobbySessions = new ConcurrentHashMap<>();
    // Map sessions to player IDs
    private final Map<String, UUID> sessionPlayerIds = new ConcurrentHashMap<>();

    /**
     * Register a new WebSocket session for a lobby
     */
    public void registerSession(UUID lobbyId, WebSocketSession session) {
        lobbySessions.computeIfAbsent(lobbyId, k -> new ConcurrentHashMap<>())
                .put(session.getId(), session);
        log.debug("Added session {} to lobby {}", session.getId(), lobbyId);
    }

    /**
     * Unregister a WebSocket session
     */
    public void unregisterSession(UUID lobbyId, WebSocketSession session) {
        String sessionId = session.getId();

        // Remove session from lobby sessions
        Map<String, WebSocketSession> sessions = lobbySessions.get(lobbyId);
        if (sessions != null) {
            sessions.remove(sessionId);
            if (sessions.isEmpty()) {
                lobbySessions.remove(lobbyId);
                log.debug("Removed empty lobby {}", lobbyId);
            }
        }

        // Remove player ID mapping
        sessionPlayerIds.remove(sessionId);
        log.debug("Removed session {} from lobby {}", sessionId, lobbyId);
    }

    /**
     * Get all active sessions for a lobby
     */
    public List<WebSocketSession> getLobbySessions(UUID lobbyId) {
        Map<String, WebSocketSession> sessions = lobbySessions.get(lobbyId);
        if (sessions == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(sessions.values());
    }

    /**
     * Get the player ID associated with a session
     */
    public UUID getPlayerIdForSession(WebSocketSession session) {
        return sessionPlayerIds.get(session.getId());
    }

    /**
     * Associate a player ID with a session
     */
    public void setPlayerIdForSession(WebSocketSession session, UUID playerId) {
        sessionPlayerIds.put(session.getId(), playerId);
        log.debug("Mapped session {} to player {}", session.getId(), playerId);
    }
}