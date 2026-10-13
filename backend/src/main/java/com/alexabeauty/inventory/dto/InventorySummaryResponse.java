package com.alexabeauty.inventory.dto;

import com.alexabeauty.catalog.domain.ProductVariant;

import java.util.UUID;

public record InventorySummaryResponse(
        UUID variantId,
        UUID productId,
        String sku,
        String productName,
        String brandName,
        String categoryName,
        String shadeName,
        int stockOnHand,
        int stockReserved,
        int availableStock,
        int minStock,
        boolean isLowStock,
        boolean active
) {
    public static InventorySummaryResponse from(ProductVariant variant) {
        if (variant == null) return null;
        int stockOnHand = variant.getStockOnHand();
        int stockReserved = variant.getStockReserved();
        int availableStock = stockOnHand - stockReserved;
        int minStock = variant.getMinStock();
        boolean isLowStock = availableStock <= minStock;

        UUID productId = variant.getProduct() != null ? variant.getProduct().getId() : null;
        String productName = variant.getProduct() != null ? variant.getProduct().getName() : null;
        String brandName = (variant.getProduct() != null && variant.getProduct().getBrand() != null)
                ? variant.getProduct().getBrand().getName() : null;
        String categoryName = (variant.getProduct() != null && variant.getProduct().getCategory() != null)
                ? variant.getProduct().getCategory().getName() : null;

        return new InventorySummaryResponse(
                variant.getId(),
                productId,
                variant.getSku(),
                productName,
                brandName,
                categoryName,
                variant.getShadeName(),
                stockOnHand,
                stockReserved,
                availableStock,
                minStock,
                isLowStock,
                variant.isActive()
        );
    }
}
