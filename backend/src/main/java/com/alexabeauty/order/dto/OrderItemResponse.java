package com.alexabeauty.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private UUID id;
    private UUID variantId;
    private String sku;
    private String productName;
    private String variantName;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal subtotal;
}
