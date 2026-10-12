package com.alexabeauty.catalog.dto;

import com.alexabeauty.catalog.domain.ProductVariant;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record ProductVariantResponse(
        UUID id,
        UUID productId,
        String sku,
        String shadeName,
        String presentation,
        BigDecimal salePrice,
        BigDecimal referenceCost,
        int minStock,
        boolean active,
        int stockOnHand,
        int stockReserved,
        int availableStock,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductVariantResponse from(ProductVariant variant) {
        if (variant == null) return null;
        int available = Math.max(0, variant.getStockOnHand() - variant.getStockReserved());
        return new ProductVariantResponse(
                variant.getId(),
                variant.getProduct() != null ? variant.getProduct().getId() : null,
                variant.getSku(),
                variant.getShadeName(),
                variant.getPresentation(),
                variant.getSalePrice(),
                variant.getReferenceCost(),
                variant.getMinStock(),
                variant.isActive(),
                variant.getStockOnHand(),
                variant.getStockReserved(),
                available,
                variant.getCreatedAt(),
                variant.getUpdatedAt()
        );
    }
}
