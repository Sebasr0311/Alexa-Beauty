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
        BigDecimal referenceCost,
        Integer totalStockOnHand,
        int totalAvailableStock,
        String coverImageUrl,
        java.util.List<String> imageUrls,
        java.util.List<ProductVariantResponse> variants,
        Instant createdAt,
        Instant updatedAt
) {
    public ProductSummaryResponse(
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
        BigDecimal referenceCost,
            Integer totalStockOnHand,
            int totalAvailableStock,
            String coverImageUrl,
            java.util.List<ProductVariantResponse> variants,
            Instant createdAt,
            Instant updatedAt
    ) {
        this(
                id,
                sku,
                name,
                slug,
                brand,
                category,
                description,
                active,
                featured,
                minPrice,
                maxPrice,
                referenceCost,
                totalStockOnHand,
                totalAvailableStock,
                coverImageUrl,
                coverImageUrl != null ? java.util.List.of(coverImageUrl) : java.util.List.of(),
                variants,
                createdAt,
                updatedAt
        );
    }
}
