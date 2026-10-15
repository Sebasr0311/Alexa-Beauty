package com.alexabeauty.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductSummaryResponse(
        UUID id,
        String sku,
        String name,
        String slug,
        BrandResponse brand,
        CategoryResponse category,
        String description,
        boolean active,
        boolean featured,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        Integer totalStockOnHand,
        int totalAvailableStock,
        String coverImageUrl,
        java.util.List<ProductVariantResponse> variants,
        Instant createdAt,
        Instant updatedAt
) {}
