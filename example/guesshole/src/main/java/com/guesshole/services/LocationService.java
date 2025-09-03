package com.guesshole.services;

import com.guesshole.entities.LocationPoint;
import com.guesshole.repositories.LocationPointRepository;
import io.r2dbc.spi.ConnectionFactory;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Duration;

/**
 * Service for managing LocationPoint entities
 */
@Service
public class LocationService {
    private static final Logger log = LoggerFactory.getLogger(LocationService.class);

    private final LocationPointRepository locationPointRepository;
    private final ConnectionFactory connectionFactory;

    @Autowired
    public LocationService(
            LocationPointRepository locationPointRepository,
            ConnectionFactory connectionFactory
    ) {
        this.locationPointRepository = locationPointRepository;
        this.connectionFactory = connectionFactory;
    }

    /**
     * Get a location point by its ID
     * @param id the location point ID
     * @return a Mono containing the location point if found
     */
    public Mono<LocationPoint> getLocationById(Long id) {
        return locationPointRepository.findById(id);
    }

    /**
     * Find a location point by exact coordinates
     * @param latitude the latitude coordinate
     * @param longitude the longitude coordinate
     * @return a Mono containing the location point if found
     */
    public Mono<LocationPoint> findByCoordinates(Double latitude, Double longitude) {
        return locationPointRepository.findByLatitudeAndLongitude(latitude, longitude);
    }

    /**
     * Save a new location point
     * @param locationPoint the location point to save
     * @return a Mono containing the saved location point
     */
    @Transactional
    public Mono<LocationPoint> saveLocation(LocationPoint locationPoint) {
        return locationPointRepository.save(locationPoint);
    }

    /**
     * Search for location points by name across any administrative level
     * @param searchTerm the term to search for
     * @return a Flux of matching location points
     */
    public Flux<LocationPoint> searchByName(String searchTerm) {
        return locationPointRepository.searchByAnyName(searchTerm);
    }

    /**
     * Delete a location point by ID
     * @param id the ID of the location point to delete
     * @return a Mono completing when the deletion is done
     */
    @Transactional
    public Mono<Void> deleteLocation(Long id) {
        return locationPointRepository.deleteById(id);
    }

    /**
     * Query the GADM data to find administrative information for a given coordinate
     * This method connects directly to the GADM boundaries table with no repository,
     * as these queries are a bit much for r2dbc.
     *
     * @param latitude the latitude coordinate
     * @param longitude the longitude coordinate
     * @return a Mono containing a new LocationPoint with administrative info
     */
    public Mono<LocationPoint> findAdministrativeInfoForCoordinate(Double longitude, Double latitude) {
        String sql =
                "WITH point_check AS (SELECT ST_SetSRID(ST_Point($1, $2), 4326) AS point), " +
                        "location AS (" +
                        "   SELECT " +
                        "       gb.*, " +
                        "       CASE " +
                        "           WHEN gb.name_5 IS NOT NULL AND gb.name_5 <> '' THEN 5 " +
                        "           WHEN gb.name_4 IS NOT NULL AND gb.name_4 <> '' THEN 4 " +
                        "           WHEN gb.name_3 IS NOT NULL AND gb.name_3 <> '' THEN 3 " +
                        "           WHEN gb.name_2 IS NOT NULL AND gb.name_2 <> '' THEN 2 " +
                        "           WHEN gb.name_1 IS NOT NULL AND gb.name_1 <> '' THEN 1 " +
                        "           ELSE 0 " +
                        "       END AS admin_level " +
                        "   FROM " +
                        "       gadm_boundaries gb, " +
                        "       point_check " +
                        "   WHERE " +
                        "       ST_Contains(gb.geom, point_check.point) " +
                        "   ORDER BY " +
                        "       admin_level DESC " +
                        "   LIMIT 1 " +
                        ") " +
                        "SELECT " +
                        "   $1 as longitude, " +
                        "   $2 as latitude, " +
                        "   gid_0, name_0, engtype_1 as admin0_type, " +
                        "   gid_1, name_1, engtype_1 as admin1_type, " +
                        "   gid_2, name_2, engtype_2 as admin2_type, " +
                        "   gid_3, name_3, engtype_3 as admin3_type, " +
                        "   gid_4, name_4, engtype_4 as admin4_type, " +
                        "   gid_5, name_5, engtype_5 as admin5_type " +
                        "FROM location";

        return Mono.from(connectionFactory.create())
                .flatMap(connection -> {
                    log.info("GADM connection created");
                    return Mono.from(connection
                                    .createStatement(sql)
                                    .bind(0, longitude)
                                    .bind(1, latitude)
                                    .execute())
                            .flatMap(result -> {
                                return Mono.from(result.map((row, metadata) -> {
                                    LocationPoint.Builder builder = new LocationPoint.Builder()
                                        .coordinates(
                                                row.get("latitude", Double.class),
                                                row.get("longitude", Double.class)
                                        )
                                        .level0(
                                                row.get("admin0_type", String.class),
                                                row.get("name_0", String.class),
                                                row.get("gid_0", String.class)
                                        )
                                        .level1(
                                                row.get("admin1_type", String.class),
                                                row.get("name_1", String.class),
                                                row.get("gid_1", String.class)
                                        )
                                        .level2(
                                                row.get("admin2_type", String.class),
                                                row.get("name_2", String.class),
                                                row.get("gid_2", String.class)
                                        );

                                    // Handle optional fields for levels 3-5
                                    if (row.get("name_3", String.class) != null) {
                                        builder.level3(
                                            row.get("admin3_type", String.class),
                                            row.get("name_3", String.class),
                                            row.get("gid_3", String.class)
                                        );
                                    }

                                    if (row.get("name_4", String.class) != null) {
                                        builder.level4(
                                            row.get("admin4_type", String.class),
                                            row.get("name_4", String.class),
                                            row.get("gid_4", String.class)
                                        );
                                    }

                                    if (row.get("name_5", String.class) != null) {
                                        builder.level5(
                                            row.get("admin5_type", String.class),
                                            row.get("name_5", String.class),
                                            row.get("gid_5", String.class)
                                        );
                                    }

                                    return builder.build();
                                }))
                                .switchIfEmpty(Mono.defer(() -> {
                                    log.info("No administrative boundaries found for coordinates ({}, {})", longitude, latitude);
                                    return Mono.just(new LocationPoint.Builder()
                                            .coordinates(latitude, longitude)
                                            .level0("Ocean/Uninhabited", "International Waters", "INTL.WATERS")
                                            .build());
                                }));
                            })
                            .timeout(Duration.ofSeconds(10))
                            .doFinally(signalType -> {
                                Mono.from(connection.close()).subscribe();
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error querying administrative info", e);
                    return Mono.empty();
                });
    }

    /**
     * Create a new LocationPoint with administrative information
     * and save it to the database
     *
     * @param longitude the longitude
     * @param latitude the latitude
     * @return a Mono containing the saved LocationPoint
     */
    public Mono<LocationPoint> createAndSaveLocationPoint(Double longitude, Double latitude) {
        return findAdministrativeInfoForCoordinate(longitude, latitude)
                .flatMap(locationPoint -> {
                    Mono<LocationPoint> result = locationPointRepository.save(locationPoint)
                            .onErrorResume(e -> {
                                log.error("Error saving location point: {}", e.getMessage(), e);
                                return Mono.empty();
                            });
                    log.info("Finished creating location point save mono");
                    return result;
                });
    }
}