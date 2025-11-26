package com.mallowlink.gateway.model;

import java.time.LocalDateTime;

public record InBoundLogginLogs(
    String timestamp,
    String traceId,
    String spanId,
    String service,
    String method,
    String path,
    int status,
    long durationMs,
    String clientIp,
    String userAgent,
    String requestHeaders,
    String requestParams,
    long responseBodySize,
    String error
) {
    public static InBoundLogginLogs of(
            String traceId,
            String spanId,
            String service,
            String method,
            String path,
            int status,
            long durationMs,
            String clientIp,
            String userAgent,
            String requestHeaders,
            String requestParams,
            long responseBodySize,
            String error
    ) {
        return new InBoundLogginLogs(
                LocalDateTime.now().toString(),
                traceId,
                spanId,
                service,
                method,
                path,
                status,
                durationMs,
                clientIp,
                userAgent,
                requestHeaders,
                requestParams,
                responseBodySize,
                error
        );
    }
}
