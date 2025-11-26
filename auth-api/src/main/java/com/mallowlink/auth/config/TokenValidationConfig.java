package com.mallowlink.auth.config;

import com.mallowlink.common.filter.TokenValidationFilter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import jakarta.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Configuration
public class TokenValidationConfig {

    private final TokenValidationFilter tokenValidationFilter;

    // List of paths in AuthController that should be excluded from token validation
    private static final List<String> AUTH_CONTROLLER_PATHS = List.of(
            "/token/", // Covers /token/{userId} endpoints
            "/{subject}/token", // Covers /{subject}/token endpoint
            "/{subject}/token/refresh" // Covers /{subject}/token/refresh endpoint
    );

    public static final String[] AUTHORIZED_PATHS = {
            "/status",
            "/token/**",
            "/gateway/token/**",
            "/user/token/**",
            "/payment/token/**",
            "/devOffice/client/create/**",
            "/client/exists/**",
            "/client/get/**",
            "/devOffice/client/update/**",
            "/devOffice/client/delete/**"
    };

    @Autowired
    public TokenValidationConfig(TokenValidationFilter tokenValidationFilter) {
        this.tokenValidationFilter = tokenValidationFilter;
    }

    @PostConstruct
    public void init() {
        // Configure the TokenValidationFilter with auth-api specific paths
        tokenValidationFilter.setAuthControllerPaths(AUTH_CONTROLLER_PATHS);
        tokenValidationFilter.setAuthorizedPaths(AUTHORIZED_PATHS);

        log.info("TokenValidationFilter configured with auth-api specific paths");
        log.info("AUTH_CONTROLLER_PATHS: {}", AUTH_CONTROLLER_PATHS);
        log.info("AUTHORIZED_PATHS: {}", (Object) AUTHORIZED_PATHS);
    }
}
