package com.mallowlink.auth.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Repository;
import reactor.core.publisher.Mono;

import java.util.Set;

@Slf4j
@Repository
public class ClientUuidRepository {

    private final ReactiveRedisTemplate<String, Object> redisTemplate;
    private static final String KEY_NAME = "client:token";
    private static final double DEFAULT_SCORE = 1.0;

    public ClientUuidRepository(ReactiveRedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
        log.info("ClientUuidRepository initialized");
    }

    public Mono<Boolean> saveClientUuid(String clientId, String uuid) {
        String value = clientId + ":" + uuid;
        log.trace("Saving client UUID to sorted set with key: {}, value: {}", KEY_NAME, value);
        return redisTemplate.opsForZSet().add(KEY_NAME, value, DEFAULT_SCORE)
                .doOnSuccess(result -> log.trace("Client UUID save operation completed with result: {}", result))
                .doOnError(error -> log.error("Error saving client UUID to sorted set with key: {}", KEY_NAME, error));
    }

    public Mono<Object> getClientUuid(String clientId) {
        log.trace("Getting client UUID for clientId: {} from sorted set with key: {}", clientId, KEY_NAME);
        return redisTemplate.opsForZSet().scan(KEY_NAME)
                .filter(entry -> ((String) entry.getValue()).startsWith(clientId + ":"))
                .map(entry -> ((String) entry.getValue()).substring(clientId.length() + 1))
                .next()
                .cast(Object.class)
                .doOnSuccess(result -> log.trace("Client UUID retrieval completed for clientId: {}, found: {}", clientId, (result != null)))
                .doOnError(error -> log.error("Error getting client UUID for clientId: {} from sorted set with key: {}", clientId, KEY_NAME, error));
    }

    public Mono<Boolean> deleteClientUuid(String clientId) {
        log.trace("Deleting client UUID for clientId: {} from sorted set with key: {}", clientId, KEY_NAME);
        return redisTemplate.opsForZSet().scan(KEY_NAME)
                .filter(entry -> ((String) entry.getValue()).startsWith(clientId + ":"))
                .map(entry -> entry.getValue())
                .collectList()
                .flatMap(values -> {
                    if (values.isEmpty()) {
                        return Mono.just(false);
                    }
                    return redisTemplate.opsForZSet().remove(KEY_NAME, values.toArray())
                            .map(count -> count > 0);
                })
                .doOnSuccess(result -> log.trace("Client UUID deletion completed for clientId: {} with result: {}", clientId, result))
                .doOnError(error -> log.error("Error deleting client UUID for clientId: {} from sorted set with key: {}", clientId, KEY_NAME, error));
    }

    public Mono<String> existsClientId(String clientId) {
        log.debug("Checking if client ID exists for clientToken: {} in sorted set with key: {}", clientId, KEY_NAME);

        return redisTemplate.opsForZSet().scan(KEY_NAME)
                .filter(entry -> ((String) entry.getValue()).startsWith(clientId + ":"))
                .map(entry -> {
                    String value = (String) entry.getValue();
                    return value.substring(value.lastIndexOf(":") + 1);
                })
                .next()
                .doOnSuccess(result -> log.trace("Client UUID existence check completed for clientId: {} with result: {}", clientId, result))
                .doOnError(error -> log.error("Error checking if client UUID exists for clientId: {} in sorted set with key: {}", clientId, KEY_NAME, error));
    }

    public Mono<String> existsClientUuid(String clientToken) {
        log.debug("Checking if client UUID exists for clientToken: {} in sorted set with key: {}", clientToken, KEY_NAME);

        return redisTemplate.opsForZSet().scan(KEY_NAME)
                .filter(entry -> ((String) entry.getValue()).endsWith(":" + clientToken))
                .map(entry -> {
                    String value = (String) entry.getValue();
                    return value.substring(0, value.lastIndexOf(":"));
                })
                .next()
                .doOnSuccess(result -> log.trace("Client UUID existence check completed for clientToken: {} with result: {}", clientToken, result))
                .doOnError(error -> log.error("Error checking if client UUID exists for clientToken: {} in sorted set with key: {}", clientToken, KEY_NAME, error));
    }
}
