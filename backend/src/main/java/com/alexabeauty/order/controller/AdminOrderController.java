package com.alexabeauty.order.controller;

import com.alexabeauty.common.dto.ApiResponse;
import com.alexabeauty.order.domain.OrderStatus;
import com.alexabeauty.order.dto.AdminOrderDetailResponse;
import com.alexabeauty.order.dto.AdminOrderSummaryResponse;
import com.alexabeauty.order.dto.ExtendReservationRequest;
import com.alexabeauty.order.dto.OrderStatusTransitionRequest;
import com.alexabeauty.order.service.AdminOrderService;
import com.alexabeauty.payment.dto.ConfirmPaymentRequest;
import com.alexabeauty.payment.dto.PaymentResponse;
import com.alexabeauty.payment.service.PaymentVerificationService;
import com.alexabeauty.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/orders")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
@RequiredArgsConstructor
@Tag(name = "Admin Orders", description = "Gestión administrativa de pedidos, verificación de pagos y transiciones de estado")
public class AdminOrderController {

    private final AdminOrderService adminOrderService;
    private final PaymentVerificationService paymentVerificationService;

    @GetMapping
    @Operation(summary = "Listar pedidos con filtros y paginación")
    public ResponseEntity<ApiResponse<Page<AdminOrderSummaryResponse>>> listOrders(
            @RequestParam(required = false) OrderStatus status,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        log.info("Listing admin orders with status={}, search='{}'", status, search);
        Page<AdminOrderSummaryResponse> orders = adminOrderService.listOrders(status, search, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Pedidos obtenidos exitosamente", orders));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener detalle completo de un pedido")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> getOrderDetail(@PathVariable UUID id) {
        log.info("Fetching admin order detail for id={}", id);
        AdminOrderDetailResponse detail = adminOrderService.getOrderDetail(id);
        return ResponseEntity.ok(ApiResponse.ok("Detalle de pedido obtenido exitosamente", detail));
    }

    @PostMapping("/{id}/confirm-payment")
    @Operation(summary = "Verificar y confirmar pago de un pedido")
    public ResponseEntity<ApiResponse<PaymentResponse>> confirmPayment(
            @PathVariable UUID id,
            @Valid @RequestBody ConfirmPaymentRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        log.info("Admin confirming payment for orderId={}, reference='{}'", id, request.getTransferReference());
        PaymentResponse response = paymentVerificationService.confirmPayment(id, request, idempotencyKey, principal);
        return ResponseEntity.ok(ApiResponse.ok("Pago verificado exitosamente. Stock descontado.", response));
    }

    @PostMapping("/{id}/transition")
    @Operation(summary = "Transicionar estado de un pedido")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> transitionOrderStatus(
            @PathVariable UUID id,
            @Valid @RequestBody OrderStatusTransitionRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        log.info("Admin transitioning status for orderId={} to {}", id, request.getNewStatus());
        AdminOrderDetailResponse response = adminOrderService.transitionOrderStatus(id, request, principal);
        return ResponseEntity.ok(ApiResponse.ok("Estado del pedido actualizado exitosamente", response));
    }

    @PostMapping("/{id}/extend-reservation")
    @Operation(summary = "Extender tiempo de reserva de inventario de un pedido")
    public ResponseEntity<ApiResponse<AdminOrderDetailResponse>> extendReservation(
            @PathVariable UUID id,
            @Valid @RequestBody ExtendReservationRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        log.info("Admin extending reservation for orderId={} by {} minutes", id, request.getAdditionalMinutes());
        AdminOrderDetailResponse response = adminOrderService.extendReservation(id, request, principal);
        return ResponseEntity.ok(ApiResponse.ok("Reserva extendida exitosamente", response));
    }
}
