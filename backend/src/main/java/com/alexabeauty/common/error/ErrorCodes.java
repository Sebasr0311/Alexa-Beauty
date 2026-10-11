package com.alexabeauty.common.error;

/**
 * Standard error codes across the Alexa Beauty platform.
 */
public final class ErrorCodes {

    private ErrorCodes() {
        // Prevent instantiation
    }

    public static final String RESOURCE_NOT_FOUND = "RESOURCE_NOT_FOUND";
    public static final String CONFLICT = "CONFLICT";
    public static final String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static final String BAD_REQUEST = "BAD_REQUEST";
    public static final String UNAUTHORIZED = "UNAUTHORIZED";
    public static final String FORBIDDEN = "FORBIDDEN";
    public static final String INTERNAL_SERVER_ERROR = "INTERNAL_SERVER_ERROR";
    public static final String INSUFFICIENT_STOCK = "INSUFFICIENT_STOCK";
    public static final String ORDER_EXPIRED = "ORDER_EXPIRED";
    public static final String IDEMPOTENCY_CONFLICT = "IDEMPOTENCY_CONFLICT";
    public static final String EXCEL_IMPORT_ERROR = "EXCEL_IMPORT_ERROR";
    public static final String PAYMENT_VERIFICATION_FAILED = "PAYMENT_VERIFICATION_FAILED";
}
