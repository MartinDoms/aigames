package com.guesshole.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.*;

/**
 * Represents a game instance that contains rounds and is associated with players
 */
@Table("game_instance")
public class GameInstance {
    @Id
    private UUID id;

    @Column("game_type")
    private String gameType = GameType.CITY_GUESSER.name();

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    // Rounds are fetched separately since R2DBC doesn't support nested entities
    @Transient
    private List<Round> rounds = new ArrayList<>();

    // Default constructor
    public GameInstance() {
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GameType getGameType() {
        return GameType.valueOf(gameType);
    }

    public void setGameType(GameType gameType) {
        this.gameType = gameType.name();
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

    // Transient getter/setter for rounds
    public List<Round> getRounds() {
        return rounds;
    }

    public void setRounds(List<Round> rounds) {
        rounds.sort(Comparator.comparingInt(Round::getRoundOrder));

        this.rounds = rounds;
    }

    @Override
    public String toString() {
        return "GameInstance{" +
                "id=" + id +
                ", gameType='" + gameType + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", rounds=" + rounds +
                '}';
    }
}