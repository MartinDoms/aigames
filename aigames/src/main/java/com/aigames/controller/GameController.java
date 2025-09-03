package com.aigames.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import reactor.core.publisher.Mono;

@Controller
@RequestMapping("/games")
public class GameController {

    @GetMapping("/{gameName}")
    public Mono<String> playGame(@PathVariable String gameName) {
        return switch (gameName) {
            case "word-ladder" -> Mono.just("games/word-ladder");
            case "number-sequence" -> Mono.just("games/number-sequence");
            case "geography-quiz" -> Mono.just("games/geography-quiz");
            default -> Mono.just("redirect:/");
        };
    }
}