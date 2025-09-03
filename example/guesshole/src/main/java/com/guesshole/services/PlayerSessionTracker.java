package com.guesshole.services;

import com.guesshole.entities.Player;
import com.guesshole.repositories.PlayerRepository;
import com.guesshole.websocket.messages.outgoing.PlayerStatusChangeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Tracks player connection states and manages disconnect/reconnect scenarios.
 * This services allows players to temporarily disconnect and reconnect without
 * losing their identity within a lobby.
 */
@Service
public class PlayerSessionTracker {
    private static final Logger log = LoggerFactory.getLogger(PlayerSessionTracker.class);

    /**
     * Represents the connection state of a player
     */
    public enum ConnectionState {
        CONNECTED,      // Player has an active websocket connection
        DISCONNECTED,   // Player's websocket disconnected but within reconnection window
        INACTIVE        // Player disconnected and exceeded reconnection window, but still in lobby
    }

    /**
     * Tracks connection information for a player
     */
    private static class PlayerConnectionInfo {
        private ConnectionState state;
        private Instant lastStateChange;
        private UUID playerId;
        private UUID lobbyId;
        private WebSocketSession session;

        public PlayerConnectionInfo(UUID playerId, UUID lobbyId, WebSocketSession session) {
            this.playerId = playerId;
            this.lobbyId = lobbyId;
            this.session = session;
            this.state = ConnectionState.CONNECTED;
            this.lastStateChange = Instant.now();
        }

        public void updateState(ConnectionState newState) {
            this.state = newState;
            this.lastStateChange = Instant.now();
        }

        public Duration getTimeSinceStateChange() {
            return Duration.between(lastStateChange, Instant.now());
        }
    }

    private final Map<UUID, PlayerConnectionInfo> playerConnections = new ConcurrentHashMap<>();
    private final PlayerRepository playerRepository;
    private final LobbyService lobbyService;

    @Value("${app.player.disconnected-timeout-seconds:60}")
    private long disconnectedTimeoutSeconds;

    @Value("${app.player.inactive-timeout-minutes:10}")
    private long inactiveTimeoutMinutes;

    public PlayerSessionTracker(PlayerRepository playerRepository, LobbyService lobbyService) {
        this.playerRepository = playerRepository;
        this.lobbyService = lobbyService;
        log.info("PlayerSessionTracker initialized with default timeouts: disconnected={}s, inactive={}m",
                disconnectedTimeoutSeconds, inactiveTimeoutMinutes);
    }

    /**
     * Register a player's active connection
     */
    public void registerConnection(UUID playerId, UUID lobbyId, WebSocketSession session) {
        PlayerConnectionInfo existingInfo = playerConnections.get(playerId);

        if (existingInfo != null) {
            log.info("Player {} reconnected to lobby {}", playerId, lobbyId);
            existingInfo.session = session;
            existingInfo.updateState(ConnectionState.CONNECTED);
        } else {
            log.info("Player {} connected to lobby {}", playerId, lobbyId);
            playerConnections.put(playerId, new PlayerConnectionInfo(playerId, lobbyId, session));
        }

        // Update player's active status in database
        updatePlayerActiveStatus(playerId, true);
    }

    /**
     * Handle a player disconnection
     */
    public void handleDisconnection(UUID playerId) {
        PlayerConnectionInfo info = playerConnections.get(playerId);
        if (info != null) {
            log.info("Player {} disconnected, starting reconnection window", playerId);
            info.updateState(ConnectionState.DISCONNECTED);
        }
    }

    /**
     * Check if a player is currently connected
     */
    public boolean isPlayerConnected(UUID playerId) {
        PlayerConnectionInfo info = playerConnections.get(playerId);
        return info != null && info.state == ConnectionState.CONNECTED;
    }

    /**
     * Get the session for a connected player
     */
    public WebSocketSession getPlayerSession(UUID playerId) {
        PlayerConnectionInfo info = playerConnections.get(playerId);
        if (info != null && info.state == ConnectionState.CONNECTED) {
            return info.session;
        }
        return null;
    }

    /**
     * Get the connection state for a player
     */
    public ConnectionState getPlayerConnectionState(UUID playerId) {
        PlayerConnectionInfo info = playerConnections.get(playerId);
        return info != null ? info.state : null;
    }

