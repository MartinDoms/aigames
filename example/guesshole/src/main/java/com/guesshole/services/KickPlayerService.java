package com.guesshole.services;

import com.guesshole.entities.Player;
import com.guesshole.repositories.PlayerRepository;
import com.guesshole.websocket.messages.outgoing.PlayersUpdateMessage;
import com.guesshole.websocket.services.WebSocketSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.socket.CloseStatus;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Service for handling player kick operations
 */
@Service
public class KickPlayerService {
    private static final Logger log = LoggerFactory.getLogger(KickPlayerService.class);

    private final PlayerRepository playerRepository;
    private final WebSocketSessionService sessionService;
    private final PlayerSessionTracker sessionTracker;
    private final LobbyService lobbyService;

    public KickPlayerService(
            PlayerRepository playerRepository,
            WebSocketSessionService sessionService,
            PlayerSessionTracker sessionTracker,
            LobbyService lobbyService) {
        this.playerRepository = playerRepository;
        this.sessionService = sessionService;
        this.sessionTracker = sessionTracker;
        this.lobbyService = lobbyService;
    }

    /**
     * Kick a player from a lobby
     *
     * @param hostPlayerId The ID of the player attempting to kick (must be host)
     * @param playerIdToKick The ID of the player to kick
     * @param lobbyId The lobby ID
     * @return Mono<Void> when complete
     */
    public Mono<Void> kickPlayer(UUID hostPlayerId, UUID playerIdToKick, UUID lobbyId) {
        log.info("Attempting to kick player {} from lobby {}", playerIdToKick, lobbyId);

        // Verify the host is actually the host
        return playerRepository.findById(hostPlayerId)
                .flatMap(hostPlayer -> {
                    if (!hostPlayer.isHost()) {
                        log.warn("Player {} attempted to kick but is not the host", hostPlayerId);
                        return Mono.error(new IllegalStateException("Only the host can kick players"));
                    }

                    if (!hostPlayer.getLobbyId().equals(lobbyId)) {
                        log.warn("Host player {} is not in the specified lobby {}", hostPlayerId, lobbyId);
                        return Mono.error(new IllegalStateException("Host is not in the specified lobby"));
                    }

                    // Find the player to kick
                    return playerRepository.findById(playerIdToKick)
                            .flatMap(playerToKick -> {
                                if (!playerToKick.getLobbyId().equals(lobbyId)) {
                                    log.warn("Player {} is not in the specified lobby {}", playerIdToKick, lobbyId);
                                    return Mono.error(new IllegalStateException("Player is not in the specified lobby"));
                                }

                                // Close the WebSocket connection if the player is currently connected
                                WebSocketSession sessionToClose = sessionTracker.getPlayerSession(playerIdToKick);
                                if (sessionToClose != null) {
                                    log.info("Closing WebSocket connection for kicked player {}", playerIdToKick);
                                    // Use CloseStatus.GOING_AWAY with a specific reason message
                                    sessionToClose.close(new CloseStatus(1001, "Kicked by host")).subscribe();
                                }

                                // Set the kicked flag to true instead of changing lobbyId
                                log.info("Marking player {} as kicked from lobby {}", playerIdToKick, lobbyId);
                                playerToKick.setKicked(true);

                                return playerRepository.save(playerToKick)
                                        .then(sendPlayersUpdateMessage(lobbyId));
                            })
                            .switchIfEmpty(Mono.error(new IllegalStateException("Player to kick not found")));
                })
                .switchIfEmpty(Mono.error(new IllegalStateException("Host player not found")));
    }

    /**
     * Send updated players list to all clients in the lobby
     * This will only include non-kicked players due to the repository query
     */
    private Mono<Void> sendPlayersUpdateMessage(UUID lobbyId) {
        return playerRepository.findByLobbyId(lobbyId)
                .collectList()
                .flatMap(players -> {
                    log.info("Sending updated players list with {} players", players.size());
                    return lobbyService.broadcastToLobby(lobbyId, new PlayersUpdateMessage(players));
                });
    }
}