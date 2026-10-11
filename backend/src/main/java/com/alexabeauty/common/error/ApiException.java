package com.alexabeauty.common.error;

import org.springframework.http.HttpStatus;

import java.util.Collections;
import java.util.Map;

/**
 * Base domain and application exception for Alexa Beauty API.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;
    private final String errorCode;
    private final Map<String, Object> details;

    public ApiException(HttpStatus status, String errorCode, String message) {
        this(status, errorCode, message, Collections.emptyMap());
    }

    public ApiException(HttpStatus status, String errorCode, String message, Map<String, Object> details) {
        super(message);
        this.status = status;
        this.errorCode = errorCode;
        this.details = details != null ? details : Collections.emptyMap();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
