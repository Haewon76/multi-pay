package com.mallowlink.gateway.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
@RequiredArgsConstructor
public class ExternalApiClient {

    @Value("${internal.service.gateway.url")
    private String gatewayUrl;

    private final WebClient webClient = WebClient.builder()
            .baseUrl(gatewayUrl)
            .build();




}
