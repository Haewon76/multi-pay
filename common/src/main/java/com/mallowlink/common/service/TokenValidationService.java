package com.mallowlink.common.service;

import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.Function;

/**
 * Interface for token validation service
 */
public interface TokenValidationService {

    /**
     * Extracts and validates the token from the Authorization header
     *
     * @param request the server HTTP request
     * @return the validated token
     */
    Mono<String> validateTokenFromHeader(ServerHttpRequest request);

    /**
     * Extracts and validates the token from the Authorization header in a ServerWebExchange
     *
     * @param exchange the server web exchange
     * @return the validated token
     */
    Mono<String> validateTokenFromExchange(ServerWebExchange exchange);

    /**
     * Executes a function with a validated token
     *
     * @param request the server HTTP request
     * @param tokenFunction the function to execute with the validated token
     * @param <T> the type of the result
     * @return the result of the function
     */
    <T> Mono<T> withValidToken(ServerHttpRequest request, Function<String, Mono<T>> tokenFunction);

    /**
     * Executes a function with a validated token from a ServerWebExchange
     *
     * @param exchange the server web exchange
     * @param tokenFunction the function to execute with the validated token
     * @param <T> the type of the result
     * @return the result of the function
     */
    <T> Mono<T> withValidTokenExchange(ServerWebExchange exchange, Function<String, Mono<T>> tokenFunction);

    /**
     * Gets the subject from a validated token
     *
     * @param token the validated token
     * @return the subject
     */
    String getSubjectFromToken(String token);
}