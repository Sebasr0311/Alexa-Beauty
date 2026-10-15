package com.alexabeauty.catalog.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
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
        Integer totalStockOnHand,
        int totalAvailableStock,
        List<ProductVariantResponse> variants,
        List<ProductImageResponse> images,
        Instant createdAt,
        Instant updatedAt
) {}
