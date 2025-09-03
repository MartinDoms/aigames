package com.guesshole.config;

import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.util.function.Function;

@Configuration
public class WebClientConfig {

    @Bean
    public WebClient webClient(MeterRegistry meterRegistry) {
        // Enable Netty metrics by creating a client with metrics
        HttpClient httpClient = HttpClient.create()
                .metrics(true, Function.identity()); // This enables the Reactor Netty metrics

        return WebClient.builder()
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .build();
    }
}