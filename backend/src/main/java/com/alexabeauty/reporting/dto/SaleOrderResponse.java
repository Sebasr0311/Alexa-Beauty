package com.alexabeauty.reporting.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record SaleOrderResponse(
        UUID id,
        String reference,
        String customerName,
        String customerPhone,
        String paymentMethod,
        BigDecimal subtotal,
        BigDecimal discount,
        BigDecimal shippingFee,
        BigDecimal total,
        String verifiedBy,
        String status,
        String channel,
        Instant paidAt,
        String localPaidAt,
        List<SaleOrderItemResponse> items
) {}
