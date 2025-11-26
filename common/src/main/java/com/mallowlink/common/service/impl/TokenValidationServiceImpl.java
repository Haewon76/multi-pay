package com.mallowlink.common.service.impl;

import com.mallowlink.common.util.JwtTokenUtil;
import com.mallowlink.common.service.TokenValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.function.Function;

@Slf4j
@Service
@RequiredArgsConstructor
public class TokenValidationServiceImpl implements TokenValidationService {

    private final JwtTokenUtil jwtTokenUtil;
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Extracts and validates the token from the Authorization header
     *
     * @param request the server HTTP request
     * @return the validated token
     */
    @Override
    public Mono<String> validateTokenFromHeader(ServerHttpRequest request) {
        log.debug("Validating token from header");

        String authHeader = request.getHeaders().getFirst(AUTHORIZATION_HEADER);
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            log.warn("No Authorization header found or it doesn't start with Bearer prefix");
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Authorization header"));
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        if (!jwtTokenUtil.validateToken(token)) {
            log.warn("Invalid token");
            return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid token"));
        }

        log.debug("Token validated successfully");
        return Mono.just(token);
    }

    /**
     * Extracts and validates the token from the Authorization header in a ServerWebExchange
     *
     * @param exchange the server web exchange
     * @return the validated token
     */
    @Override
    public Mono<String> validateTokenFromExchange(ServerWebExchange exchange) {
        return validateTokenFromHeader(exchange.getRequest());
    }

    /**
     * Executes a function with a validated token
     *
     * @param request the server HTTP request
     * @param tokenFunction the function to execute with the validated token
     * @param <T> the type of the result
     * @return the result of the function
     */
    @Override
    public <T> Mono<T> withValidToken(ServerHttpRequest request, Function<String, Mono<T>> tokenFunction) {
        return validateTokenFromHeader(request)
                .flatMap(tokenFunction)
                .onErrorResume(ResponseStatusException.class, error -> {
                    log.error("Token validation error: {}", error.getMessage());
                    // Return the original error to maintain the correct status code (e.g., 401 for unauthorized)
                    return Mono.error(error);
                })
                .onErrorResume(error -> {
                    log.error("Unexpected error during token validation: {}", error.getMessage(), error);
                    // For security endpoints, it's better to return 401 Unauthorized than 500 Internal Server Error
                    // This prevents information leakage and provides a clearer response to the client
                    return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing authentication"));
                });
    }

    /**
     * Executes a function with a validated token from a ServerWebExchange
     *
     * @param exchange the server web exchange
     * @param tokenFunction the function to execute with the validated token
     * @param <T> the type of the result
     * @return the result of the function
     */
    @Override
    public <T> Mono<T> withValidTokenExchange(ServerWebExchange exchange, Function<String, Mono<T>> tokenFunction) {
        return withValidToken(exchange.getRequest(), tokenFunction);
    }

    /**
     * Gets the subject from a validated token
     *
     * @param token the validated token
     * @return the subject
     */
    @Override
    public String getSubjectFromToken(String token) {
        return jwtTokenUtil.getSubjectFromToken(token);
    }
}
