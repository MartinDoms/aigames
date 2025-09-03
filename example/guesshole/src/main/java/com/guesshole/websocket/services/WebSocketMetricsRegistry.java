package com.guesshole.websocket.services;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class WebSocketMetricsRegistry {
    private final MeterRegistry registry;
    private final Counter messagesReceived;
    private final Counter messagesSent;
    private final Counter errors;
    private final Gauge activeSessions;

    private final ConcurrentHashMap<String, AtomicInteger> sessionsByPath = new ConcurrentHashMap<>();

    public WebSocketMetricsRegistry(MeterRegistry registry) {
        this.registry = registry;

        this.messagesReceived = Counter.builder("websocket.messages.received")
                .description("Number of WebSocket messages received")
                .register(registry);

        this.messagesSent = Counter.builder("websocket.messages.sent")
                .description("Number of WebSocket messages sent")
                .register(registry);

        this.errors = Counter.builder("websocket.errors")
                .description("Number of WebSocket errors")
                .register(registry);

        this.activeSessions = Gauge.builder("websocket.sessions.active", sessionsByPath, m -> m.values().stream().mapToInt(AtomicInteger::get).sum())
                .description("Number of active WebSocket sessions")
                .register(registry);
    }

    public void recordMessageReceived(String path) {
        registry.counter("websocket.messages.received", "path", path).increment();
        messagesReceived.increment();
    }

    public void recordMessageSent(String path) {
        registry.counter("websocket.messages.sent", "path", path).increment();
        messagesSent.increment();
    }

    public void recordError(String path) {
        registry.counter("websocket.errors", "path", path).increment();
        errors.increment();
    }

    public void recordSessionConnected(String path) {
        sessionsByPath.computeIfAbsent(path, k -> new AtomicInteger()).incrementAndGet();
        registry.counter("websocket.sessions.opened", "path", path).increment();
    }

    public void recordSessionDisconnected(String path) {
        AtomicInteger count = sessionsByPath.get(path);
        if (count != null) {
            count.decrementAndGet();
        }
        registry.counter("websocket.sessions.closed", "path", path).increment();
    }
}