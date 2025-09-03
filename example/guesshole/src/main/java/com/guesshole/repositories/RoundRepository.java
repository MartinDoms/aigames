package com.guesshole.repositories;

import com.guesshole.entities.Round;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface RoundRepository extends ReactiveCrudRepository<Round, UUID> {

    Flux<Round> findByGameInstanceId(UUID gameInstanceId);

    Flux<Round> findByGameInstanceIdOrderByRoundOrder(UUID gameInstanceId);

    Mono<Round> findByGameInstanceIdAndRoundOrder(UUID gameInstanceId, int roundOrder);
}