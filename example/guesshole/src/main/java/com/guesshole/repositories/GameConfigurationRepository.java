package com.guesshole.repositories;

import com.guesshole.entities.GameConfiguration;
import com.guesshole.entities.Lobby;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.r2dbc.repository.R2dbcRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Repository
public interface GameConfigurationRepository extends R2dbcRepository<GameConfiguration, UUID> {

}
