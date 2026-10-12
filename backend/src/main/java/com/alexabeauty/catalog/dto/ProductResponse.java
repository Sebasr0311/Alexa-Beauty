package com.alexabeauty.catalog.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ProductResponse(
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
) {
    public static ProductResponse from(ProductDetailResponse detail) {
        if (detail == null) return null;
        return new ProductResponse(
                detail.id(),
                detail.sku(),
                detail.name(),
                detail.slug(),
                detail.brand(),
                detail.category(),
                detail.description(),
                detail.active(),
                detail.featured(),
                detail.minPrice(),
                detail.maxPrice(),
                detail.totalStockOnHand(),
                detail.totalAvailableStock(),
                detail.variants(),
                detail.images(),
                detail.createdAt(),
                detail.updatedAt()
        );
    }
}
