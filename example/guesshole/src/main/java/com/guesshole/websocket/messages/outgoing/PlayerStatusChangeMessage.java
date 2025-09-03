package com.guesshole.websocket.messages.outgoing;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.guesshole.websocket.messages.WebSocketMessage;

import java.util.UUID;

/**
 * Message class for player status changes
 */
@JsonPropertyOrder({ "type" })
public class PlayerStatusChangeMessage implements WebSocketMessage {
    private final String type = "PLAYER_STATUS_CHANGE";
    private final UUID playerId;
    private final boolean active;

    public PlayerStatusChangeMessage(UUID playerId, boolean active) {
        this.playerId = playerId;
        this.active = active;
    }

    public String getType() {
        return type;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public boolean isActive() {
        return active;
    }
}
