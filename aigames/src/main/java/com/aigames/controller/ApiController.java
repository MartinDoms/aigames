package com.aigames.controller;

import com.aigames.model.Game;
import com.aigames.service.GameService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

@RestController
@RequestMapping("/api")
public class ApiController {

    private final GameService gameService;

    public ApiController(GameService gameService) {
        this.gameService = gameService;
    }

    @GetMapping("/games")
    public Flux<Game> getGames() {
        return gameService.getAllActiveGames();
    }

}