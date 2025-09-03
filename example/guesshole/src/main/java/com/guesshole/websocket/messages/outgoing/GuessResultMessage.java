package com.guesshole.websocket.messages.outgoing;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.guesshole.entities.LocationPoint;
import com.guesshole.entities.Player;
import com.guesshole.entities.ScoreMultiplier;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@JsonPropertyOrder({ "type" })
public class GuessResultMessage {
    private final String type = "GUESS_RESULT";
    private final UUID guessId;
    private final UUID roundId;
    private final Player player;
    private final LocationPoint guessLocation;
    private final LocationPoint actualLocation;

    private final double distanceKm;
    private final int baseScore;
    private final int score;
    private final List<ScoreMultiplier> multipliers;

    public GuessResultMessage(
            UUID guessId,
            UUID roundId,
            Player player,
            LocationPoint guessLocation,
            LocationPoint actualLocation,
            double distanceKm,
            int baseScore,
            int score,
            List<ScoreMultiplier> multipliers) {
        this.guessId = guessId;
        this.roundId = roundId;
        this.player = player;
        this.guessLocation = guessLocation;
        this.actualLocation = actualLocation;
        this.distanceKm = distanceKm;
        this.baseScore = baseScore;
        this.score = score;
        this.multipliers = multipliers;
    }

    public String getType() {
        return type;
    }

    public UUID getGuessId() {
        return guessId;
    }

    public UUID getRoundId() {
        return roundId;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public int getBaseScore() {
        return baseScore;
    }

    public int getScore() {
        return score;
    }

    public List<ScoreMultiplier> getMultipliers() {
        return multipliers;
    }

    public LocationPoint getGuessLocation() {
        return guessLocation;
    }

    public LocationPoint getActualLocation() {
        return actualLocation;
    }

    public Player getPlayer() {
        return player;
    }
}