package com.guesshole.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Join table entity for the many-to-many relationship between GameInstance and Player
 */
@Table("game_instance_player")
public class GameInstancePlayer {
    @Id
    private UUID id;

    @Column("game_instance_id")
    private UUID gameInstanceId;

    @Column("player_id")
    private UUID playerId;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    // Default constructor
    public GameInstancePlayer() {
    }

    // Constructor with associations
    public GameInstancePlayer(UUID gameInstanceId, UUID playerId) {
        this.id = UUID.randomUUID(); // Explicitly set ID
        this.gameInstanceId = gameInstanceId;
        this.playerId = playerId;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getGameInstanceId() {
        return gameInstanceId;
    }

    public void setGameInstanceId(UUID gameInstanceId) {
        this.gameInstanceId = gameInstanceId;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
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

    @Override
    public String toString() {
        return "GameInstancePlayer{" +
                "id=" + id +
                ", gameInstanceId=" + gameInstanceId +
                ", playerId=" + playerId +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}