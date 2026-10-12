package com.alexabeauty.common.error;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when business or input validation fails (HTTP 400).
 */
public class ValidationException extends ApiException {

    public ValidationException(String message) {
        super(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, message);
    }

    public ValidationException(String message, Map<String, Object> details) {
        super(HttpStatus.BAD_REQUEST, ErrorCodes.VALIDATION_ERROR, message, details);
    }

    public ValidationException(String errorCode, String message, Map<String, Object> details) {
        super(HttpStatus.BAD_REQUEST, errorCode, message, details);
    }
}
