package com.alexabeauty.order.dto;

import com.alexabeauty.order.domain.Order;
import com.alexabeauty.order.domain.OrderItem;
import com.alexabeauty.order.domain.OrderStatus;
import com.alexabeauty.payment.dto.PaymentResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderDetailResponse {

    private UUID id;
    private String reference;
    private String customerName;
    private String customerPhone;
    private String deliveryType;
    private String city;
    private String address;
    private String customerNotes;
    private BigDecimal subtotal;
    private BigDecimal discount;
    private BigDecimal shippingFee;
    private BigDecimal total;
    private OrderStatus status;
    private String channel;
    private Instant expiresAt;
    private Instant paidAt;
    private Instant createdAt;
    private Instant updatedAt;

    @Builder.Default
    private List<OrderItemResponse> items = new ArrayList<>();

    @Builder.Default
    private List<PaymentResponse> payments = new ArrayList<>();

    @Builder.Default
    private List<StockReservationResponse> reservations = new ArrayList<>();

    @Builder.Default
    private List<OrderStatusHistoryResponse> statusHistory = new ArrayList<>();

    public static AdminOrderDetailResponse from(Order order) {
        if (order == null) return null;

        List<OrderItemResponse> itemResponses = order.getItems() != null
                ? order.getItems().stream().map(item -> OrderItemResponse.builder()
                        .id(item.getId())
                        .variantId(item.getVariant() != null ? item.getVariant().getId() : null)
                        .sku(item.getSku())
                        .productName(item.getProductName())
                        .variantName(item.getVariantName())
                        .unitPrice(item.getUnitPrice())
                        .quantity(item.getQuantity())
                        .subtotal(item.getSubtotal())
                        .build()).toList()
                : new ArrayList<>();

        List<PaymentResponse> paymentResponses = order.getPayments() != null
                ? order.getPayments().stream().map(PaymentResponse::from).toList()
                : new ArrayList<>();

        List<StockReservationResponse> reservationResponses = order.getReservations() != null
                ? order.getReservations().stream().map(StockReservationResponse::from).toList()
                : new ArrayList<>();

        List<OrderStatusHistoryResponse> historyResponses = order.getStatusHistory() != null
                ? order.getStatusHistory().stream().map(OrderStatusHistoryResponse::from).toList()
                : new ArrayList<>();

        return AdminOrderDetailResponse.builder()
                .id(order.getId())
                .reference(order.getReference())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .deliveryType(order.getDeliveryType())
                .city(order.getCity())
                .address(order.getAddress())
                .customerNotes(order.getCustomerNotes())
                .subtotal(order.getSubtotal())
                .discount(order.getDiscount())
                .shippingFee(order.getShippingFee())
                .total(order.getTotal())
                .status(order.getStatus())
                .channel(order.getChannel())
                .expiresAt(order.getExpiresAt())
                .paidAt(order.getPaidAt())
                .createdAt(order.getCreatedAt())
                .updatedAt(order.getUpdatedAt())
                .items(itemResponses)
                .payments(paymentResponses)
                .reservations(reservationResponses)
                .statusHistory(historyResponses)
                .build();
    }
}
