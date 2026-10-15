package com.alexabeauty.reporting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;
import java.util.UUID;

/**
 * Diagnostic preview response for an uploaded Excel import file (supports grouped V2 views).
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
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
        List<ExcelImportPreviewItemDto> previewItems,
        Integer totalProducts,
        List<ExcelGroupedProductPreviewDto> groupedProducts
) {
    public ExcelImportPreviewResponse(
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
        this(jobId, fileName, fileHash, totalRows, validRows, rejectedRows, duplicateSkuCount, isDuplicateFile, errors, previewItems, null, null);
    }
}
