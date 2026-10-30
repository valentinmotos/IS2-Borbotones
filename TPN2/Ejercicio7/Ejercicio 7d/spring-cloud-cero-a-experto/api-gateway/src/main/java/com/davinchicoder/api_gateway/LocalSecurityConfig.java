package com.davinchicoder.api_gateway;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;
import reactor.core.publisher.Mono;

@Configuration
@Profile("local")
public class LocalSecurityConfig {
    @Bean
    SecurityWebFilterChain localSecurityFilterChain(ServerHttpSecurity http) {
        return http.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .authorizeExchange(auth -> auth.anyExchange().permitAll()).build();
    }

    @Bean
    KeyResolver localKeyResolver() {
        return exchange -> Mono.just(exchange.getRequest().getRemoteAddress() == null
                ? "local" : exchange.getRequest().getRemoteAddress().getAddress().getHostAddress());
    }
}
