
package com.mallowlink.gateway.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallowlink.common.exception.ExceptionType;
import com.mallowlink.common.model.response.ErrorResponse;
import com.mallowlink.gateway.service.ClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuthorizationFilter implements WebFilter {

    public static final String CLIENT_HEADER = "ML-Client-Id";
    private static final String CLIENT_ID_HEADER = "CLIENT-ID";
    private static final String CLIENT_ID_REGEX = "^[0-9a-fA-F\\-]{36}$";
    private static final int MAX_CLIENT_ID_LENGTH = 128;

    private final ClientService clientService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {

        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.info("Request path: {}", path);
        // Whitelist 처리
        if (isWhitelisted(path)) {
            log.debug("Whitelisted path: {}", path);
            return chain.filter(exchange);
        }

        // Continue with client token validation if present
        String clientToken = request.getHeaders().getFirst(CLIENT_HEADER);
        if (clientToken == null) {
            log.debug("Request without client token: {}", clientToken);
            return reject(exchange, ExceptionType.CLIENT_TOKEN_REQUIRED);
        }

        log.debug("Validating client token: {}", clientToken);

        return validateClientToken(clientToken)
                .flatMap(clientId -> {
                    if (clientId != null && !clientId.isEmpty()) {
                        log.debug("Client token validated successfully, clientId: {}", clientId);
                        exchange.getAttributes().put("clientId", clientId);
                        return chain.filter(exchange);
                    } else {
                        return reject(exchange, ExceptionType.INVALID_CLIENT_TOKEN);
                    }
                })
                .onErrorResume(IllegalArgumentException.class, e -> {
                    log.error("Client token format validation failed: {}", e.getMessage());
                    // Find the matching exception type based on the message
                    ExceptionType exceptionType = ExceptionType.CLIENT_ID_INVALID_FORMAT; // Default
                    for (ExceptionType type : new ExceptionType[]{
                            ExceptionType.CLIENT_ID_EMPTY,
                            ExceptionType.CLIENT_ID_TOO_LONG,
                            ExceptionType.CLIENT_ID_INVALID_FORMAT
                    }) {
                        if (e.getMessage().equals(type.getMessage())) {
                            exceptionType = type;
                            break;
                        }
                    }
                    return reject(exchange, exceptionType);
                })
                .onErrorResume(WebClientResponseException.class, e -> {
                    log.error("Error validating client token: {}", e.getMessage());
                    return reject(exchange, ExceptionType.CLIENT_VALIDATION_ERROR);
                })
                .onErrorResume(e -> {
                    // Catch-all for any other exceptions
                    log.error("Unexpected error during client token validation: {}", e.getMessage());
                    return reject(exchange, ExceptionType.CLIENT_VALIDATION_ERROR);
                });
    }

    /**
     * 클라이언트 토큰을 검증합니다.
     *
     * @param clientToken 검증할 클라이언트 토큰
     * @return 유효성 여부를 나타내는 Mono<String>
     */
    private Mono<String> validateClientToken(String clientToken) {
        return validateClientIdFormat(clientToken)
                .map(exceptionType -> {
                    log.warn("Client token format validation failed: {}", exceptionType.getMessage());
                    // We'll handle this error in the flatMap of the filter method
                    return Mono.<String>error(new IllegalArgumentException(exceptionType.getMessage()));
                })
                .orElseGet(() -> clientService.validateClientToken(clientToken));
    }

    /**
     * 클라이언트 ID 형식을 검증합니다.
     *
     * @param clientToken 검증할 클라이언트 토큰
     * @return 오류 유형을 담은 Optional 또는 형식이 유효한 경우 empty Optional
     */
    private Optional<ExceptionType> validateClientIdFormat(String clientToken) {
        if (clientToken.trim().isEmpty()) {
            return Optional.of(ExceptionType.CLIENT_ID_EMPTY);
        }
        if (clientToken.length() > MAX_CLIENT_ID_LENGTH) {
            return Optional.of(ExceptionType.CLIENT_ID_TOO_LONG);
        }
        if (!clientToken.matches(CLIENT_ID_REGEX)) {
            return Optional.of(ExceptionType.CLIENT_ID_INVALID_FORMAT);
        }
        return Optional.empty();
    }

    /**
     * 인증 실패 시 오류를 반환합니다.
     *
     * @param exchange ServerWebExchange
     * @param exceptionType 오류 유형
     * @return 오류를 포함한 Mono<Void>
     */
    private Mono<Void> reject(ServerWebExchange exchange, ExceptionType exceptionType) {
        log.warn("Authorization rejected: {}", exceptionType.getMessage());

        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        ErrorResponse errorResponse = new ErrorResponse(
                String.valueOf(exceptionType.getCode()),
                exceptionType.getMessage()
        );

        try {
            ObjectMapper objectMapper = new ObjectMapper();
            byte[] bytes = objectMapper.writeValueAsBytes(errorResponse);
            DataBuffer buffer = response.bufferFactory().wrap(bytes);
            return response.writeWith(Mono.just(buffer));
        } catch (Exception e) {
            log.error("Error writing error response", e);
            return Mono.error(e);
        }
    }

    /**
     * 추후 whitelist 정리 필요
     * 어드민에서 devOffice 요청에 따른 방법 추가
     */
    private static final List<String> WHITELIST = List.of(
            "/client/get/",
            "/client/exists/",
            "/devOffice/client/create/",
            "/devOffice/client/update/",
            "/devOffice/client/delete/"
    );

    private static boolean isWhitelisted(String path) {
        return WHITELIST.stream().anyMatch(path::contains);
    }
}
