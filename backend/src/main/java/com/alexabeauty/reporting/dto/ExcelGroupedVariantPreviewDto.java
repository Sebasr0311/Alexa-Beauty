package com.alexabeauty.reporting.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

/**
 * Preview summary for a variant row within a grouped product.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ExcelGroupedVariantPreviewDto(
        int rowNumber,
        String sku,
        String shadeCode,
        String shadeName,
        String shadeHex,
        String presentation,
        BigDecimal salePrice,
        BigDecimal referenceCost,
        int initialStock,
        int minStock
) {
}
