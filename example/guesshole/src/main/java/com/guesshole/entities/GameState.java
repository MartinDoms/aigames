package com.guesshole.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing the current state of a game lobby
 */
@Table("game_state")
public class GameState {
    @Id
    private UUID id;

    @Column("lobby_state")
    private String lobbyState;

    @Column("round_id")
    private UUID roundId;

    @Column("lobby_id")
    private UUID lobbyId;

    @Column("game_instance_id")
    private UUID gameInstanceId;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    // Default constructor
    public GameState() {
        this.lobbyState = "LOBBY"; // Default state
    }

    // Constructor with lobby
    public GameState(UUID lobbyId) {
        this.lobbyId = lobbyId;
        this.lobbyState = "LOBBY"; // Default state
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getLobbyState() {
        return lobbyState;
    }

    public void setLobbyState(String lobbyState) {
        this.lobbyState = lobbyState;
    }

    public UUID getRoundId() {
        return roundId;
    }

    public void setRoundId(UUID roundId) {
        this.roundId = roundId;
    }

    public UUID getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(UUID lobbyId) {
        this.lobbyId = lobbyId;
    }

    public UUID getGameInstanceId() {
        return gameInstanceId;
    }

    public void setGameInstanceId(UUID gameInstanceId) {
        this.gameInstanceId = gameInstanceId;
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
        return "GameState{" +
                "id=" + id +
                ", lobbyState='" + lobbyState + '\'' +
                ", roundId=" + roundId +
                ", lobbyId=" + lobbyId +
                ", gameInstanceId=" + gameInstanceId +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}