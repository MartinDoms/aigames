package com.guesshole.controllers;

import com.guesshole.entities.RoundTemplate;
import com.guesshole.repositories.RoundTemplateRepository;
import com.guesshole.services.RoundTemplateService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

/**
 * Controller for round template administration operations.
 * Provides endpoints for creating, listing, and managing round templates.
 */
@RestController
@RequestMapping("/admin/api/round-templates")
public class AdminRoundTemplateController {
    private static final Logger logger = LoggerFactory.getLogger(AdminRoundTemplateController.class);

    private final RoundTemplateService roundTemplateService;
    private final RoundTemplateRepository roundTemplateRepository;

    @Autowired
    public AdminRoundTemplateController(RoundTemplateService roundTemplateService,
                                        RoundTemplateRepository roundTemplateRepository) {
        this.roundTemplateService = roundTemplateService;
        this.roundTemplateRepository = roundTemplateRepository;
    }

    /**
     * Create a new round template.
     *
     * @param templateRequest The data for the new round template
     * @return A Mono containing the created round template
     */
    @PostMapping
    public Mono<ResponseEntity<RoundTemplate>> createTemplate(@RequestBody RoundTemplateRequest templateRequest) {
        logger.info("Creating new round template with YouTube ID: {}", templateRequest.getYoutubeVideoId());

        // Validate the request
        if (templateRequest.getYoutubeVideoId() == null || templateRequest.getYoutubeVideoId().isEmpty()) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "YouTube video ID is required"));
        }

        if (templateRequest.getLatitude() == null || templateRequest.getLongitude() == null) {
            return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Latitude and longitude are required"));
        }

        // Create and save the new template
        RoundTemplate template = new RoundTemplate();
        template.setYoutubeVideoId(templateRequest.getYoutubeVideoId());
        template.setStartTime(templateRequest.getStartTime() != null ? templateRequest.getStartTime() : 300);
        template.setVideoLength(templateRequest.getVideoLength());
        template.setLatitude(new BigDecimal(templateRequest.getLatitude()));
        template.setLongitude(new BigDecimal(templateRequest.getLongitude()));
        template.setSource(templateRequest.getSource() != null ? templateRequest.getSource() : "admin_page");

        return roundTemplateService.save(template)
                .map(savedTemplate -> ResponseEntity.status(HttpStatus.CREATED).body(savedTemplate))
                .onErrorResume(e -> {
                    logger.error("Error creating round template", e);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create round template"));
                });
    }

    /**
     * Get recent round templates.
     *
     * @param limit The maximum number of templates to return (default 5)
     * @return A Flux of round templates
     */
    @GetMapping("/recent")
    public Flux<RoundTemplate> getRecentTemplates(@RequestParam(defaultValue = "5") int limit) {
        logger.info("Fetching {} recent round templates", limit);

        return roundTemplateService.findAll()
                .sort((a, b) -> b.getCreatedAt().compareTo(a.getCreatedAt()))
                .take(limit)
                .onErrorResume(e -> {
                    logger.error("Error retrieving recent templates", e);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve recent templates"));
                });
    }

    /**
     * Get a specific round template by ID.
     *
     * @param id The ID of the template to retrieve
     * @return A Mono containing the round template
     */
    @GetMapping("/{id}")
    public Mono<RoundTemplate> getTemplateById(@PathVariable UUID id) {
        return roundTemplateService.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Round template not found")))
                .onErrorResume(e -> {
                    logger.error("Error retrieving template by ID: {}", id, e);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to retrieve template"));
                });
    }

    /**
     * Delete a round template by ID.
     *
     * @param id The ID of the template to delete
     * @return A Mono containing a success message
     */
    @DeleteMapping("/{id}")
    public Mono<ResponseEntity<Map<String, String>>> deleteTemplate(@PathVariable UUID id) {
        logger.info("Deleting round template with ID: {}", id);

        return roundTemplateService.findById(id)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Round template not found")))
                .flatMap(template -> roundTemplateRepository.delete(template)
                        .then(Mono.just(ResponseEntity.ok(Map.of("message", "Round template deleted successfully"))))
                )
                .onErrorResume(e -> {
                    logger.error("Error deleting template by ID: {}", id, e);
                    return Mono.error(new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete template"));
                });
    }

    /**
     * Request class for creating a round template.
     */
    public static class RoundTemplateRequest {
        private String youtubeVideoId;
        private Integer startTime;
        private Integer videoLength;
        private Double latitude;
        private Double longitude;
        private String source;

        // Getters and setters
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

        public Double getLatitude() {
            return latitude;
        }

        public void setLatitude(Double latitude) {
            this.latitude = latitude;
        }

        public Double getLongitude() {
            return longitude;
        }

        public void setLongitude(Double longitude) {
            this.longitude = longitude;
        }

        public String getSource() {
            return source;
        }

        public void setSource(String source) {
            this.source = source;
        }
    }
}