package com.guesshole.services;

import com.guesshole.entities.Player;
import com.guesshole.repositories.PlayerRepository;
import com.guesshole.websocket.messages.incoming.UpdatePlayerMessage;
import com.guesshole.websocket.messages.outgoing.*;
import com.guesshole.websocket.services.WebSocketSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import java.util.*;

/**
 * Updated PlayerService that integrates with PlayerSessionTracker
 */
@Service
public class PlayerService {
    private static final Logger log = LoggerFactory.getLogger(PlayerService.class);

    private final PlayerRepository playerRepository;
    private final WebSocketSessionService sessionHandler;
    private final LobbyService lobbyService;
    private final PlayerSessionTracker sessionTracker;

    public PlayerService(
            PlayerRepository playerRepository,
            WebSocketSessionService sessionHandler,
            LobbyService lobbyService,
            PlayerSessionTracker sessionTracker
    ) {
        this.playerRepository = playerRepository;
        this.sessionHandler = sessionHandler;
        this.lobbyService = lobbyService;
        this.sessionTracker = sessionTracker;
    }

    /**
     * Create a new player
     */
    public Mono<Player> createPlayer(String name, UUID lobbyId, String avatar) {
        Player newPlayer = new Player(name, lobbyId, false, avatar);
        return playerRepository.save(newPlayer);
    }

    /**
     * Handle player creation
     */
    public Mono<Void> handleNewPlayer(String name, String avatar, WebSocketSession session, UUID lobbyId) {
        log.info("Creating new player in lobby {} with name: {} and avatar: {}",
                lobbyId, name, avatar);

        return createPlayer(name, lobbyId, avatar)
                .flatMap(player -> {
                    // Store session to player mapping
                    UUID playerId = player.getId();
                    sessionHandler.setPlayerIdForSession(session, playerId);

                    // Register player connection in session tracker
                    sessionTracker.registerConnection(playerId, lobbyId, session);

                    // Send player ID to client
                    return lobbyService.sendToSession(session, new PlayerIdAssignedMessage(playerId))
                            .thenReturn(player);
                })
                .flatMap(player -> {
                    // Broadcast player joined to all clients in this lobby
                    return lobbyService.broadcastToLobby(lobbyId, new PlayerJoinedMessage(player))
                            .thenReturn(player);
                })
                .flatMap(player -> {
                    // Also send updated player list to all clients
                    return sendUpdatedPlayersList(lobbyId);
                })
                .onErrorResume(error -> {
                    log.error("Error creating player: {}", error.getMessage(), error);
                    return Mono.empty();
                });
    }

    /**
     * Handle reconnection of an existing player
     */
    public Mono<Void> handlePlayerReconnection(UUID playerId, WebSocketSession session, UUID lobbyId) {
        return playerRepository.findById(playerId)
                .flatMap(player -> {
                    log.info("Found player {} attempting to reconnect", playerId);

                    // Check if this player was kicked
                    boolean wasKicked = player.isKicked();

                    // Update session tracking
                    sessionHandler.setPlayerIdForSession(session, playerId);
                    sessionTracker.registerConnection(playerId, lobbyId, session);

                    // If player was kicked, clear the kicked flag
                    if (wasKicked) {
                        log.info("Player {} was previously kicked. Allowing rejoin to lobby {}",
                                playerId, lobbyId);
                        player.setKicked(false);
                    }

                    // Check if player is attempting to reconnect to a different lobby
                    if (!player.getLobbyId().equals(lobbyId)) {
                        log.info("Player {} is joining a different lobby {} (was in {})",
                                playerId, lobbyId, player.getLobbyId());
                        player.setLobbyId(lobbyId);
                    }

                    // Always set player as active when reconnecting
                    player.setActive(true);

                    return playerRepository.save(player)
                            .flatMap(savedPlayer -> {
                                // If player was kicked, they're effectively "rejoining" the lobby
                                if (wasKicked) {
                                    log.info("Player {} is rejoining lobby {} after being kicked", playerId, lobbyId);
                                    return lobbyService.broadcastToLobby(lobbyId,
                                            new PlayerJoinedMessage(savedPlayer));
                                } else {
                                    // Regular reconnection - just notify of status change
                                    log.info("Player {} reconnected to lobby {}", playerId, lobbyId);
                                    return lobbyService.broadcastToLobby(lobbyId,
                                            new PlayerStatusChangeMessage(playerId, true));
                                }
                            })
                            .then(sendUpdatedPlayersList(lobbyId)); // Always send updated player list
                })
                .onErrorResume(error -> {
                    log.error("Error handling player reconnection: {}", error.getMessage(), error);
                    return Mono.empty();
                });
    }

