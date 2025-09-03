package com.guesshole.controllers.api;

import com.guesshole.entities.Lobby;
import com.guesshole.services.LobbyService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/lobbies")
public class LobbyApiController {

    private final LobbyService lobbyService;

    public LobbyApiController(LobbyService lobbyService) {
        this.lobbyService = lobbyService;
    }

    @GetMapping("/{shortCode}")
    public Mono<Lobby> getLobbyByShortCode(@PathVariable("shortCode") String shortCode) {
        return lobbyService.findByShortCode(shortCode)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Lobby not found")));
    }
}
