package com.alexabeauty.reporting.dto;

import com.alexabeauty.reporting.domain.ExcelImportError;

/**
 * Diagnostic error detail for a rejected Excel row.
 */
public record ExcelRowErrorResponse(
        int rowNumber,
        String columnName,
        String rawValue,
        String errorMessage,
        String suggestion
) {
    public static ExcelRowErrorResponse from(ExcelImportError error) {
        return new ExcelRowErrorResponse(
                error.getRowNumber(),
                error.getColumnName(),
                error.getRawValue(),
                error.getErrorMessage(),
                error.getSuggestion()
        );
    }
}
