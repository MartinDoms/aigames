package com.guesshole.websocket.handler;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.guesshole.entities.Round;
import com.guesshole.services.GameStateService;
import com.guesshole.services.LobbyService;
import com.guesshole.services.RoundService;
import com.guesshole.websocket.messages.incoming.StartGameMessage;
import com.guesshole.websocket.messages.incoming.StartNextRoundMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

import java.util.UUID; /**
 * Handler for HEARTBEAT_ACK messages
 */
@Component
public class StartNextRoundMessageHandler implements MessageHandler {
    private static final Logger log = LoggerFactory.getLogger(StartNextRoundMessageHandler.class);
    private static final String MESSAGE_TYPE = "START_NEXT_ROUND";
    private final GameStateService gameStateService;
    private final RoundService roundService;
    private final LobbyService lobbyService;
    private final ObjectMapper objectMapper;

    public StartNextRoundMessageHandler(
            GameStateService gameStateService,
            RoundService roundService,
            LobbyService lobbyService,
            ObjectMapper objectMapper
    ) {
        this.gameStateService = gameStateService;
        this.roundService = roundService;
        this.lobbyService = lobbyService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String getMessageType() {
        return MESSAGE_TYPE;
    }

    @Override
    public Mono<Void> handle(JsonNode payload, WebSocketSession session, UUID lobbyId) {
        try {
            StartNextRoundMessage message = objectMapper.treeToValue(payload, StartNextRoundMessage.class);

            return roundService.findById(message.getCurrentRoundId())
                    .flatMap(round -> this.gameStateService.advanceToNextRound(lobbyId, round));

        }  catch (Exception e) {
            log.error("Error processing START_NEXT_ROUND message", e);
            return Mono.error(e);
        }
    }
}
