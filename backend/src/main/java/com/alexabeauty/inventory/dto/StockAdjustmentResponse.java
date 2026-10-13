package com.alexabeauty.inventory.dto;

import java.time.Instant;
import java.util.UUID;

public record StockAdjustmentResponse(
        UUID variantId,
        String sku,
        String productName,
        int previousStockOnHand,
        int newStockOnHand,
        int stockReserved,
        int availableStock,
        int quantityDelta,
        String reason,
        Instant updatedAt
) {
}
