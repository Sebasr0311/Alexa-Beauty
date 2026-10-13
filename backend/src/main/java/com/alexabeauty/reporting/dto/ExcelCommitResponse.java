package com.alexabeauty.reporting.dto;

import java.util.UUID;

/**
 * Result of committing an Excel product import job.
 */
public record ExcelCommitResponse(
        UUID jobId,
        int importedProductsCount,
        int totalInitialStockAdded,
        String status
) {
}
