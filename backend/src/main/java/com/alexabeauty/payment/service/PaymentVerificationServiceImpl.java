package com.alexabeauty.payment.service;

import com.alexabeauty.auth.domain.User;
import com.alexabeauty.auth.repository.UserRepository;
import com.alexabeauty.catalog.domain.ProductVariant;
import com.alexabeauty.catalog.repository.ProductVariantRepository;
import com.alexabeauty.common.audit.AuditLog;
import com.alexabeauty.common.audit.AuditLogRepository;
import com.alexabeauty.common.error.ConflictException;
import com.alexabeauty.common.error.ResourceNotFoundException;
import com.alexabeauty.inventory.domain.InventoryMovement;
import com.alexabeauty.inventory.domain.MovementType;
import com.alexabeauty.inventory.repository.InventoryMovementRepository;
import com.alexabeauty.order.domain.Order;
import com.alexabeauty.order.domain.OrderItem;
import com.alexabeauty.order.domain.OrderStatus;
import com.alexabeauty.order.domain.OrderStatusHistory;
import com.alexabeauty.order.domain.ReservationStatus;
import com.alexabeauty.order.domain.StockReservation;
import com.alexabeauty.order.repository.OrderRepository;
import com.alexabeauty.order.repository.OrderStatusHistoryRepository;
import com.alexabeauty.order.repository.StockReservationRepository;
import com.alexabeauty.payment.domain.Payment;
import com.alexabeauty.payment.domain.PaymentStatus;
import com.alexabeauty.payment.dto.ConfirmPaymentRequest;
import com.alexabeauty.payment.dto.PaymentResponse;
import com.alexabeauty.payment.repository.PaymentRepository;
import com.alexabeauty.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentVerificationServiceImpl implements PaymentVerificationService {

    private final OrderRepository orderRepository;
    private final PaymentRepository paymentRepository;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final StockReservationRepository stockReservationRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;

    @Override
    @Transactional
    public PaymentResponse confirmPayment(UUID orderId, ConfirmPaymentRequest request, String idempotencyKey, UserPrincipal actor) {
        // 1. Pessimistic lock on Order
        Order order = orderRepository.findByIdForUpdate(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado: " + orderId));

        String transferRef = (request.getTransferReference() != null && !request.getTransferReference().isBlank())
                ? request.getTransferReference().trim()
                : "MANUAL-" + order.getReference();

        log.info("Starting confirmPayment for orderId={}, transferReference='{}', idempotencyKey='{}'",
                orderId, transferRef, idempotencyKey);

        // 2. Idempotency Check: if already PAGADO, return existing verified payment immediately
        if (order.getStatus() == OrderStatus.PAGADO) {
            log.info("Order reference='{}' is already PAGADO. Returning existing verified payment without mutating inventory.",
                    order.getReference());
            Optional<Payment> existingVerified = paymentRepository.findFirstByOrderIdAndStatus(orderId, PaymentStatus.VERIFICADO);
            if (existingVerified.isPresent()) {
                return PaymentResponse.from(existingVerified.get());
            }
            List<Payment> allPayments = paymentRepository.findByOrderId(orderId);
            if (!allPayments.isEmpty()) {
                return PaymentResponse.from(allPayments.get(0));
            }
        }

        // 3. Inviolable Preconditions Check
        if (order.getStatus() == OrderStatus.EXPIRADO || order.getStatus() == OrderStatus.CANCELADO) {
            throw new ConflictException("No se puede verificar el pago: el pedido se encuentra en estado "
                    + order.getStatus() + ". El stock fue liberado y debe ser revalidado manualmente.");
        }

        if (order.getStatus() != OrderStatus.PENDIENTE_CONTACTO
                && order.getStatus() != OrderStatus.PENDIENTE_PAGO
                && order.getStatus() != OrderStatus.PAGO_EN_REVISION
                && order.getStatus() != OrderStatus.PENDIENTE
                && order.getStatus() != OrderStatus.DESPACHADO
                && order.getStatus() != OrderStatus.ENTREGADO) {
            throw new ConflictException("No se puede verificar el pago para un pedido en estado " + order.getStatus());
        }

        if (order.getExpiresAt() == null || !order.getExpiresAt().isAfter(Instant.now())) {
            throw new ConflictException("La reserva ha vencido. Cree un nuevo pedido antes de verificar el pago.");
        }
        if (request.getAmount() == null || request.getAmount().signum() <= 0
                || request.getAmount().compareTo(order.getTotal()) != 0) {
            throw new ConflictException("El pago manual debe cubrir exactamente el total del pedido.");
        }

        // 4. Deterministically lock variants in ascending UUID order
        List<UUID> variantIds = order.getItems().stream()
                .map(item -> item.getVariant().getId())
                .distinct()
                .sorted()
                .toList();

        List<ProductVariant> lockedVariants = productVariantRepository.findByIdInOrderByIdForUpdate(variantIds);
        Map<UUID, ProductVariant> lockedMap = lockedVariants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, Function.identity()));

        Map<UUID, Integer> requested = order.getItems().stream().collect(Collectors.groupingBy(
                item -> item.getVariant().getId(), Collectors.summingInt(OrderItem::getQuantity)));
        Map<UUID, Integer> reserved = order.getReservations().stream()
                .filter(res -> res.getStatus() == ReservationStatus.ACTIVE)
                .collect(Collectors.groupingBy(res -> res.getVariant().getId(),
                        Collectors.summingInt(StockReservation::getQuantity)));
        for (Map.Entry<UUID, Integer> entry : requested.entrySet()) {
            ProductVariant variant = lockedMap.get(entry.getKey());
            int qty = entry.getValue();
            if (variant == null || qty <= 0 || variant.getStockOnHand() < qty
                    || variant.getStockReserved() < qty
                    || variant.getStockOnHand() < variant.getStockReserved()
                    || reserved.getOrDefault(entry.getKey(), 0) != qty) {
                throw new ConflictException("Inventario o reserva inconsistente. No se registró el pago.");
            }
        }

        // Resolve actor user entity
        User actorEntity = null;
        if (actor != null && actor.getId() != null) {
            actorEntity = userRepository.findById(actor.getId()).orElse(null);
        }

        // 5. Consume inventory (one time only)
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = lockedMap.get(item.getVariant().getId());
            if (variant == null) {
                throw new ResourceNotFoundException("Variante no encontrada: " + item.getVariant().getId());
            }
            int qty = item.getQuantity();
            int qtyBefore = variant.getStockOnHand();
            int qtyAfter = qtyBefore - qty;

            variant.setStockReserved(variant.getStockReserved() - qty);
            variant.setStockOnHand(qtyAfter);
            productVariantRepository.save(variant);

            InventoryMovement movement = InventoryMovement.builder()
                    .variant(variant)
                    .movementType(MovementType.VENTA)
                    .quantityDelta(-qty)
                    .qtyBefore(qtyBefore)
                    .qtyAfter(qtyAfter)
                    .reference(order.getReference())
                    .reason("Venta verificada pago ref: " + transferRef)
                    .user(actorEntity)
                    .build();
            inventoryMovementRepository.save(movement);
        }

        // 6. Update reservations: mark active reservations as CONSUMED
        for (StockReservation res : order.getReservations()) {
            if (res.getStatus() == ReservationStatus.ACTIVE) {
                res.setStatus(ReservationStatus.CONSUMED);
            }
        }
        stockReservationRepository.saveAll(order.getReservations());

        // 7. Create Payment record
        String notes = request.getNotes();
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            String idTag = "[Idempotency-Key: " + idempotencyKey.trim() + "]";
            notes = (notes == null || notes.isBlank()) ? idTag : (idTag + " " + notes.trim());
        }

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(request.getPaymentMethod())
                .transferReference(transferRef)
                .amount(request.getAmount())
                .status(PaymentStatus.VERIFICADO)
                .verifiedBy(actorEntity)
                .verifiedAt(Instant.now())
                .notes(notes)
                .receiptImageUrl(request.getReceiptImageUrl())
                .build();
        Payment savedPayment = paymentRepository.save(payment);
        order.getPayments().add(savedPayment);

        // 8. Update Order status to PAGADO or ENTREGADO
        OrderStatus previousStatus = order.getStatus();
        OrderStatus targetStatus = (previousStatus == OrderStatus.DESPACHADO || previousStatus == OrderStatus.ENTREGADO)
                ? OrderStatus.ENTREGADO
                : OrderStatus.PAGADO;
        order.setStatus(targetStatus);
        order.setPaidAt(Instant.now());
        orderRepository.save(order);

        // 9. Record history & audit
        String actorEmail = actor != null ? actor.getUsername() : "sistema";
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .previousStatus(previousStatus)
                .newStatus(targetStatus)
                .user(actorEntity)
                .notes("Pago verificado por " + actorEmail)
                .build();
        order.getStatusHistory().add(history);
        orderStatusHistoryRepository.save(history);

        AuditLog auditLog = AuditLog.builder()
                .actorEmail(actorEmail)
                .action("PAYMENT_VERIFIED")
                .entityType("ORDER")
                .entityId(order.getId().toString())
                .beforeSnapshot(previousStatus.name())
                .afterSnapshot(targetStatus.name() + " | Ref: " + transferRef + " | Monto: " + request.getAmount())
                .build();
        auditLogRepository.save(auditLog);

        log.info("Payment confirmed successfully for order reference='{}', paymentId={}",
                order.getReference(), savedPayment.getId());

        return PaymentResponse.from(savedPayment);
    }
}
