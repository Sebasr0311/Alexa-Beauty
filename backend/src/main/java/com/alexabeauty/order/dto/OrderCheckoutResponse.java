package com.alexabeauty.order.dto;

import com.alexabeauty.order.domain.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderCheckoutResponse {
    private UUID orderId;
    private String reference;
    private String trackingToken;
    private OrderStatus status;
    private Instant expiresAt;
    private BigDecimal subtotal;
    private BigDecimal shippingFee;
    private BigDecimal total;
    private String whatsappUrl;
    private String whatsappMessageText;
    private List<OrderItemResponse> items;
}
