package com.aigames.service;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class NumberSequenceService {

    private static final Random random = new Random();

    public enum SequenceType {
        ARITHMETIC("Arithmetic sequence (constant difference)"),
        GEOMETRIC("Geometric sequence (constant ratio)"),
        FIBONACCI("Fibonacci-like sequence"),
        SQUARES("Perfect squares"),
        CUBES("Perfect cubes"),
        PRIMES("Prime numbers"),
        POWERS_OF_TWO("Powers of 2"),
        TRIANGULAR("Triangular numbers");

        private final String description;

        SequenceType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    public SequencePuzzle generateRandomSequence() {
        SequenceType[] types = SequenceType.values();
        SequenceType type = types[random.nextInt(types.length)];
        return generateSequence(type);
    }

    public SequencePuzzle generateSequence(SequenceType type) {
        return switch (type) {
            case ARITHMETIC -> generateArithmeticSequence();
            case GEOMETRIC -> generateGeometricSequence();
            case FIBONACCI -> generateFibonacciSequence();
            case SQUARES -> generateSquareSequence();
            case CUBES -> generateCubeSequence();
            case PRIMES -> generatePrimeSequence();
            case POWERS_OF_TWO -> generatePowersOfTwoSequence();
            case TRIANGULAR -> generateTriangularSequence();
        };
    }

    private SequencePuzzle generateArithmeticSequence() {
        int start = random.nextInt(10) + 1; // 1-10
        int diff = random.nextInt(9) + 1;   // 1-9
        List<Integer> sequence = new ArrayList<>();
        
        for (int i = 0; i < 5; i++) {
            sequence.add(start + i * diff);
        }
        
        int nextValue = start + 5 * diff;
        String hint = "Each number increases by " + diff;
        
        return new SequencePuzzle(SequenceType.ARITHMETIC, sequence, nextValue, hint);
    }

    private SequencePuzzle generateGeometricSequence() {
        int start = random.nextInt(5) + 1; // 1-5
        int ratio = random.nextInt(3) + 2;  // 2-4
        List<Integer> sequence = new ArrayList<>();
        
        int current = start;
        for (int i = 0; i < 4; i++) {
            sequence.add(current);
            current *= ratio;
        }
        
        int nextValue = current;
        String hint = "Each number is multiplied by " + ratio;
        
        return new SequencePuzzle(SequenceType.GEOMETRIC, sequence, nextValue, hint);
    }

    private SequencePuzzle generateFibonacciSequence() {
        // Start with random first two numbers
        int a = random.nextInt(5) + 1;
        int b = random.nextInt(5) + 1;
        List<Integer> sequence = new ArrayList<>();
        
        sequence.add(a);
        sequence.add(b);
        
        for (int i = 2; i < 5; i++) {
            int next = sequence.get(i-1) + sequence.get(i-2);
            sequence.add(next);
        }
        
        int nextValue = sequence.get(3) + sequence.get(4);
        String hint = "Each number is the sum of the two preceding numbers";
        
        return new SequencePuzzle(SequenceType.FIBONACCI, sequence, nextValue, hint);
    }

    private SequencePuzzle generateSquareSequence() {
        int start = random.nextInt(3) + 1; // Start from 1, 2, or 3
        List<Integer> sequence = new ArrayList<>();
        
        for (int i = start; i < start + 5; i++) {
            sequence.add(i * i);
        }
        
        int nextValue = (start + 5) * (start + 5);
        String hint = "Each number is a perfect square: n²";
        
        return new SequencePuzzle(SequenceType.SQUARES, sequence, nextValue, hint);
    }

    private SequencePuzzle generateCubeSequence() {
        int start = random.nextInt(2) + 1; // Start from 1 or 2
        List<Integer> sequence = new ArrayList<>();
        
        for (int i = start; i < start + 4; i++) {
            sequence.add(i * i * i);
        }
        
        int nextValue = (start + 4) * (start + 4) * (start + 4);
        String hint = "Each number is a perfect cube: n³";
        
        return new SequencePuzzle(SequenceType.CUBES, sequence, nextValue, hint);
    }

    private SequencePuzzle generatePrimeSequence() {
        List<Integer> primes = List.of(2, 3, 5, 7, 11, 13, 17, 19, 23, 29, 31, 37, 41, 43, 47);
        int start = random.nextInt(6); // Start from different positions
        List<Integer> sequence = new ArrayList<>();
        
        for (int i = start; i < start + 5; i++) {
            sequence.add(primes.get(i));
        }
        
        int nextValue = primes.get(start + 5);
        String hint = "Each number is a prime number";
        
        return new SequencePuzzle(SequenceType.PRIMES, sequence, nextValue, hint);
    }

    private SequencePuzzle generatePowersOfTwoSequence() {
        int start = random.nextInt(4); // Start from 2^0, 2^1, 2^2, or 2^3
        List<Integer> sequence = new ArrayList<>();
        
        for (int i = start; i < start + 5; i++) {
            sequence.add((int) Math.pow(2, i));
        }
        
        int nextValue = (int) Math.pow(2, start + 5);
        String hint = "Each number is a power of 2: 2ⁿ";
        
        return new SequencePuzzle(SequenceType.POWERS_OF_TWO, sequence, nextValue, hint);
    }

    private SequencePuzzle generateTriangularSequence() {
        int start = random.nextInt(3) + 1; // Start from 1, 2, or 3
        List<Integer> sequence = new ArrayList<>();
        
        for (int i = start; i < start + 5; i++) {
            sequence.add(i * (i + 1) / 2);
        }
        
        int nextValue = (start + 5) * (start + 6) / 2;
        String hint = "Each number is a triangular number: n(n+1)/2";
        
        return new SequencePuzzle(SequenceType.TRIANGULAR, sequence, nextValue, hint);
    }

    public boolean checkAnswer(SequencePuzzle puzzle, int userAnswer) {
        return puzzle.nextValue() == userAnswer;
    }

    public record SequencePuzzle(
        SequenceType type,
        List<Integer> sequence,
        int nextValue,
        String hint
    ) {}
}