package com.alexabeauty.order.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemRequest {

    @NotNull(message = "El ID de la variante es obligatorio")
    private UUID variantId;

    @Min(value = 1, message = "La cantidad mínima por ítem es 1")
    private int quantity;
}
