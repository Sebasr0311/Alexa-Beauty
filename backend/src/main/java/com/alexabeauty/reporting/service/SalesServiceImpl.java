package com.alexabeauty.reporting.service;

import com.alexabeauty.auth.domain.User;
import com.alexabeauty.auth.repository.UserRepository;
import com.alexabeauty.catalog.domain.ProductVariant;
import com.alexabeauty.catalog.repository.ProductVariantRepository;
import com.alexabeauty.common.audit.AuditLog;
import com.alexabeauty.common.audit.AuditLogRepository;
import com.alexabeauty.common.error.ConflictException;
import com.alexabeauty.common.error.ResourceNotFoundException;
import com.alexabeauty.common.error.ValidationException;
import com.alexabeauty.config.repository.AppSettingRepository;
import com.alexabeauty.inventory.domain.InventoryMovement;
import com.alexabeauty.inventory.domain.MovementType;
import com.alexabeauty.inventory.repository.InventoryMovementRepository;
import com.alexabeauty.order.domain.Order;
import com.alexabeauty.order.domain.OrderItem;
import com.alexabeauty.order.domain.OrderStatus;
import com.alexabeauty.order.domain.OrderStatusHistory;
import com.alexabeauty.order.repository.OrderRepository;
import com.alexabeauty.order.repository.OrderStatusHistoryRepository;
import com.alexabeauty.order.service.OrderReferenceGenerator;
import com.alexabeauty.payment.domain.Payment;
import com.alexabeauty.payment.domain.PaymentMethod;
import com.alexabeauty.payment.domain.PaymentStatus;
import com.alexabeauty.payment.repository.PaymentRepository;
import com.alexabeauty.reporting.dto.CreatePhysicalSaleRequest;
import com.alexabeauty.reporting.dto.DailySalesDto;
import com.alexabeauty.reporting.dto.PaymentMethodBreakdownDto;
import com.alexabeauty.reporting.dto.PhysicalSaleItemRequest;
import com.alexabeauty.reporting.dto.SaleOrderItemResponse;
import com.alexabeauty.reporting.dto.SaleOrderResponse;
import com.alexabeauty.reporting.dto.SalesAnalyticsResponse;
import com.alexabeauty.reporting.dto.SalesListResponse;
import com.alexabeauty.reporting.dto.SalesSummaryDto;
import com.alexabeauty.reporting.dto.TopProductDto;
import com.alexabeauty.reporting.excel.ExcelSalesReportGenerator;
import com.alexabeauty.security.UserPrincipal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
public class SalesServiceImpl implements SalesService {

    public static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");
    public static final DateTimeFormatter LOCAL_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final OrderRepository orderRepository;
    private final ExcelSalesReportGenerator excelSalesReportGenerator;
    private final ProductVariantRepository productVariantRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AppSettingRepository appSettingRepository;
    private final OrderReferenceGenerator orderReferenceGenerator;

    @Autowired
    public SalesServiceImpl(
            OrderRepository orderRepository,
            ExcelSalesReportGenerator excelSalesReportGenerator,
            ProductVariantRepository productVariantRepository,
            InventoryMovementRepository inventoryMovementRepository,
            PaymentRepository paymentRepository,
            OrderStatusHistoryRepository orderStatusHistoryRepository,
            UserRepository userRepository,
            AuditLogRepository auditLogRepository,
            AppSettingRepository appSettingRepository,
            OrderReferenceGenerator orderReferenceGenerator
    ) {
        this.orderRepository = orderRepository;
        this.excelSalesReportGenerator = excelSalesReportGenerator;
        this.productVariantRepository = productVariantRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.paymentRepository = paymentRepository;
        this.orderStatusHistoryRepository = orderStatusHistoryRepository;
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
        this.appSettingRepository = appSettingRepository;
        this.orderReferenceGenerator = orderReferenceGenerator;
    }

    public SalesServiceImpl(
            OrderRepository orderRepository,
            ExcelSalesReportGenerator excelSalesReportGenerator
    ) {
        this(orderRepository, excelSalesReportGenerator, null, null, null, null, null, null, null, null);
    }

