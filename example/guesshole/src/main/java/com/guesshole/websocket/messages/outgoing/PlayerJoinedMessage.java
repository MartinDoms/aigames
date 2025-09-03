package com.guesshole.websocket.messages.outgoing;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.guesshole.entities.Player;
import com.guesshole.websocket.messages.WebSocketMessage;

@JsonPropertyOrder({ "type" })
public class PlayerJoinedMessage implements WebSocketMessage {
    private final String type = "PLAYER_JOINED";
    private final Player player;

    public PlayerJoinedMessage(Player player) {
        this.player = player;
    }

    @Override
    public String getType() {
        return type;
    }

    public Player getPlayer() {
        return player;
    }
}