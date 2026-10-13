package com.alexabeauty.order.service;

import com.alexabeauty.catalog.domain.ProductVariant;
import com.alexabeauty.catalog.repository.ProductVariantRepository;
import com.alexabeauty.common.error.ConflictException;
import com.alexabeauty.common.error.ResourceNotFoundException;
import com.alexabeauty.common.error.ValidationException;
import com.alexabeauty.config.domain.AppSetting;
import com.alexabeauty.config.repository.AppSettingRepository;
import com.alexabeauty.order.domain.Order;
import com.alexabeauty.order.domain.OrderItem;
import com.alexabeauty.order.domain.OrderStatus;
import com.alexabeauty.order.domain.OrderStatusHistory;
import com.alexabeauty.order.domain.ReservationStatus;
import com.alexabeauty.order.domain.StockReservation;
import com.alexabeauty.order.dto.CreateOrderRequest;
import com.alexabeauty.order.dto.OrderCheckoutResponse;
import com.alexabeauty.order.dto.OrderItemRequest;
import com.alexabeauty.order.dto.OrderItemResponse;
import com.alexabeauty.order.dto.OrderTrackingResponse;
import com.alexabeauty.order.repository.OrderRepository;
import com.alexabeauty.whatsapp.WhatsAppGateway;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderCheckoutServiceImpl implements OrderCheckoutService {

    private static final Pattern TRACKING_TOKEN_PATTERN = Pattern.compile("\\[Tracking-Token:\\s*([^\\]]+)\\]");

    private final OrderRepository orderRepository;
    private final ProductVariantRepository productVariantRepository;
    private final AppSettingRepository appSettingRepository;
    private final WhatsAppGateway whatsAppGateway;
    private final OrderReferenceGenerator orderReferenceGenerator;

    private final Map<String, OrderCheckoutResponse> idempotencyCache = new ConcurrentHashMap<>();

    @Override
    @Transactional
    public OrderCheckoutResponse checkout(CreateOrderRequest request, String idempotencyKey) {
        String trimmedKey = (idempotencyKey != null && !idempotencyKey.isBlank()) ? idempotencyKey.trim() : null;

        // 1. Idempotency Check
        if (trimmedKey != null) {
            OrderCheckoutResponse cached = idempotencyCache.get(trimmedKey);
            if (cached != null) {
                log.info("Returning cached response for idempotencyKey='{}', reference='{}'", trimmedKey, cached.getReference());
                return cached;
            }

            String idTag = "[Idempotency-Key: " + trimmedKey + "]";
            Optional<Order> existingOrderOpt = orderRepository.findByIdempotencyTag(idTag);
            if (existingOrderOpt.isPresent()) {
                Order existing = existingOrderOpt.get();
                String recoveredToken = extractTrackingToken(existing.getCustomerNotes());
                String businessPhone = resolveBusinessPhone();
                String messageText = whatsAppGateway.formatOrderMessage(existing);
                String whatsappUrl = whatsAppGateway.generateClickToChatUrl(existing, businessPhone);

                OrderCheckoutResponse response = mapToResponse(existing, recoveredToken, whatsappUrl, messageText);
                idempotencyCache.put(trimmedKey, response);
                log.info("Found existing order for idempotencyKey='{}', reference='{}'", trimmedKey, existing.getReference());
                return response;
            }
        }

        // 2. Deterministically lock all requested variants in ascending UUID order
        List<UUID> variantIds = request.getItems().stream()
                .map(OrderItemRequest::getVariantId)
                .distinct()
                .sorted()
                .toList();

        List<ProductVariant> lockedVariants = productVariantRepository.findByIdInOrderByIdForUpdate(variantIds);
        Map<UUID, ProductVariant> variantMap = lockedVariants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        for (OrderItemRequest itemReq : request.getItems()) {
            if (!variantMap.containsKey(itemReq.getVariantId())) {
                throw new ResourceNotFoundException("Variante no encontrada: " + itemReq.getVariantId());
            }
        }

        // Aggregate requested quantities per variant to validate available stock
        Map<UUID, Integer> totalRequestedByVariant = request.getItems().stream()
                .collect(Collectors.groupingBy(OrderItemRequest::getVariantId, Collectors.summingInt(OrderItemRequest::getQuantity)));

        for (Map.Entry<UUID, Integer> entry : totalRequestedByVariant.entrySet()) {
            ProductVariant variant = variantMap.get(entry.getKey());
            int available = variant.getStockOnHand() - variant.getStockReserved();
            int requestedQty = entry.getValue();
            if (available < requestedQty) {
                throw new ConflictException("Stock insuficiente para el producto: "
                        + variant.getProduct().getName() + " (" + variant.getSku() + "). Disponibles: " + available);
            }
            variant.setStockReserved(variant.getStockReserved() + requestedQty);
        }

        // 3. Freeze unit price from database, calculate line subtotal and order total
        BigDecimal subtotal = BigDecimal.ZERO;
        List<OrderItem> orderItems = new ArrayList<>();

        for (OrderItemRequest itemReq : request.getItems()) {
            ProductVariant variant = variantMap.get(itemReq.getVariantId());
            BigDecimal unitPrice = variant.getSalePrice();
            BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            subtotal = subtotal.add(lineSubtotal);

            String variantDisplayName = variant.getShadeName() != null && !variant.getShadeName().isBlank()
                    ? variant.getShadeName().trim()
                    : (variant.getPresentation() != null ? variant.getPresentation().trim() : null);

            OrderItem item = OrderItem.builder()
                    .variant(variant)
                    .sku(variant.getSku())
                    .productName(variant.getProduct().getName())
                    .variantName(variantDisplayName)
                    .unitPrice(unitPrice)
                    .quantity(itemReq.getQuantity())
                    .subtotal(lineSubtotal)
                    .costSnapshot(variant.getReferenceCost())
                    .build();
            orderItems.add(item);
        }

        BigDecimal shippingFee = BigDecimal.ZERO;
        BigDecimal total = subtotal.add(shippingFee);

        // 4. Generate reference & tracking token
        String reference = orderReferenceGenerator.generateReference();
        String trackingToken = UUID.randomUUID().toString();
        String trackingTokenHash = hashToken(trackingToken);

        // 5. Read reservation minutes & compute expiration
        int reservationMinutes = resolveReservationMinutes();
        Instant expiresAt = Instant.now().plus(Duration.ofMinutes(reservationMinutes));
        String businessPhone = resolveBusinessPhone();

        // 6. Embed idempotency key & tracking token into customer notes
        String rawNotes = request.getCustomerNotes() != null ? request.getCustomerNotes().trim() : "";
        StringBuilder notesBuilder = new StringBuilder();
        if (trimmedKey != null) {
            notesBuilder.append("[Idempotency-Key: ").append(trimmedKey).append("] ");
        }
        notesBuilder.append("[Tracking-Token: ").append(trackingToken).append("] ");
        if (!rawNotes.isBlank()) {
            notesBuilder.append(rawNotes);
        }

        // 7. Create Order entity
        Order order = Order.builder()
                .reference(reference)
                .customerName(request.getCustomerName().trim())
                .customerPhone(request.getCustomerPhone().trim())
                .deliveryType(request.getDeliveryType().trim())
                .city(request.getCity() != null ? request.getCity().trim() : null)
                .address(request.getAddress() != null ? request.getAddress().trim() : null)
                .customerNotes(notesBuilder.toString().trim())
                .subtotal(subtotal)
                .discount(BigDecimal.ZERO)
                .shippingFee(shippingFee)
                .total(total)
                .status(OrderStatus.PENDIENTE_CONTACTO)
                .channel("WHATSAPP")
                .expiresAt(expiresAt)
                .trackingTokenHash(trackingTokenHash)
                .build();

        // 8. Add OrderItems, StockReservations, OrderStatusHistory
        for (OrderItem item : orderItems) {
            item.setOrder(order);
            order.getItems().add(item);

            StockReservation reservation = StockReservation.builder()
                    .order(order)
                    .variant(item.getVariant())
                    .quantity(item.getQuantity())
                    .expiresAt(expiresAt)
                    .status(ReservationStatus.ACTIVE)
                    .build();
            order.getReservations().add(reservation);
        }

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .previousStatus(null)
                .newStatus(OrderStatus.PENDIENTE_CONTACTO)
                .notes("Pedido creado vía WhatsApp checkout. Reserva activa por " + reservationMinutes + " minutos.")
                .build();
        order.getStatusHistory().add(history);

        Order savedOrder = orderRepository.save(order);

        // 9. Call WhatsAppGateway to build prefilled message and wa.me URL
        String messageText = whatsAppGateway.formatOrderMessage(savedOrder);
        String whatsappUrl = whatsAppGateway.generateClickToChatUrl(savedOrder, businessPhone);

        OrderCheckoutResponse response = mapToResponse(savedOrder, trackingToken, whatsappUrl, messageText);

        if (trimmedKey != null) {
            idempotencyCache.put(trimmedKey, response);
        }

        log.info("Order created successfully: reference='{}', total={}, expiresAt={}",
                reference, total, expiresAt);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderTrackingResponse trackOrder(String reference, String token) {
        if (reference == null || reference.isBlank()) {
            throw new ValidationException("La referencia del pedido es requerida");
        }
        if (token == null || token.isBlank()) {
            throw new ValidationException("El token de rastreo es requerido");
        }

        Order order = orderRepository.findByReference(reference.trim())
                .orElseThrow(() -> new ResourceNotFoundException("Pedido no encontrado con la referencia: " + reference));

        String expectedHash = hashToken(token.trim());
        if (!expectedHash.equalsIgnoreCase(order.getTrackingTokenHash())) {
            throw new ValidationException("Token de rastreo inválido para el pedido " + reference);
        }

        int itemCount = order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
        String businessPhone = resolveBusinessPhone();
        String whatsappUrl = whatsAppGateway.generateClickToChatUrl(order, businessPhone);
        String whatsappMessageText = whatsAppGateway.formatOrderMessage(order);

        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::mapItemToResponse)
                .toList();

        return OrderTrackingResponse.builder()
                .reference(order.getReference())
                .status(order.getStatus())
                .expiresAt(order.getExpiresAt())
                .total(order.getTotal())
                .itemCount(itemCount)
                .createdAt(order.getCreatedAt())
                .customerName(order.getCustomerName())
                .deliveryType(order.getDeliveryType())
                .whatsappUrl(whatsappUrl)
                .whatsappMessageText(whatsappMessageText)
                .items(itemResponses)
                .build();
    }

    @Override
    @Transactional
    public void expireOrder(UUID orderId) {
        Order order = orderRepository.findById(orderId).orElse(null);
        if (order == null) {
            return;
        }

        if (order.getStatus() != OrderStatus.PENDIENTE_CONTACTO && order.getStatus() != OrderStatus.PENDIENTE_PAGO) {
            return;
        }

        OrderStatus previousStatus = order.getStatus();

        // Lock variants deterministically
        List<UUID> variantIds = order.getItems().stream()
                .map(item -> item.getVariant().getId())
                .distinct()
                .sorted()
                .toList();

        List<ProductVariant> lockedVariants = productVariantRepository.findByIdInOrderByIdForUpdate(variantIds);
        Map<UUID, ProductVariant> variantMap = lockedVariants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        // Release reserved stock (stock_on_hand is untouched!)
        for (OrderItem item : order.getItems()) {
            ProductVariant variant = variantMap.get(item.getVariant().getId());
            if (variant != null) {
                variant.setStockReserved(Math.max(0, variant.getStockReserved() - item.getQuantity()));
            }
        }

        // Mark StockReservations as RELEASED
        for (StockReservation res : order.getReservations()) {
            if (res.getStatus() == ReservationStatus.ACTIVE) {
                res.setStatus(ReservationStatus.RELEASED);
            }
        }

        // Transition Order.status = EXPIRADO
        order.setStatus(OrderStatus.EXPIRADO);

        // Add OrderStatusHistory entry
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .previousStatus(previousStatus)
                .newStatus(OrderStatus.EXPIRADO)
                .notes("Tiempo de reserva agotado (45 min)")
                .build();
        order.getStatusHistory().add(history);

        orderRepository.save(order);
        log.info("Order reference='{}' expired. Stock reservations released.", order.getReference());
    }

    @Override
    @Transactional
    public int expireOverdueReservations() {
        Instant now = Instant.now();
        List<Order> overdueOrders = orderRepository.findByStatusInAndExpiresAtBefore(
                List.of(OrderStatus.PENDIENTE_CONTACTO, OrderStatus.PENDIENTE_PAGO),
                now
        );

        int count = 0;
        for (Order order : overdueOrders) {
            try {
                expireOrder(order.getId());
                count++;
            } catch (Exception e) {
                log.error("Failed to expire order id={}", order.getId(), e);
            }
        }
        return count;
    }

    private OrderCheckoutResponse mapToResponse(Order order, String trackingToken, String whatsappUrl, String whatsappMessageText) {
        List<OrderItemResponse> itemResponses = order.getItems().stream()
                .map(this::mapItemToResponse)
                .toList();

        return OrderCheckoutResponse.builder()
                .orderId(order.getId())
                .reference(order.getReference())
                .trackingToken(trackingToken)
                .status(order.getStatus())
                .expiresAt(order.getExpiresAt())
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .total(order.getTotal())
                .whatsappUrl(whatsappUrl)
                .whatsappMessageText(whatsappMessageText)
                .items(itemResponses)
                .build();
    }

    private OrderItemResponse mapItemToResponse(OrderItem item) {
        return OrderItemResponse.builder()
                .id(item.getId())
                .variantId(item.getVariant().getId())
                .sku(item.getSku())
                .productName(item.getProductName())
                .variantName(item.getVariantName())
                .unitPrice(item.getUnitPrice())
                .quantity(item.getQuantity())
                .subtotal(item.getSubtotal())
                .build();
    }

    private int resolveReservationMinutes() {
        return appSettingRepository.findBySettingKey("reservation_minutes")
                .map(s -> {
                    try {
                        return Integer.parseInt(s.getSettingValue().trim());
                    } catch (Exception e) {
                        return 45;
                    }
                })
                .orElse(45);
    }

    private String resolveBusinessPhone() {
        return appSettingRepository.findBySettingKey("whatsapp_number")
                .map(AppSetting::getSettingValue)
                .orElse("573001234567");
    }

    private String extractTrackingToken(String customerNotes) {
        if (customerNotes != null) {
            Matcher matcher = TRACKING_TOKEN_PATTERN.matcher(customerNotes);
            if (matcher.find()) {
                return matcher.group(1).trim();
            }
        }
        return null;
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
