package com.mallowlink.common.service;

import com.mallowlink.common.model.response.JwtResponse;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Interface for token management service
 */
public interface TokenService {

    /**
     * Fetch a new token from auth-api
     * @return the token response
     */
    Mono<JwtResponse> fetchNewToken();

    /**
     * Refresh the token using the refresh token
     * @return the new token response
     */
    Mono<JwtResponse> refreshToken();

    /**
     * Get the current access token
     * If no token exists, automatically fetch a new one
     * @return the access token
     */
    Mono<String> getAccessToken();

    /**
     * Adds an authorization token to a WebClient request
     *
     * @param requestSpec the WebClient request specification function
     * @param <T> the type of the response
     * @return a Mono that emits the response
     */
    <T> Mono<T> withAuthToken(Function<String, Mono<T>> requestSpec);

    /**
     * Adds an authorization token to a ServerHttpRequest
     *
     * @param exchange the server web exchange
     * @return a Mono that emits the modified exchange
     */
    Mono<ServerWebExchange> withAuthTokenExchange(ServerWebExchange exchange);

    /**
     * Creates an Authorization header value with the Bearer prefix
     *
     * @param token the token to use
     * @return the header value
     */
    String createAuthorizationHeader(String token);
}