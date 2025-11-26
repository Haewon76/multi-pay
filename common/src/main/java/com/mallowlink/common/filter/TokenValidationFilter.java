package com.mallowlink.common.filter;

import com.mallowlink.common.service.TokenValidationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
public class TokenValidationFilter implements WebFilter {

    private final TokenValidationService tokenValidationService;

    // Paths that should be excluded from token validation
    private List<String> authControllerPaths = List.of(
            "/token/", // Default value, can be overridden
            "/{subject}/token",
            "/{subject}/token/refresh"
    );

    // Authorized paths that don't require token validation
    private String[] authorizedPaths = new String[0]; // Empty array by default, should be set by each module

    /**
     * Constructor with required dependencies
     * 
     * @param tokenValidationService the token validation service
     */
    public TokenValidationFilter(TokenValidationService tokenValidationService) {
        this.tokenValidationService = tokenValidationService;
    }

    /**
     * Sets the paths in AuthController that should be excluded from token validation
     * This method should be called by each module to configure its specific paths
     * 
     * @param authControllerPaths the paths to exclude
     */
    public void setAuthControllerPaths(List<String> authControllerPaths) {
        this.authControllerPaths = authControllerPaths;
        log.info("AuthController paths set: {}", authControllerPaths);
    }

    /**
     * Sets the authorized paths that don't require token validation
     * This method should be called by each module to configure its specific paths
     * 
     * @param authorizedPaths the authorized paths
     */
    public void setAuthorizedPaths(String[] authorizedPaths) {
        this.authorizedPaths = authorizedPaths;
        log.info("Authorized paths set: {}", Arrays.toString(authorizedPaths));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        if (path.startsWith("/api/auth/devOffice")) {
            log.debug("Skipping token validation for devOffice path: {}", path);
            return chain.filter(exchange);
        }

        // Skip token validation for whitelisted paths
        if (isWhitelisted(path)) {
            log.debug("Skipping token validation for whitelisted path: {}", path);
            return chain.filter(exchange);
        }

        // Skip token validation for AuthController paths
        if (isAuthControllerPath(path)) {
            log.debug("Skipping token validation for AuthController path: {}", path);
            return chain.filter(exchange);
        }

        log.debug("Validating token for path: {}", path);

        // Validate token and continue with the chain if valid
        return tokenValidationService.validateTokenFromExchange(exchange)
                .flatMap(token -> {
                    log.debug("Token validated successfully for path: {}", path);

                    // Add the subject to the exchange attributes for potential use by controllers
                    String subject = tokenValidationService.getSubjectFromToken(token);
                    exchange.getAttributes().put("subject", subject);

                    return chain.filter(exchange);
                })
                .onErrorResume(ResponseStatusException.class, error -> {
                    log.warn("Token validation failed for path {}: {}", path, error.getMessage());
                    // Return the original error to maintain the correct status code (e.g., 401 for unauthorized)
                    return Mono.error(error);
                })
                .onErrorResume(error -> {
                    log.error("Unexpected error during token validation for path {}: {}", path, error.getMessage(), error);
                    // For security endpoints, it's better to return 401 Unauthorized than 500 Internal Server Error
                    // This prevents information leakage and provides a clearer response to the client
                    return Mono.error(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or missing authentication"));
                });
    }

    /**
     * Checks if the path is whitelisted (doesn't require token validation)
     *
     * @param path the request path
     * @return true if the path is whitelisted, false otherwise
     */
    private boolean isWhitelisted(String path) {
        // Check if the path ends with any of the whitelisted paths
        // This handles the case where the path has a prefix like /api/auth
        return Arrays.stream(authorizedPaths)
                .anyMatch(whitelistedPath -> path.endsWith(whitelistedPath) || 
                          // Also check if the path starts with the whitelisted path (original behavior)
                          path.startsWith(whitelistedPath));
    }

    /**
     * Checks if the path belongs to the AuthController
     *
     * @param path the request path
     * @return true if the path belongs to the AuthController, false otherwise
     */
    private boolean isAuthControllerPath(String path) {
        return authControllerPaths.stream()
                .anyMatch(authPath -> {
                    if (authPath.contains("{")) {
                        // For paths with path variables, we need to check if the path starts with or ends with the part before the variable
                        // and contains the part after the variable
                        String[] parts = authPath.split("\\{");
                        String prefix = parts[0];

                        // Check if path starts with prefix or ends with prefix (to handle base-path)
                        if (path.startsWith(prefix) || path.endsWith(prefix) || path.contains("/api/auth" + prefix)) {
                            if (parts.length > 1) {
                                // If there's a part after the variable, check if the path contains it
                                String suffix = parts[1].substring(parts[1].indexOf('}') + 1);
                                return suffix.isEmpty() || path.contains(suffix);
                            }
                            return true;
                        }
                        return false;
                    } else {
                        // For paths without variables, check if path starts with or ends with authPath
                        return path.startsWith(authPath) || path.endsWith(authPath) || path.contains("/api/auth" + authPath);
                    }
                });
    }
}
