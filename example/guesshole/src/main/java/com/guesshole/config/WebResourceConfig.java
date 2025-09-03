package com.guesshole.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.http.CacheControl;
import org.springframework.web.reactive.config.ResourceHandlerRegistry;
import org.springframework.web.reactive.config.WebFluxConfigurer;

import java.util.concurrent.TimeUnit;

@Configuration
public class WebResourceConfig implements WebFluxConfigurer {

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler("/js/**", "/css/**", "/images/**", "/audio/**")
                .addResourceLocations("classpath:/static/js/", "classpath:/static/css/",
                        "classpath:/static/images/", "classpath:/static/audio/",
                        "classpath:/public/js/", "classpath:/public/css/",
                        "classpath:/public/images/", "classpath:/public/audio/")
                .setCacheControl(CacheControl.maxAge(30, TimeUnit.DAYS));
    }
}