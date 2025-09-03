package com.guesshole.config;

import org.springframework.boot.web.error.ErrorAttributeOptions;
import org.springframework.boot.web.reactive.error.DefaultErrorAttributes;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.server.ServerRequest;

import java.util.Arrays;
import java.util.Map;

@Component
public class GlobalErrorAttributes extends DefaultErrorAttributes {

    private final Environment environment;

    public GlobalErrorAttributes(Environment environment) {
        this.environment = environment;
    }

    @Override
    public Map<String, Object> getErrorAttributes(ServerRequest request, ErrorAttributeOptions options) {
        // In the "dev" profile, always include exception details and the stack trace.
        if (isDevelopmentProfileActive()) {
            options = options.including(ErrorAttributeOptions.Include.STACK_TRACE, ErrorAttributeOptions.Include.EXCEPTION);
        }

        Map<String, Object> errorAttributes = super.getErrorAttributes(request, options);

        // Ensure the message is never null for the template.
        if (errorAttributes.get("message") == null) {
            errorAttributes.put("message", "No message available");
        }

        // For security, explicitly remove exception details in non-development profiles.
        if (!isDevelopmentProfileActive()) {
            errorAttributes.remove("exception");
            errorAttributes.remove("trace");
        }

        return errorAttributes;
    }

    /**
     * Checks if the 'dev' profile is active.
     * @return true if the 'dev' profile is active, false otherwise.
     */
    private boolean isDevelopmentProfileActive() {
        return Arrays.asList(environment.getActiveProfiles()).contains("dev");
    }
}
