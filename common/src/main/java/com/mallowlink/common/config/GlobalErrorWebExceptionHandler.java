package com.mallowlink.common.config;

import com.mallowlink.common.exception.ExceptionType;
import com.mallowlink.common.model.response.ErrorResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.autoconfigure.web.WebProperties;
import org.springframework.boot.autoconfigure.web.reactive.error.AbstractErrorWebExceptionHandler;
import org.springframework.boot.web.reactive.error.ErrorAttributes;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerCodecConfigurer;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.*;
import org.springframework.web.server.ResponseStatusException;
import reactor.core.publisher.Mono;

@Component
@Order(-2) // Higher precedence than the default error handler
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.REACTIVE)
public class GlobalErrorWebExceptionHandler extends AbstractErrorWebExceptionHandler {

    public GlobalErrorWebExceptionHandler(ErrorAttributes errorAttributes,
                                         WebProperties webProperties,
                                         ApplicationContext applicationContext,
                                         ServerCodecConfigurer serverCodecConfigurer) {
        super(errorAttributes, webProperties.getResources(), applicationContext);
        super.setMessageWriters(serverCodecConfigurer.getWriters());
        super.setMessageReaders(serverCodecConfigurer.getReaders());
    }

    @Override
    protected RouterFunction<ServerResponse> getRoutingFunction(ErrorAttributes errorAttributes) {
        return RouterFunctions.route(RequestPredicates.all(), this::renderErrorResponse);
    }

    private Mono<ServerResponse> renderErrorResponse(ServerRequest request) {
        Throwable error = getError(request);

        // Check if it's a Netty connection exception
        if (isNettyConnectionException(error)) {
            ErrorResponse errorResponse = new ErrorResponse(
                    String.valueOf(ExceptionType.CONNECTION_ERROR.getCode()),
                    ExceptionType.CONNECTION_ERROR.getMessage()
            );

            return ServerResponse.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(errorResponse));
        }

        HttpStatus status = determineHttpStatus(error);

        // Customize the error response format
        ErrorResponse errorResponse = new ErrorResponse(
                String.valueOf(status.value()),
                getErrorMessage(error)
        );

        return ServerResponse.status(status)
                .contentType(MediaType.APPLICATION_JSON)
                .body(BodyInserters.fromValue(errorResponse));
    }

    private boolean isNettyConnectionException(Throwable error) {
        return error != null && 
               error.getClass().getName().contains("AbstractChannel$AnnotatedConnectException");
    }

    private HttpStatus determineHttpStatus(Throwable error) {
        if (error instanceof ResponseStatusException) {
            return HttpStatus.valueOf(((ResponseStatusException) error).getStatusCode().value());
        }
        return HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private String getErrorMessage(Throwable error) {
        if (error instanceof ResponseStatusException) {
            ResponseStatusException responseStatusException = (ResponseStatusException) error;
            String errorMessage = responseStatusException.getReason();
            if (errorMessage != null && !errorMessage.isEmpty()) {
                return errorMessage;
            }
        }

        if (error.getMessage() != null && !error.getMessage().isEmpty()) {
            return error.getMessage();
        }

        return "An error occurred";
    }
}
