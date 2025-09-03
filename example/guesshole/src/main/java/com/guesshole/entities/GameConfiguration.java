package com.guesshole.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("game_configurations")
public class GameConfiguration {

    @Id
    private UUID id;

    @Column("game_type")
    private GameType gameType;

    @Column("num_rounds")
    private int numRounds;

    @Column("round_length_seconds")
    private int roundLengthSeconds;

    @Column("geography_type")
    private String geographyType;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    public GameConfiguration() {
        // Default constructor
    }

    public GameConfiguration(GameType gameType, int numRounds, int roundLengthSeconds, String geographyType) {
        this.gameType = gameType;
        this.numRounds = numRounds;
        this.roundLengthSeconds = roundLengthSeconds;
        this.geographyType = geographyType;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }


    public GameType getGameType() {
        return gameType;
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType;
    }

    public int getNumRounds() {
        return numRounds;
    }

    public void setNumRounds(int numRounds) {
        this.numRounds = numRounds;
    }

    public int getRoundLengthSeconds() {
        return roundLengthSeconds;
    }

    public void setRoundLengthSeconds(int roundLengthSeconds) {
        this.roundLengthSeconds = roundLengthSeconds;
    }

    public String getGeographyType() {
        return geographyType;
    }

    public void setGeographyType(String geographyType) {
        this.geographyType = geographyType;
    }

    @Override
    public String toString() {
        return "Lobby{" +
                "id=" + id +
                ", gameType='" + gameType + '\'' +
                ", numRounds='" + numRounds + '\'' +
                ", roundLengthSeconds='" + roundLengthSeconds + '\'' +
                ", geographyType='" + geographyType + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}