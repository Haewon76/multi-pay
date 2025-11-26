package com.mallowlink.gateway.service;

import com.mallowlink.common.service.TokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Slf4j
@Service
public class ClientServiceImpl implements ClientService {

    private final WebClient webClient;
    private final TokenService tokenService;
    private static final String AUTH_SERVICE_URL = "http://auth-rollout-active-svc:8080";
    private static final String CLIENT_EXISTS_ENDPOINT = "/api/auth/client/exists/";

    public ClientServiceImpl(WebClient.Builder webClientBuilder, TokenService tokenService) {
        this.webClient = webClientBuilder.baseUrl(AUTH_SERVICE_URL).build();
        this.tokenService = tokenService;
    }

    @Override
    public Mono<String> validateClientToken(String clientToken) {
        log.debug("Validating client UUID: {}", clientToken);

        return tokenService.withAuthToken(token -> 
            webClient.get()
                .uri(CLIENT_EXISTS_ENDPOINT + clientToken)
                .header(AUTHORIZATION, tokenService.createAuthorizationHeader(token))
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    log.debug("Client validation response: {}", response);
                    return response;
                })
        ).onErrorResume(error -> {
            log.error("Error validating client token: {}", clientToken, error);
            return Mono.empty();
        });
    }
}
