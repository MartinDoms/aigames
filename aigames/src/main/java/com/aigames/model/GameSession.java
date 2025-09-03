package com.aigames.model;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

@Table("game_sessions")
public record GameSession(
    @Id Long id,
    @Column("game_id") Long gameId,
    @Column("player_name") String playerName,
    Integer score,
    @Column("is_completed") Boolean isCompleted,
    @Column("session_data") JsonNode sessionData,
    @Column("started_at") LocalDateTime startedAt,
    @Column("completed_at") LocalDateTime completedAt
) {
    public GameSession(Long gameId, String playerName, JsonNode sessionData) {
        this(null, gameId, playerName, 0, false, sessionData, null, null);
    }
}