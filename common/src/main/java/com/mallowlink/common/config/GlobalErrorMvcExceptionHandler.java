package com.mallowlink.common.config;

import com.mallowlink.common.exception.ExceptionType;
import com.mallowlink.common.model.response.ErrorResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.server.ResponseStatusException;

import java.net.ConnectException;

/**
 * Global exception handler for MVC applications.
 * Provides standardized error responses for various exception types.
 */
@ControllerAdvice
@Order(-2) // Higher precedence than the default error handler
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class GlobalErrorMvcExceptionHandler {

    /**
     * Handles all exceptions that are not specifically handled by other methods.
     *
     * @param ex The exception that was thrown
     * @return A standardized error response
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception ex) {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        
        // Customize the error response format
        ErrorResponse errorResponse = new ErrorResponse(
                String.valueOf(status.value()),
                getErrorMessage(ex)
        );
        
        return new ResponseEntity<>(errorResponse, status);
    }
    
    /**
     * Handles ResponseStatusException specifically to extract the correct status code.
     *
     * @param ex The ResponseStatusException that was thrown
     * @return A standardized error response with the appropriate status code
     */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ErrorResponse> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        
        // Customize the error response format
        ErrorResponse errorResponse = new ErrorResponse(
                String.valueOf(status.value()),
                getErrorMessage(ex)
        );
        
        return new ResponseEntity<>(errorResponse, status);
    }
    
    /**
     * Handles connection exceptions specifically.
     *
     * @param ex The ConnectException that was thrown
     * @return A standardized error response for connection errors
     */
    @ExceptionHandler(ConnectException.class)
    public ResponseEntity<ErrorResponse> handleConnectException(ConnectException ex) {
        // Use the predefined CONNECTION_ERROR type
        ErrorResponse errorResponse = new ErrorResponse(
                String.valueOf(ExceptionType.CONNECTION_ERROR.getCode()),
                ExceptionType.CONNECTION_ERROR.getMessage()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.SERVICE_UNAVAILABLE);
    }
    
    /**
     * Extracts a meaningful error message from the exception.
     *
     * @param error The exception to extract the message from
     * @return A meaningful error message
     */
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