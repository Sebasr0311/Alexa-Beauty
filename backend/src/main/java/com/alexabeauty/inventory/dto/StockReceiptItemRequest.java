package com.alexabeauty.inventory.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

public record StockReceiptItemRequest(
        @NotNull(message = "El ID de la variante es obligatorio")
        UUID variantId,

        @Min(value = 1, message = "La cantidad debe ser mayor o igual a 1")
        int quantity,

        @DecimalMin(value = "0.00", message = "El costo unitario no puede ser negativo")
        BigDecimal unitCost,

        String notes
) {
}
