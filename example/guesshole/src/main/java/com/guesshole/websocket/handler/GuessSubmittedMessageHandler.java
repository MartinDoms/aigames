package com.guesshole.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guesshole.entities.Round;
import com.guesshole.services.GameStateService;
import com.guesshole.services.GuessService;
import com.guesshole.services.RoundService;
import com.guesshole.websocket.messages.incoming.GuessSubmittedMessage;
import com.guesshole.websocket.services.WebSocketSessionService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Handler for GUESS_SUBMITTED messages
 */
@Component
public class GuessSubmittedMessageHandler implements MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(GuessSubmittedMessageHandler.class);
    private static final String MESSAGE_TYPE = "GUESS_SUBMITTED";

    private final ObjectMapper objectMapper;
    private final WebSocketSessionService sessionService;
    private final GuessService guessService;
    private final GameStateService gameStateService;
    private final RoundService roundService;

    public GuessSubmittedMessageHandler(
            ObjectMapper objectMapper,
            WebSocketSessionService sessionService,
            GuessService guessService, GameStateService gameStateService, RoundService roundService) {
        this.objectMapper = objectMapper;
        this.sessionService = sessionService;
        this.guessService = guessService;
        this.gameStateService = gameStateService;
        this.roundService = roundService;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public Mono<Void> handle(JsonNode payload, WebSocketSession session, UUID lobbyId) {
        try {
            // Parse the message
            GuessSubmittedMessage message = objectMapper.treeToValue(payload, GuessSubmittedMessage.class);

            log.info("Received guess from session {} in lobby {}: lat={}, lng={}, roundId={}, roundDuration={}, guessTime={}",
                    session.getId(), lobbyId,
                    message.getLatitude(),
                    message.getLongitude(),
                    message.getRoundId(),
                    message.getRoundDuration(),
                    message.getGuessTime());

            // Get the player ID associated with this session
            UUID playerId = sessionService.getPlayerIdForSession(session);
            if (playerId == null) {
                log.warn("GUESS_SUBMITTED message received from session with no player ID");
                return Mono.empty();
            }

            log.info("Processing guess from player {}", playerId);

            // Process the guess with timing information
            Mono<Void> processGuessMono = guessService.processGuess(
                    playerId,
                    message.getRoundId(),
                    message.getLatitude(),
                    message.getLongitude(),
                    message.getRoundDuration(),
                    message.getGuessTime(),
                    session
            );

            Mono<Round> roundMono = roundService.findById(message.getRoundId());
            Mono<Void> playerGuessCheckMono = roundMono.flatMap(round -> gameStateService.checkAllPlayersGuessed(lobbyId, round));

            return processGuessMono.then(playerGuessCheckMono);
        } catch (Exception e) {
            log.error("Error processing GUESS_SUBMITTED message", e);
            return Mono.error(e);
        }
    }
}