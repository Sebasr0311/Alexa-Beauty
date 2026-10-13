package com.alexabeauty.order.dto;

import com.alexabeauty.order.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusTransitionRequest {

    @NotNull(message = "El nuevo estado es requerido")
    private OrderStatus newStatus;

    private String notes;
}
