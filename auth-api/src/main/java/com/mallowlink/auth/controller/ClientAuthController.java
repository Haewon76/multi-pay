package com.mallowlink.auth.controller;

import com.mallowlink.auth.service.ClientUuidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/client")
public class ClientAuthController {

    private final ClientUuidService clientUuidService;

    @GetMapping("/exists/{clientToken}")
    public Mono<String> getClientExists(
            @PathVariable String clientToken,
            ServerWebExchange exchange
    ) {
        log.info("Checking if client exists: {}", clientToken);

        // The filter already validates the token, but we can add additional validation if needed
        return clientUuidService.existsClientUuid(clientToken)
                .doOnSuccess(exists -> log.debug("Client existence check for client: {}, exists: {}", clientToken, exists))
                .doOnError(error -> log.error("Error checking if client exists: {}", clientToken, error));
    }

    @GetMapping("/get/{clientId}")
    public Mono<Object> getClientUuid(
            @PathVariable String clientId,
            ServerWebExchange exchange
    ) {
        log.debug("Getting UUID for client: {}", clientId);

        // The filter already validates the token, but we can add additional validation if needed
        return clientUuidService.getClientUuid(clientId)
                .switchIfEmpty(Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Client UUID not found")))
                .doOnError(error -> log.error("Error getting client UUID: {}", clientId, error));
    }


}
