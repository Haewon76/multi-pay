package com.mallowlink.gateway.service;

import reactor.core.publisher.Mono;

public interface ClientService {
    /**
     * Validates if the client ID exists and is valid
     * @param clientToken the client ID to validate
     * @return a Mono that emits true if the client ID is valid, false otherwise
     */
    Mono<String> validateClientToken(String clientToken);
}