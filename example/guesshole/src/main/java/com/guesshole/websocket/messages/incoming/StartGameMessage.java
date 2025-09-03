package com.guesshole.websocket.messages.incoming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

public class StartGameMessage {
    private final String type = "START_GAME";
    private final int rounds;
    private final int roundLength;
    private final String geoType;

    @JsonCreator
    public StartGameMessage(
            @JsonProperty("rounds") Integer rounds,
            @JsonProperty("roundLength") Integer roundLength,
            @JsonProperty("geoType") String geoType) {
        this.rounds = rounds != null ? rounds : 10; // Default to 10 rounds
        this.roundLength = roundLength != null ? roundLength : 75; // Default to 75 seconds
        this.geoType = geoType != null ? geoType : "WORLD"; // Default to WORLD
    }

    public String getType() {
        return type;
    }

    public int getRounds() {
        return rounds;
    }

    public int getRoundLength() {
        return roundLength;
    }

    public String getGeoType() {
        return geoType;
    }
}