    /**
     * Scheduled task to manage player state transitions
     * - DISCONNECTED -> INACTIVE after disconnectedTimeoutSeconds
     * - Update player active status in database based on state
     */
    @Scheduled(fixedRate = 30000) // Run every 30 seconds
    public void processDisconnectedPlayers() {
        log.debug("Running scheduled task: processDisconnectedPlayers");
        Instant now = Instant.now();
        Duration disconnectTimeout = Duration.ofSeconds(disconnectedTimeoutSeconds);
        int processedCount = 0;

        for (Map.Entry<UUID, PlayerConnectionInfo> entry : playerConnections.entrySet()) {
            UUID playerId = entry.getKey();
            PlayerConnectionInfo info = entry.getValue();

            if (info.state == ConnectionState.DISCONNECTED &&
                    info.getTimeSinceStateChange().compareTo(disconnectTimeout) > 0) {
                log.info("Player {} exceeded reconnection window of {}s, marking as inactive",
                        playerId, disconnectedTimeoutSeconds);
                info.updateState(ConnectionState.INACTIVE);
                processedCount++;

                // Update player status in database and notify lobby
                updatePlayerActiveStatus(playerId, false)
                        .subscribe(
                                null,
                                error -> log.error("Error updating player status: {}", error.getMessage(), error)
                        );
            }
        }

        log.debug("Processed {} disconnected players", processedCount);
    }

    /**
     * Scheduled task to check for inactive players that should be reported
     * but not removed from the player tracker yet
     */
    @Scheduled(fixedRate = 300000) // Run every 5 minutes
    public void processInactivePlayers() {
        log.debug("Running scheduled task: processInactivePlayers");
        Duration inactiveTimeout = Duration.ofMinutes(inactiveTimeoutMinutes);
        int longInactiveCount = 0;

        for (Map.Entry<UUID, PlayerConnectionInfo> entry : playerConnections.entrySet()) {
            UUID playerId = entry.getKey();
            PlayerConnectionInfo info = entry.getValue();

            if (info.state == ConnectionState.INACTIVE &&
                    info.getTimeSinceStateChange().compareTo(inactiveTimeout) > 0) {
                longInactiveCount++;
                log.info("Player {} has been inactive for more than {} minutes",
                        playerId, inactiveTimeoutMinutes);
                // At this point, we could broadcast a message about long-term inactive players
                // Or implement any specific logic for very long-term inactive players
            }
        }

        log.debug("Found {} long-term inactive players", longInactiveCount);
    }

    /**
     * Get statistics about current player connections for monitoring
     */
    public String getConnectionStatistics() {
        int connectedCount = 0;
        int disconnectedCount = 0;
        int inactiveCount = 0;

        for (PlayerConnectionInfo info : playerConnections.values()) {
            switch (info.state) {
                case CONNECTED: connectedCount++; break;
                case DISCONNECTED: disconnectedCount++; break;
                case INACTIVE: inactiveCount++; break;
            }
        }

        return String.format("PlayerSessionTracker stats: connected=%d, disconnected=%d, inactive=%d, total=%d",
                connectedCount, disconnectedCount, inactiveCount, playerConnections.size());
    }

    /**
     * Scheduled task to log connection statistics for monitoring
     */
    @Scheduled(fixedRate = 60000) // Log stats every minute
    public void logConnectionStatistics() {
        log.info(getConnectionStatistics());
    }

    /**
     * Update a player's active status in the database and broadcast the change
     */
    private Mono<Void> updatePlayerActiveStatus(UUID playerId, boolean active) {
        return playerRepository.findById(playerId)
                .flatMap(player -> {
                    if (player.isActive() != active) {
                        log.debug("Updating player {} active status to: {}", playerId, active);
                        player.setActive(active);
                        return playerRepository.save(player)
                                .flatMap(savedPlayer -> {
                                    // Notify lobby of player status change
                                    return notifyPlayerStatusChange(savedPlayer);
                                });
                    }
                    return Mono.empty();
                })
                .then();
    }

    /**
     * Notify the lobby about a player's status change
     */
    private Mono<Void> notifyPlayerStatusChange(Player player) {
        UUID lobbyId = player.getLobbyId();
        log.info("Broadcasting player {} status change in lobby {}. Active: {}",
                player.getId(), lobbyId, player.isActive());

        // You'd implement a message class for player status changes
        // and broadcast it via the LobbyService
        return lobbyService.broadcastToLobby(lobbyId,
                new PlayerStatusChangeMessage(player.getId(), player.isActive()));
    }
}