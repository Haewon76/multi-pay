package com.mallowlink.auth.service;

import com.mallowlink.auth.model.JwtResponse;
import com.mallowlink.common.util.JwtTokenUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;

@Slf4j
@Service
@RequiredArgsConstructor
public class JwtTokenService {

    private final JwtTokenUtil jwtTokenUtil;

    public Mono<ResponseEntity<JwtResponse>> generateToken(
            String subject
    ) {

        String token = jwtTokenUtil.generateToken(subject);
        String refreshToken = jwtTokenUtil.generateRefreshToken(subject);
        Date expiresAt = jwtTokenUtil.getExpirationDateFromToken(token);

        JwtResponse response = JwtResponse.of(token, refreshToken, expiresAt);

        return Mono.just(ResponseEntity.ok(response));
    }

    public Mono<ResponseEntity<JwtResponse>> refreshToken(
            String refreshToken
    ) {
        String subject = jwtTokenUtil.getSubjectFromToken(refreshToken);
        String newToken = jwtTokenUtil.generateToken(subject);
        String newRefreshToken = jwtTokenUtil.generateRefreshToken(subject);
        Date expiresAt = jwtTokenUtil.getExpirationDateFromToken(newToken);

        JwtResponse response = JwtResponse.of(newToken, newRefreshToken, expiresAt);

        return Mono.just(ResponseEntity.ok(response));
    }

}