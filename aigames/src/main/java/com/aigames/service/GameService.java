package com.aigames.service;

import com.aigames.model.Game;
import com.aigames.repository.GameRepository;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

@Service
public class GameService {

    private final GameRepository gameRepository;

    public GameService(GameRepository gameRepository) {
        this.gameRepository = gameRepository;
    }

    public Flux<Game> getAllActiveGames() {
        return gameRepository.findByIsActiveTrue();
    }

}