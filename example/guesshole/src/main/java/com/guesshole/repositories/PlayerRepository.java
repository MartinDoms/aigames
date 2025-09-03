package com.guesshole.repositories;

import com.guesshole.entities.Player;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface PlayerRepository extends ReactiveCrudRepository<Player, UUID> {

    /**
     * Find all non-kicked players in a lobby
     */
    @Query("SELECT * FROM players WHERE lobby_id = :lobbyId AND kicked = false")
    Flux<Player> findByLobbyId(UUID lobbyId);

    /**
     * Find all players in a lobby regardless of kicked status
     * (Useful for administrative purposes)
     */
    @Query("SELECT * FROM players WHERE lobby_id = :lobbyId")
    Flux<Player> findAllByLobbyId(UUID lobbyId);
}