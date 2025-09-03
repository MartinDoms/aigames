package com.guesshole.websocket.messages.incoming;

import java.util.UUID;

/**
 * Message class for kicking a player
 */
public class KickPlayerMessage {
    private String type;
    private UUID playerId;

    // Getters and setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }
}