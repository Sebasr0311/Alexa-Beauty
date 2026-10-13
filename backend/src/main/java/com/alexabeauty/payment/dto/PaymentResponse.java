package com.alexabeauty.payment.dto;

import com.alexabeauty.payment.domain.Payment;
import com.alexabeauty.payment.domain.PaymentMethod;
import com.alexabeauty.payment.domain.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentResponse {

    private UUID id;
    private UUID orderId;
    private PaymentMethod paymentMethod;
    private String transferReference;
    private BigDecimal amount;
    private PaymentStatus status;
    private String verifiedByEmail;
    private Instant verifiedAt;
    private String notes;
    private String receiptImageUrl;
    private Instant createdAt;

    public static PaymentResponse from(Payment payment) {
        if (payment == null) {
            return null;
        }
        return PaymentResponse.builder()
                .id(payment.getId())
                .orderId(payment.getOrder() != null ? payment.getOrder().getId() : null)
                .paymentMethod(payment.getPaymentMethod())
                .transferReference(payment.getTransferReference())
                .amount(payment.getAmount())
                .status(payment.getStatus())
                .verifiedByEmail(payment.getVerifiedBy() != null ? payment.getVerifiedBy().getEmail() : null)
                .verifiedAt(payment.getVerifiedAt())
                .notes(payment.getNotes())
                .receiptImageUrl(payment.getReceiptImageUrl())
                .createdAt(payment.getCreatedAt())
                .build();
    }
}
