package com.aigames.controller;

import com.aigames.service.WordLadderService;
import com.aigames.service.WordLadderService.WordPair;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Set;

@RestController
@RequestMapping("/api/word-ladder")
public class WordLadderApiController {

    private final WordLadderService wordLadderService;

    public WordLadderApiController(WordLadderService wordLadderService) {
        this.wordLadderService = wordLadderService;
    }

    @PostMapping("/validate-move")
    public Mono<ValidateMoveResponse> validateMove(@RequestBody ValidateMoveRequest request) {
        boolean isValid = wordLadderService.isValidMove(request.fromWord(), request.toWord());
        String message = isValid ? "Valid move!" : "Invalid word or not one letter different";
        return Mono.just(new ValidateMoveResponse(isValid, message));
    }

    @GetMapping("/possible-words/{currentWord}")
    public Mono<Set<String>> getPossibleWords(@PathVariable String currentWord) {
        Set<String> possibleWords = wordLadderService.getPossibleNextWords(currentWord);
        return Mono.just(possibleWords);
    }

    @PostMapping("/check-solution")
    public Mono<SolutionResponse> checkSolution(@RequestBody SolutionRequest request) {
        List<String> shortestPath = wordLadderService.findShortestPath(
            request.startWord(), request.endWord()
        );
        
        boolean hasPath = !shortestPath.isEmpty();
        int optimalSteps = hasPath ? shortestPath.size() - 1 : -1;
        
        return Mono.just(new SolutionResponse(
            hasPath,
            optimalSteps,
            hasPath ? shortestPath : null
        ));
    }

    @GetMapping("/random-pair")
    public Mono<WordPair> getRandomWordPair() {
        return Mono.just(wordLadderService.generateRandomWordPair());
    }

    public record ValidateMoveRequest(String fromWord, String toWord) {}
    public record ValidateMoveResponse(boolean isValid, String message) {}
    
    public record SolutionRequest(String startWord, String endWord) {}
    public record SolutionResponse(boolean hasSolution, int optimalSteps, List<String> shortestPath) {}
}