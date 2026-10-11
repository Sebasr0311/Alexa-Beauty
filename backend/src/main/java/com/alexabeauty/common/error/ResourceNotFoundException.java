package com.alexabeauty.common.error;

import org.springframework.http.HttpStatus;

import java.util.Map;

/**
 * Thrown when an expected entity or resource is not found (HTTP 404).
 */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resourceName, Object identifier) {
        super(
            HttpStatus.NOT_FOUND,
            ErrorCodes.RESOURCE_NOT_FOUND,
            String.format("%s with identifier '%s' not found", resourceName, identifier),
            Map.of("resource", resourceName, "identifier", identifier)
        );
    }

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, ErrorCodes.RESOURCE_NOT_FOUND, message);
    }
}
