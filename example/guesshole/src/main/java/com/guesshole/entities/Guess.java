package com.guesshole.entities;

import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Table("guesses")
public class Guess {

    @Id
    private UUID id;

    @Column("player_id")
    private UUID playerId;

    @Column("round_id")
    private UUID roundId;

    @Column("game_instance_id")
    private UUID gameInstanceId;

    @Column("latitude")
    private BigDecimal latitude;

    @Column("longitude")
    private BigDecimal longitude;

    @Column("location_point_id")
    private Long locationPointId;

    @Transient
    private LocationPoint locationPoint;

    @Column("distance_km")
    private Double distanceKm;

    @Column("base_score")
    private Integer baseScore;

    @Column("score")
    private Integer score;

    @Transient
    private List<ScoreMultiplier> scoreMultipliers;

    @Column("round_duration")
    private Integer roundDuration;

    @Column("guess_time")
    private Integer guessTime;

    @Column("timestamp")
    private Instant timestamp;

    // Default constructor
    public Guess() {
        this.timestamp = Instant.now();
    }

    // Constructor with fields
    public Guess(UUID playerId,
                 UUID roundId,
                 UUID gameInstanceId,
                 BigDecimal latitude,
                 BigDecimal longitude,
                 Long locationPointId,
                 Double distanceKm,
                 Integer baseScore,
                 Integer score,
                 List<ScoreMultiplier> scoreMultipliers,
                 Integer roundDuration,
                 Integer guessTime) {
        this.playerId = playerId;
        this.roundId = roundId;
        this.gameInstanceId = gameInstanceId;
        this.latitude = latitude;
        this.longitude = longitude;
        this.locationPointId = locationPointId;
        this.distanceKm = distanceKm;
        this.baseScore = baseScore;
        this.score = score;
        this.scoreMultipliers = scoreMultipliers;
        this.roundDuration = roundDuration;
        this.guessTime = guessTime;
        this.timestamp = Instant.now();
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getPlayerId() {
        return playerId;
    }

    public void setPlayerId(UUID playerId) {
        this.playerId = playerId;
    }

    public UUID getRoundId() {
        return roundId;
    }

    public void setRoundId(UUID roundId) {
        this.roundId = roundId;
    }

    public UUID getGameInstanceId() {
        return gameInstanceId;
    }

    public void setGameInstanceId(UUID gameInstanceId) {
        this.gameInstanceId = gameInstanceId;
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

    public Double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(Double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getRoundDuration() {
        return roundDuration;
    }

    public void setRoundDuration(Integer roundDuration) {
        this.roundDuration = roundDuration;
    }

    public Integer getGuessTime() {
        return guessTime;
    }

    public void setGuessTime(Integer guessTime) {
        this.guessTime = guessTime;
    }

    public Instant getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Instant timestamp) {
        this.timestamp = timestamp;
    }

    public List<ScoreMultiplier> getScoreMultipliers() {
        return scoreMultipliers;
    }

    public void setScoreMultipliers(List<ScoreMultiplier> scoreMultipliers) {
        this.scoreMultipliers = scoreMultipliers;
    }

    public Integer getBaseScore() {
        return baseScore;
    }

    public void setBaseScore(Integer baseScore) {
        this.baseScore = baseScore;
    }

    /**
     * Get the country name from the associated LocationPoint, if available
     * @return The country name or null if LocationPoint is not loaded
     */
    public String getCountryName() {
        return locationPoint != null ? locationPoint.getAdmin0Name() : null;
    }

    /**
     * Get the state/province name from the associated LocationPoint, if available
     * @return The state/province name or null if LocationPoint is not loaded
     */
    public String getStateName() {
        return locationPoint != null ? locationPoint.getAdmin1Name() : null;
    }

    /**
     * Get the city/district name from the associated LocationPoint, if available
     * This returns the most specific locality available (admin2 through admin5)
     * @return The city/district name or null if LocationPoint is not loaded
     */
    public String getCityName() {
        if (locationPoint == null) {
            return null;
        }
        return locationPoint.getMostSpecificName();
    }

    /**
     * Get a formatted display of the location hierarchy
     * @return A string like "City, State, Country" or null if LocationPoint is not loaded
     */
    public String getFormattedLocation() {
        return locationPoint != null ? locationPoint.getFormattedLocation() : null;
    }
}