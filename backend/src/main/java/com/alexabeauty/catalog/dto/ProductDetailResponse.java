package com.alexabeauty.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductDetailResponse(
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
        List<ProductVariantResponse> variants,
        List<ProductImageResponse> images,
        Instant createdAt,
        Instant updatedAt
) {}
