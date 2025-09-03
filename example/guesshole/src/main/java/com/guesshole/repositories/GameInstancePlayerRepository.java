package com.guesshole.repositories;

import com.guesshole.entities.GameInstancePlayer;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface GameInstancePlayerRepository extends ReactiveCrudRepository<GameInstancePlayer, UUID> {
    Flux<GameInstancePlayer> findByGameInstanceId(UUID gameInstanceId);
    Flux<GameInstancePlayer> findByPlayerId(UUID playerId);
    Mono<Void> deleteByGameInstanceIdAndPlayerId(UUID gameInstanceId, UUID playerId);
}