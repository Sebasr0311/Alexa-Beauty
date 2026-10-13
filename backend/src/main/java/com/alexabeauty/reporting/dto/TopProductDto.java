package com.alexabeauty.reporting.dto;

import java.math.BigDecimal;

public record TopProductDto(
        int rank,
        String sku,
        String productName,
        String brandName,
        long unitsSold,
        BigDecimal grossRevenue,
        BigDecimal revenueSharePercentage
) {}
