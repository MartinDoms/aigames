    package com.guesshole.websocket.messages.outgoing;

    import com.fasterxml.jackson.annotation.JsonPropertyOrder;
    import com.guesshole.entities.*;
    import com.guesshole.websocket.messages.WebSocketMessage;

    import java.time.LocalDateTime;
    import java.util.List;
    import java.util.UUID;

    /**
     * Message containing complete game state information
     * TODO refactor this into subclasses which disallow illegal states
     * eg ROUND_SCOREBOARD must has playerScores and round information etc
     */
    @JsonPropertyOrder({ "type" })
    public class GameStateMessage implements WebSocketMessage {
        private String state;
        private UUID gameInstanceId;

        // Current round is populated when we're in a round or round scoreboard
        private Round currentRound;

        // Game information
        private Integer totalRounds;

        // Scoreboard data - populated for ROUND_SCOREBOARD and SCOREBOARD states
        private List<PlayerScore> playerScores;
        private int roundOrder;
        private boolean lastRound;
        private UUID roundId;

        private GameConfiguration gameConfiguration;

        // Default constructor
        public GameStateMessage() {
        }

        // Getters and setters
        public String getState() {
            return state;
        }

        public void setState(String state) {
            this.state = state;
        }

        public UUID getGameInstanceId() {
            return gameInstanceId;
        }

        public void setGameInstanceId(UUID gameInstanceId) {
            this.gameInstanceId = gameInstanceId;
        }

        public Round getCurrentRound() {
            return currentRound;
        }

        public void setCurrentRound(Round currentRound) {
            this.currentRound = currentRound;
        }

        public Integer getTotalRounds() {
            return totalRounds;
        }

        public void setTotalRounds(Integer totalRounds) {
            this.totalRounds = totalRounds;
        }

        public List<PlayerScore> getPlayerScores() {
            return playerScores;
        }

        public void setPlayerScores(List<PlayerScore> playerScores) {
            this.playerScores = playerScores;
        }

        public int getRoundOrder() {
            return roundOrder;
        }

        public void setRoundOrder(int roundOrder) {
            this.roundOrder = roundOrder;
        }

        public boolean isLastRound() {
            return lastRound;
        }

        public void setLastRound(boolean lastRound) {
            this.lastRound = lastRound;
        }

        public UUID getRoundId() {
            return roundId;
        }

        public void setRoundId(UUID roundId) {
            this.roundId = roundId;
        }

        @Override
        public String getType() {
            return "GAME_STATE";
        }

        public GameConfiguration getGameConfiguration() {
            return gameConfiguration;
        }

        public void setGameConfiguration(GameConfiguration gameConfiguration) {
            this.gameConfiguration = gameConfiguration;
        }

        /**
         * Inner class to represent a player's score
         */
        public static class PlayerScore {
            private final Player player;
            private final Guess guess;
            private final Integer totalScore;

            public PlayerScore(Player player, Guess guess, Integer totalScore
            ) {
                this.player = player;
                this.guess = guess;
                this.totalScore = totalScore;
            }

            public Player getPlayer() {
                return player;
            }

            public Guess getGuess() {
                return guess;
            }

            public Integer getTotalScore() {
                return totalScore;
            }
        }


    }