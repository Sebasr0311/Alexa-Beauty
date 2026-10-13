package com.alexabeauty.order.service;

import com.alexabeauty.order.dto.CreateOrderRequest;
import com.alexabeauty.order.dto.OrderCheckoutResponse;
import com.alexabeauty.order.dto.OrderTrackingResponse;

import java.util.UUID;

public interface OrderCheckoutService {

    /**
     * Executes transactional checkout with pessimistic variant locking,
     * stock reservation, frozen prices, and Click-to-Chat URL generation.
     */
    OrderCheckoutResponse checkout(CreateOrderRequest request, String idempotencyKey);

    /**
     * Validates tracking token and retrieves order tracking state.
     */
    OrderTrackingResponse trackOrder(String reference, String token);

    /**
     * Expires a specific order and releases its reserved stock.
     */
    void expireOrder(UUID orderId);

    /**
     * Finds and expires all overdue orders in PENDIENTE_CONTACTO or PENDIENTE_PAGO.
     */
    int expireOverdueReservations();
}
