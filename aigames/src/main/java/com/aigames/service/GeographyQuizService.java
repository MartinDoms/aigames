package com.aigames.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class GeographyQuizService {

    // Simple geography data - in a real app this could come from a database
    private static final Map<String, String> COUNTRIES_AND_CAPITALS = Map.ofEntries(
        Map.entry("France", "Paris"),
        Map.entry("Germany", "Berlin"),
        Map.entry("Italy", "Rome"),
        Map.entry("Spain", "Madrid"),
        Map.entry("United Kingdom", "London"),
        Map.entry("Japan", "Tokyo"),
        Map.entry("China", "Beijing"),
        Map.entry("India", "New Delhi"),
        Map.entry("Australia", "Canberra"),
        Map.entry("Canada", "Ottawa"),
        Map.entry("Brazil", "Brasilia"),
        Map.entry("Argentina", "Buenos Aires"),
        Map.entry("Egypt", "Cairo"),
        Map.entry("South Africa", "Cape Town"),
        Map.entry("Russia", "Moscow"),
        Map.entry("United States", "Washington D.C."),
        Map.entry("Mexico", "Mexico City"),
        Map.entry("Netherlands", "Amsterdam"),
        Map.entry("Sweden", "Stockholm"),
        Map.entry("Norway", "Oslo")
    );

    private static final Map<String, List<String>> COUNTRY_FACTS = Map.ofEntries(
        Map.entry("France", List.of("Has the most time zones of any country", "Home to the Louvre Museum", "Invented the metric system")),
        Map.entry("Australia", List.of("Is both a country and continent", "Has more species of venomous snakes than any other country", "The Great Barrier Reef is visible from space")),
        Map.entry("Japan", List.of("Consists of over 6,800 islands", "Has the world's oldest continuous monarchy", "Invented instant noodles")),
        Map.entry("Russia", List.of("Spans 11 time zones", "Contains about 20% of the world's fresh water", "Has a border with 16 countries")),
        Map.entry("Brazil", List.of("Contains 60% of the Amazon rainforest", "Speaks Portuguese, not Spanish", "Has won the FIFA World Cup 5 times"))
    );

    private final Random random = new Random();

    public QuizQuestion generateCapitalQuestion() {
        List<String> countries = new ArrayList<>(COUNTRIES_AND_CAPITALS.keySet());
        String correctCountry = countries.get(random.nextInt(countries.size()));
        String correctCapital = COUNTRIES_AND_CAPITALS.get(correctCountry);
        
        // Generate wrong answers
        List<String> allCapitals = new ArrayList<>(COUNTRIES_AND_CAPITALS.values());
        allCapitals.remove(correctCapital);
        Collections.shuffle(allCapitals);
        
        List<String> options = new ArrayList<>();
        options.add(correctCapital);
        options.addAll(allCapitals.subList(0, Math.min(3, allCapitals.size())));
        Collections.shuffle(options);
        
        return new QuizQuestion(
            QuestionType.CAPITAL,
            "What is the capital of " + correctCountry + "?",
            options,
            correctCapital,
            "The capital of " + correctCountry + " is " + correctCapital
        );
    }

    public QuizQuestion generateCountryQuestion() {
        List<String> countries = new ArrayList<>(COUNTRIES_AND_CAPITALS.keySet());
        String correctCountry = countries.get(random.nextInt(countries.size()));
        String capital = COUNTRIES_AND_CAPITALS.get(correctCountry);
        
        // Generate wrong answers
        List<String> wrongCountries = new ArrayList<>(countries);
        wrongCountries.remove(correctCountry);
        Collections.shuffle(wrongCountries);
        
        List<String> options = new ArrayList<>();
        options.add(correctCountry);
        options.addAll(wrongCountries.subList(0, Math.min(3, wrongCountries.size())));
        Collections.shuffle(options);
        
        return new QuizQuestion(
            QuestionType.COUNTRY,
            capital + " is the capital of which country?",
            options,
            correctCountry,
            capital + " is the capital of " + correctCountry
        );
    }

    public QuizQuestion generateFactQuestion() {
        List<String> countriesWithFacts = new ArrayList<>(COUNTRY_FACTS.keySet());
        String correctCountry = countriesWithFacts.get(random.nextInt(countriesWithFacts.size()));
        List<String> facts = COUNTRY_FACTS.get(correctCountry);
        String fact = facts.get(random.nextInt(facts.size()));
        
        // Generate wrong answers
        List<String> wrongCountries = new ArrayList<>(COUNTRIES_AND_CAPITALS.keySet());
        wrongCountries.remove(correctCountry);
        Collections.shuffle(wrongCountries);
        
        List<String> options = new ArrayList<>();
        options.add(correctCountry);
        options.addAll(wrongCountries.subList(0, Math.min(3, wrongCountries.size())));
        Collections.shuffle(options);
        
        return new QuizQuestion(
            QuestionType.FACT,
            "Which country " + fact.toLowerCase() + "?",
            options,
            correctCountry,
            correctCountry + " " + fact.toLowerCase()
        );
    }

    public QuizQuestion generateRandomQuestion() {
        QuestionType[] types = QuestionType.values();
        QuestionType randomType = types[random.nextInt(types.length)];
        
        return switch (randomType) {
            case CAPITAL -> generateCapitalQuestion();
            case COUNTRY -> generateCountryQuestion();
            case FACT -> generateFactQuestion();
        };
    }

    public boolean checkAnswer(String question, String userAnswer, String correctAnswer) {
        return correctAnswer.equalsIgnoreCase(userAnswer.trim());
    }

    public enum QuestionType {
        CAPITAL, COUNTRY, FACT
    }

    public record QuizQuestion(
        QuestionType type,
        String question,
        List<String> options,
        String correctAnswer,
        String explanation
    ) {}
}