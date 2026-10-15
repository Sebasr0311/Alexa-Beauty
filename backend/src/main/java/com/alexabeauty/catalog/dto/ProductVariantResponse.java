package com.alexabeauty.catalog.dto;

import com.alexabeauty.catalog.domain.ProductVariant;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductVariantResponse(
        UUID id,
        UUID productId,
        String sku,
        String shadeName,
        String shadeCode,
        String shadeHex,
        String presentation,
        BigDecimal salePrice,
        BigDecimal referenceCost,
        Integer minStock,
        boolean active,
        Integer stockOnHand,
        Integer stockReserved,
        int availableStock,
        Instant createdAt,
        Instant updatedAt
) {
    public static ProductVariantResponse from(ProductVariant variant) {
        return adminView(variant);
    }

    public static ProductVariantResponse adminView(ProductVariant variant) {
        if (variant == null) return null;
        int available = Math.max(0, variant.getStockOnHand() - variant.getStockReserved());
        return new ProductVariantResponse(
                variant.getId(),
                variant.getProduct() != null ? variant.getProduct().getId() : null,
                variant.getSku(),
                variant.getShadeName(),
                variant.getShadeCode(),
                variant.getShadeHex(),
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

    public static ProductVariantResponse publicView(ProductVariant variant) {
        if (variant == null) return null;
        int available = Math.max(0, variant.getStockOnHand() - variant.getStockReserved());
        return new ProductVariantResponse(
                variant.getId(),
                variant.getProduct() != null ? variant.getProduct().getId() : null,
                variant.getSku(),
                variant.getShadeName(),
                variant.getShadeCode(),
                variant.getShadeHex(),
                variant.getPresentation(),
                variant.getSalePrice(),
                null, // referenceCost omitted for public security
                null, // minStock omitted
                variant.isActive(),
                null, // stockOnHand omitted
                null, // stockReserved omitted
                available,
                variant.getCreatedAt(),
                variant.getUpdatedAt()
        );
    }
}
