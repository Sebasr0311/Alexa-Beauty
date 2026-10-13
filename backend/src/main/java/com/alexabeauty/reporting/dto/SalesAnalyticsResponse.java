package com.alexabeauty.reporting.dto;

import java.util.List;

public record SalesAnalyticsResponse(
        SalesSummaryDto summary,
        List<DailySalesDto> dailySales,
        List<TopProductDto> topProducts,
        List<PaymentMethodBreakdownDto> paymentMethods
) {}
