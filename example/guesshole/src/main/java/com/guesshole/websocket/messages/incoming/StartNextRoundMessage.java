package com.guesshole.websocket.messages.incoming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class StartNextRoundMessage {

    private final String type = "START_NEXT_ROUND";
    private final UUID currentRoundId;

    @JsonCreator
    public StartNextRoundMessage(
            @JsonProperty("currentRoundId") UUID currentRoundId
    ) {
        this.currentRoundId = currentRoundId;
    }

    public String getType() {
        return type;
    }

    public UUID getCurrentRoundId() { return currentRoundId; }
}
