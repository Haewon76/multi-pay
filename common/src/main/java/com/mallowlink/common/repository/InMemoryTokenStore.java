package com.mallowlink.common.repository;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory token store for the gateway service.
 * This class stores tokens in memory instead of Redis.
 */
@Slf4j
@Component
public class InMemoryTokenStore {

    private static class TokenEntry {
        private final String token;
        private final Instant expiresAt;

        public TokenEntry(String token, Duration expiration) {
            this.token = token;
            this.expiresAt = Instant.now().plus(expiration);
        }

        public String getToken() {
            return token;
        }

        public boolean isExpired() {
            return Instant.now().isAfter(expiresAt);
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }
    }

    private final Map<String, TokenEntry> tokenStore = new ConcurrentHashMap<>();

    /**
     * Save a token with the given key and expiration.
     *
     * @param key the key to store the token under
     * @param token the token to store
     * @param expiration the expiration duration
     * @return true if the token was saved successfully
     */
    public boolean saveToken(String key, String token, Duration expiration) {
        log.debug("Saving token with key: {} and expiration: {}", key, expiration);
        tokenStore.put(key, new TokenEntry(token, expiration));
        return true;
    }

    /**
     * Get a token with the given key.
     *
     * @param key the key to get the token for
     * @return the token, or null if not found or expired
     */
    public String getToken(String key) {
        log.trace("Getting token with key: {}", key);
        TokenEntry entry = tokenStore.get(key);
        
        if (entry == null) {
            log.trace("No token found for key: {}", key);
            return null;
        }
        
        if (entry.isExpired()) {
            log.debug("Token expired for key: {}", key);
            tokenStore.remove(key);
            return null;
        }
        
        log.trace("Token found for key: {}", key);
        return entry.getToken();
    }

    /**
     * Delete a token with the given key.
     *
     * @param key the key to delete the token for
     * @return true if the token was deleted, false if it didn't exist
     */
    public boolean deleteToken(String key) {
        log.trace("Deleting token with key: {}", key);
        return tokenStore.remove(key) != null;
    }

    /**
     * Delete multiple tokens with the given keys.
     *
     * @param keys the keys to delete the tokens for
     * @return the number of tokens deleted
     */
    public long deleteTokens(String... keys) {
        log.debug("Deleting tokens with keys: {}", (Object) keys);
        long count = 0;
        for (String key : keys) {
            if (tokenStore.remove(key) != null) {
                count++;
            }
        }
        return count;
    }
}