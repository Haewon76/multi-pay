package com.mallowlink.gateway.model;

import java.time.LocalDateTime;

public record OutBoundLoggingLogs (
        String timestamp,         // 로그 시간
        String traceId,           // Trace ID
        String spanId,            // Span ID
        String service,           // 서비스명
        String method,            // GET, POST 등
        String url,               // 요청한 외부 URL
        String requestHeaders,    // 요청 헤더 (JSON String)
        String requestBody,       // 요청 바디 (문자열)
        int status,               // 응답 상태 코드
        String responseHeaders,   // 응답 헤더 (JSON String)
        String responseBody,      // 응답 바디 (문자열)
        long durationMs,          // 처리 시간
        String error              // 에러 메시지 (nullable)
) {
    public static OutBoundLoggingLogs of(
            String traceId,
            String spanId,
            String service,
            String method,
            String url,
            String requestHeadersJson,
            String requestBody,
            int status,
            String responseHeadersJson,
            String responseBody,
            long durationMs,
            String error
    ) {
        return new OutBoundLoggingLogs(
                LocalDateTime.now().toString(),
                traceId,
                spanId,
                service,
                method,
                url,
                requestHeadersJson,
                requestBody,
                status,
                responseHeadersJson,
                responseBody,
                durationMs,
                error
        );
    }
}
