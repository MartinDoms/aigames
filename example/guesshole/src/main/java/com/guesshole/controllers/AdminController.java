package com.guesshole.controllers;

import com.guesshole.entities.RoundTemplate;
import com.guesshole.repositories.RoundTemplateRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.Set;
import java.util.UUID;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final Logger log = LoggerFactory.getLogger(AdminController.class);
    private final RoundTemplateRepository roundTemplateRepository;

    @Autowired
    public AdminController(RoundTemplateRepository roundTemplateRepository) {
        this.roundTemplateRepository = roundTemplateRepository;
    }

    @GetMapping("/videos")
    public String videoList(Model model,
                            @RequestParam(defaultValue = "0") int page,
                            @RequestParam(defaultValue = "20") int size,
                            @RequestParam(required = false) String filter) {
        model.addAttribute("currentPage", page);
        model.addAttribute("pageSize", size);
        model.addAttribute("filter", filter);
        return "pages/admin/videos";
    }


    @GetMapping(value = "/api/videos", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Mono<VideoPageResponse> getVideosPage(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String filter,
            @RequestParam(defaultValue = "created_at") String sort,
            @RequestParam(defaultValue = "DESC") String direction) {

        // Validate and sanitize the sort field
        String sortField = validateSortField(sort);

        // Validate sort direction
        Sort.Direction sortDirection = "ASC".equalsIgnoreCase(direction) ?
                Sort.Direction.ASC : Sort.Direction.DESC;

        // Create a page request with specified sorting
        PageRequest pageRequest = PageRequest.of(
                page,
                size,
                Sort.by(sortDirection, sortField)
        );

        // Default to recent approvals filter if none is specified
        String activeFilter = filter != null ? filter : "recent";

        if ("recent".equals(activeFilter)) {
            // Existing code for recent filter...
            Instant sixMonthsAgo = ZonedDateTime.now(ZoneOffset.UTC)
                    .minusMonths(6)
                    .toInstant();


            return roundTemplateRepository.findByApproveAtIsNullOrApproveAtBefore(sixMonthsAgo, pageRequest)
                    .collectList()
                    .zipWith(roundTemplateRepository.countByApproveAtIsNullOrApproveAtBefore(sixMonthsAgo))
                    .map(tuple -> {
                        var videos = tuple.getT1();
                        var total = tuple.getT2();
                        log.info("Found {} videos with the filter", total);
                        return new VideoPageResponse(videos, page, size, total, activeFilter, sortField, sortDirection.toString());
                    });
        } else if ("no-coordinates".equals(activeFilter)) {
            // New code for no-coordinates filter
            log.info("Filtering videos without coordinates, sorting by {} {}", sortField, sortDirection);

            return roundTemplateRepository.findByCoordinatesNull(pageRequest)
                    .collectList()
                    .zipWith(roundTemplateRepository.countByCoordinatesNull())
                    .map(tuple -> {
                        var videos = tuple.getT1();
                        var total = tuple.getT2();
                        log.info("Found {} videos without coordinates", total);
                        return new VideoPageResponse(videos, page, size, total, activeFilter, sortField, sortDirection.toString());
                    });
        } else {
            // Return all templates regardless of approval status
            log.info("Getting all videos, sorting by {} {}", sortField, sortDirection);

            return roundTemplateRepository.findAllBy(pageRequest)
                    .collectList()
                    .zipWith(roundTemplateRepository.count())
                    .map(tuple -> {
                        var videos = tuple.getT1();
                        var total = tuple.getT2();
                        return new VideoPageResponse(videos, page, size, total, activeFilter, sortField, sortDirection.toString());
                    });
        }
    }

    // Add a new endpoint to update coordinates
    @PostMapping(value = "/api/videos/{id}/update-coordinates", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Mono<ResponseEntity<ApiResponse>> updateCoordinates(
            @PathVariable UUID id,
            @RequestBody CoordinatesRequest request) {

        log.info("Updating coordinates for template with ID: {}, lat: {}, lng: {}",
                id, request.getLatitude(), request.getLongitude());

        return roundTemplateRepository.updateCoordinates(id, request.getLatitude(), request.getLongitude())
                .map(rowsAffected -> {
                    if (rowsAffected > 0) {
                        log.info("Successfully updated coordinates for template with ID: {}", id);
                        return ResponseEntity.ok(new ApiResponse("Coordinates updated successfully", true));
                    } else {
                        log.warn("No template found with ID: {}", id);
                        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                                .body(new ApiResponse("Template not found", false));
                    }
                })
                .defaultIfEmpty(ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(new ApiResponse("Template not found", false)))
                .onErrorResume(e -> {
                    log.error("Error updating coordinates for template with ID: {}", id, e);
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new ApiResponse("Error updating coordinates: " + e.getMessage(), false)));
                });
    }

    // Helper method to validate sort field
    private String validateSortField(String sortField) {
        // List of allowed sort fields
        Set<String> allowedFields = Set.of(
                "created_at", "updated_at", "approve_at", "video_length"
        );

        // If the requested sort field is not allowed, default to created_at
        if (!allowedFields.contains(sortField)) {
            log.warn("Invalid sort field requested: {}, defaulting to created_at", sortField);
            return "created_at";
        }

        return sortField;
    }

    @PostMapping(value = "/api/videos/{id}/approve", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Mono<ResponseEntity<ApiResponse>> approveVideo(@PathVariable UUID id) {
        log.info("Approving video template with ID: {}", id);

        return roundTemplateRepository.findById(id)
                .flatMap(template -> {
                    template.setApproveAt(Instant.now());
                    return roundTemplateRepository.save(template);
                })
                .map(savedTemplate -> {
                    log.info("Successfully approved video template with ID: {}", id);
                    return ResponseEntity.ok(new ApiResponse("Video approved successfully", true));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @DeleteMapping(value = "/api/videos/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Mono<ResponseEntity<ApiResponse>> deleteVideo(@PathVariable UUID id) {
        log.info("Deleting video template with ID: {}", id);

        return roundTemplateRepository.findById(id)
                .flatMap(template -> roundTemplateRepository.delete(template)
                        .then(Mono.just(ResponseEntity.ok(new ApiResponse("Video deleted successfully", true))))
                )
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(e -> {
                    log.error("Error deleting video template with ID: {}", id, e);
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new ApiResponse("Error deleting video: " + e.getMessage(), false)));
                });
    }

    @PostMapping(value = "/api/videos/{id}/update-length", produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseBody
    public Mono<ResponseEntity<ApiResponse>> updateVideoLength(
            @PathVariable UUID id,
            @RequestBody VideoLengthRequest request) {

        log.info("Updating video length for template with ID: {}, length: {}", id, request.getVideoLength());

        return roundTemplateRepository.findById(id)
                .flatMap(template -> {
                    template.setVideoLength(request.getVideoLength());
                    return roundTemplateRepository.save(template);
                })
                .map(savedTemplate -> {
                    log.info("Successfully updated video length for template with ID: {}", id);
                    return ResponseEntity.ok(new ApiResponse("Video length updated successfully", true));
                })
                .defaultIfEmpty(ResponseEntity.notFound().build())
                .onErrorResume(e -> {
                    log.error("Error updating video length for template with ID: {}", id, e);
                    return Mono.just(ResponseEntity
                            .status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new ApiResponse("Error updating video length: " + e.getMessage(), false)));
                });
    }

    // Request class for coordinates updates
    public static class CoordinatesRequest {
        private BigDecimal latitude;
        private BigDecimal longitude;

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
    }

    // Request class for video length updates
    public static class VideoLengthRequest {
        private Integer videoLength;

        public Integer getVideoLength() {
            return videoLength;
        }

        public void setVideoLength(Integer videoLength) {
            this.videoLength = videoLength;
        }
    }

    // Response class for pagination data
    public static class VideoPageResponse {
        private final Iterable<RoundTemplate> content;
        private final int pageNumber;
        private final int pageSize;
        private final long totalElements;
        private final int totalPages;
        private final String filter;
        private final String sortField;
        private final String sortDirection;

        public VideoPageResponse(
                Iterable<RoundTemplate> content,
                int pageNumber,
                int pageSize,
                long totalElements,
                String filter,
                String sortField,
                String sortDirection) {

            this.content = content;
            this.pageNumber = pageNumber;
            this.pageSize = pageSize;
            this.totalElements = totalElements;
            this.totalPages = pageSize > 0 ? (int) Math.ceil((double) totalElements / pageSize) : 0;
            this.filter = filter;
            this.sortField = sortField;
            this.sortDirection = sortDirection;
        }

        public Iterable<RoundTemplate> getContent() {
            return content;
        }

        public int getPageNumber() {
            return pageNumber;
        }

        public int getPageSize() {
            return pageSize;
        }

        public long getTotalElements() {
            return totalElements;
        }

        public int getTotalPages() {
            return totalPages;
        }

        public String getFilter() {
            return filter;
        }

        public String getSortField() {
            return sortField;
        }

        public String getSortDirection() {
            return sortDirection;
        }
    }

    // Simple API response class
    public static class ApiResponse {
        private final String message;
        private final boolean success;

        public ApiResponse(String message, boolean success) {
            this.message = message;
            this.success = success;
        }

        public String getMessage() {
            return message;
        }

        public boolean isSuccess() {
            return success;
        }
    }
}