package com.guesshole.config;

import com.guesshole.websocket.handler.MetricsWebSocketHandler;
import com.guesshole.websocket.services.WebSocketMetricsRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.HandlerMapping;
import org.springframework.web.reactive.handler.SimpleUrlHandlerMapping;
import org.springframework.web.reactive.socket.WebSocketHandler;
import org.springframework.web.reactive.socket.server.support.WebSocketHandlerAdapter;
import reactor.netty.tcp.TcpServer;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Configuration
public class WebSocketConfig {

    private final WebSocketHandler lobbyWebSocketHandler;

    public WebSocketConfig(WebSocketHandler lobbyWebSocketHandler) {
        this.lobbyWebSocketHandler = lobbyWebSocketHandler;
    }

    @Bean
    public HandlerMapping webSocketHandlerMapping(MeterRegistry meterRegistry) {
        Map<String, WebSocketHandler> map = new HashMap<>();
        map.put("/ws/lobbies/{lobbyId}", lobbyWebSocketHandler);

        SimpleUrlHandlerMapping handlerMapping = new SimpleUrlHandlerMapping();
        handlerMapping.setUrlMap(map);
        handlerMapping.setOrder(-1); // Higher precedence than other handlers

        Map<String, WebSocketHandler> metricsWrappedHandlers = new HashMap<>();
        for (Map.Entry<String, WebSocketHandler> entry : map.entrySet()) {
            metricsWrappedHandlers.put(
                    entry.getKey(),
                    new MetricsWebSocketHandler(entry.getValue(), webSocketMetricsRegistry(meterRegistry))
            );
        }
        handlerMapping.setUrlMap(metricsWrappedHandlers);

        return handlerMapping;
    }

    @Bean
    public WebSocketHandlerAdapter handlerAdapter() {
        return new WebSocketHandlerAdapter();
    }

    @Bean
    public WebSocketMetricsRegistry webSocketMetricsRegistry(MeterRegistry registry) {
        return new WebSocketMetricsRegistry(registry);
    }
}