package com.guesshole.websocket.messages.outgoing;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.guesshole.entities.Player;

/**
 * Message to notify clients of an updated player list
 */
@JsonPropertyOrder({ "type" })
public class PlayersUpdateMessage {
    private final String type = "PLAYERS_UPDATE";
    private final Iterable<Player> players;

    public PlayersUpdateMessage(Iterable<Player> players) {
        this.players = players;
    }

    public String getType() {
        return type;
    }

    public Iterable<Player> getPlayers() {
        return players;
    }
}