package com.alexabeauty.reporting.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

public record DailySalesDto(
        LocalDate date,
        BigDecimal totalSales,
        long ordersCount,
        long unitsCount
) {}
