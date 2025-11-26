package com.mallowlink.gateway.config;

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

    // List of paths in GatewayController that should be excluded from token validation
    private static final List<String> AUTH_CONTROLLER_PATHS = List.of(
            // Add gateway-specific paths here
            "/gateway/public/",
            "/gateway/status"
    );

    // Paths that don't require authentication
    public static final String[] AUTHORIZED_PATHS = {
            "/**",
            "/client/get/**",
            "/client/exists/**",
            "/devOffice/client/create/**",
            "/devOffice/client/update/**",
            "/devOffice/client/delete/**"
    };

    @Autowired
    public TokenValidationConfig(TokenValidationFilter tokenValidationFilter) {
        this.tokenValidationFilter = tokenValidationFilter;
    }

    @PostConstruct
    public void init() {
        // Configure the TokenValidationFilter with gateway-specific paths
        tokenValidationFilter.setAuthControllerPaths(AUTH_CONTROLLER_PATHS);
        tokenValidationFilter.setAuthorizedPaths(AUTHORIZED_PATHS);
        
        log.info("TokenValidationFilter configured with gateway-specific paths");
        log.info("AUTH_CONTROLLER_PATHS: {}", AUTH_CONTROLLER_PATHS);
        log.info("AUTHORIZED_PATHS: {}", (Object) AUTHORIZED_PATHS);
    }
}