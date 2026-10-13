package com.alexabeauty.reporting.dto;

import java.util.List;
import java.util.UUID;

/**
 * Diagnostic preview response for an uploaded Excel import file.
 */
public record ExcelImportPreviewResponse(
        UUID jobId,
        String fileName,
        String fileHash,
        int totalRows,
        int validRows,
        int rejectedRows,
        int duplicateSkuCount,
        boolean isDuplicateFile,
        List<ExcelRowErrorResponse> errors,
        List<ExcelImportPreviewItemDto> previewItems
) {
}
