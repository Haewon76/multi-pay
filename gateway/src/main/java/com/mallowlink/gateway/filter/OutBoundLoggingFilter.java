package com.mallowlink.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mallowlink.gateway.model.OutBoundLoggingLogs;
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

@Slf4j
@Component
public class OutBoundLoggingFilter implements GlobalFilter, Ordered {

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

        return chain.filter(exchange).doFinally(signalType -> {
            long duration = System.currentTimeMillis() - startTime;
            ServerHttpResponse response = exchange.getResponse();

            String traceId = MDC.get("trace_id");
            String spanId = MDC.get("span_id");
            String traceFlags = MDC.get("trace_flags");

            try {
                OutBoundLoggingLogs outBoundLoggingLogs = OutBoundLoggingLogs.of(
                        traceId,
                        spanId,
                        clientId,
                        request.getMethod().toString(),
                        request.getURI().toString(),
                        request.getHeaders().toString(),
                        "",
                        response.getStatusCode().value(),
                        response.getHeaders().toString(),
                        "",
                        duration,
                        ""
                );

                String json = new ObjectMapper().writeValueAsString(outBoundLoggingLogs);
                log.info("Logging gateway request: {}", json);

                // Send log to Kafka
                kafkaLogService.sendOutboundLog(outBoundLoggingLogs);

            } catch (JsonProcessingException e) {
                log.error("Failed to log gateway request", e);
            }
        });
    }



    @Override
    public int getOrder() {
        return ORDER_LOGGING_FILTER;
    }


}
