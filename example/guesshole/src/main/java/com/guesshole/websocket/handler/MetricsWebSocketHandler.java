package com.guesshole.websocket.handler;

import com.guesshole.websocket.services.WebSocketMetricsRegistry;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.WebSocketSession;
import reactor.core.publisher.Mono;

public class MetricsWebSocketHandler implements WebSocketHandler {
    private final WebSocketMetricsRegistry metricsRegistry;
    private final WebSocketHandler delegate;

    public MetricsWebSocketHandler(WebSocketHandler delegate, WebSocketMetricsRegistry metricsRegistry) {
        this.delegate = delegate;
        this.metricsRegistry = metricsRegistry;
    }

    @Override
    public Mono<Void> handle(WebSocketSession session) {
        String path = session.getHandshakeInfo().getUri().getPath();
        metricsRegistry.recordSessionConnected(path);

        return delegate.handle(session)
                .doFinally(signalType -> metricsRegistry.recordSessionDisconnected(path));
    }
}