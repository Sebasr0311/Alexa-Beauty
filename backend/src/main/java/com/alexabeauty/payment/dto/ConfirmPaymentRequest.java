package com.alexabeauty.payment.dto;

import com.alexabeauty.payment.domain.PaymentMethod;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmPaymentRequest {

    @NotNull(message = "El método de pago es requerido")
    private PaymentMethod paymentMethod;

    @NotBlank(message = "La referencia de transferencia es requerida")
    private String transferReference;

    @NotNull(message = "El monto es requerido")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero")
    private BigDecimal amount;

    private String notes;

    private String receiptImageUrl;
}
