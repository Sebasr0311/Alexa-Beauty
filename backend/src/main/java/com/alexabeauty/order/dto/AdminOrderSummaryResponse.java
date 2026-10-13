package com.alexabeauty.order.dto;

import com.alexabeauty.order.domain.Order;
import com.alexabeauty.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminOrderSummaryResponse {

    private UUID id;
    private String reference;
    private String customerName;
    private String customerPhone;
    private String deliveryType;
    private BigDecimal total;
    private OrderStatus status;
    private String channel;
    private Instant createdAt;
    private Instant paidAt;
    private Instant expiresAt;

    public static AdminOrderSummaryResponse from(Order order) {
        if (order == null) return null;
        return AdminOrderSummaryResponse.builder()
                .id(order.getId())
                .reference(order.getReference())
                .customerName(order.getCustomerName())
                .customerPhone(order.getCustomerPhone())
                .deliveryType(order.getDeliveryType())
                .total(order.getTotal())
                .status(order.getStatus())
                .channel(order.getChannel())
                .createdAt(order.getCreatedAt())
                .paidAt(order.getPaidAt())
                .expiresAt(order.getExpiresAt())
                .build();
    }
}
