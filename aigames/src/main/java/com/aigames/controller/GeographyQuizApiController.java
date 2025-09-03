package com.aigames.controller;

import com.aigames.service.GeographyQuizService;
import com.aigames.service.GeographyQuizService.QuizQuestion;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/geography-quiz")
public class GeographyQuizApiController {

    private final GeographyQuizService geographyQuizService;

    public GeographyQuizApiController(GeographyQuizService geographyQuizService) {
        this.geographyQuizService = geographyQuizService;
    }

    @GetMapping("/new-question")
    public Mono<QuizQuestion> getNewQuestion() {
        return Mono.just(geographyQuizService.generateRandomQuestion());
    }

    @GetMapping("/question/{type}")
    public Mono<QuizQuestion> getQuestionByType(@PathVariable String type) {
        try {
            GeographyQuizService.QuestionType questionType = 
                GeographyQuizService.QuestionType.valueOf(type.toUpperCase());
            
            return Mono.just(switch (questionType) {
                case CAPITAL -> geographyQuizService.generateCapitalQuestion();
                case COUNTRY -> geographyQuizService.generateCountryQuestion();
                case FACT -> geographyQuizService.generateFactQuestion();
            });
        } catch (IllegalArgumentException e) {
            return Mono.just(geographyQuizService.generateRandomQuestion());
        }
    }

    @PostMapping("/check-answer")
    public Mono<AnswerResponse> checkAnswer(@RequestBody AnswerRequest request) {
        boolean isCorrect = geographyQuizService.checkAnswer(
            request.question(), 
            request.userAnswer(), 
            request.correctAnswer()
        );
        
        String message = isCorrect 
            ? "Correct! " + request.explanation()
            : "Incorrect. " + request.explanation();
        
        return Mono.just(new AnswerResponse(isCorrect, message));
    }

    public record AnswerRequest(
        String question, 
        String userAnswer, 
        String correctAnswer, 
        String explanation
    ) {}
    
    public record AnswerResponse(boolean isCorrect, String message) {}
}