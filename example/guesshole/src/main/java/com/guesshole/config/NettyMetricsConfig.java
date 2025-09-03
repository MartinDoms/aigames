package com.guesshole.config;

import org.springframework.boot.web.embedded.netty.NettyServerCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.ReactorResourceFactory;
import org.springframework.web.reactive.socket.server.upgrade.ReactorNettyRequestUpgradeStrategy;

import java.util.function.Function;

@Configuration
public class NettyMetricsConfig {

    @Bean
    public NettyServerCustomizer nettyServerCustomizer() {
        return httpServer -> httpServer.metrics(true, Function.identity());
    }

    // This customizes the Netty server created by WebFlux
    @Bean
    public ReactorResourceFactory reactorResourceFactory() {
        ReactorResourceFactory factory = new ReactorResourceFactory();
        factory.setUseGlobalResources(false); // Use dedicated resources
        return factory;
    }

    @Bean
    public ReactorNettyRequestUpgradeStrategy reactorNettyRequestUpgradeStrategy(
            ReactorResourceFactory resourceFactory) {
        ReactorNettyRequestUpgradeStrategy strategy = new ReactorNettyRequestUpgradeStrategy();
        strategy.setMaxFramePayloadLength(64 * 1024); // 64KB max frame size
        return strategy;
    }
}