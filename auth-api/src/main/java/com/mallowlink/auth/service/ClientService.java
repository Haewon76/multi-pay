package com.mallowlink.auth.service;

import com.mallowlink.auth.entity.Client;
import com.mallowlink.auth.repository.ClientRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.LocalDateTime;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ClientService {

    private final ClientRepository clientRepository;

    public Mono<Boolean> createClientUuid(String clientId, String clientKey) {
        log.debug("Creating UUID for client: {}, {}", clientId, clientKey);

        return Mono.fromCallable(() -> {
            Client client = Client.builder()
                    .clientId(clientId)
                    .clientKey(clientKey)
                    .build();
            clientRepository.save(client);
            log.debug("UUID created successfully for client: {}", clientId);
            return true;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(error -> {
            log.error("Error creating UUID for client: {}", clientId, error);
            return Mono.error(new RuntimeException("Failed to create UUID for client: " + clientId, error));
        });
    }

    @Transactional
    public Mono<Boolean> updateClientUuid(String clientId, String clientKey) {
        log.debug("Updating UUID for client: {}, {}", clientId, clientKey);

        return Mono.fromCallable(() -> {
            clientRepository.findByClientId(clientId).ifPresent(client -> {
                client.setClientKey(clientKey);
                clientRepository.save(client);
                log.debug("UUID updated successfully for client: {}", clientId);
            });
            
            if (!clientRepository.existsByClientId(clientId)) {
                Client client = Client.builder()
                        .clientId(clientId)
                        .clientKey(clientKey)
                        .build();
                clientRepository.save(client);
                log.debug("Client not found, created new UUID for client: {}", clientId);
            }
            return true;
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(error -> {
            log.error("Error updating UUID for client: {}", clientId, error);
            return Mono.error(new RuntimeException("Failed to update UUID for client: " + clientId, error));
        });
    }

    public Mono<String> getClientUuid(String clientId) {
        log.debug("Getting UUID for client: {}", clientId);
        
        return Mono.fromCallable(() -> 
            clientRepository.findByClientId(clientId)
                .map(Client::getClientKey)
                .orElse(null)
        )
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(uuid -> {
            if (uuid != null) {
                log.debug("UUID retrieved successfully for client: {}", clientId);
            } else {
                log.debug("No UUID found for client: {}", clientId);
            }
        })
        .onErrorResume(error -> {
            log.error("Error retrieving UUID for client: {}", clientId, error);
            return Mono.error(new RuntimeException("Failed to retrieve UUID for client: " + clientId, error));
        });
    }

    public Mono<String> existsClientId(String clientId) {
        log.debug("Checking if UUID exists for client: {}", clientId);
        
        return Mono.fromCallable(() -> 
            clientRepository.findByClientId(clientId)
                .map(Client::getClientKey)
                .orElse(null)
        )
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(uuid -> log.debug("UUID existence check for client: {}, exists: {}", clientId, uuid != null))
        .onErrorResume(error -> {
            log.error("Error checking if UUID exists for client: {}", clientId, error);
            return Mono.error(new RuntimeException("Failed to check if UUID exists for client: " + clientId, error));
        });
    }

    public Mono<String> existsClientUuid(String uuid) {
        log.debug("Checking if client exists for UUID: {}", uuid);
        
        return Mono.fromCallable(() -> 
            clientRepository.findByClientKey(uuid)
                .map(Client::getClientId)
                .orElse(null)
        )
        .subscribeOn(Schedulers.boundedElastic())
        .doOnSuccess(clientId -> log.debug("Client existence check for UUID: {}, exists: {}", uuid, clientId != null))
        .onErrorResume(error -> {
            log.error("Error checking if client exists for UUID: {}", uuid, error);
            return Mono.error(new RuntimeException("Failed to check if client exists for UUID: " + uuid, error));
        });
    }

    @Transactional
    public Mono<Boolean> deleteClientUuid(String clientId) {
        log.debug("Deleting UUID for client: {}", clientId);
        
        return Mono.fromCallable(() -> {
            if (clientRepository.existsByClientId(clientId)) {
                clientRepository.deleteByClientId(clientId);
                log.debug("UUID deleted successfully for client: {}", clientId);
                return true;
            } else {
                log.warn("No UUID found to delete for client: {}", clientId);
                return false;
            }
        })
        .subscribeOn(Schedulers.boundedElastic())
        .onErrorResume(error -> {
            log.error("Error deleting UUID for client: {}", clientId, error);
            return Mono.error(new RuntimeException("Failed to delete UUID for client: " + clientId, error));
        });
    }
}