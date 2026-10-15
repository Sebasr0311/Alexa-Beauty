package com.alexabeauty.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record SalesSummaryDto(
        BigDecimal totalSales,
        long paidOrdersCount,
        long totalUnitsSold,
        BigDecimal averageTicket,
        LocalDate startDate,
        LocalDate endDate,
        BigDecimal merchandiseRevenue,
        BigDecimal coveredMerchandiseRevenue,
        BigDecimal knownCostTotal,
        BigDecimal grossProfit,
        BigDecimal grossMarginPercentage,
        long unitsWithoutCost
 ) {
    public SalesSummaryDto(BigDecimal totalSales, long paidOrdersCount, long totalUnitsSold, BigDecimal averageTicket,
            LocalDate startDate, LocalDate endDate) {
        this(totalSales, paidOrdersCount, totalUnitsSold, averageTicket, startDate, endDate,
                BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, null, totalUnitsSold);
    }
}
