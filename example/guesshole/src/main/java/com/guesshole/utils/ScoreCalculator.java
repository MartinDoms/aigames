package com.guesshole.utils;

import com.guesshole.entities.LocationPoint;
import com.guesshole.entities.ScoreMultiplier;
import com.guesshole.entities.MultiplierType;

import java.util.*;

public class ScoreCalculator {

    /**
     * Calculate a score based on distance
     *
     * @param distanceKm    Distance in kilometers
     * @param roundDuration
     * @param guessTime
     * @return Score value
     */
    public static int calculateBaseScore(
            double distanceKm,
            Integer roundDuration,
            Integer guessTime,
            double maxDistanceThreshold,
            double minDistanceThreshold,
            double distanceScoreMultiplier,
            double timeScoreMultiplier,
            double graceTimeThreshold
    ) {

        // Cap the distance at the maximum threshold
        double cappedDistance = Math.min(distanceKm, maxDistanceThreshold);

        // Calculate accuracy score (decreases linearly as distance increases)
        // Perfect accuracy (at or below minDistanceThreshold) gives 1000 points
        // Worst accuracy (at or above maxDistanceThreshold) gives 0 points
        double accuracyPercentage = 0;
        if (cappedDistance <= minDistanceThreshold) {
            accuracyPercentage = 1.0; // 100% accuracy for very close guesses
        } else if (cappedDistance >= maxDistanceThreshold) {
            accuracyPercentage = 0.0; // 0% accuracy for guesses beyond the max threshold
        } else {
            // Linear decrease from 100% to 0% between min and max distance thresholds
            accuracyPercentage = 1.0 - ((cappedDistance - minDistanceThreshold) / (maxDistanceThreshold - minDistanceThreshold));
        }

        double baseDistanceScore = 1000 * accuracyPercentage;

        // Calculate time bonus (decreases linearly as time increases)
        // If time taken is below grace threshold, give full time bonus
        // If time taken is the full round duration, give no time bonus
        double timePercentage = 0;
        if (guessTime <= graceTimeThreshold) {
            timePercentage = 1.0; // 100% time bonus for very quick guesses
        } else if (guessTime >= roundDuration) {
            timePercentage = 0.0; // 0% time bonus for using all available time
        } else {
            // Linear decrease from 100% to 0% between grace time and full duration
            timePercentage = 1.0 - ((guessTime - graceTimeThreshold) / (roundDuration - graceTimeThreshold));
        }

        // Calculate time-weighted accuracy score
        // This ensures that even very fast guesses get 0 points if they're completely inaccurate
        double timeWeightedScore = baseDistanceScore * timePercentage * timeScoreMultiplier;

        // Final score is the sum of base distance score and time-weighted score, with respective multipliers
        int finalScore = (int) Math.round(
                (baseDistanceScore * distanceScoreMultiplier) + timeWeightedScore
        );

        // Ensure the score is never negative
        return Math.max(0, finalScore);
    }

    public static List<ScoreMultiplier> calculateScoreMultipliers(
            Double distanceKm,
            Integer guessTime,
            boolean isFirstGuess,
            LocationPoint guessedLocation,
            LocationPoint targetLocation,
            boolean isSoloGame
    ) {

        List<ScoreMultiplier> multipliers = new ArrayList<>();

        // Time bonus (fast guess)
        if (guessTime < 5) { // If player guessed in 3 seconds
            float timeMultiplier = 1.2f;
            multipliers.add(new ScoreMultiplier(
                    timeMultiplier,
                    MultiplierType.TRIGGER_HAPPY,
                    "Trigger Happy!",
                    "Trigger Happy Bonus: Guess within 5 seconds"
            ));
        }

        // First guess bonus - only in games with multiple players
        if (!isSoloGame && isFirstGuess) {
            float firstGuessMultiplier = 1.1f;
            multipliers.add(new ScoreMultiplier(
                    firstGuessMultiplier,
                    MultiplierType.FIRST_GUESS,
                    "First Guess!",
                    "First Guess Bonus: You were the first to guess this round"
            ));
        }

        if (Objects.equals(guessedLocation.getAdmin0Name(), targetLocation.getAdmin0Name())) {
            float continentMultiplier = 1.1f;
            multipliers.add(new ScoreMultiplier(
                    continentMultiplier,
                    MultiplierType.CORRECT_COUNTRY,
                    "Country Bonus!",
                    "Country Bonus: Correctly identified " + targetLocation.getAdmin0Name()
            ));
        }

        // Sometimes for the right state, other times the metropolitan, province, region or even city
        if (Objects.equals(guessedLocation.getAdmin1Name(), targetLocation.getAdmin1Name())) {
            float cityMultiplier = 1.3f;
            multipliers.add(new ScoreMultiplier(
                    cityMultiplier,
                    MultiplierType.CORRECT_CITY,
                    targetLocation.getAdmin1Type() + " Bonus!",
                    targetLocation.getAdmin1Type() + " Bonus: Correctly identified " + targetLocation.getAdmin1Name()
            ));
        }

        // Sometimes for the right city, or other level-2 administrative region
        if (Objects.equals(guessedLocation.getAdmin2Name(), targetLocation.getAdmin2Name())) {
            float cityMultiplier = 1.5f;
            multipliers.add(new ScoreMultiplier(
                    cityMultiplier,
                    MultiplierType.CORRECT_COUNTY,
                    targetLocation.getAdmin2Type() + " Bonus!",
                    targetLocation.getAdmin2Type() + " Bonus: Correctly identified " + targetLocation.getAdmin2Name()
            ));
        }

        return multipliers;
    }

