package com.guesshole.websocket.messages.outgoing;

import com.guesshole.entities.GameConfiguration;
import com.guesshole.entities.Round;

import java.util.List;
import java.util.UUID;

/**
 * Builder for GameStateMessage to simplify creating instances with many optional parameters
 */
public class GameStateMessageBuilder {
    private String state;
    private UUID gameInstanceId;
    private Round currentRound;
    private Integer totalRounds;
    private List<GameStateMessage.PlayerScore> playerScores;
    private int roundOrder;
    private boolean lastRound;
    private UUID roundId;
    private GameConfiguration gameConfiguration;

    public GameStateMessageBuilder() {
        // Initialize with default values if needed
        this.roundOrder = 0;
        this.lastRound = false;
    }

    public GameStateMessageBuilder state(String state) {
        this.state = state;
        return this;
    }

    public GameStateMessageBuilder gameInstanceId(UUID gameInstanceId) {
        this.gameInstanceId = gameInstanceId;
        return this;
    }

    public GameStateMessageBuilder currentRound(Round currentRound) {
        this.currentRound = currentRound;
        return this;
    }

    public GameStateMessageBuilder totalRounds(Integer totalRounds) {
        this.totalRounds = totalRounds;
        return this;
    }

    public GameStateMessageBuilder playerScores(List<GameStateMessage.PlayerScore> playerScores) {
        this.playerScores = playerScores;
        return this;
    }

    public GameStateMessageBuilder roundOrder(int roundOrder) {
        this.roundOrder = roundOrder;
        return this;
    }

    public GameStateMessageBuilder lastRound(boolean lastRound) {
        this.lastRound = lastRound;
        return this;
    }

    public GameStateMessageBuilder roundId(UUID roundId) {
        this.roundId = roundId;
        return this;
    }

    public GameStateMessageBuilder gameConfiguration(GameConfiguration gameConfiguration) {
        this.gameConfiguration = gameConfiguration;
        return this;
    }

    /**
     * Build the GameStateMessage instance with the configured values
     * @return A new GameStateMessage instance
     */
    public GameStateMessage build() {
        // Create a new message
        GameStateMessage message = new GameStateMessage();

        // Set all the fields
        message.setState(state);
        message.setGameInstanceId(gameInstanceId);
        message.setCurrentRound(currentRound);
        message.setTotalRounds(totalRounds);
        message.setPlayerScores(playerScores);
        message.setRoundOrder(roundOrder);
        message.setLastRound(lastRound);
        message.setRoundId(roundId);
        message.setGameConfiguration(gameConfiguration);

        return message;
    }

    /**
     * Convenience static method for creating a builder
     * @return A new GameStateMessageBuilder instance
     */
    public static GameStateMessageBuilder builder() {
        return new GameStateMessageBuilder();
    }
}