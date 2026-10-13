package com.alexabeauty.inventory.dto;

import com.alexabeauty.inventory.domain.StockReceiptItem;

import java.math.BigDecimal;
import java.util.UUID;

public record StockReceiptItemResponse(
        UUID id,
        UUID variantId,
        String sku,
        String productName,
        String shadeName,
        int quantity,
        BigDecimal unitCost,
        String notes
) {
    public static StockReceiptItemResponse from(StockReceiptItem item) {
        if (item == null) return null;
        String sku = item.getVariant() != null ? item.getVariant().getSku() : null;
        String productName = (item.getVariant() != null && item.getVariant().getProduct() != null)
                ? item.getVariant().getProduct().getName() : null;
        String shadeName = item.getVariant() != null ? item.getVariant().getShadeName() : null;

        return new StockReceiptItemResponse(
                item.getId(),
                item.getVariant() != null ? item.getVariant().getId() : null,
                sku,
                productName,
                shadeName,
                item.getQuantity(),
                item.getUnitCost(),
                item.getNotes()
        );
    }
}
