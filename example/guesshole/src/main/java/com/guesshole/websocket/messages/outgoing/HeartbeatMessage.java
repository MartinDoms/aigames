package com.guesshole.websocket.messages.outgoing;

import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import com.guesshole.websocket.messages.WebSocketMessage;

@JsonPropertyOrder({ "type" })
public class HeartbeatMessage implements WebSocketMessage {
    private final String type = "HEARTBEAT";
    private final long timestamp = System.currentTimeMillis();

    @Override
    public String getType() {
        return type;
    }

    public long getTimestamp() {
        return timestamp;
    }
}