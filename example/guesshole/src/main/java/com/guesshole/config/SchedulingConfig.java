package com.guesshole.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Configuration to enable scheduled tasks in the application.
 * This is required for @Scheduled annotations to work.
 */
@Configuration
@EnableScheduling
public class SchedulingConfig {
    // No additional configuration needed.
    // The @EnableScheduling annotation does all the work.
}
