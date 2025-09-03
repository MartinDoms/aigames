package com.guesshole.websocket.messages.incoming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.UUID;

public class GuessSubmittedMessage {
    private final String type = "GUESS_SUBMITTED";
    private final BigDecimal latitude;
    private final BigDecimal longitude;
    private final UUID roundId;
    private final Integer roundDuration;
    private final Integer guessTime;

    @JsonCreator
    public GuessSubmittedMessage(
            @JsonProperty("latitude") BigDecimal latitude,
            @JsonProperty("longitude") BigDecimal longitude,
            @JsonProperty("roundId") UUID roundId,
            @JsonProperty("roundDuration") Integer roundDuration,
            @JsonProperty("guessTime") Integer guessTime) {
        this.latitude = latitude;
        this.longitude = longitude;
        this.roundId = roundId;
        this.roundDuration = roundDuration;
        this.guessTime = guessTime;
    }

    public String getType() {
        return type;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public UUID getRoundId() {
        return roundId;
    }

    public Integer getRoundDuration() {
        return roundDuration;
    }

    public Integer getGuessTime() {
        return guessTime;
    }
}