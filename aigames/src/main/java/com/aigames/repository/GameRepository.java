package com.aigames.repository;

import com.aigames.model.Game;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;

@Repository
public interface GameRepository extends ReactiveCrudRepository<Game, Long> {
    
    Flux<Game> findByIsActiveTrue();
    
}