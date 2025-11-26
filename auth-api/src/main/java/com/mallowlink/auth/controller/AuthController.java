package com.mallowlink.auth.controller;

import com.mallowlink.auth.model.JwtResponse;
import com.mallowlink.auth.service.JwtTokenService;
import com.mallowlink.common.service.TokenValidationService;
import com.mallowlink.common.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Date;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("")
public class AuthController {

    private final JwtTokenService tokenService;
    private final JwtTokenUtil jwtTokenUtil;
    private final TokenValidationService tokenValidationService;

    @Deprecated
    @PostMapping("/token/{userId}")
    public Mono<ResponseEntity<JwtResponse>> saveToken(
            @PathVariable String userId, 
            @RequestBody String token,
            ServerWebExchange exchange) {
        log.info("Saving token for user: {}", userId);

        // The filter already validates the token, but we can add additional validation if needed
        return tokenService.generateToken(userId);
    }

    /**
     * Generate JWT token for service authentication
     * @return JWT token response
     */


    @PostMapping("/{serviceName}/token")
    public Mono<ResponseEntity<JwtResponse>> generateToken(
            @PathVariable String serviceName,
            @RequestBody String instanceKey
    ) {
        log.info("Generating token {}, {}", serviceName, instanceKey);

        String subject = serviceName + ":" + instanceKey;
        return tokenService.generateToken(subject);

    }

    /**
     * Refresh JWT token using refresh token
     * @param refreshToken the refresh token
     * @return new JWT token response
     */
    @PostMapping("/{serviceName}/token/refresh")
    public Mono<ResponseEntity<JwtResponse>> refreshToken(
            @PathVariable String serviceName,
            @RequestBody String refreshToken) {
        log.info("Refreshing gateway token");

        // The filter already validates the token, but we can add additional validation if needed
        try {
            if (!jwtTokenUtil.validateToken(refreshToken)) {
                log.error("Invalid refresh token");
                return Mono.just(ResponseEntity.badRequest().body(null));
            }

            return tokenService.refreshToken(refreshToken);
        } catch (Exception e) {
            log.error("Error refreshing token", e);
            return Mono.just(ResponseEntity.badRequest().body(null));
        }
    }
}
