package com.guesshole.controllers;

import com.guesshole.dto.LobbyDto;
import com.guesshole.entities.Lobby;
import com.guesshole.entities.Player;
import com.guesshole.repositories.LobbyRepository;
import com.guesshole.repositories.PlayerRepository;
import com.guesshole.services.LobbyService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Controller
@RequestMapping("/lobbies")
public class LobbyController {

    private static final Logger log = LoggerFactory.getLogger(LobbyController.class);
    private final LobbyRepository lobbyRepository;
    private final PlayerRepository playerRepository;
    private final LobbyService lobbyService;

    @Autowired
    public LobbyController(LobbyRepository lobbyRepository, PlayerRepository playerRepository, LobbyService lobbyService) {
        this.lobbyRepository = lobbyRepository;
        this.playerRepository = playerRepository;
        this.lobbyService = lobbyService;
    }

    @GetMapping("/new")
    public String newLobbyForm(Model model) {
        model.addAttribute("lobbyDto", new LobbyDto());
        return "/pages/lobbies/new";
    }

    /**
     * Create a solo game lobby automatically and redirect user directly to it
     */
    @GetMapping("/solo")
    public Mono<String> createSoloLobby() {
        log.info("Creating new solo lobby");

        // Create lobby with default name "Solo Game"
        Lobby lobby = new Lobby("Solo Game", "private");
        lobby.setId(UUID.randomUUID());

        return lobbyRepository.insertWithGeneratedShortCode(lobby)
                .doOnSuccess(savedLobby -> log.info("Solo lobby saved successfully: {}", savedLobby))
                .doOnError(e -> log.error("Error saving solo lobby: {}", e.getMessage(), e))
                .flatMap(savedLobby -> {
                    // Create default player
                    Player soloPlayer = new Player("Player", savedLobby.getId(), true);
                    soloPlayer.setAvatar("avatar4"); // Set default avatar

                    return playerRepository.save(soloPlayer)
                            .doOnSuccess(player -> log.info("Solo player saved successfully: {}", player))
                            .doOnError(e -> log.error("Error saving solo player: {}", e.getMessage(), e))
                            .map(savedPlayer -> {
                                log.info("Created solo lobby: {} with player: {}", savedLobby, savedPlayer);

                                // Redirect to the lobby page
                                String shortCode = savedLobby.getShortCode() != null ?
                                        savedLobby.getShortCode() :
                                        savedLobby.getId().toString();

                                return "redirect:/lobbies/" + shortCode;
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error in createSoloLobby: {}", e.getMessage(), e);
                    return Mono.just("redirect:/lobbies/new");
                });
    }

    @PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
    @ResponseBody
    public Mono<String> createLobby(ServerWebExchange exchange) {
        return exchange.getFormData()
                .flatMap(formData -> {
                    // Extract data from form
                    String lobbyName = formData.getFirst("name");
                    String privacy = formData.getFirst("privacy");
                    String playerName = formData.getFirst("playerName");
                    String playerAvatar = formData.getFirst("playerAvatar");

                    log.info("Creating new lobby: {} with host player: {} (avatar: {})", lobbyName, playerName, playerAvatar);

                    if (lobbyName == null || lobbyName.isEmpty() ||
                            playerName == null || playerName.isEmpty()) {
                        log.warn("Missing required form data: lobbyName={}, playerName={}", lobbyName, playerName);
                        return Mono.error(new IllegalArgumentException("Missing required form fields"));
                    }

                    // Create lobby
                    Lobby lobby = new Lobby(lobbyName, privacy);
                    lobby.setId(UUID.randomUUID());

                    return lobbyRepository.insertWithGeneratedShortCode(lobby)
                            .doOnSuccess(savedLobby -> log.info("Lobby saved successfully: {}", savedLobby))
                            .doOnError(e -> log.error("Error saving lobby: {}", e.getMessage(), e))
                            .flatMap(savedLobby -> {
                                // Create host player
                                Player hostPlayer = new Player(playerName, savedLobby.getId(), true);
                                if (playerAvatar != null && !playerAvatar.isEmpty()) {
                                    hostPlayer.setAvatar(playerAvatar);
                                }
                                return playerRepository.save(hostPlayer)
                                        .doOnSuccess(player -> log.info("Player saved successfully: {}", player))
                                        .doOnError(e -> log.error("Error saving player: {}", e.getMessage(), e))
                                        .map(savedPlayer -> {
                                            log.info("Created lobby: {} with host player: {}", savedLobby, savedPlayer);
                                            // Check if shortCode is null
                                            if (savedLobby.getShortCode() == null) {
                                                log.warn("Lobby saved but shortCode is null: {}", savedLobby);
                                            }
                                            // Return both IDs (shortCode|playerId)
                                            return (savedLobby.getShortCode() != null ? savedLobby.getShortCode() : savedLobby.getId().toString())
                                                    + "|" + savedPlayer.getId().toString();
                                        });
                            });
                })
                .onErrorResume(e -> {
                    log.error("Error in createLobby: {}", e.getMessage(), e);
                    // Return a more informative error message for debugging
                    return Mono.just("ERROR: " + e.getClass().getSimpleName() + " - " + e.getMessage());
                });
    }

    /**
     * Handle /lobbies/{shortCode} - Look up lobby by short code
     */
    @GetMapping("/{shortCode}")
    public Mono<String> viewLobbyByShortCode(@PathVariable("shortCode") String shortCode, Model model) {
        log.info("Looking up lobby with short code: {}", shortCode);

        return lobbyRepository.findByShortCode(shortCode)
                .map(lobby -> {
                    model.addAttribute("id", lobby.getId());
                    model.addAttribute("lobby", lobby);
                    model.addAttribute("shortCode", shortCode);
                    return "/pages/lobbies/view";
                })
                .defaultIfEmpty("redirect:/lobbies/new");
    }

    /**
     * Handle legacy UUID-based URLs - redirect to short code URL
     * This is a temporary method to handle transition from UUID to short codes
     */
    @GetMapping("/id/{id}")
    public Mono<String> viewLobbyByUuid(@PathVariable("id") String id) {
        UUID lobbyId;
        try {
            lobbyId = UUID.fromString(id);
        } catch (IllegalArgumentException e) {
            log.warn("Invalid lobby ID format: {}", id);
            return Mono.just("redirect:/lobbies/new");
        }

        log.info("Looking up lobby with legacy UUID: {}, will redirect to short code URL", lobbyId);

        return lobbyRepository.findById(lobbyId)
                .map(lobby -> "redirect:/lobbies/" + lobby.getShortCode())
                .defaultIfEmpty("redirect:/lobbies/new");
    }

}