package com.guesshole.repositories;

import com.guesshole.entities.ScoreMultiplier;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

import java.util.UUID;

@Repository
public interface ScoreMultiplierRepository extends ReactiveCrudRepository<ScoreMultiplier, UUID> {
    Flux<ScoreMultiplier> findByGuessId(UUID guessId);
}
