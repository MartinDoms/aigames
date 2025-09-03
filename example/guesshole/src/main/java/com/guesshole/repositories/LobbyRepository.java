package com.guesshole.repositories;

import com.guesshole.entities.Lobby;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface LobbyRepository extends R2dbcRepository<Lobby, UUID> {

    /**
     * Find a lobby by its short code
     * @param shortCode The short code to search for
     * @return A Mono containing the lobby if found
     */
    Mono<Lobby> findByShortCode(String shortCode);

    /**
     * Generate a new short code directly from the database
     * @return A Mono containing the generated short code
     */
    @Query("SELECT generate_lobby_short_code()")
    Mono<String> generateShortCode();

    /**
     * Insert a new lobby with auto-generated short code
     * @param lobby The lobby to save (without short code)
     * @return A Mono containing the saved lobby with generated short code
     */
    @Query("INSERT INTO lobbies (id, name, privacy, created_at) " +
            "VALUES (:#{#lobby.id}, :#{#lobby.name}, :#{#lobby.privacy}, NOW()) " +
            "RETURNING *")
    Mono<Lobby> insertWithGeneratedShortCode(Lobby lobby);

    /**
     * Check if a short code already exists
     * @param shortCode The short code to check
     * @return A Mono containing true if the short code exists, false otherwise
     */
    @Query("SELECT EXISTS(SELECT 1 FROM lobbies WHERE short_code = :shortCode)")
    Mono<Boolean> existsByShortCode(String shortCode);
}