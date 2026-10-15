package com.alexabeauty.reporting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.util.List;

/**
 * Preview summary for a product grouped from multiple Excel V2 variant rows.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExcelGroupedProductPreviewDto(
        String productCode,
        String name,
        String brand,
        String category,
        String description,
        boolean active,
        int totalStock,
        int variantCount,
        List<ExcelGroupedVariantPreviewDto> variants
) {
}
