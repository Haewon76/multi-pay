package com.mallowlink.payment.config;

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

    // List of paths in PaymentController that should be excluded from token validation
    private static final List<String> AUTH_CONTROLLER_PATHS = List.of(
            // Add payment-specific paths here
            "/payment/public/",
            "/payment/status"
    );

    // Paths that don't require authentication
    private static final String[] AUTHORIZED_PATHS = {
            "/payment/status",
            "/payment/public/**"
    };

    @Autowired
    public TokenValidationConfig(TokenValidationFilter tokenValidationFilter) {
        this.tokenValidationFilter = tokenValidationFilter;
    }

    @PostConstruct
    public void init() {
        // Configure the TokenValidationFilter with payment-api specific paths
        tokenValidationFilter.setAuthControllerPaths(AUTH_CONTROLLER_PATHS);
        tokenValidationFilter.setAuthorizedPaths(AUTHORIZED_PATHS);

        log.info("TokenValidationFilter configured with payment-api specific paths");
        log.info("AUTH_CONTROLLER_PATHS: {}", AUTH_CONTROLLER_PATHS);
        log.info("AUTHORIZED_PATHS: {}", (Object) AUTHORIZED_PATHS);
    }
}