    @Override
    @Transactional(readOnly = true)
    public SalesListResponse getSales(LocalDate from, LocalDate to, PaymentMethod method, Pageable pageable) {
        Instant startInstant = from.atStartOfDay(BOGOTA_ZONE).toInstant();
        Instant endInstant = to.atTime(LocalTime.MAX).atZone(BOGOTA_ZONE).toInstant();
        List<OrderStatus> statuses = OrderRepository.VERIFIED_PAID_STATUSES;

        // Fetch page for listing
        Page<Order> orderPage = orderRepository.findPaidOrders(statuses, startInstant, endInstant, method, pageable);

        // Fetch all matching orders in range for exact KPI summary
        List<Order> allOrders = orderRepository.findPaidOrdersList(statuses, startInstant, endInstant, method);
        SalesSummaryDto summary = calculateSummary(allOrders, from, to);

        List<SaleOrderResponse> orders = orderPage.getContent().stream()
                .map(this::toSaleOrderResponse)
                .toList();

        return new SalesListResponse(
                summary,
                orders,
                orderPage.getNumber(),
                orderPage.getSize(),
                orderPage.getTotalElements(),
                orderPage.getTotalPages()
        );
    }

    @Override
    @Transactional
    public SaleOrderResponse createPhysicalSale(CreatePhysicalSaleRequest request, UserPrincipal actor) {
        if (request == null || request.items() == null || request.items().isEmpty()) {
            throw new ValidationException("Debe incluir al menos un producto para registrar la venta en punto físico.");
        }
        if (request.paymentMethod() == null) {
            throw new ValidationException("El método de pago es obligatorio.");
        }

        if (appSettingRepository != null) {
            appSettingRepository.lockBootstrapSetting();
        }

        // 1. Deterministically lock requested variants in ascending UUID order
        List<UUID> variantIds = request.items().stream()
                .map(PhysicalSaleItemRequest::variantId)
                .distinct()
                .sorted()
                .toList();

        List<ProductVariant> lockedVariants = productVariantRepository.findByIdInOrderByIdForUpdate(variantIds);
        Map<UUID, ProductVariant> variantMap = lockedVariants.stream()
                .collect(Collectors.toMap(ProductVariant::getId, v -> v));

        for (PhysicalSaleItemRequest itemReq : request.items()) {
            if (itemReq.variantId() == null || !variantMap.containsKey(itemReq.variantId())) {
                throw new ResourceNotFoundException("Variante no encontrada: " + itemReq.variantId());
            }
            if (itemReq.quantity() == null || itemReq.quantity() < 1) {
                throw new ValidationException("La cantidad de cada artículo debe ser al menos 1.");
            }
        }

        // Aggregate requested quantities per variant preserving insertion order
        Map<UUID, Integer> requestedByVariant = new LinkedHashMap<>();
        for (PhysicalSaleItemRequest itemReq : request.items()) {
            requestedByVariant.merge(itemReq.variantId(), itemReq.quantity(), Integer::sum);
        }

        // 2. Validate active status & available stock (stockOnHand - stockReserved)
        for (Map.Entry<UUID, Integer> entry : requestedByVariant.entrySet()) {
            ProductVariant variant = variantMap.get(entry.getKey());
            if (!variant.isActive() || !variant.getProduct().isActive() || variant.getProduct().isDeleted()) {
                throw new ConflictException("El producto o variante no está activo para la venta: "
                        + variant.getProduct().getName() + " (" + variant.getSku() + ")");
            }
            int available = variant.getStockOnHand() - variant.getStockReserved();
            int requestedQty = entry.getValue();
            if (available < requestedQty) {
                throw new ConflictException("Stock disponible insuficiente para el producto: "
                        + variant.getProduct().getName() + " (" + variant.getSku() + "). Disponibles: " + available);
            }
        }

        // 3. Build OrderItems with frozen unitPrice and costSnapshot
        BigDecimal subtotal = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        List<OrderItem> orderItems = new ArrayList<>();

        for (Map.Entry<UUID, Integer> entry : requestedByVariant.entrySet()) {
            ProductVariant variant = variantMap.get(entry.getKey());
            int qty = entry.getValue();
            BigDecimal unitPrice = variant.getSalePrice().setScale(2, RoundingMode.HALF_UP);
            BigDecimal lineSubtotal = unitPrice.multiply(BigDecimal.valueOf(qty)).setScale(2, RoundingMode.HALF_UP);
            subtotal = subtotal.add(lineSubtotal);

            OrderItem item = OrderItem.builder()
                    .variant(variant)
                    .sku(variant.getSku())
                    .productName(variant.getProduct().getName())
                    .variantName(variant.displayName())
                    .unitPrice(unitPrice)
                    .quantity(qty)
                    .subtotal(lineSubtotal)
                    .costSnapshot(variant.getReferenceCost())
                    .build();
            orderItems.add(item);
        }

        BigDecimal discount = request.discount() != null
                ? request.discount().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        if (discount.signum() < 0) {
            throw new ValidationException("El descuento no puede ser negativo.");
        }
        if (discount.compareTo(subtotal) > 0) {
            throw new ValidationException("El descuento no puede superar el subtotal de la venta.");
        }

        BigDecimal shippingFee = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        BigDecimal total = subtotal.subtract(discount).add(shippingFee).setScale(2, RoundingMode.HALF_UP);

        String reference = orderReferenceGenerator.generateReference();
        Instant now = Instant.now();
        String trackingToken = UUID.randomUUID().toString();
        String trackingTokenHash = hashToken(trackingToken);

        String customerName = (request.customerName() != null && !request.customerName().isBlank())
                ? request.customerName().trim()
                : "Cliente Punto Físico";
        String customerPhone = (request.customerPhone() != null && !request.customerPhone().isBlank())
                ? request.customerPhone().trim()
                : "Mostrador";
        String notes = (request.notes() != null && !request.notes().isBlank())
                ? request.notes().trim()
                : "Venta presencial registrada en punto físico";
        String transferRef = (request.transferReference() != null && !request.transferReference().isBlank())
                ? request.transferReference().trim()
                : ("POS-" + reference);

        User actorEntity = null;
        if (actor != null && actor.getId() != null && userRepository != null) {
            actorEntity = userRepository.findById(actor.getId()).orElse(null);
        }
        String actorEmail = actor != null ? actor.getUsername() : "sistema";

        // 4. Create Order in ENTREGADO status and PUNTO_FISICO channel
        Order order = Order.builder()
                .reference(reference)
                .customerName(customerName)
                .customerPhone(customerPhone)
                .deliveryType("TIENDA")
                .customerNotes(notes)
                .subtotal(subtotal)
                .discount(discount)
                .shippingFee(shippingFee)
                .total(total)
                .status(OrderStatus.ENTREGADO)
                .channel("PUNTO_FISICO")
                .expiresAt(now)
                .paidAt(now)
                .trackingToken(trackingToken)
                .trackingTokenHash(trackingTokenHash)
                .build();

        for (OrderItem item : orderItems) {
            item.setOrder(order);
            order.getItems().add(item);
        }

        Order savedOrder = orderRepository.save(order);

        // 5. Deduct physical stock immediately and record VENTA movements in Kardex
        for (OrderItem item : savedOrder.getItems()) {
            ProductVariant variant = variantMap.get(item.getVariant().getId());
            int qty = item.getQuantity();
            int qtyBefore = variant.getStockOnHand();
            int qtyAfter = qtyBefore - qty;

            variant.setStockOnHand(qtyAfter);
            productVariantRepository.save(variant);

            InventoryMovement movement = InventoryMovement.builder()
                    .variant(variant)
                    .movementType(MovementType.VENTA)
                    .quantityDelta(-qty)
                    .qtyBefore(qtyBefore)
                    .qtyAfter(qtyAfter)
                    .reference(reference)
                    .reason("Venta en punto físico (" + request.paymentMethod().name() + ") ref: " + transferRef)
                    .user(actorEntity)
                    .build();
            inventoryMovementRepository.save(movement);
        }

        // 6. Create VERIFICADO Payment
        Payment payment = Payment.builder()
                .order(savedOrder)
                .paymentMethod(request.paymentMethod())
                .transferReference(transferRef)
                .amount(total)
                .status(PaymentStatus.VERIFICADO)
                .verifiedBy(actorEntity)
                .verifiedAt(now)
                .notes(notes)
                .build();
        Payment savedPayment = paymentRepository.save(payment);
        savedOrder.getPayments().add(savedPayment);

        // 7. Record OrderStatusHistory and AuditLog
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(savedOrder)
                .previousStatus(null)
                .newStatus(OrderStatus.ENTREGADO)
                .user(actorEntity)
                .notes("Venta en punto físico registrada y pagada (" + request.paymentMethod().name() + ") por " + actorEmail)
                .build();
        savedOrder.getStatusHistory().add(history);
        orderStatusHistoryRepository.save(history);

        AuditLog auditLog = AuditLog.builder()
                .actorEmail(actorEmail)
                .action("PHYSICAL_SALE_CREATED")
                .entityType("ORDER")
                .entityId(savedOrder.getId().toString())
                .beforeSnapshot(null)
                .afterSnapshot("ENTREGADO | PUNTO_FISICO | Método: " + request.paymentMethod().name() + " | Total: " + total)
                .build();
        auditLogRepository.save(auditLog);

        log.info("Physical sale registered: reference='{}', method={}, total={}, actor='{}'",
                reference, request.paymentMethod(), total, actorEmail);

        return toSaleOrderResponse(savedOrder);
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

    @Override
    @Transactional(readOnly = true)
    public SalesAnalyticsResponse getAnalytics(LocalDate from, LocalDate to) {
        Instant startInstant = from.atStartOfDay(BOGOTA_ZONE).toInstant();
        Instant endInstant = to.atTime(LocalTime.MAX).atZone(BOGOTA_ZONE).toInstant();
        List<OrderStatus> statuses = OrderRepository.VERIFIED_PAID_STATUSES;

        List<Order> orders = orderRepository.findPaidOrdersList(statuses, startInstant, endInstant, null);
        SalesSummaryDto summary = calculateSummary(orders, from, to);
        List<DailySalesDto> dailySales = calculateDailySales(orders, from, to);
        List<TopProductDto> topProducts = calculateTopProducts(orders);
        List<PaymentMethodBreakdownDto> paymentMethods = calculatePaymentMethods(orders, summary.totalSales());

        return new SalesAnalyticsResponse(summary, dailySales, topProducts, paymentMethods);
    }

    @Override
    @Transactional(readOnly = true)
    public List<TopProductDto> getTopProducts(LocalDate from, LocalDate to) {
        Instant startInstant = from.atStartOfDay(BOGOTA_ZONE).toInstant();
        Instant endInstant = to.atTime(LocalTime.MAX).atZone(BOGOTA_ZONE).toInstant();
        List<OrderStatus> statuses = OrderRepository.VERIFIED_PAID_STATUSES;

        List<Order> orders = orderRepository.findPaidOrdersList(statuses, startInstant, endInstant, null);
        return calculateTopProducts(orders);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportSalesReport(LocalDate from, LocalDate to, PaymentMethod method, UserPrincipal actor) {
        Instant startInstant = from.atStartOfDay(BOGOTA_ZONE).toInstant();
        Instant endInstant = to.atTime(LocalTime.MAX).atZone(BOGOTA_ZONE).toInstant();
        List<OrderStatus> statuses = OrderRepository.VERIFIED_PAID_STATUSES;

        List<Order> orders = orderRepository.findPaidOrdersList(statuses, startInstant, endInstant, method);
        SalesSummaryDto summary = calculateSummary(orders, from, to);
        List<TopProductDto> topProducts = calculateTopProducts(orders);

        List<SaleOrderResponse> saleOrderResponses = orders.stream()
                .map(this::toSaleOrderResponse)
                .toList();

        return excelSalesReportGenerator.generateSalesReport(
                saleOrderResponses,
                summary,
                topProducts,
                from,
                to,
                actor
        );
    }

    private SalesSummaryDto calculateSummary(List<Order> orders, LocalDate startDate, LocalDate endDate) {
        BigDecimal totalSales = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long paidOrdersCount = orders.size();
        long totalUnitsSold = 0;
        long unitsWithoutCost = 0;
        BigDecimal merchandiseRevenue = BigDecimal.ZERO;
        BigDecimal coveredRevenue = BigDecimal.ZERO;
        BigDecimal knownCost = BigDecimal.ZERO;

        for (Order order : orders) {
            if (order.getTotal() != null) {
                totalSales = totalSales.add(order.getTotal());
            }
            if (order.getItems() != null) {
                BigDecimal orderMerchandise = order.getItems().stream().map(item -> item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal()).reduce(BigDecimal.ZERO, BigDecimal::add);
                BigDecimal orderCovered = BigDecimal.ZERO;
                for (OrderItem item : order.getItems()) {
                    totalUnitsSold += item.getQuantity();
                    BigDecimal lineRevenue = item.getSubtotal() == null ? BigDecimal.ZERO : item.getSubtotal();

                    if (item.getCostSnapshot() == null) {
                        unitsWithoutCost += item.getQuantity();
                    } else {
                        orderCovered = orderCovered.add(lineRevenue);
                        knownCost = knownCost.add(item.getCostSnapshot().multiply(BigDecimal.valueOf(item.getQuantity())));
                    }
                }
                BigDecimal discount = order.getDiscount() == null ? BigDecimal.ZERO : order.getDiscount();
                BigDecimal netMerchandise = orderMerchandise.subtract(discount);
                merchandiseRevenue = merchandiseRevenue.add(netMerchandise);
                // Allocate an order-level discount proportionally; shipping is excluded.
                if (orderMerchandise.signum() != 0) {
                    coveredRevenue = coveredRevenue.add(orderCovered.multiply(netMerchandise).divide(orderMerchandise, 2, RoundingMode.HALF_UP));
                }
            }
        }

        BigDecimal averageTicket = paidOrdersCount > 0
                ? totalSales.divide(BigDecimal.valueOf(paidOrdersCount), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        return new SalesSummaryDto(
                totalSales,
                paidOrdersCount,
                totalUnitsSold,
                averageTicket,
                startDate,
                endDate,
                merchandiseRevenue, coveredRevenue, knownCost, coveredRevenue.subtract(knownCost),
                coveredRevenue.signum() == 0 ? null : coveredRevenue.subtract(knownCost).multiply(BigDecimal.valueOf(100))
                        .divide(coveredRevenue, 2, RoundingMode.HALF_UP),
                unitsWithoutCost
        );
    }

    private List<DailySalesDto> calculateDailySales(List<Order> orders, LocalDate from, LocalDate to) {
        Map<LocalDate, DailySalesDto> dailyMap = new TreeMap<>();
        LocalDate curr = from;
        while (!curr.isAfter(to)) {
            dailyMap.put(curr, new DailySalesDto(curr, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0L, 0L));
            curr = curr.plusDays(1);
        }

        for (Order order : orders) {
            if (order.getPaidAt() == null) continue;
            LocalDate localDate = order.getPaidAt().atZone(BOGOTA_ZONE).toLocalDate();
            DailySalesDto existing = dailyMap.getOrDefault(localDate,
                    new DailySalesDto(localDate, BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP), 0L, 0L));

            long orderUnits = 0;
            if (order.getItems() != null) {
                orderUnits = order.getItems().stream().mapToLong(OrderItem::getQuantity).sum();
            }

            dailyMap.put(localDate, new DailySalesDto(
                    localDate,
                    existing.totalSales().add(order.getTotal() != null ? order.getTotal() : BigDecimal.ZERO),
                    existing.ordersCount() + 1,
                    existing.unitsCount() + orderUnits
            ));
        }

        return new ArrayList<>(dailyMap.values());
    }

    private List<TopProductDto> calculateTopProducts(List<Order> orders) {
        Map<String, ProductAgg> aggMap = new LinkedHashMap<>();

        for (Order order : orders) {
            if (order.getItems() == null) continue;
            for (OrderItem item : order.getItems()) {
                String sku = item.getSku();
                ProductAgg agg = aggMap.computeIfAbsent(sku, k -> {
                    ProductAgg a = new ProductAgg();
                    a.sku = sku;
                    a.productName = item.getProductName();
                    if (item.getVariant() != null && item.getVariant().getProduct() != null
                            && item.getVariant().getProduct().getBrand() != null) {
                        a.brandName = item.getVariant().getProduct().getBrand().getName();
                    } else {
                        a.brandName = "";
                    }
                    return a;
                });

                agg.unitsSold += item.getQuantity();
                BigDecimal sub = item.getSubtotal() != null ? item.getSubtotal() : BigDecimal.ZERO;
                agg.grossRevenue = agg.grossRevenue.add(sub);
            }
        }

        List<ProductAgg> sortedList = aggMap.values().stream()
                .sorted(Comparator.comparing(ProductAgg::getGrossRevenue).reversed()
                        .thenComparing(Comparator.comparing(ProductAgg::getUnitsSold).reversed()))
                .toList();

        BigDecimal totalGrossRevenue = sortedList.stream()
                .map(ProductAgg::getGrossRevenue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<TopProductDto> result = new ArrayList<>();
        int rank = 1;
        for (ProductAgg agg : sortedList) {
            BigDecimal share = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (totalGrossRevenue.compareTo(BigDecimal.ZERO) > 0) {
                share = agg.grossRevenue.multiply(BigDecimal.valueOf(100))
                        .divide(totalGrossRevenue, 2, RoundingMode.HALF_UP);
            }

            result.add(new TopProductDto(
                    rank++,
                    agg.sku,
                    agg.productName,
                    agg.brandName,
                    agg.unitsSold,
                    agg.grossRevenue,
                    share
            ));
        }

        return result;
    }

    private List<PaymentMethodBreakdownDto> calculatePaymentMethods(List<Order> orders, BigDecimal totalSales) {
        Map<String, MethodAgg> map = new LinkedHashMap<>();

        for (Order order : orders) {
            String method = resolvePaymentMethodName(order);
            MethodAgg agg = map.computeIfAbsent(method, k -> new MethodAgg());
            agg.orderCount++;
            if (order.getTotal() != null) {
                agg.totalAmount = agg.totalAmount.add(order.getTotal());
            }
        }

        List<PaymentMethodBreakdownDto> result = new ArrayList<>();
        for (Map.Entry<String, MethodAgg> entry : map.entrySet()) {
            MethodAgg agg = entry.getValue();
            BigDecimal percentage = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
            if (totalSales != null && totalSales.compareTo(BigDecimal.ZERO) > 0) {
                percentage = agg.totalAmount.multiply(BigDecimal.valueOf(100))
                        .divide(totalSales, 2, RoundingMode.HALF_UP);
            }
            result.add(new PaymentMethodBreakdownDto(
                    entry.getKey(),
                    agg.totalAmount,
                    agg.orderCount,
                    percentage
            ));
        }

        result.sort(Comparator.comparing(PaymentMethodBreakdownDto::totalAmount).reversed());
        return result;
    }

    private SaleOrderResponse toSaleOrderResponse(Order order) {
        Payment verifiedPayment = resolveVerifiedPayment(order);

        String paymentMethod = (verifiedPayment != null && verifiedPayment.getPaymentMethod() != null)
                ? verifiedPayment.getPaymentMethod().name()
                : "DESCONOCIDO";

        String verifiedBy = "Sistema";
        if (verifiedPayment != null && verifiedPayment.getVerifiedBy() != null) {
            String name = verifiedPayment.getVerifiedBy().getFullName();
            verifiedBy = (name != null && !name.isBlank()) ? name : verifiedPayment.getVerifiedBy().getEmail();
        }

        String localPaidAt = order.getPaidAt() != null
                ? order.getPaidAt().atZone(BOGOTA_ZONE).format(LOCAL_DATE_TIME_FORMATTER)
                : "";

        List<SaleOrderItemResponse> itemResponses = new ArrayList<>();
        if (order.getItems() != null) {
            for (OrderItem item : order.getItems()) {
                String brandName = "";
                String shadeName = "";
                BigDecimal costSnapshot = item.getCostSnapshot();

                if (item.getVariant() != null) {
                    ProductVariant variant = item.getVariant();
                    if (variant.getShadeName() != null) {
                        shadeName = variant.getShadeName();
                    }
                    if (variant.getProduct() != null && variant.getProduct().getBrand() != null) {
                        brandName = variant.getProduct().getBrand().getName();
                    }
                }
                if (shadeName.isBlank() && item.getVariantName() != null) {
                    shadeName = item.getVariantName();
                }

                itemResponses.add(new SaleOrderItemResponse(
                        item.getId(),
                        item.getSku(),
                        item.getProductName(),
                        brandName,
                        shadeName,
                        item.getQuantity(),
                        item.getUnitPrice(),
                        item.getSubtotal(),
                        costSnapshot
                ));
            }
        }

        return new SaleOrderResponse(
                order.getId(),
                order.getReference(),
                order.getCustomerName(),
                order.getCustomerPhone(),
                paymentMethod,
                order.getSubtotal(),
                order.getDiscount(),
                order.getShippingFee(),
                order.getTotal(),
                verifiedBy,
                order.getStatus().name(),
                order.getChannel(),
                order.getPaidAt(),
                localPaidAt,
                itemResponses
        );
    }

    private Payment resolveVerifiedPayment(Order order) {
        if (order.getPayments() == null || order.getPayments().isEmpty()) {
            return null;
        }
        return order.getPayments().stream()
                .filter(p -> p.getStatus() == PaymentStatus.VERIFICADO)
                .findFirst()
                .or(() -> order.getPayments().stream().findFirst())
                .orElse(null);
    }

    private String resolvePaymentMethodName(Order order) {
        Payment p = resolveVerifiedPayment(order);
        if (p != null && p.getPaymentMethod() != null) {
            return p.getPaymentMethod().name();
        }
        return "OTRO";
    }

    private static class ProductAgg {
        String sku;
        String productName;
        String brandName;
        long unitsSold = 0;
        BigDecimal grossRevenue = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

        BigDecimal getGrossRevenue() {
            return grossRevenue;
        }

        long getUnitsSold() {
            return unitsSold;
        }
    }

    private static class MethodAgg {
        BigDecimal totalAmount = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        long orderCount = 0;
    }
}
