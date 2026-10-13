package com.alexabeauty.reporting.dto;

import java.util.List;

public record SalesListResponse(
        SalesSummaryDto summary,
        List<SaleOrderResponse> orders,
        int page,
        int size,
        long totalElements,
        int totalPages
) {}
