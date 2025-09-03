package com.guesshole.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.util.UUID;

@Table("score_multipliers")
public class ScoreMultiplier {

    @Id
    private UUID id;

    @Column("guess_id")
    private UUID guessId;

    @Column("multiplier_type")
    private MultiplierType multiplierType;

    @Column("multiplier_value")
    private float multiplierValue;

    @Column("display_name")
    private String displayName;

    @Column("tooltip")
    private String tooltip;

    // Default constructor
    public ScoreMultiplier() {}

    // Constructor for use in code
    public ScoreMultiplier(float multiplierValue, MultiplierType multiplierType,
                           String displayName, String tooltip) {
        this.multiplierValue = multiplierValue;
        this.multiplierType = multiplierType;
        this.displayName = displayName;
        this.tooltip = tooltip;
    }

    // Constructor with guess ID for persistence
    public ScoreMultiplier(UUID guessId, float multiplierValue, MultiplierType multiplierType,
                           String displayName, String tooltip) {
        this.guessId = guessId;
        this.multiplierValue = multiplierValue;
        this.multiplierType = multiplierType;
        this.displayName = displayName;
        this.tooltip = tooltip;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getGuessId() {
        return guessId;
    }

    public void setGuessId(UUID guessId) {
        this.guessId = guessId;
    }

    public MultiplierType getMultiplierType() {
        return multiplierType;
    }

    public void setMultiplierType(MultiplierType multiplierType) {
        this.multiplierType = multiplierType;
    }

    public float getMultiplierValue() {
        return multiplierValue;
    }

    public void setMultiplierValue(float multiplierValue) {
        this.multiplierValue = multiplierValue;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getTooltip() {
        return tooltip;
    }

    public void setTooltip(String tooltip) {
        this.tooltip = tooltip;
    }

    // Used for serialization to clients
    public String getType() {
        return multiplierType.name();
    }

    public double getMultiplier() {
        return multiplierValue;
    }
}