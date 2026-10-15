package com.alexabeauty.reporting.service;

import com.alexabeauty.catalog.domain.ProductVariant;
import com.alexabeauty.order.domain.Order;
import com.alexabeauty.order.domain.OrderItem;
import com.alexabeauty.order.domain.OrderStatus;
import com.alexabeauty.order.repository.OrderRepository;
import com.alexabeauty.payment.domain.Payment;
import com.alexabeauty.payment.domain.PaymentMethod;
import com.alexabeauty.payment.domain.PaymentStatus;
import com.alexabeauty.reporting.dto.DailySalesDto;
import com.alexabeauty.reporting.dto.PaymentMethodBreakdownDto;
import com.alexabeauty.reporting.dto.SaleOrderItemResponse;
import com.alexabeauty.reporting.dto.SaleOrderResponse;
import com.alexabeauty.reporting.dto.SalesAnalyticsResponse;
import com.alexabeauty.reporting.dto.SalesListResponse;
import com.alexabeauty.reporting.dto.SalesSummaryDto;
import com.alexabeauty.reporting.dto.TopProductDto;
import com.alexabeauty.reporting.excel.ExcelSalesReportGenerator;
import com.alexabeauty.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SalesServiceImpl implements SalesService {

    public static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");
    public static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OrderRepository orderRepository;
    private final ExcelSalesReportGenerator excelSalesReportGenerator;

    @Override
    @Transactional(readOnly = true)
    public SalesListResponse getSales(LocalDate from, LocalDate to, PaymentMethod method, Pageable pageable) {
        Instant startInstant = from.atStartOfDay(BOGOTA_ZONE).toInstant();
        Instant endInstant = to.atTime(LocalTime.MAX).atZone(BOGOTA_ZONE).toInstant();
        List<OrderStatus> statuses = OrderRepository.VERIFIED_PAID_STATUSES;

        // Fetch page for listing
        Page<Order> orderPage = orderRepository.findPaidOrders(statuses, startInstant, endInstant, method, pageable);

        // Fetch all matching orders in range for exact KPI summary
        List<Order> allOrders = orderRepository.findPaidOrdersList(statuses, startInstant, endInstant, method);
        SalesSummaryDto summary = calculateSummary(allOrders, from, to);

        List<SaleOrderResponse> orders = orderPage.getContent().stream()
                .map(this::toSaleOrderResponse)
                .toList();

        return new SalesListResponse(
                summary,
                orders,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages()
        );
    }

    @Override
    @Transactional(readOnly = true)
    public SalesAnalyticsResponse getAnalytics(LocalDate from, LocalDate to) {
        Instant startInstant = from.atStartOfDay(BOGOTA_ZONE).toInstant();
        Instant endInstant = to.atTime(LocalTime.MAX).atZone(BOGOTA_ZONE).toInstant();
        List<OrderStatus> statuses = OrderRepository.VERIFIED_PAID_STATUSES;

        List<Order> orders = orderRepository.findPaidOrdersList(statuses, startInstant, endInstant, null);
        SalesSummaryDto summary = calculateSummary(orders, from, to);
        List<DailySalesDto> dailySales = calculateDailySales(orders, from, to);
        List<TopProductDto> topProducts = calculateTopProducts(orders);
        List<PaymentMethodBreakdownDto> paymentMethods = calculatePaymentMethods(orders, summary.totalSales());

        return new SalesAnalyticsResponse(summary, dailySales, topProducts, paymentMethods);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopProductDto> getTopProducts(LocalDate from, LocalDate to) {
        Instant startInstant = from.atStartOfDay(BOGOTA_ZONE).toInstant();
        Instant endInstant = to.atTime(LocalTime.MAX).atZone(BOGOTA_ZONE).toInstant();
        List<OrderStatus> statuses = OrderRepository.VERIFIED_PAID_STATUSES;

        List<Order> orders = orderRepository.findPaidOrdersList(statuses, startInstant, endInstant, null);
        return calculateTopProducts(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportSalesReport(LocalDate from, LocalDate to, PaymentMethod method, UserPrincipal actor) {
        Instant startInstant = from.atStartOfDay(BOGOTA_ZONE).toInstant();
        Instant endInstant = to.atTime(LocalTime.MAX).atZone(BOGOTA_ZONE).toInstant();
        List<OrderStatus> statuses = OrderRepository.VERIFIED_PAID_STATUSES;

        List<Order> orders = orderRepository.findPaidOrdersList(statuses, startInstant, endInstant, method);
        SalesSummaryDto summary = calculateSummary(orders, from, to);
        List<TopProductDto> topProducts = calculateTopProducts(orders);

        List<SaleOrderResponse> saleOrderResponses = orders.stream()
                .map(this::toSaleOrderResponse)
                .toList();

        return excelSalesReportGenerator.generateSalesReport(
                saleOrderResponses,
                summary,
                topProducts,
                from,
                to,
                actor
        );
    }

    private SalesSummaryDto calculateSummary(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        BigDecimal totalSales = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long paidOrdersCount = orders.size();
        long totalUnitsSold = 0;
        long unitsWithoutCost = 0;
        BigDecimal merchandiseRevenue = BigDecimal.ZERO;
        BigDecimal coveredRevenue = BigDecimal.ZERO;
        BigDecimal knownCost = BigDecimal.ZERO;

        for (Order order : orders) {
            if (order.getTotal() != null) {
                totalSales = totalSales.add(order.getTotal());
            }
            if (order.getItems() != null) {
                BigDecimal orderMerchandise = order.getItems().stream().map(item -> item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal()).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal orderCovered = BigDecimal.ZERO;
                for (OrderItem item : order.getItems()) {
                    totalUnitsSold += item.getQuantity();
                    BigDecimal lineRevenue = item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal();

                    if (item.getCostSnapshot() == null) {
                        unitsWithoutCost += item.getQuantity();
                    } else {
                        orderCovered = orderCovered.add(lineRevenue);
                        knownCost = knownCost.add(item.getCostSnapshot().multiply(BigDecimal.valueOf(item.getQuantity())));
                    }
                }
                BigDecimal discount = order.getDiscount() == null ? BigDecimal.ZERO : order.getDiscount();
                BigDecimal netMerchandise = orderMerchandise.subtract(discount);
                merchandiseRevenue = merchandiseRevenue.add(netMerchandise);
                // Allocate an order-level discount proportionally; shipping is excluded.
                if (orderMerchandise.signum() != 0) {
                    coveredRevenue = coveredRevenue.add(orderCovered.multiply(netMerchandise).divide(orderMerchandise, 2, RoundingMode.HALF_UP));
                }
            }
        }

        BigDecimal averageTicket = paidOrdersCount > 0
                ? totalSales.divide(BigDecimal.valueOf(paidOrdersCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new SalesSummaryDto(
                totalSales,
                paidOrdersCount,
                totalUnitsSold,
                averageTicket,
                startDate,
                endDate,
                merchandiseRevenue, coveredRevenue, knownCost, coveredRevenue.subtract(knownCost),
                coveredRevenue.signum() == 0 ? null : coveredRevenue.subtract(knownCost).multiply(BigDecimal.valueOf(100))
                        .divide(coveredRevenue, 2, RoundingMode.HALF_UP),
                unitsWithoutCost
        );
    }

    private List<DailySalesDto> calculateDailySales(List<Order> orders, LocalDate from, LocalDate to) {
        Map<LocalDate, DailySalesDto> dailyMap = new TreeMap<>();
        LocalDate curr = from;
        while (!curr.isAfter(to)) {
            dailyMap.put(curr, new DailySalesDto(curr, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0L, 0L));
            curr = curr.plusDays(1);
        }

        for (Order order : orders) {
            if (order.getPaidAt() == null) continue;
            LocalDate localDate = order.getPaidAt().atZone(BOGOTA_ZONE).toLocalDate();
            DailySalesDto existing = dailyMap.getOrDefault(localDate,
                    new DailySalesDto(localDate, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0L, 0L));

            long orderUnits = 0;
            if (order.getItems() != null) {
                orderUnits = order.getItems().stream().mapToLong(OrderItem::getQuantity).sum();
            }

            dailyMap.put(localDate, new DailySalesDto(
                    localDate,
                    existing.totalSales().add(order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO),
                    existing.ordersCount() + 1,
                    existing.unitsCount() + orderUnits
            ));
        }

        return new ArrayList<>(dailyMap.values());
    }

    private List<TopProductDto> calculateTopProducts(List<Order> orders) {
        Map<String, ProductAgg> aggMap = new LinkedHashMap<>();

        for (Order order : orders) {
            if (order.getItems() == null) continue;
            for (OrderItem item : order.getItems()) {
                String sku = item.getSku();
                ProductAgg agg = aggMap.computeIfAbsent(sku, k -> {
                    ProductAgg a = new ProductAgg();
                    a.sku = sku;
                    a.productName = item.getProductName();
                    if (item.getVariant() != null && item.getVariant().getProduct() != null
                            && item.getVariant().getProduct().getBrand() != null) {
                        a.brandName = item.getVariant().getProduct().getBrand().getName();
                    } else {
                        a.brandName = "";
                    }
                    return a;
                });

                agg.unitsSold += item.getQuantity();
                BigDecimal sub = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
                agg.grossRevenue = agg.grossRevenue.add(sub);
            }
        }

        List<ProductAgg> sortedList = aggMap.values().stream()
                .sorted(Comparator.comparing(ProductAgg::getGrossRevenue).reversed()
                        .thenComparing(Comparator.comparing(ProductAgg::getUnitsSold).reversed()))
                .toList();

        BigDecimal totalGrossRevenue = sortedList.stream()
                .map(ProductAgg::getGrossRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TopProductDto> result = new ArrayList<>();
        int rank = 1;
        for (ProductAgg agg : sortedList) {
            BigDecimal share = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (totalGrossRevenue.compareTo(BigDecimal.ZERO) > 0) {
                share = agg.grossRevenue.multiply(BigDecimal.valueOf(100))
                        .divide(totalGrossRevenue, 2, RoundingMode.HALF_UP);
            }

            result.add(new TopProductDto(
                    rank++,
                    agg.sku,
                    agg.productName,
                    agg.brandName,
                    agg.unitsSold,
                    agg.grossRevenue,
                    share
            ));
        }

        return result;
    }

    private List<PaymentMethodBreakdownDto> calculatePaymentMethods(List<Order> orders, BigDecimal totalSales) {
        Map<String, MethodAgg> map = new LinkedHashMap<>();

        for (Order order : orders) {
            String method = resolvePaymentMethodName(order);
            MethodAgg agg = map.computeIfAbsent(method, k -> new MethodAgg());
            agg.orderCount++;
            if (order.getTotal() != null) {
                agg.totalAmount = agg.totalAmount.add(order.getTotal());
            }
        }

        List<PaymentMethodBreakdownDto> result = new ArrayList<>();
        for (Map.Entry<String, MethodAgg> entry : map.entrySet()) {
            MethodAgg agg = entry.getValue();
            BigDecimal percentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (totalSales != null && totalSales.compareTo(BigDecimal.ZERO) > 0) {
                percentage = agg.totalAmount.multiply(BigDecimal.valueOf(100))
                        .divide(totalSales, 2, RoundingMode.HALF_UP);
            }
            result.add(new PaymentMethodBreakdownDto(
                    entry.getKey(),
                    agg.totalAmount,
                    agg.orderCount,
                    percentage
            ));
        }

        result.sort(Comparator.comparing(PaymentMethodBreakdownDto::totalAmount).reversed());
        return result;
    }

    private SaleOrderResponse toSaleOrderResponse(Order order) {
        Payment verifiedPayment = resolveVerifiedPayment(order);

        String paymentMethod = (verifiedPayment != null && verifiedPayment.getPaymentMethod() != null)
                ? verifiedPayment.getPaymentMethod().name()
                : "DESCONOCIDO";

        String verifiedBy = "Sistema";
        if (verifiedPayment != null && verifiedPayment.getVerifiedBy() != null) {
            String name = verifiedPayment.getVerifiedBy().getFullName();
            verifiedBy = (name != null && !name.isBlank()) ? name : verifiedPayment.getVerifiedBy().getEmail();
        }

        String localPaidAt = order.getPaidAt() != null
                ? order.getPaidAt().atZone(BOGOTA_ZONE).format(LOCAL_DATE_TIME_FORMATTER)
                : "";

        List<SaleOrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                String brandName = "";
                String shadeName = "";
                BigDecimal costSnapshot = item.getCostSnapshot();

                if (item.getVariant() != null) {
                    ProductVariant variant = item.getVariant();
                    if (variant.getShadeName() != null) {
                        shadeName = variant.getShadeName();
                    }
                    if (variant.getProduct() != null && variant.getProduct().getBrand() != null) {
                        brandName = variant.getProduct().getBrand().getName();
                    }
                }
                if (shadeName.isBlank() && item.getVariantName() != null) {
                    shadeName = item.getVariantName();
                }

                itemResponses.add(new SaleOrderItemResponse(
                        item.getId(),
                        item.getSku(),
                        item.getProductName(),
                        brandName,
                        shadeName,
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal(),
                        costSnapshot
                ));
            }
        }

        return new SaleOrderResponse(
                order.getId(),
                order.getReference(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                paymentMethod,
                order.getSubtotal(),
                order.getDiscount(),
                order.getShippingFee(),
                order.getTotal(),
                verifiedBy,
                order.getStatus().name(),
                order.getChannel(),
                order.getPaidAt(),
                localPaidAt,
                itemResponses
        );
    }

    private Payment resolveVerifiedPayment(Order order) {
        if (order.getPayments() == null || order.getPayments().isEmpty()) {
            return null;
        }
        return order.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.VERIFICADO)
                .findFirst()
                .or(() -> order.getPayments().stream().findFirst())
                .orElse(null);
    }

    private String resolvePaymentMethodName(Order order) {
        Payment p = resolveVerifiedPayment(order);
        if (p != null && p.getPaymentMethod() != null) {
            return p.getPaymentMethod().name();
        }
        return "OTRO";
    }

    private static class ProductAgg {
        String sku;
        String productName;
        String brandName;
        long unitsSold = 0;
        BigDecimal grossRevenue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal getGrossRevenue() {
            return grossRevenue;
        }

        long getUnitsSold() {
            return unitsSold;
        }
    }

    private static class MethodAgg {
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long orderCount = 0;
    }
}
