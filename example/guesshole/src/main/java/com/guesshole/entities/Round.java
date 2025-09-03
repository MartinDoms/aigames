package com.guesshole.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a round in a game instance
 */
@Table("round")
public class Round {
    @Id
    private UUID id;

    @Column("round_order")
    private int roundOrder;

    @Column("youtube_video_id")
    private String youtubeVideoId;

    @Column("start_time_seconds")
    private int startTimeSeconds;

    @Column("duration_seconds")
    private int durationSeconds;

    @Column("latitude")
    @JsonIgnore // This will exclude the field when serializing to JSON
    private BigDecimal latitude;

    @Column("longitude")
    @JsonIgnore // This will exclude the field when serializing to JSON
    private BigDecimal longitude;

    @Column("location_point_id")
    private Long locationPointId;

    @Transient
    private LocationPoint locationPoint;

    @Column("game_instance_id")
    @JsonIgnore // Prevent circular references during serialization
    private UUID gameInstanceId;

    @Column("created_at")
    private LocalDateTime createdAt;

    @Column("updated_at")
    private LocalDateTime updatedAt;

    // Default constructor
    public Round() {
    }

    // Constructor with all fields
    public Round(int roundOrder, String youtubeVideoId, int startTimeSeconds, int durationSeconds,
                 BigDecimal latitude, BigDecimal longitude, Long locationPointId) {
        this.roundOrder = roundOrder;
        this.youtubeVideoId = youtubeVideoId;
        this.startTimeSeconds = startTimeSeconds;
        this.durationSeconds = durationSeconds;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationPointId = locationPointId;
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public int getRoundOrder() {
        return roundOrder;
    }

    public void setRoundOrder(int roundOrder) {
        this.roundOrder = roundOrder;
    }

    public String getYoutubeVideoId() {
        return youtubeVideoId;
    }

    public void setYoutubeVideoId(String youtubeVideoId) {
        this.youtubeVideoId = youtubeVideoId;
    }

    public int getStartTimeSeconds() {
        return startTimeSeconds;
    }

    public void setStartTimeSeconds(int startTimeSeconds) {
        this.startTimeSeconds = startTimeSeconds;
    }

    public int getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(int durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public BigDecimal getLatitude() {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude) {
        this.latitude = latitude;
    }

    public BigDecimal getLongitude() {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude) {
        this.longitude = longitude;
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

    // Helper method to create a client-safe copy (without lat/long)
    @JsonIgnore
    public Round getClientSafeRound() {
        Round safeCopy = new Round();
        safeCopy.setId(this.id);
        safeCopy.setGameInstanceId(this.gameInstanceId);
        safeCopy.setRoundOrder(this.roundOrder);
        safeCopy.setYoutubeVideoId(this.youtubeVideoId);
        safeCopy.setStartTimeSeconds(this.startTimeSeconds);
        safeCopy.setDurationSeconds(this.durationSeconds);
        safeCopy.setCreatedAt(this.createdAt);
        safeCopy.setUpdatedAt(this.updatedAt);
        // Deliberately not setting latitude and longitude
        return safeCopy;
    }

    public Long getLocationPointId() {
        return locationPointId;
    }

    public LocationPoint getLocationPoint() {
        return locationPoint;
    }

    public void setLocationPoint(LocationPoint locationPoint) {
        this.locationPoint = locationPoint;
    }
}