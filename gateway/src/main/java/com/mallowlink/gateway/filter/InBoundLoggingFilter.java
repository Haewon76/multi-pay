package com.mallowlink.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallowlink.gateway.model.InBoundLogginLogs;
import com.mallowlink.gateway.service.KafkaLogService;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Map;
import java.util.Optional;

import static com.mallowlink.gateway.filter.AuthorizationFilter.CLIENT_HEADER;

@Slf4j
@Component
public class InBoundLoggingFilter implements GlobalFilter, Ordered {

    private static final int ORDER_LOGGING_FILTER = -999;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    KafkaLogService kafkaLogService;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String clientId = request.getHeaders().getFirst("clientId");

        // Log the request
        log.debug("Request: [{}] {} {} from {}", 
                clientId,
                request.getMethod(), 
                request.getURI(), 
                request.getRemoteAddress());

        // Log request headers
        request.getHeaders().forEach((name, values) -> {
            values.forEach(value -> log.debug("Request Header: [{}] {}: {}", clientId, name, value));
        });

        // Record the start time
        long startTime = System.currentTimeMillis();

        // Continue the filter chain and log the response when it's complete
        /*return chain.filter(exchange)
                .then(Mono.fromRunnable(() -> {
                    long endTime = System.currentTimeMillis();
                    long duration = endTime - startTime;

                    log.debug("Response: [{}] Status: {} Duration: {}ms",
                            clientId,
                            exchange.getResponse().getStatusCode(),
                            duration);

                    // Log response headers
                    exchange.getResponse().getHeaders().forEach((name, values) -> {
                        values.forEach(value -> log.debug("Response Header: [{}] {}: {}", clientId, name, value));
                    });
                }));*/
        return chain.filter(exchange).doFinally(signalType -> {
            long duration = System.currentTimeMillis() - startTime;
            ServerHttpResponse response = exchange.getResponse();

            String traceId = MDC.get("trace_id");
            String spanId = MDC.get("span_id");
            String traceFlags = MDC.get("trace_flags");

            String clientToken = request.getHeaders().getFirst(CLIENT_HEADER);
            String serviceName = extractApiName(request.getPath().value());
            try {
                InBoundLogginLogs inboundLogginLogs = InBoundLogginLogs.of(
                        traceId,
                        spanId,
                        serviceName,
                        request.getMethod().name(),
                        request.getPath().value(),
                        response.getStatusCode() != null ? response.getStatusCode().value() : 500,
                        duration,
                        Optional.ofNullable(request.getRemoteAddress())
                                .map(addr -> addr.getAddress().getHostAddress())
                                .orElse("unknown"),
                        request.getHeaders().getFirst("User-Agent"),
                        objectMapper.writeValueAsString(
                                Map.of(
                                CLIENT_HEADER, clientToken != null ? clientToken : ""
//                            ,"Authorization", "***" // 마스킹 처리
                                )
                        ),
                        objectMapper.writeValueAsString(request.getQueryParams().toSingleValueMap()),
                        0, // Body size 측정은 별도 필요
                        null);

                String json = new ObjectMapper().writeValueAsString(inboundLogginLogs);
                log.info("Logging gateway request: {}", json);

                // Send log to Kafka
                kafkaLogService.sendInboundLog(inboundLogginLogs);


            } catch (JsonProcessingException e) {
                log.error("Failed to log gateway request", e);
            }
        });
    }

    private String extractApiName(String path) {
        // 경로가 비어있거나 null인 경우 처리
        if (path == null || path.isEmpty() || path.equals("/")) {
            return "root";
        }

        // 슬래시로 분리하고 첫 번째 의미 있는 경로 부분 가져오기
        String[] parts = path.split("/");
        for (String part : parts) {
            if (!part.isEmpty()) {
                // 버전 접두사(v1, v2 등)가 있으면 건너뛰기 옵션
                if (part.matches("v\\d+") || part.matches("api")) {
                    continue;
                }
                return part;
            }
        }

        // 기본값
        return "unknown";
    }

    @Override
    public int getOrder() {
        return ORDER_LOGGING_FILTER;
    }
}
