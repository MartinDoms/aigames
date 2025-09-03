package com.guesshole.websocket.messages.outgoing;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.guesshole.entities.Player;

import java.util.ArrayList;
import java.util.List;

@JsonPropertyOrder({ "type" })
public class PlayerUpdatedMessage {
    private final String type = "PLAYER_UPDATED";
    private final Player player;
    private final List<String> changedFields;

    public PlayerUpdatedMessage(Player player) {
        this.player = player;
        this.changedFields = new ArrayList<>();
    }

    public PlayerUpdatedMessage(Player player, List<String> changedFields) {
        this.player = player;
        this.changedFields = changedFields;
    }

    public String getType() {
        return type;
    }

    public Player getPlayer() {
        return player;
    }

    public List<String> getChangedFields() {
        return changedFields;
    }
}