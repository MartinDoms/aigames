package com.guesshole.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.userdetails.MapReactiveUserDetailsService;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity http) {
        return http
                .authorizeExchange(exchanges ->
                        exchanges
                                .pathMatchers("/admin/**").hasRole("ADMIN")
                                .pathMatchers("/**").permitAll()
                )
                .formLogin(formLogin -> {}) // Empty lambda to use default login page
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .headers(headers -> headers
                        // Configure cache control differently for different paths
                        .cache(ServerHttpSecurity.HeaderSpec.CacheSpec::disable)
                )
                .build();
    }

    @Bean
    public MapReactiveUserDetailsService userDetailsService() {
        UserDetails admin = User.builder()
                .username("admin")
                .password("{noop}nitramsmod") // lol don't hardcode this.. also.... endcode the pw
                .roles("ADMIN")
                .build();
        return new MapReactiveUserDetailsService(admin);
    }
}