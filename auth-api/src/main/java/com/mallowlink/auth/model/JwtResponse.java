package com.mallowlink.auth.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JwtResponse {
    private String token;
    private String refreshToken;
    private Date expiresAt;
    private String tokenType;
    
    public static JwtResponse of(String token, String refreshToken, Date expiresAt) {
        return JwtResponse.builder()
                .token(token)
                .refreshToken(refreshToken)
                .expiresAt(expiresAt)
                .tokenType("Bearer")
                .build();
    }
}