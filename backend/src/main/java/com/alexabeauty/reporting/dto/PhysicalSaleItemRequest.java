package com.alexabeauty.reporting.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record PhysicalSaleItemRequest(
        @NotNull(message = "El ID de la variante es obligatorio")
        UUID variantId,

        @NotNull(message = "La cantidad es obligatoria")
        @Min(value = 1, message = "La cantidad mínima es 1")
        Integer quantity
) {
}
