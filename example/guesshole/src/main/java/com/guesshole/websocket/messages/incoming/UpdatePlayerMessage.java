package com.guesshole.websocket.messages.incoming;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.UUID;

public class UpdatePlayerMessage {
    private final String type = "UPDATE_PLAYER";
    private final UUID playerId;
    private final String name;
    private final String avatar;

    @JsonCreator
    public UpdatePlayerMessage(
            @JsonProperty("playerId") UUID playerId,
            @JsonProperty("name") String name,
            @JsonProperty("avatar") String avatar) {
        this.playerId = playerId;
        this.name = name;
        this.avatar = avatar;
    }

    public String getType() {
        return type;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public String getName() {
        return name;
    }

    public String getAvatar() {
        return avatar;
    }
}