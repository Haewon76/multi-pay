package com.mallowlink.auth.controller;

import com.mallowlink.auth.service.ClientUuidService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import static java.lang.Boolean.TRUE;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/devOffice/client")
public class DevOfficeClientAuthController {

    private final ClientUuidService clientUuidService;

    @PostMapping("/create/{clientId}")
    public Mono<String> createClientUuid(
            @PathVariable String clientId,
            ServerWebExchange exchange
    ) {
        log.debug("Creating UUID for client: {}", clientId);

        // The filter already validates the token, but we can add additional validation if needed
        return clientUuidService.existsClientId(clientId)
                .flatMap(exists -> {
                    if (exists != null) {
                        log.warn("Client UUID already exists for client: {}", clientId);
                        return Mono.error(new ResponseStatusException(HttpStatus.CONFLICT, "Client UUID already exists"));
                    } else {
                        return clientUuidService.createClientUuid(clientId);
                    }
                })
                .doOnError(error -> log.error("Error creating client UUID: {}", clientId, error));
    }

    @PutMapping("/update/{clientId}")
    public Mono<String> updateClientUuid(
            @PathVariable String clientId,
            ServerWebExchange exchange
    ) {
        log.debug("Updating UUID for client id: {}", clientId);

        // The filter already validates the token, but we can add additional validation if needed
        return clientUuidService.existsClientUuid(clientId)
                .flatMap(exists -> {
                    if (TRUE.equals(exists)) {
                        return clientUuidService.updateClientUuid(clientId);
                    } else {
                        log.warn("Client UUID does not exist for client id: {}", clientId);
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Client UUID not found"));
                    }
                })
                .doOnError(error -> log.error("Error updating client ID: {}", clientId, error));
    }

    @DeleteMapping("/delete/{clientId}")
    public Mono<Void> deleteClientUuid(
            @PathVariable String clientId,
            ServerWebExchange exchange
    ) {
        log.debug("Deleting UUID for client: {}", clientId);

        // The filter already validates the token, but we can add additional validation if needed
        return clientUuidService.existsClientUuid(clientId)
                .flatMap(exists -> {
                    if (TRUE.equals(exists)) {
                        return clientUuidService.deleteClientUuid(clientId)
                                .then(Mono.empty());
                    } else {
                        log.warn("Client UUID does not exist for client: {}", clientId);
                        return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Client UUID not found"));
                    }
                })
                .then()
                .doOnError(error -> log.error("Error deleting client UUID: {}", clientId, error));
    }

}