    /**
     * Update an existing player by ID
     * Returns the player and a list of changed fields
     */
    public Mono<Tuple2<Player, List<String>>> updateExistingPlayer(
            UUID playerId,
            String name,
            String avatar,
            UUID lobbyId
    ) {
        return playerRepository.findById(playerId)
                .flatMap(player -> {
                    // Track changed fields
                    List<String> changedFields = new ArrayList<>();

                    // Verify player belongs to this lobby
                    if (!player.getLobbyId().equals(lobbyId)) {
                        log.warn("Player {} doesn't belong to lobby {}", playerId, lobbyId);
                        return Mono.empty();
                    }

                    // Update player info
                    if (!player.getName().equals(name)) {
                        player.setName(name);
                        changedFields.add("name");
                    }

                    if (avatar != null && !avatar.equals(player.getAvatar())) {
                        player.setAvatar(avatar);
                        changedFields.add("avatar");
                    }

                    if (changedFields.isEmpty()) {
                        // No changes, return as is
                        return Mono.just(Tuples.of(player, changedFields));
                    }

                    return playerRepository.save(player)
                            .map(savedPlayer -> Tuples.of(savedPlayer, changedFields));
                })
                .switchIfEmpty(
                        // Player ID doesn't exist, create new player
                        createPlayer(name, lobbyId, avatar)
                                .map(player -> Tuples.of(player, Arrays.asList("name", "avatar")))
                );
    }

    /**
     * Handle player update message
     */
    public Mono<Void> handlePlayerUpdate(UpdatePlayerMessage message, WebSocketSession session, UUID lobbyId) {
        UUID playerId = message.getPlayerId();
        String playerName = message.getName();
        String playerAvatar = message.getAvatar();

        log.info("Processing player update request: playerId={}, name={}, lobby={}",
                playerId, playerName, lobbyId);

        if (playerId == null || playerId.toString().isEmpty()) {
            // No player ID provided - create new player
            return handleNewPlayer(playerName, playerAvatar, session, lobbyId);
        } else {
            // Check if this is a reconnection
            if (sessionTracker.getPlayerConnectionState(playerId) != null &&
                    !sessionTracker.isPlayerConnected(playerId)) {
                // This is a reconnection - handle it specially
                return handlePlayerReconnection(playerId, session, lobbyId)
                        .then(updateExistingPlayer(playerId, playerName, playerAvatar, lobbyId)
                                .flatMap(tuple -> {
                                    Player player = tuple.getT1();
                                    List<String> changedFields = tuple.getT2();

                                    if (!changedFields.isEmpty()) {
                                        // Only broadcast update if fields actually changed
                                        log.info("Reconnected player {} updated fields: {}",
                                                player.getId(), changedFields);
                                        return lobbyService.broadcastToLobby(lobbyId,
                                                new PlayerUpdatedMessage(player, changedFields));
                                    }
                                    return Mono.empty();
                                }));
            } else {
                // Normal update for existing player
                return updateExistingPlayer(playerId, playerName, playerAvatar, lobbyId)
                        .flatMap(tuple -> {
                            Player player = tuple.getT1();
                            List<String> changedFields = tuple.getT2();

                            // Store session to player mapping
                            sessionHandler.setPlayerIdForSession(session, player.getId());

                            // Register with session tracker if not already tracked
                            if (sessionTracker.getPlayerConnectionState(playerId) == null) {
                                sessionTracker.registerConnection(playerId, lobbyId, session);
                            }

                            if (!changedFields.isEmpty()) {
                                // Only broadcast update if fields actually changed
                                log.info("Player {} updated fields: {}", player.getId(), changedFields);
                                return lobbyService.broadcastToLobby(lobbyId,
                                                new PlayerUpdatedMessage(player, changedFields))
                                        .thenReturn(player);
                            }
                            return Mono.just(player);
                        })
                        .flatMap(player -> {
                            // Send updated player list to all clients
                            return sendUpdatedPlayersList(lobbyId);
                        })
                        .onErrorResume(e -> {
                            log.error("Error handling player update: {}", e.getMessage(), e);
                            return Mono.empty();
                        });
            }
        }
    }

    /**
     * Send updated players list to all clients in a lobby
     */
    private Mono<Void> sendUpdatedPlayersList(UUID lobbyId) {
        return playerRepository.findByLobbyId(lobbyId)
                .collectList()
                .flatMap(players -> lobbyService.broadcastToLobby(lobbyId, new PlayersUpdateMessage(players)));
    }
}