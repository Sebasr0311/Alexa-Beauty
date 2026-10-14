package com.alexabeauty.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

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
        int totalStockOnHand,
        int totalAvailableStock,
        String coverImageUrl,
        java.util.List<ProductVariantResponse> variants,
        Instant createdAt,
        Instant updatedAt
) {}
