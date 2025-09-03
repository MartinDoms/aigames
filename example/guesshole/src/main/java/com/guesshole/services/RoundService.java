package com.guesshole.services;

import com.guesshole.entities.Round;
import com.guesshole.repositories.LocationPointRepository;
import com.guesshole.repositories.RoundRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Service
public class RoundService {
    private static final Logger log = LoggerFactory.getLogger(RoundService.class);

    private final RoundRepository roundRepository;
    private final LocationPointRepository locationPointRepository;

    public RoundService(RoundRepository roundRepository, LocationPointRepository locationPointRepository) {
        this.roundRepository = roundRepository;
        this.locationPointRepository = locationPointRepository;
    }

    public Mono<Round> findById(UUID roundId) {
        return roundRepository.findById(roundId)
            .flatMap(this::populateLocationPoint);
    }

    public Flux<Round> findByGameInstanceId(UUID gameInstanceId) {
        return roundRepository.findByGameInstanceId(gameInstanceId)
                .flatMap(this::populateLocationPoint);
    }

    private Mono<Round> populateLocationPoint(Round round) {
        if (round.getLocationPointId() == null) {
            return Mono.just(round);
        }

        return locationPointRepository.findById(round.getLocationPointId())
                .map(locationPoint -> {
                    round.setLocationPoint(locationPoint);
                    return round;
                })
                .defaultIfEmpty(round);
    }
}
