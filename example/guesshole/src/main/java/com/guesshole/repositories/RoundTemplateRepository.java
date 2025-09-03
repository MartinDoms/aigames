package com.guesshole.repositories;

import com.guesshole.entities.RoundTemplate;
import org.springframework.data.domain.Pageable;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Repository for RoundTemplate entities.
 * Provides reactive operations for interacting with round templates in the database.
 */
@Repository
public interface RoundTemplateRepository extends ReactiveCrudRepository<RoundTemplate, UUID> {

    /**
     * Find round templates by source.
     *
     * @param source The source of the round templates
     * @return A Flux of RoundTemplate entities matching the source
     */
    Flux<RoundTemplate> findBySource(String source);

    /**
     * Find round templates by YouTube video ID.
     *
     * @param youtubeVideoId The YouTube video ID
     * @return A Flux of RoundTemplate entities matching the YouTube video ID
     */
    Flux<RoundTemplate> findByYoutubeVideoId(String youtubeVideoId);

    /**
     * Find round templates within a geographical bounding box.
     *
     * @param minLat The minimum latitude of the bounding box
     * @param maxLat The maximum latitude of the bounding box
     * @param minLng The minimum longitude of the bounding box
     * @param maxLng The maximum longitude of the bounding box
     * @return A Flux of RoundTemplate entities within the specified bounding box
     */
    @Query("SELECT * FROM round_template WHERE latitude BETWEEN :minLat AND :maxLat AND longitude BETWEEN :minLng AND :maxLng")
    Flux<RoundTemplate> findWithinBounds(
            @Param("minLat") BigDecimal minLat,
            @Param("maxLat") BigDecimal maxLat,
            @Param("minLng") BigDecimal minLng,
            @Param("maxLng") BigDecimal maxLng
    );

    /**
     * Select a specific number of random RoundTemplates.
     * This method ensures we get exactly the number requested or fewer if not enough exist.
     *
     * @param numRounds The exact number of round templates to return
     * @return A Flux of randomly selected RoundTemplate entities
     */
    @Query("SELECT * FROM round_template ORDER BY RANDOM() LIMIT :numRounds")
    Flux<RoundTemplate> selectRandomTemplates(@Param("numRounds") int numRounds);

    /**
     * Count round templates by source.
     *
     * @param source The source to count templates for
     * @return A Mono with the count of templates for the specified source
     */
    Mono<Long> countBySource(String source);

    /**
     * Find all round templates with pagination support.
     *
     * @param pageable The pagination information
     * @return A Flux of paginated RoundTemplate entities
     */
    Flux<RoundTemplate> findAllBy(Pageable pageable);

    /**
     * Find templates where approveAt is null or older than (before) the cutoff date.
     *
     * @param cutoffDate The date to filter by
     * @param pageable The pagination information
     * @return A Flux of RoundTemplate entities based on approval criteria
     */
    @Query("SELECT * FROM round_template WHERE approve_at IS NULL OR approve_at < :cutoffDate ORDER BY created_at DESC LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<RoundTemplate> findByApproveAtIsNullOrApproveAtBefore(@Param("cutoffDate") Instant cutoffDate, Pageable pageable);

    /**
     * Count templates where approveAt is null or older than (before) the cutoff date.
     *
     * @param cutoffDate The date to filter by
     * @return A Mono with the count
     */
    @Query("SELECT COUNT(*) FROM round_template WHERE approve_at IS NULL OR approve_at < :cutoffDate")
    Mono<Long> countByApproveAtIsNullOrApproveAtBefore(@Param("cutoffDate") Instant cutoffDate);

    /**
     * Find templates where latitude or longitude is null.
     *
     * @param pageable The pagination information
     * @return A Flux of RoundTemplate entities without coordinates
     */
    @Query("SELECT * FROM round_template WHERE latitude IS NULL OR longitude IS NULL ORDER BY created_at DESC LIMIT :#{#pageable.pageSize} OFFSET :#{#pageable.offset}")
    Flux<RoundTemplate> findByCoordinatesNull(Pageable pageable);

    /**
     * Count templates where latitude or longitude is null.
     *
     * @return A Mono with the count
     */
    @Query("SELECT COUNT(*) FROM round_template WHERE latitude IS NULL OR longitude IS NULL")
    Mono<Long> countByCoordinatesNull();

    /**
     * Update the coordinates for a template.
     *
     * @param id The ID of the template to update
     * @param latitude The new latitude value
     * @param longitude The new longitude value
     * @return A Mono containing the number of rows affected
     */
    @Query("UPDATE round_template SET latitude = :latitude, longitude = :longitude, updated_at = NOW() WHERE id = :id")
    Mono<Integer> updateCoordinates(
            @Param("id") UUID id,
            @Param("latitude") BigDecimal latitude,
            @Param("longitude") BigDecimal longitude
    );
}