package com.alexabeauty.inventory.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record StockAdjustmentRequest(
        @NotNull(message = "El ID de la variante es obligatorio")
        UUID variantId,

        @Min(value = 0, message = "El nuevo stock físico no puede ser negativo")
        int newStockOnHand,

        @NotBlank(message = "El motivo del ajuste es obligatorio")
        @Size(min = 5, message = "El motivo debe tener al menos 5 caracteres")
        String reason,

        String notes
) {
}