    // Method to calculate final score with multipliers
    public static int calculateFinalScore(int baseScore, List<ScoreMultiplier> multipliers) {
        double finalScore = baseScore;

        // Apply each multiplier sequentially
        for (ScoreMultiplier multiplier : multipliers) {
            finalScore *= multiplier.getMultiplierValue();
        }

        return (int) Math.round(finalScore);
    }

    /**
     * Generates data for the scoring chart based on current application properties.
     * This doesn't belong here but I can factor it out later.
     *
     * @param roundDuration           The duration of the round in seconds
     * @param maxDistanceThreshold
     * @param minDistanceThreshold
     * @param distanceScoreMultiplier
     * @param timeScoreMultiplier
     * @param graceTimeThreshold
     * @return Map containing chart data for different distances and times
     */
    public static Map<String, Object> getChartData(int roundDuration,
                                            Double maxDistanceThreshold,
                                            Double minDistanceThreshold,
                                            Double distanceScoreMultiplier,
                                            Double timeScoreMultiplier,
                                            Double graceTimeThreshold) {

        Map<String, Object> result = new HashMap<>();

        // Add configuration values to the result
        result.put("maxDistanceThreshold", maxDistanceThreshold);
        result.put("minDistanceThreshold", minDistanceThreshold);
        result.put("distanceScoreMultiplier", distanceScoreMultiplier);
        result.put("timeScoreMultiplier", timeScoreMultiplier);
        result.put("graceTimeThreshold", graceTimeThreshold);
        result.put("roundDuration", roundDuration);

        // Generate distance points (logarithmic scale for better visualization)
        List<Integer> distances = new ArrayList<>();
        for (int i = 0; i <= 72; i++) {
            // Use logarithmic scale to get more points in lower ranges
            double factor = i / 72.0;
            double distance = (maxDistanceThreshold + 1000) * factor; //Math.round(Math.exp(Math.log(1) + factor * (Math.log(maxDistanceThreshold) - Math.log(1))));
            distances.add((int) distance);
        }
        result.put("distances", distances);

        // Generate time points
        List<Integer> times = new ArrayList<>();
        for (int i = 0; i <= 10; i++) {
            times.add(i * (roundDuration / 10));
        }
        result.put("times", times);

        // Calculate scores for each distance/time combination
        List<List<Integer>> scores = new ArrayList<>();
        for (Integer time : times) {
            List<Integer> timeScores = new ArrayList<>();
            for (Integer distance : distances) {
                timeScores.add(calculateBaseScore(
                        distance,
                        roundDuration,
                        time,
                        maxDistanceThreshold,
                        minDistanceThreshold,
                        distanceScoreMultiplier,
                        timeScoreMultiplier,
                        graceTimeThreshold
                ));
            }
            scores.add(timeScores);
        }
        result.put("scores", scores);

        // Calculate max score for color scaling
        int maxScore = 0;
        for (List<Integer> timeScores : scores) {
            for (Integer score : timeScores) {
                maxScore = Math.max(maxScore, score);
            }
        }
        result.put("maxScore", maxScore);

        return result;
    }

}
