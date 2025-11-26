package com.mallowlink.common.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Enum representing client exception types with their corresponding codes and messages.
 * Used for standardizing error responses across services.
 */
@Getter
@RequiredArgsConstructor
public enum ExceptionType {

    // Success response
    SUCCESS(200, "SUCCESS"),

    // Client authentication errors (40 - Unauthorized)
    CLIENT_TOKEN_REQUIRED(4001, "Client token is required"),
    INVALID_CLIENT_TOKEN(4002, "Invalid client token"),
    CLIENT_VALIDATION_ERROR(4003, "Error during client validation"),

    // Client token format errors (41 - Unauthorized)
    CLIENT_ID_EMPTY(4101, "Client ID cannot be empty"),
    CLIENT_ID_TOO_LONG(4102, "Client ID exceeds maximum allowed length"),
    CLIENT_ID_INVALID_FORMAT(4103, "Client ID has invalid format"),

    // Token service errors (50 - Internal Server Error)
    TOKEN_FETCH_ERROR(5001, "Error fetching new token"),
    TOKEN_REFRESH_ERROR(5002, "Error refreshing token"),

    // Auth service errors (53 - Service Unavailable)
    AUTH_SERVICE_UNAVAILABLE(5301, "Authentication service is unavailable"),

    // Connection errors (54 - Service Unavailable)
    CONNECTION_ERROR(5401, "Connection to service failed");

    private final int code;
    private final String message;
}
