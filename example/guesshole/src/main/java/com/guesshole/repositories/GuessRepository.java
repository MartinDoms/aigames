package com.guesshole.repositories;

import com.guesshole.entities.Guess;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface GuessRepository extends ReactiveCrudRepository<Guess, UUID> {

    Mono<Long>  countByRoundId(UUID roundId);

    Flux<Guess> findByPlayerId(UUID playerId);

    Flux<Guess> findByRoundId(UUID roundId);

    Flux<Guess> findByGameInstanceId(UUID gameInstanceId);

    Mono<Guess> findByPlayerIdAndRoundId(UUID playerId, UUID roundId);

    Flux<Guess> findByGameInstanceIdAndPlayerId(UUID gameInstanceId, UUID playerId);
}