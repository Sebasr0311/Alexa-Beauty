package com.alexabeauty.order.dto;

import com.alexabeauty.order.domain.OrderStatus;
import com.alexabeauty.order.domain.OrderStatusHistory;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OrderStatusHistoryResponse {
    private UUID id;
    private OrderStatus previousStatus;
    private OrderStatus newStatus;
    private String userEmail;
    private String notes;
    private Instant createdAt;

    public static OrderStatusHistoryResponse from(OrderStatusHistory history) {
        if (history == null) return null;
        return OrderStatusHistoryResponse.builder()
                .id(history.getId())
                .previousStatus(history.getPreviousStatus())
                .newStatus(history.getNewStatus())
                .userEmail(history.getUser() != null ? history.getUser().getEmail() : null)
                .notes(history.getNotes())
                .createdAt(history.getCreatedAt())
                .build();
    }
}
