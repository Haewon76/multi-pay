package com.mallowlink.common.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Slf4j
@Repository
public class TokenRepository {

    public static final String KEY_PREFIX = "token:";
    public static final String REFRESH_TOKEN_KEY = KEY_PREFIX + "refresh";
    public static final String ACCESS_TOKEN_KEY = KEY_PREFIX + "access";

    private final InMemoryTokenStore tokenStore;

    public TokenRepository(InMemoryTokenStore tokenStore) {
        this.tokenStore = tokenStore;
        log.info("TokenRepository initialized in gateway with in-memory storage");
    }

    public Mono<Boolean> saveAccessToken(String token, Duration expiration) {
        log.debug("Saving access token with expiration: {}", expiration);
        return Mono.fromCallable(() -> tokenStore.saveToken(ACCESS_TOKEN_KEY, token, expiration))
                .doOnSuccess(result -> log.debug("Access token save operation completed with result: {}", result))
                .doOnError(error -> log.error("Error saving access token", error));
    }

    public Mono<Boolean> saveRefreshToken(String token, Duration expiration) {
        log.debug("Saving refresh token with expiration: {}", expiration);
        return Mono.fromCallable(() -> tokenStore.saveToken(REFRESH_TOKEN_KEY, token, expiration))
                .doOnSuccess(result -> log.debug("Refresh token save operation completed with result: {}", result))
                .doOnError(error -> log.error("Error saving refresh token", error));
    }

    public Mono<Object> getAccessToken() {
        log.trace("Getting access token");
        return Mono.fromCallable(() -> (Object) tokenStore.getToken(ACCESS_TOKEN_KEY))
                .doOnSuccess(result -> log.trace("Access token retrieval completed, found: {}", (result != null)))
                .doOnError(error -> {
                    log.error("Error getting access token", error);
                    // TODO: Slack notification
                });
    }

    public Mono<Object> getRefreshToken() {
        log.trace("Getting refresh token");
        return Mono.fromCallable(() -> (Object) tokenStore.getToken(REFRESH_TOKEN_KEY))
                .doOnSuccess(result -> log.trace("Refresh token retrieval completed, found: {}", (result != null)))
                .doOnError(error -> {
                    log.error("Error getting refresh token", error);
                    // TODO: Slack notification
                });
    }

    public Mono<Boolean> deleteTokens() {
        log.debug("Deleting all tokens");
        return Mono.fromCallable(() -> tokenStore.deleteTokens(ACCESS_TOKEN_KEY, REFRESH_TOKEN_KEY) > 0)
                .doOnSuccess(result -> log.debug("Token deletion completed with result: {}", result))
                .doOnError(error -> log.error("Error deleting tokens", error));
    }
}
