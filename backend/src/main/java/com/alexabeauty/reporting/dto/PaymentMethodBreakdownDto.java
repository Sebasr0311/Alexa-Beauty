package com.alexabeauty.reporting.dto;

import java.math.BigDecimal;

public record PaymentMethodBreakdownDto(
        String paymentMethod,
        BigDecimal totalAmount,
        long orderCount,
        BigDecimal percentage
) {}
