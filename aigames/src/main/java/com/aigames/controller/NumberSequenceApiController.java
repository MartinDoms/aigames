package com.aigames.controller;

import com.aigames.service.NumberSequenceService;
import com.aigames.service.NumberSequenceService.SequencePuzzle;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/number-sequence")
public class NumberSequenceApiController {

    private final NumberSequenceService numberSequenceService;

    public NumberSequenceApiController(NumberSequenceService numberSequenceService) {
        this.numberSequenceService = numberSequenceService;
    }

    @GetMapping("/new-puzzle")
    public Mono<SequencePuzzle> getNewPuzzle() {
        return Mono.just(numberSequenceService.generateRandomSequence());
    }

    @GetMapping("/puzzle/{type}")
    public Mono<SequencePuzzle> getPuzzleByType(@PathVariable String type) {
        try {
            NumberSequenceService.SequenceType sequenceType = 
                NumberSequenceService.SequenceType.valueOf(type.toUpperCase());
            return Mono.just(numberSequenceService.generateSequence(sequenceType));
        } catch (IllegalArgumentException e) {
            return Mono.just(numberSequenceService.generateRandomSequence());
        }
    }

    @PostMapping("/check-answer")
    public Mono<AnswerResponse> checkAnswer(@RequestBody AnswerRequest request) {
        boolean isCorrect = numberSequenceService.checkAnswer(request.puzzle(), request.answer());
        String message = isCorrect 
            ? "Correct! Well done!" 
            : "Incorrect. The answer was " + request.puzzle().nextValue();
        
        return Mono.just(new AnswerResponse(isCorrect, message, request.puzzle().nextValue()));
    }

    public record AnswerRequest(SequencePuzzle puzzle, int answer) {}
    public record AnswerResponse(boolean isCorrect, String message, int correctAnswer) {}
}