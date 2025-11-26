package com.mallowlink.gateway.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import reactor.core.publisher.Mono;

@Slf4j
@Configuration
public class RateLimiterConfig {

    /**
     * Rate limit by IP address
     * This resolver uses the client's IP address as the key for rate limiting
     */
    @Bean
    @Primary
    public KeyResolver ipKeyResolver() {
        return exchange -> {
            String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            log.debug("Rate limiting by IP: {}", ip);
            return Mono.just(ip);
        };
    }

    /**
     * Rate limit by user ID
     * This resolver attempts to extract a user ID from the request, falling back to IP if not found
     * In a real application, this would extract the user ID from an authenticated user
     */
    @Bean
    public KeyResolver userKeyResolver() {
        return exchange -> {
            // In a real application, you would extract the user ID from the authenticated user
            // For example, from a JWT token or session

            // For demonstration, we'll check if there's a user ID header
            String userId = exchange.getRequest().getHeaders().getFirst("X-User-ID");

            if (userId != null && !userId.isEmpty()) {
                log.debug("Rate limiting by User ID: {}", userId);
                return Mono.just(userId);
            }

            // Fall back to IP address if no user ID is found
            String ip = exchange.getRequest().getRemoteAddress().getAddress().getHostAddress();
            log.debug("No User ID found, rate limiting by IP: {}", ip);
            return Mono.just("ip:" + ip);
        };
    }

    /**
     * Rate limit by path
     * This resolver uses the request path as the key for rate limiting
     * Useful for limiting specific endpoints regardless of user
     */
    @Bean
    public KeyResolver pathKeyResolver() {
        return exchange -> {
            String path = exchange.getRequest().getPath().value();
            log.debug("Rate limiting by Path: {}", path);
            return Mono.just(path);
        };
    }
}
