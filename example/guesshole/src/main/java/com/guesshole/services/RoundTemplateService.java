package com.guesshole.services;

import com.guesshole.entities.LocationPoint;
import com.guesshole.entities.RoundTemplate;
import com.guesshole.repositories.LocationPointRepository;
import com.guesshole.repositories.RoundTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Service class for RoundTemplate operations.
 * Ensures that location point data is always populated when round templates are fetched.
 */
@Service
public class RoundTemplateService {
    private static final Logger logger = LoggerFactory.getLogger(RoundTemplateService.class);

    private final RoundTemplateRepository roundTemplateRepository;
    private final LocationPointRepository locationPointRepository;

    @Autowired
    public RoundTemplateService(RoundTemplateRepository roundTemplateRepository,
                                LocationPointRepository locationPointRepository) {
        this.roundTemplateRepository = roundTemplateRepository;
        this.locationPointRepository = locationPointRepository;
    }

    /**
     * Find a round template by ID and populate its location point.
     *
     * @param id The ID of the round template
     * @return A Mono containing the round template with populated location point
     */
    public Mono<RoundTemplate> findById(UUID id) {
        return roundTemplateRepository.findById(id)
                .flatMap(this::populateLocationPoint);
    }

    /**
     * Get all round templates with populated location points.
     *
     * @return A Flux of round templates with populated location points
     */
    public Flux<RoundTemplate> findAll() {
        return roundTemplateRepository.findAll()
                .flatMap(this::populateLocationPoint);
    }

    /**
     * Get all round templates with pagination and populated location points.
     *
     * @param pageable The pagination information
     * @return A Flux of paginated round templates with populated location points
     */
    public Flux<RoundTemplate> findAllPaginated(Pageable pageable) {
        return roundTemplateRepository.findAllBy(pageable)
                .collectList()
                .flatMapMany(this::populateLocationPoints);
    }

    /**
     * Find round templates by source and populate location points.
     *
     * @param source The source to filter by
     * @return A Flux of round templates with populated location points
     */
    public Flux<RoundTemplate> findBySource(String source) {
        return roundTemplateRepository.findBySource(source)
                .collectList()
                .flatMapMany(this::populateLocationPoints);
    }

    /**
     * Find round templates by YouTube video ID and populate location points.
     *
     * @param youtubeVideoId The YouTube video ID to filter by
     * @return A Flux of round templates with populated location points
     */
    public Flux<RoundTemplate> findByYoutubeVideoId(String youtubeVideoId) {
        return roundTemplateRepository.findByYoutubeVideoId(youtubeVideoId)
                .collectList()
                .flatMapMany(this::populateLocationPoints);
    }

    /**
     * Find round templates within a geographical bounding box and populate location points.
     *
     * @param minLat The minimum latitude of the bounding box
     * @param maxLat The maximum latitude of the bounding box
     * @param minLng The minimum longitude of the bounding box
     * @param maxLng The maximum longitude of the bounding box
     * @return A Flux of round templates within the specified bounding box with populated location points
     */
    public Flux<RoundTemplate> findWithinBounds(BigDecimal minLat, BigDecimal maxLat,
                                                BigDecimal minLng, BigDecimal maxLng) {
        return roundTemplateRepository.findWithinBounds(minLat, maxLat, minLng, maxLng)
                .collectList()
                .flatMapMany(this::populateLocationPoints);
    }

    /**
     * Select a specific number of random round templates and populate location points.
     *
     * @param numRounds The exact number of round templates to return
     * @return A Flux of randomly selected round templates with populated location points
     */
    public Flux<RoundTemplate> getRandomTemplates(int numRounds) {
        return roundTemplateRepository.selectRandomTemplates(numRounds)
                .collectList()
                .flatMapMany(this::populateLocationPoints);
    }

    /**
     * Find templates where approveAt is null or older than the cutoff date and populate location points.
     *
     * @param cutoffDate The date to filter by
     * @param pageable The pagination information
     * @return A Flux of round templates based on approval criteria with populated location points
     */
    public Flux<RoundTemplate> findByApproveAtIsNullOrApproveAtBefore(Instant cutoffDate, Pageable pageable) {
        return roundTemplateRepository.findByApproveAtIsNullOrApproveAtBefore(cutoffDate, pageable)
                .collectList()
                .flatMapMany(this::populateLocationPoints);
    }

    /**
     * Save a round template.
     *
     * @param roundTemplate The round template to save
     * @return A Mono containing the saved round template with populated location point
     */
    public Mono<RoundTemplate> save(RoundTemplate roundTemplate) {
        return roundTemplateRepository.save(roundTemplate)
                .flatMap(this::populateLocationPoint);
    }

    /**
     * Populate the location point for a single round template.
     *
     * @param template The round template to populate
     * @return A Mono containing the round template with populated location point
     */
    private Mono<RoundTemplate> populateLocationPoint(RoundTemplate template) {
        if (template.getLocationPointId() == null) {
            return Mono.just(template);
        }

        return locationPointRepository.findById(template.getLocationPointId())
                .doOnNext(template::setLocationPoint)
                .thenReturn(template)
                .defaultIfEmpty(template);
    }

    /**
     * Populate location points for a list of round templates in a batch operation.
     *
     * @param templates The list of round templates to populate
     * @return A Flux of round templates with populated location points
     */
    private Flux<RoundTemplate> populateLocationPoints(List<RoundTemplate> templates) {
        // Extract locationPointIds that are not null
        List<Long> locationPointIds = templates.stream()
                .map(RoundTemplate::getLocationPointId)
                .filter(id -> id != null)
                .collect(Collectors.toList());

        if (locationPointIds.isEmpty()) {
            return Flux.fromIterable(templates);
        }

        // Fetch all location points in one batch
        return locationPointRepository.findAllByIdIn(locationPointIds)
                .collectMap(LocationPoint::getId, Function.identity())
                .flatMapMany(locationPointMap -> {
                    // Assign location points to templates
                    templates.forEach(template -> {
                        Long locationPointId = template.getLocationPointId();
                        if (locationPointId != null) {
                            LocationPoint locationPoint = locationPointMap.get(locationPointId);
                            template.setLocationPoint(locationPoint);
                        }
                    });
                    return Flux.fromIterable(templates);
                });
    }
}