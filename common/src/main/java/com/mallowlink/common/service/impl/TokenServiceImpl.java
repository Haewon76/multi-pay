package com.mallowlink.common.service.impl;

import com.mallowlink.common.model.response.JwtResponse;
import com.mallowlink.common.repository.TokenRepository;
import com.mallowlink.common.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.function.Function;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@Service
public class TokenServiceImpl implements TokenService {

    @Value("${instance.id}")
    private String instanceId;
    @Value("${instance.key}")
    private String instanceKey;

    private final TokenRepository tokenRepository;
    private final WebClient webClient;
    private static final String AUTH_SERVICE_URL = "http://auth-rollout-active-svc:8080";
    private static final String AUTHORIZATION_HEADER = AUTHORIZATION;
    private static final String BEARER_PREFIX = "Bearer ";

    public TokenServiceImpl(TokenRepository tokenRepository, WebClient.Builder webClientBuilder) {
        this.tokenRepository = tokenRepository;
        this.webClient = webClientBuilder.baseUrl(AUTH_SERVICE_URL).build();
        log.info("TokenService initialized");
    }

    /**
     * Get the token endpoint URL
     * @return the token endpoint URL
     */
    private String getTokenEndpoint() {
        return "/api/auth/" + instanceId + "/token";
    }

    /**
     * Get the refresh token endpoint URL
     * @return the refresh token endpoint URL
     */
    private String getRefreshTokenEndpoint() {
        return "/api/auth/" + instanceId + "/token/refresh";
    }

    /**
     * Fetch a new token from auth-api
     * @return the token response
     */
    @Override
    public Mono<JwtResponse> fetchNewToken() {
        log.info("Fetching new token from auth-api");

        return webClient.post()
                .uri(getTokenEndpoint())
                .bodyValue(instanceKey)
                .retrieve()
                .bodyToMono(JwtResponse.class)
                .flatMap(this::saveTokens)
                .doOnSuccess(response -> log.info("Successfully fetched and saved new token"))
                .doOnError(error -> {
                    log.error("Error fetching new token", error);
                })
                .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2))
                        .maxBackoff(Duration.ofSeconds(10))
                        .doBeforeRetry(retrySignal ->
                            log.warn("Retrying token fetch attempt: {} due to: {}",
                                    retrySignal.totalRetries() + 1,
                                    retrySignal.failure().getMessage()))
                        .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                            log.error("Failed to fetch token after {} attempts", retrySignal.totalRetries());
                            return retrySignal.failure();
                        }));
    }

    /**
     * Refresh the token using the refresh token
     * @return the new token response
     */
    @Override
    public Mono<JwtResponse> refreshToken() {
        log.info("Refreshing token");

        return tokenRepository.getRefreshToken()
                .flatMap(refreshToken -> {
                    if (refreshToken == null) {
                        log.warn("No refresh token found, fetching new token");
                        return fetchNewToken();
                    }

                    log.debug("Using refresh token to get new access token");
                    return webClient.post()
                            .uri(getRefreshTokenEndpoint())
                            .bodyValue(refreshToken.toString())
                            .retrieve()
                            .bodyToMono(JwtResponse.class)
                            .flatMap(this::saveTokens)
                            .doOnSuccess(response -> log.info("Successfully refreshed and saved token"))
                            .onErrorResume(error -> {
                                log.error("Error refreshing token, fetching new token", error);
                                return fetchNewToken();
                            })
                            .retryWhen(reactor.util.retry.Retry.backoff(3, Duration.ofSeconds(2))
                                    .maxBackoff(Duration.ofSeconds(10))
                                    .doBeforeRetry(retrySignal ->
                                        log.warn("Retrying token refresh attempt: {} due to: {}",
                                                retrySignal.totalRetries() + 1,
                                                retrySignal.failure().getMessage()))
                                    .onRetryExhaustedThrow((retryBackoffSpec, retrySignal) -> {
                                        log.error("Failed to refresh token after {} attempts", retrySignal.totalRetries());
                                        return retrySignal.failure();
                                    }));
                });
    }

    /**
     * Get the current access token
     * If no token exists, automatically fetch a new one
     * @return the access token
     */
    @Override
    public Mono<String> getAccessToken() {
        log.debug("Getting access token");

        return tokenRepository.getAccessToken()
                .filter(token -> token != null)
                .map(Object::toString)
                .doOnNext(token -> log.debug("Access token retrieved: {}", token))
                .switchIfEmpty(
                    Mono.defer(() -> {
                        log.warn("No access token found, fetching new token");
                        return fetchNewToken()
                                .map(JwtResponse::getToken)
                                .doOnNext(token -> log.debug("New access token fetched: {}", token));
                    })
                );
    }

    /**
     * Save tokens to Redis
     * @param response the token response
     * @return the token response
     */
    private Mono<JwtResponse> saveTokens(JwtResponse response) {
        log.debug("Saving tokens to Redis");

        // Calculate token expiration duration (subtract 5 minutes for safety)
        long expiryTimeMillis = response.getExpiresAt().getTime() - System.currentTimeMillis() - (5 * 60 * 1000);
        Duration tokenExpiration = Duration.ofMillis(Math.max(expiryTimeMillis, 0));

        // Refresh token expiration is 7 days
        Duration refreshExpiration = Duration.ofDays(7);

        return tokenRepository.saveAccessToken(response.getToken(), tokenExpiration)
                .then(tokenRepository.saveRefreshToken(response.getRefreshToken(), refreshExpiration))
                .thenReturn(response);
    }

    /**
     * Adds an authorization token to a WebClient request
     *
     * @param requestSpec the WebClient request specification function
     * @param <T> the type of the response
     * @return a Mono that emits the response
     */
    @Override
    public <T> Mono<T> withAuthToken(Function<String, Mono<T>> requestSpec) {
        log.debug("Adding authorization token to request");

        return getAccessToken()
                .flatMap(token -> {
                    log.debug("Adding token to request header");
                    return requestSpec.apply(token);
                })
                .onErrorResume(error -> {
                    log.error("Error getting access token: {}", error.getMessage());
                    return Mono.error(error);
                });
    }

    /**
     * Adds an authorization token to a ServerHttpRequest
     *
     * @param exchange the server web exchange
     * @return a Mono that emits the modified exchange
     */
    @Override
    public Mono<ServerWebExchange> withAuthTokenExchange(ServerWebExchange exchange) {
        log.debug("Adding authorization token to exchange");

        return getAccessToken()
                .map(token -> {
                    ServerHttpRequest request = exchange.getRequest();

                    // Create new request with Authorization header
                    ServerHttpRequest modifiedRequest = request.mutate()
                            .header(AUTHORIZATION_HEADER, BEARER_PREFIX + token)
                            .build();

                    // Create new exchange with modified request
                    return exchange.mutate()
                            .request(modifiedRequest)
                            .build();
                });
    }

    /**
     * Creates an Authorization header value with the Bearer prefix
     *
     * @param token the token to use
     * @return the header value
     */
    @Override
    public String createAuthorizationHeader(String token) {
        return BEARER_PREFIX + token;
    }
}