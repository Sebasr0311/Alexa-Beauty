package com.alexabeauty.common.error;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown on business state conflict, concurrent edit, or unique violation (HTTP 409).
 */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, ErrorCodes.CONFLICT, message);
    }

    public ConflictException(String message, Map<String, Object> details) {
        super(HttpStatus.CONFLICT, ErrorCodes.CONFLICT, message, details);
    }

    public ConflictException(String errorCode, String message, Map<String, Object> details) {
        super(HttpStatus.CONFLICT, errorCode, message, details);
    }
}
