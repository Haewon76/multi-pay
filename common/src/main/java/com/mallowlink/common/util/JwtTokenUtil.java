package com.mallowlink.common.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Component
public class JwtTokenUtil {

    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long jwtTokenValidity;

    @Value("${jwt.refresh-expiration}")
    private long jwtRefreshTokenValidity;

    // Generate token for gateway
    public String generateToken(String subject) {
        log.debug("Generating token for subject: {}", subject);
        return generateToken(new HashMap<>(), subject);
    }

    // Generate token with custom claims
    public String generateToken(Map<String, Object> claims, String subject) {
        return doGenerateToken(claims, subject);
    }

    // Generate refresh token with longer validity
    public String generateRefreshToken(String subject) {
        log.debug("Generating refresh token for subject: {}", subject);
        // Refresh token valid for 7 days
        return doGenerateToken(new HashMap<>(), subject, jwtRefreshTokenValidity);
    }

    private String doGenerateToken(Map<String, Object> claims, String subject) {
        return doGenerateToken(claims, subject, jwtTokenValidity);
    }

    private String doGenerateToken(Map<String, Object> claims, String subject, long expiration) {
        log.debug("Creating JWT token with expiration: {} ms", expiration);

        Date now = new Date();
        Date expiryDate = new Date(now.getTime() + expiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(subject)
                .setIssuedAt(now)
                .setExpiration(expiryDate)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    // Validate token
    public Boolean validateToken(String token) {
        try {
            return !isTokenExpired(token);
        } catch (Exception e) {
            log.error("Error validating token", e);
            return false;
        }
    }

    // Check if token is expired
    private Boolean isTokenExpired(String token) {
        final Date expiration = getExpirationDateFromToken(token);
        return expiration.before(new Date());
    }

    // Get expiration date from token
    public Date getExpirationDateFromToken(String token) {
        return getClaimFromToken(token, Claims::getExpiration);
    }

    // Get subject from token
    public String getSubjectFromToken(String token) {
        return getClaimFromToken(token, Claims::getSubject);
    }

    // Get claim from token
    public <T> T getClaimFromToken(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = getAllClaimsFromToken(token);
        return claimsResolver.apply(claims);
    }

    // Get all claims from token
    private Claims getAllClaimsFromToken(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // Get signing key
    private Key getSigningKey() {
        // Use Keys.secretKeyFor to generate a secure key for HMAC-SHA256
        if (secret.length() < 32) {
            // If the configured secret is too short, use a secure random key
            log.warn("Configured JWT secret is too short, using a secure random key instead");
            return Keys.secretKeyFor(SignatureAlgorithm.HS256);
        } else {
            // If the secret is long enough, use it directly
            byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);
            return Keys.hmacShaKeyFor(keyBytes);
        }
    }
}