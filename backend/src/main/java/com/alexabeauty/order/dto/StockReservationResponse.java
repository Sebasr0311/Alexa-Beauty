package com.alexabeauty.order.dto;

import com.alexabeauty.order.domain.ReservationStatus;
import com.alexabeauty.order.domain.StockReservation;
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
public class StockReservationResponse {
    private UUID id;
    private UUID variantId;
    private String sku;
    private String productName;
    private int quantity;
    private Instant expiresAt;
    private ReservationStatus status;
    private Instant createdAt;

    public static StockReservationResponse from(StockReservation reservation) {
        if (reservation == null) return null;
        return StockReservationResponse.builder()
                .id(reservation.getId())
                .variantId(reservation.getVariant() != null ? reservation.getVariant().getId() : null)
                .sku(reservation.getVariant() != null ? reservation.getVariant().getSku() : null)
                .productName(reservation.getVariant() != null && reservation.getVariant().getProduct() != null
                        ? reservation.getVariant().getProduct().getName() : null)
                .quantity(reservation.getQuantity())
                .expiresAt(reservation.getExpiresAt())
                .status(reservation.getStatus())
                .createdAt(reservation.getCreatedAt())
                .build();
    }
}
