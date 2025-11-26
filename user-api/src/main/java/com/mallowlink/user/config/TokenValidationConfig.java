package com.mallowlink.user.config;

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

    // List of paths in UserController that should be excluded from token validation
    private static final List<String> AUTH_CONTROLLER_PATHS = List.of(
            // Add user-specific paths here
            "/user/public/",
            "/user/status"
    );

    // Paths that don't require authentication
    private static final String[] AUTHORIZED_PATHS = {
            "/user/status",
            "/user/public/**"
    };

    @Autowired
    public TokenValidationConfig(TokenValidationFilter tokenValidationFilter) {
        this.tokenValidationFilter = tokenValidationFilter;
    }

    @PostConstruct
    public void init() {
        // Configure the TokenValidationFilter with user-api specific paths
        tokenValidationFilter.setAuthControllerPaths(AUTH_CONTROLLER_PATHS);
        tokenValidationFilter.setAuthorizedPaths(AUTHORIZED_PATHS);
        
        log.info("TokenValidationFilter configured with user-api specific paths");
        log.info("AUTH_CONTROLLER_PATHS: {}", AUTH_CONTROLLER_PATHS);
        log.info("AUTHORIZED_PATHS: {}", (Object) AUTHORIZED_PATHS);
    }
}