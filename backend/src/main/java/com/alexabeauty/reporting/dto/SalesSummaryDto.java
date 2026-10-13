package com.alexabeauty.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesSummaryDto(
        BigDecimal totalSales,
        long paidOrdersCount,
        long totalUnitsSold,
        BigDecimal averageTicket,
        LocalDate startDate,
        LocalDate endDate
) {}
