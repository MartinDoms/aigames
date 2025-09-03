package com.guesshole.repositories;

import com.guesshole.entities.LocationPoint;
import org.springframework.data.r2dbc.repository.Query;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;

@Repository
public interface LocationPointRepository extends ReactiveCrudRepository<LocationPoint, Long> {

    /**
     * Find location point by exact coordinates
     * @param latitude the latitude value
     * @param longitude the longitude value
     * @return a mono of the location point if found
     */
    Mono<LocationPoint> findByLatitudeAndLongitude(Double latitude, Double longitude);

    /**
     * Search for location points where any administrative name contains the search term
     * @param searchTerm the term to search for
     * @return a flux of matching location points
     */
    @Query("SELECT * FROM location_points WHERE " +
            "admin0_name ILIKE CONCAT('%', :searchTerm, '%') OR " +
            "admin1_name ILIKE CONCAT('%', :searchTerm, '%') OR " +
            "admin2_name ILIKE CONCAT('%', :searchTerm, '%') OR " +
            "admin3_name ILIKE CONCAT('%', :searchTerm, '%') OR " +
            "admin4_name ILIKE CONCAT('%', :searchTerm, '%') OR " +
            "admin5_name ILIKE CONCAT('%', :searchTerm, '%')")
    Flux<LocationPoint> searchByAnyName(String searchTerm);

    /**
     * Find location points by IDs.
     *
     * @param ids Collection of location point IDs
     * @return Flux of LocationPoint entities with matching IDs
     */
    Flux<LocationPoint> findAllByIdIn(Collection<Long> ids);
}