package com.alexabeauty.order.service;

import com.alexabeauty.order.domain.OrderStatus;
import com.alexabeauty.order.dto.AdminOrderDetailResponse;
import com.alexabeauty.order.dto.AdminOrderSummaryResponse;
import com.alexabeauty.order.dto.ExtendReservationRequest;
import com.alexabeauty.order.dto.OrderStatusTransitionRequest;
import com.alexabeauty.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.UUID;

public interface AdminOrderService {

    Page<AdminOrderSummaryResponse> listOrders(OrderStatus status, String search, Instant from, Instant to, Pageable pageable);

    AdminOrderDetailResponse getOrderDetail(UUID id);

    AdminOrderDetailResponse transitionOrderStatus(UUID id, OrderStatusTransitionRequest request, UserPrincipal actor);

    AdminOrderDetailResponse extendReservation(UUID id, ExtendReservationRequest request, UserPrincipal actor);
}
