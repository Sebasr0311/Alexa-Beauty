package com.alexabeauty.order.dto;

import com.alexabeauty.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderTrackingResponse {
    private String reference;
    private OrderStatus status;
    private Instant expiresAt;
    private BigDecimal total;
    private int itemCount;
    private Instant createdAt;
    private String customerName;
    private String deliveryType;
    private String whatsappUrl;
    private String whatsappMessageText;
    private List<OrderItemResponse> items;
}
