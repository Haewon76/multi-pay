package com.mallowlink.auth.service;

import com.mallowlink.auth.repository.ClientUuidRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientUuidService {

    private final ClientUuidRepository clientUuidRepository;
    private final ClientService clientService;

    private static final String ERROR_MESSAGE_TEMPLATE = "Failed to create UUID for client: %s";


    public Mono<String> createClientUuid(String clientId) {
        log.debug("Creating UUID for client: {}", clientId);
        String uuid = UUID.randomUUID().toString();

        return clientUuidRepository.saveClientUuid(clientId, uuid)
                .flatMap(result -> handleSaveResult(clientId, uuid, result))
                .doOnError(error -> log.error("Error creating UUID for client: {}", clientId, error));
    }

    private Mono<String> handleSaveResult(String clientId, String uuid, Boolean result) {
        if (Boolean.TRUE.equals(result)) {
            log.debug("UUID created successfully for client: {}", clientId);
            return clientService.createClientUuid(clientId, uuid)
                    .thenReturn(uuid);
        } else {
            log.warn("Failed to create UUID for client: {}", clientId);
            return createErrorMono(clientId);
        }
    }

    private Mono<String> createErrorMono(String clientId) {
        return Mono.error(new RuntimeException(String.format(ERROR_MESSAGE_TEMPLATE, clientId)));
    }


    public Mono<String> updateClientUuid(String clientId) {
        log.debug("Updating UUID for client: {}", clientId);
        String uuid = UUID.randomUUID().toString();
        return clientUuidRepository.deleteClientUuid(clientId)
                .flatMap(deleteResult -> {
                    if (Boolean.TRUE.equals(deleteResult)) {
                        log.debug("Existing UUID deleted successfully for client: {}", clientId);
                    } else {
                        log.warn("No existing UUID found to delete for client: {}", clientId);
                    }
                    return clientUuidRepository.saveClientUuid(clientId, uuid);
                })
                .flatMap(saveResult -> {
                    if (Boolean.TRUE.equals(saveResult)) {
                        log.debug("UUID updated successfully for client: {}", clientId);
                        return clientService.updateClientUuid(clientId, uuid)
                                .thenReturn(uuid);
                    } else {
                        log.warn("Failed to update UUID for client: {}", clientId);
                        return Mono.error(new RuntimeException("Failed to update UUID for client: " + clientId));
                    }
                })
                .doOnError(error -> log.error("Error updating UUID for client: {}", clientId, error));
    }

    public Mono<Object> getClientUuid(String clientId) {
        log.debug("Getting UUID for client: {}", clientId);
        return clientUuidRepository.getClientUuid(clientId)
                .switchIfEmpty(
                    clientService.getClientUuid(clientId)
                        .doOnSuccess(uuid -> {
                            if (uuid != null) {
                                log.info("UUID retrieved from database for client: {}", clientId);
                            }
                        })
                        .cast(Object.class)
                )
                .flatMap(result -> {
                    log.info("UUID retrieved successfully for client: {}", result.toString());
                    return Mono.just(result);
                })
                .doOnError(error -> log.error("Error retrieving UUID for client: {}", clientId, error));
    }

    public Mono<String> existsClientId(String clientId) {
        log.debug("Checking if UUID exists for client: {}", clientId);
        return clientUuidRepository.existsClientId(clientId)
                .doOnSuccess(exists -> log.debug("UUID existence check for client: {}, exists: {}", clientId, exists))
                .doOnError(error -> log.error("Error checking if UUID exists for client: {}", clientId, error));
    }

    public Mono<String> existsClientUuid(String clientToken) {
        log.debug("Checking if UUID exists for client: {}", clientToken);
        return clientUuidRepository.existsClientUuid(clientToken)
                .doOnSuccess(exists -> log.debug("UUID existence check for client: {}, exists: {}", clientToken, exists))
                .doOnError(error -> log.error("Error checking if UUID exists for client: {}", clientToken, error));
    }

    public Mono<Boolean> deleteClientUuid(String clientId) {
        log.debug("Deleting UUID for client: {}", clientId);
        return clientUuidRepository.deleteClientUuid(clientId)
                .flatMap(result -> {
                    if (Boolean.TRUE.equals(result)) {
                        log.debug("UUID deleted successfully for client: {}", clientId);
                        return clientService.deleteClientUuid(clientId)
                                .thenReturn(true);
                    } else {
                        log.warn("Failed to delete UUID for client: {}", clientId);
                        return Mono.just(false);
                    }
                })
                .doOnError(error -> log.error("Error deleting UUID for client: {}", clientId, error));
    }
}
