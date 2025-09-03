package com.guesshole.entities;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Entity representing a template for creating rounds in game instances.
 * Contains geographic information, YouTube video details, and location point reference.
 */
@Table("round_template")
public class RoundTemplate {

    @Id
    private UUID id;

    private String source;

    @Column("youtube_video_id")
    private String youtubeVideoId;

    @Column("start_time")
    private Integer startTime;

    @Column("video_length")
    private Integer videoLength;

    private BigDecimal latitude;

    private BigDecimal longitude;

    @Column("location_point_id")
    private Long locationPointId;

    @Transient
    private LocationPoint locationPoint;

    @Column("approve_at")
    private Instant approveAt;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    // Default constructor
    public RoundTemplate() {
    }

    // Constructor with all fields except ID and timestamps
    public RoundTemplate(String source, String youtubeVideoId, Integer startTime,
                         BigDecimal latitude, BigDecimal longitude, Long locationPointId) {
        this.source = source;
        this.youtubeVideoId = youtubeVideoId;
        this.startTime = startTime;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationPointId = locationPointId;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getYoutubeVideoId() {
        return youtubeVideoId;
    }

    public void setYoutubeVideoId(String youtubeVideoId) {
        this.youtubeVideoId = youtubeVideoId;
    }

    public Integer getStartTime() {
        return startTime;
    }

    public void setStartTime(Integer startTime) {
        this.startTime = startTime;
    }

    public Integer getVideoLength() {
        return videoLength;
    }

    public void setVideoLength(Integer videoLength) {
        this.videoLength = videoLength;
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

    public Long getLocationPointId() {
        return locationPointId;
    }

    public void setLocationPointId(Long locationPointId) {
        this.locationPointId = locationPointId;
    }

    public LocationPoint getLocationPoint() {
        return locationPoint;
    }

    public void setLocationPoint(LocationPoint locationPoint) {
        this.locationPoint = locationPoint;
    }

    public Instant getApproveAt() {
        return approveAt;
    }

    public void setApproveAt(Instant approveAt) {
        this.approveAt = approveAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "RoundTemplate{" +
                "id=" + id +
                ", source='" + source + '\'' +
                ", youtubeVideoId='" + youtubeVideoId + '\'' +
                ", startTime=" + startTime +
                ", videoLength=" + videoLength +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", locationPointId=" + locationPointId +
                ", approveAt=" + approveAt +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}