package com.alexabeauty.order.service;

import com.alexabeauty.catalog.repository.ProductVariantRepository;
import com.alexabeauty.common.error.ConflictException;
import com.alexabeauty.order.domain.*;
import com.alexabeauty.order.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/** Each discovered ID is reloaded in a fresh transaction before taking variant locks. */
@Service
@RequiredArgsConstructor
public class OrderExpirationWorker {
    private final OrderRepository orders;
    private final ProductVariantRepository variants;
    public static final List<OrderStatus> EXPIRABLE = List.of(OrderStatus.PENDIENTE_CONTACTO,
            OrderStatus.PENDIENTE_PAGO, OrderStatus.PAGO_EN_REVISION, OrderStatus.PENDIENTE);

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean expire(UUID id) {
        Order order = orders.findByIdForUpdate(id).orElse(null);
        if (order == null || !EXPIRABLE.contains(order.getStatus()) || order.getExpiresAt() == null
                || order.getExpiresAt().isAfter(Instant.now())) return false;
        var active = order.getReservations().stream().filter(r -> r.getStatus() == ReservationStatus.ACTIVE).toList();
        var quantities = active.stream().collect(Collectors.groupingBy(r -> r.getVariant().getId(),
                Collectors.summingInt(StockReservation::getQuantity)));
        var expected = order.getItems().stream().collect(Collectors.groupingBy(i -> i.getVariant().getId(),
                Collectors.summingInt(OrderItem::getQuantity)));
        if (!quantities.equals(expected)) throw new ConflictException("Reservation mismatch; expiration requires reconciliation");
        var ids = quantities.keySet().stream().sorted().toList();
        var locked = variants.findByIdInOrderByIdForUpdate(ids).stream()
                .collect(Collectors.toMap(v -> v.getId(), Function.identity()));
        quantities.forEach((variantId, quantity) -> {
            var variant = locked.get(variantId);
            if (variant == null || quantity <= 0 || variant.getStockReserved() < quantity)
                throw new ConflictException("Reserved inventory mismatch; expiration requires reconciliation");
            variant.setStockReserved(variant.getStockReserved() - quantity);
        });
        active.forEach(r -> r.setStatus(ReservationStatus.RELEASED));
        OrderStatus previous = order.getStatus();
        order.setStatus(OrderStatus.EXPIRADO);
        order.getStatusHistory().add(OrderStatusHistory.builder().order(order).previousStatus(previous)
                .newStatus(OrderStatus.EXPIRADO).notes("Reservation expired").build());
        orders.save(order);
        return true;
    }
}
