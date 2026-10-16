package com.alexabeauty.reporting.dto;

import com.alexabeauty.payment.domain.PaymentMethod;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;

public record CreatePhysicalSaleRequest(
        @Size(max = 150, message = "El nombre del cliente no puede superar los 150 caracteres")
        String customerName,

        @Size(max = 50, message = "El teléfono del cliente no puede superar los 50 caracteres")
        String customerPhone,

        @NotNull(message = "El método de pago es obligatorio")
        PaymentMethod paymentMethod,

        @Size(max = 100, message = "La referencia de pago no puede superar los 100 caracteres")
        String transferReference,

        @DecimalMin(value = "0.00", message = "El descuento no puede ser negativo")
        BigDecimal discount,

        @Size(max = 500, message = "Las notas no pueden superar los 500 caracteres")
        String notes,

        @NotEmpty(message = "Debe incluir al menos un producto en la venta")
        @Valid
        List<PhysicalSaleItemRequest> items
) {
}
