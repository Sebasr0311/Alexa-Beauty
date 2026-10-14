package com.alexabeauty.order.service;

import com.alexabeauty.auth.domain.User;
import com.alexabeauty.auth.repository.UserRepository;
import com.alexabeauty.catalog.domain.ProductVariant;
import com.alexabeauty.catalog.repository.ProductVariantRepository;
import com.alexabeauty.common.audit.AuditLog;
import com.alexabeauty.common.audit.AuditLogRepository;
import com.alexabeauty.common.error.ConflictException;
import com.alexabeauty.common.error.ResourceNotFoundException;
import com.alexabeauty.order.domain.Order;
import com.alexabeauty.order.domain.OrderItem;
import com.alexabeauty.order.domain.OrderStatus;
import com.alexabeauty.order.domain.OrderStatusHistory;
import com.alexabeauty.order.domain.ReservationStatus;
import com.alexabeauty.order.domain.StockReservation;
import com.alexabeauty.order.dto.AdminOrderDetailResponse;
import com.alexabeauty.order.dto.AdminOrderSummaryResponse;
import com.alexabeauty.order.dto.ExtendReservationRequest;
import com.alexabeauty.order.dto.OrderStatusTransitionRequest;
import com.alexabeauty.order.repository.OrderRepository;
import com.alexabeauty.order.repository.OrderStatusHistoryRepository;
import com.alexabeauty.order.repository.StockReservationRepository;
import com.alexabeauty.security.UserPrincipal;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import com.alexabeauty.inventory.domain.InventoryMovement;
import com.alexabeauty.inventory.domain.MovementType;
import com.alexabeauty.inventory.repository.InventoryMovementRepository;
import com.alexabeauty.payment.domain.Payment;
import com.alexabeauty.payment.domain.PaymentMethod;
import com.alexabeauty.payment.domain.PaymentStatus;
import com.alexabeauty.payment.repository.PaymentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AdminOrderServiceImpl implements AdminOrderService {

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final StockReservationRepository stockReservationRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final PaymentRepository paymentRepository;

    @Override
    @Transactional(readOnly = true)
    public Page<AdminOrderSummaryResponse> listOrders(OrderStatus status, String search, Instant from, Instant to, Pageable pageable) {
        Specification<Order> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (search != null && !search.isBlank()) {
                String pattern = "%" + search.trim().toLowerCase() + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("reference")), pattern),
                        cb.like(cb.lower(root.get("customerName")), pattern),
                        cb.like(cb.lower(root.get("customerPhone")), pattern)
                ));
            }
            if (from != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), from));
            }
            if (to != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("createdAt"), to));
            }
            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return orderRepository.findAll(spec, pageable).map(AdminOrderSummaryResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public AdminOrderDetailResponse getOrderDetail(UUID id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id));
        return AdminOrderDetailResponse.from(order);
    }

    @Override
    @Transactional
    public AdminOrderDetailResponse transitionOrderStatus(UUID id, OrderStatusTransitionRequest request, UserPrincipal actor) {
        log.info("Transitioning order id={} to newStatus={}", id, request.getNewStatus());

        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id));

        OrderStatus currentStatus = order.getStatus();
        OrderStatus newStatus = request.getNewStatus();

        // Valid transitions:
        // PAGADO -> PREPARANDO / DESPACHADO / ENTREGADO
        // PREPARANDO -> DESPACHADO / ENTREGADO
        // DESPACHADO -> ENTREGADO
        // PENDIENTE / PENDIENTE_CONTACTO / PENDIENTE_PAGO / PAGO_EN_REVISION -> DESPACHADO / ENTREGADO / CANCELADO
        boolean isValid = switch (currentStatus) {
            case PAGADO -> newStatus == OrderStatus.PREPARANDO || newStatus == OrderStatus.DESPACHADO || newStatus == OrderStatus.ENTREGADO;
            case PREPARANDO -> newStatus == OrderStatus.DESPACHADO || newStatus == OrderStatus.ENTREGADO;
            case DESPACHADO -> newStatus == OrderStatus.ENTREGADO;
            case PENDIENTE, PENDIENTE_CONTACTO, PENDIENTE_PAGO, PAGO_EN_REVISION ->
                newStatus == OrderStatus.DESPACHADO || newStatus == OrderStatus.ENTREGADO || newStatus == OrderStatus.CANCELADO;
            default -> false;
        };

        if (!isValid) {
            throw new ConflictException("Transición de estado no permitida: de " + currentStatus + " a " + newStatus);
        }

        User actorEntity = null;
        if (actor != null && actor.getId() != null) {
            actorEntity = userRepository.findById(actor.getId()).orElse(null);
        }

        // If transitioning to CANCELADO before payment, release stockReserved on variants (P18 matrix)
        if (newStatus == OrderStatus.CANCELADO) {
            List<UUID> variantIds = order.getItems().stream()
                    .map(item -> item.getVariant().getId())
                    .distinct()
                    .sorted()
                    .toList();

            List<ProductVariant> lockedVariants = productVariantRepository.findByIdInOrderByIdForUpdate(variantIds);
            Map<UUID, ProductVariant> variantMap = lockedVariants.stream()
                    .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

            for (OrderItem item : order.getItems()) {
                ProductVariant variant = variantMap.get(item.getVariant().getId());
                if (variant != null) {
                    variant.setStockReserved(Math.max(0, variant.getStockReserved() - item.getQuantity()));
                    // Stock on hand remains untouched!
                }
            }
            productVariantRepository.saveAll(lockedVariants);

            for (StockReservation res : order.getReservations()) {
                if (res.getStatus() == ReservationStatus.ACTIVE) {
                    res.setStatus(ReservationStatus.RELEASED);
                }
            }
            stockReservationRepository.saveAll(order.getReservations());
            log.info("Order reference='{}' cancelled. Stock reservations released without altering stock on hand.", order.getReference());
        }

        // If transitioning to ENTREGADO and order was not yet paid (contraentrega domicilio)
        if (newStatus == OrderStatus.ENTREGADO && order.getPaidAt() == null) {
            log.info("Order reference='{}' marked as ENTREGADO without prior payment. Processing auto-payment & stock deduction.", order.getReference());

            List<UUID> variantIds = order.getItems().stream()
                    .map(item -> item.getVariant().getId())
                    .distinct()
                    .sorted()
                    .toList();

            List<ProductVariant> lockedVariants = productVariantRepository.findByIdInOrderByIdForUpdate(variantIds);
            Map<UUID, ProductVariant> variantMap = lockedVariants.stream()
                    .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

            for (OrderItem item : order.getItems()) {
                ProductVariant variant = variantMap.get(item.getVariant().getId());
                if (variant != null) {
                    int qty = item.getQuantity();
                    int qtyBefore = variant.getStockOnHand();
                    int qtyAfter = qtyBefore - qty;

                    variant.setStockReserved(Math.max(0, variant.getStockReserved() - qty));
                    variant.setStockOnHand(qtyAfter);
                    productVariantRepository.save(variant);

                    InventoryMovement movement = InventoryMovement.builder()
                            .variant(variant)
                            .movementType(MovementType.VENTA)
                            .quantityDelta(-qty)
                            .qtyBefore(qtyBefore)
                            .qtyAfter(qtyAfter)
                            .reference(order.getReference())
                            .reason("Venta contraentrega domicilio entregado: " + order.getReference())
                            .user(actorEntity)
                            .build();
                    inventoryMovementRepository.save(movement);
                }
            }

            for (StockReservation res : order.getReservations()) {
                if (res.getStatus() == ReservationStatus.ACTIVE) {
                    res.setStatus(ReservationStatus.CONSUMED);
                }
            }
            stockReservationRepository.saveAll(order.getReservations());

            Payment payment = Payment.builder()
                    .order(order)
                    .paymentMethod(PaymentMethod.EFECTIVO)
                    .transferReference("DOMICILIO-" + order.getReference())
                    .amount(order.getTotal())
                    .status(PaymentStatus.VERIFICADO)
                    .verifiedBy(actorEntity)
                    .verifiedAt(Instant.now())
                    .notes("Pago registrado automáticamente al marcar entrega de domicilio")
                    .build();
            Payment savedPayment = paymentRepository.save(payment);
            order.getPayments().add(savedPayment);

            order.setPaidAt(Instant.now());
        }

        order.setStatus(newStatus);

        String notes = request.getNotes() != null && !request.getNotes().isBlank()
                ? request.getNotes().trim()
                : ("Transición de estado a " + newStatus);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .previousStatus(currentStatus)
                .newStatus(newStatus)
                .user(actorEntity)
                .notes(notes)
                .build();
        order.getStatusHistory().add(history);
        orderStatusHistoryRepository.save(history);
        orderRepository.save(order);

        String actorEmail = actor != null ? actor.getUsername() : "sistema";
        AuditLog auditLog = AuditLog.builder()
                .actorEmail(actorEmail)
                .action("ORDER_STATUS_TRANSITION")
                .entityType("ORDER")
                .entityId(order.getId().toString())
                .beforeSnapshot(currentStatus.name())
                .afterSnapshot(newStatus.name() + (request.getNotes() != null ? " | " + request.getNotes().trim() : ""))
                .build();
        auditLogRepository.save(auditLog);

        return AdminOrderDetailResponse.from(order);
    }

    @Override
    @Transactional
    public AdminOrderDetailResponse extendReservation(UUID id, ExtendReservationRequest request, UserPrincipal actor) {
        log.info("Extending reservation for order id={} by {} minutes", id, request.getAdditionalMinutes());

        Order order = orderRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + id));

        if (order.getStatus() != OrderStatus.PENDIENTE_CONTACTO && order.getStatus() != OrderStatus.PENDIENTE_PAGO) {
            throw new ConflictException("No se puede extender la reserva: el pedido se encuentra en estado " + order.getStatus());
        }

        Instant currentExpiresAt = order.getExpiresAt() != null ? order.getExpiresAt() : Instant.now();
        Instant baseTime = currentExpiresAt.isBefore(Instant.now()) ? Instant.now() : currentExpiresAt;
        Instant newExpiresAt = baseTime.plus(Duration.ofMinutes(request.getAdditionalMinutes()));
        order.setExpiresAt(newExpiresAt);

        for (StockReservation res : order.getReservations()) {
            if (res.getStatus() == ReservationStatus.ACTIVE) {
                res.setExpiresAt(newExpiresAt);
            }
        }
        stockReservationRepository.saveAll(order.getReservations());

        User actorEntity = null;
        if (actor != null && actor.getId() != null) {
            actorEntity = userRepository.findById(actor.getId()).orElse(null);
        }

        String notes = "Reserva extendida por " + request.getAdditionalMinutes() + " minutos.";
        if (request.getNotes() != null && !request.getNotes().isBlank()) {
            notes += " " + request.getNotes().trim();
        }

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .previousStatus(order.getStatus())
                .newStatus(order.getStatus())
                .user(actorEntity)
                .notes(notes)
                .build();
        order.getStatusHistory().add(history);
        orderStatusHistoryRepository.save(history);
        orderRepository.save(order);

        String actorEmail = actor != null ? actor.getUsername() : "sistema";
        AuditLog auditLog = AuditLog.builder()
                .actorEmail(actorEmail)
                .action("ORDER_RESERVATION_EXTENDED")
                .entityType("ORDER")
                .entityId(order.getId().toString())
                .beforeSnapshot(currentExpiresAt.toString())
                .afterSnapshot(newExpiresAt.toString())
                .build();
        auditLogRepository.save(auditLog);

        return AdminOrderDetailResponse.from(order);
    }
}
