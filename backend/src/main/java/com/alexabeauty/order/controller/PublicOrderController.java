package com.alexabeauty.order.controller;

import com.alexabeauty.common.dto.ApiResponse;
import com.alexabeauty.order.dto.CreateOrderRequest;
import com.alexabeauty.order.dto.OrderCheckoutResponse;
import com.alexabeauty.order.dto.OrderTrackingResponse;
import com.alexabeauty.order.service.OrderCheckoutService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/v1/orders")
@RequiredArgsConstructor
@Tag(name = "Public Orders", description = "Endpoints públicos para checkout transaccional y rastreo de pedidos")
public class PublicOrderController {

    private final OrderCheckoutService orderCheckoutService;

    @PostMapping
    @Operation(summary = "Realizar pedido y reservar stock",
            description = "Crea un pedido con reserva de 45 minutos y genera el enlace preformateado de WhatsApp")
    public ResponseEntity<ApiResponse<OrderCheckoutResponse>> checkout(
            @Valid @RequestBody CreateOrderRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey
    ) {
        log.info("Received public checkout request for customer='{}', phone='{}'",
                request.getCustomerName(), request.getCustomerPhone());
        OrderCheckoutResponse response = orderCheckoutService.checkout(request, idempotencyKey);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Pedido registrado exitosamente. Stock reservado por 45 minutos.", response));
    }

    @GetMapping("/track")
    @Operation(summary = "Rastrear estado público de pedido",
            description = "Consulta el estado actual de un pedido mediante referencia pública y token de rastreo")
    public ResponseEntity<ApiResponse<OrderTrackingResponse>> trackOrder(
            @RequestParam("reference") String reference,
            @RequestParam("token") String token
    ) {
        log.info("Received public order tracking request for reference='{}'", reference);
        OrderTrackingResponse response = orderCheckoutService.trackOrder(reference, token);
        return ResponseEntity.ok(ApiResponse.ok("Estado del pedido obtenido exitosamente", response));
    }
}
