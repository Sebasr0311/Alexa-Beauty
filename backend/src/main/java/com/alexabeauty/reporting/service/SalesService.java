package com.alexabeauty.reporting.service;

import com.alexabeauty.payment.domain.PaymentMethod;
import com.alexabeauty.reporting.dto.CreatePhysicalSaleRequest;
import com.alexabeauty.reporting.dto.SaleOrderResponse;
import com.alexabeauty.reporting.dto.SalesAnalyticsResponse;
import com.alexabeauty.reporting.dto.SalesListResponse;
import com.alexabeauty.reporting.dto.TopProductDto;
import com.alexabeauty.security.UserPrincipal;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface SalesService {
    SalesListResponse getSales(LocalDate from, LocalDate to, PaymentMethod method, Pageable pageable);
    SaleOrderResponse createPhysicalSale(CreatePhysicalSaleRequest request, UserPrincipal actor);
    SalesAnalyticsResponse getAnalytics(LocalDate from, LocalDate to);
    List<TopProductDto> getTopProducts(LocalDate from, LocalDate to);
    byte[] exportSalesReport(LocalDate from, LocalDate to, PaymentMethod method, UserPrincipal actor);
}
