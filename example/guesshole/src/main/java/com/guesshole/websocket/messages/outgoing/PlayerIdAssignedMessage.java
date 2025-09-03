package com.guesshole.websocket.messages.outgoing;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.guesshole.websocket.messages.WebSocketMessage;

import java.util.UUID;

@JsonPropertyOrder({ "type" })
public class PlayerIdAssignedMessage implements WebSocketMessage {
    private final String type = "PLAYER_ID_ASSIGNED";
    private final UUID playerId;

    public PlayerIdAssignedMessage(UUID playerId) {
        this.playerId = playerId;
    }

    @Override
    public String getType() {
        return type;
    }

    public UUID getPlayerId() {
        return playerId;
    }
}