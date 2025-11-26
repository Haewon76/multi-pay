package com.mallowlink.gateway.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@Slf4j
@Service
public class AuthServiceHealthChecker {

    private final WebClient webClient;
    private static final String AUTH_SERVICE_URL = "http://auth-rollout-active-svc:8080";
    private static final String AUTH_STATUS_ENDPOINT = "/api/auth/status";
    
    public AuthServiceHealthChecker(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.baseUrl(AUTH_SERVICE_URL).build();
    }
    
    @Scheduled(initialDelay = 10000, fixedRate = 600000) // 10 minutes in milliseconds
    public void checkAuthServiceHealth() {
        LocalDateTime now = LocalDateTime.now();
        String formattedTime = now.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        
        log.info("Starting auth service health check at {}", formattedTime);
        
        webClient.get()
                .uri(AUTH_STATUS_ENDPOINT)
                .retrieve()
                .bodyToMono(String.class)
                .doOnSuccess(response -> log.info("Auth service health check successful: {}", response))
                .doOnError(error -> log.error("Auth service health check failed: {}", error.getMessage()))
                .onErrorResume(error -> Mono.empty())
                .subscribe();
    }
}