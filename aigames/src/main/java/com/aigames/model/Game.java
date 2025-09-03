package com.aigames.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;
import org.springframework.data.relational.core.mapping.Column;

import java.time.LocalDateTime;

@Table("games")
public record Game(
    @Id Long id,
    String name,
    @Column("display_name") String displayName,
    String description,
    String category,
    @Column("is_active") Boolean isActive,
    @Column("created_at") LocalDateTime createdAt,
    @Column("updated_at") LocalDateTime updatedAt
) {
    public Game(String name, String displayName, String description, String category) {
        this(null, name, displayName, description, category, true, null, null);
    }
}