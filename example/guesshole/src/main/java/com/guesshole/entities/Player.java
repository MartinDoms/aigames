package com.guesshole.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.time.LocalDateTime;
import java.util.UUID;

@Table("players")
public class Player {
    @Id
    private UUID id;
    private String name;
    private UUID lobbyId;
    private boolean isHost;
    private String avatar;
    private boolean active;
    private boolean kicked;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    public Player() {
        // Required for Jackson deserialization
    }

    public Player(String name, UUID lobbyId, boolean isHost) {
        this.name = name;
        this.lobbyId = lobbyId;
        this.isHost = isHost;
        this.avatar = "avatar1";
        this.active = true; // New players are active by default
        this.kicked = false; // New players are not kicked by default
    }

    public Player(String name, UUID lobbyId, boolean isHost, String avatar) {
        this.name = name;
        this.lobbyId = lobbyId;
        this.isHost = isHost;
        this.avatar = avatar != null ? avatar : "avatar1";
        this.active = true; // New players are active by default
        this.kicked = false; // New players are not kicked by default
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public UUID getLobbyId() {
        return lobbyId;
    }

    public void setLobbyId(UUID lobbyId) {
        this.lobbyId = lobbyId;
    }

    public boolean isHost() {
        return isHost;
    }

    public void setHost(boolean host) {
        this.isHost = host;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public boolean isKicked() {
        return kicked;
    }

    public void setKicked(boolean kicked) {
        this.kicked = kicked;
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
        return "Player{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", lobbyId=" + lobbyId +
                ", host=" + isHost +
                ", avatar='" + avatar + '\'' +
                ", active=" + active +
                ", kicked=" + kicked +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}