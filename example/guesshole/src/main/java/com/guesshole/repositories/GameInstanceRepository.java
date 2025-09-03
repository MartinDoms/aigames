package com.guesshole.repositories;

import com.guesshole.entities.GameInstance;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface GameInstanceRepository extends ReactiveCrudRepository<GameInstance, UUID> {
}