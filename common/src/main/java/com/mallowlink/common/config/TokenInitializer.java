package com.mallowlink.common.config;

import com.mallowlink.common.service.TokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "token.initializer.enabled", havingValue = "true", matchIfMissing = true)
public class TokenInitializer {

    private final TokenService tokenService;

    /**
     * Initialize token when application is ready
     */
    @EventListener(ApplicationReadyEvent.class)
    public void initializeToken() {
        log.info("Initializing gateway token");

        tokenService.fetchNewToken()
                 .doOnSuccess(response -> {
                    log.info("Gateway token initialized successfully, expires at: {}", response.getExpiresAt());
                    // Schedule token refresh every 50 minutes (before the 1-hour expiry)
                    scheduleTokenRefresh();
                })
                .doOnError(error -> {
                    log.error("Failed to initialize gateway token even after retries", error);
                    // Try again in 30 seconds
                    retryInitialization();
                })
                .subscribe();
    }

    /**
     * Retry token initialization after a delay
     */
    private void retryInitialization() {
        log.info("Scheduling token initialization retry in 30 seconds");

        Mono.delay(Duration.ofSeconds(30))
                .then(Mono.defer(() -> {
                    log.info("Retrying token initialization");
                    initializeToken();
                    return Mono.empty();
                }))
                .subscribe();
    }

    /**
     * Schedule token refresh to happen before token expires
     */
    private void scheduleTokenRefresh() {
        log.info("Scheduling token refresh every 50 minutes");

        // Refresh token every 50 minutes
        Mono.delay(Duration.ofMinutes(50))
                .flatMap(l -> tokenService.refreshToken())
                .doOnSuccess(response -> {
                    log.info("Token refreshed successfully, expires at: {}", response.getExpiresAt());
                    // Schedule next refresh
                    scheduleTokenRefresh();
                })
                .doOnError(error -> {
                    log.error("Failed to refresh token", error);
                    // Try again in 5 minutes
                    Mono.delay(Duration.ofMinutes(5))
                            .then(Mono.defer(() -> {
                                scheduleTokenRefresh();
                                return Mono.empty();
                            }))
                            .subscribe();
                })
                .subscribe();
    }
}
