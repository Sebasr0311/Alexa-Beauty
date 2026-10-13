package com.alexabeauty.reporting.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SaleOrderItemResponse(
        UUID id,
        String sku,
        String productName,
        String brandName,
        String shadeName,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal,
        BigDecimal costSnapshot
) {}
