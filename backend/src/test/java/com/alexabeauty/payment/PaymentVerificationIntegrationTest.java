package com.alexabeauty.payment;

import com.alexabeauty.auth.domain.User;
import com.alexabeauty.auth.repository.UserRepository;
import com.alexabeauty.catalog.domain.Brand;
import com.alexabeauty.catalog.domain.Category;
import com.alexabeauty.catalog.domain.Product;
import com.alexabeauty.catalog.domain.ProductVariant;
import com.alexabeauty.catalog.repository.BrandRepository;
import com.alexabeauty.catalog.repository.CategoryRepository;
import com.alexabeauty.catalog.repository.ProductRepository;
import com.alexabeauty.catalog.repository.ProductVariantRepository;
import com.alexabeauty.inventory.domain.InventoryMovement;
import com.alexabeauty.inventory.domain.MovementType;
import com.alexabeauty.inventory.repository.InventoryMovementRepository;
import com.alexabeauty.order.domain.Order;
import com.alexabeauty.order.domain.OrderStatus;
import com.alexabeauty.order.domain.ReservationStatus;
import com.alexabeauty.order.domain.StockReservation;
import com.alexabeauty.order.dto.CreateOrderRequest;
import com.alexabeauty.order.dto.ExtendReservationRequest;
import com.alexabeauty.order.dto.OrderCheckoutResponse;
import com.alexabeauty.order.dto.OrderItemRequest;
import com.alexabeauty.order.dto.OrderStatusTransitionRequest;
import com.alexabeauty.order.repository.OrderRepository;
import com.alexabeauty.order.repository.StockReservationRepository;
import com.alexabeauty.order.service.OrderCheckoutService;
import com.alexabeauty.payment.domain.Payment;
import com.alexabeauty.payment.domain.PaymentMethod;
import com.alexabeauty.payment.domain.PaymentStatus;
import com.alexabeauty.payment.dto.ConfirmPaymentRequest;
import com.alexabeauty.payment.repository.PaymentRepository;
import com.alexabeauty.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PaymentVerificationIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private OrderCheckoutService orderCheckoutService;

    private String adminToken;
    private Brand testBrand;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        User admin = userRepository.findByEmail("admin@alexabeauty.com").orElseThrow();
        adminToken = jwtTokenProvider.generateToken(admin);

        testBrand = brandRepository.findAll().stream().findFirst().orElseThrow();
        testCategory = categoryRepository.findAll().stream().findFirst().orElseThrow();
    }

    private ProductVariant createTestVariant(String skuPrefix, int stockOnHand, int stockReserved) {
        Product product = Product.builder()
                .name("Producto Test " + skuPrefix)
                .sku("PROD-" + skuPrefix)
                .slug("prod-test-" + UUID.randomUUID())
                .brand(testBrand)
                .category(testCategory)
                .active(true)
                .build();
        Product savedProduct = productRepository.save(product);

        ProductVariant variant = ProductVariant.builder()
                .product(savedProduct)
                .sku("VAR-" + skuPrefix)
                .shadeName("Tono " + skuPrefix)
                .salePrice(new BigDecimal("50000.00"))
                .referenceCost(new BigDecimal("25000.00"))
                .stockOnHand(stockOnHand)
                .stockReserved(stockReserved)
                .minStock(2)
                .active(true)
                .build();
        return productVariantRepository.save(variant);
    }

    private OrderCheckoutResponse createTestOrder(ProductVariant variant, int quantity) {
        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerName("María Rodríguez")
                .customerPhone("3001234567")
                .deliveryType("LOCAL_PICKUP")
                .items(List.of(
                        OrderItemRequest.builder()
                                .variantId(variant.getId())
                                .quantity(quantity)
                                .build()
                ))
                .build();

        return orderCheckoutService.checkout(request, "IDEM-TEST-" + UUID.randomUUID());
    }

    @Test
    @DisplayName("P15 Matrix: Mark payment confirmed twice -> single VENTA movement created, stock reduced exactly once")
    void testP15_doublePaymentConfirmation_isIdempotent() throws Exception {
        // Initial variant stock: onHand=10, reserved=0
        ProductVariant variant = createTestVariant("P15-" + UUID.randomUUID().toString().substring(0, 8), 10, 0);

        // Checkout order for 2 units -> onHand=10, reserved=2
        OrderCheckoutResponse checkout = createTestOrder(variant, 2);
        UUID orderId = checkout.getOrderId();

        ProductVariant variantAfterCheckout = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(variantAfterCheckout.getStockOnHand()).isEqualTo(10);
        assertThat(variantAfterCheckout.getStockReserved()).isEqualTo(2);

        ConfirmPaymentRequest confirmRequest = ConfirmPaymentRequest.builder()
                .paymentMethod(PaymentMethod.NEQUI)
                .transferReference("NEQ-987654321")
                .amount(checkout.getTotal())
                .notes("Pago transferido vía Nequi")
                .build();

        // 1. First confirmation
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/confirm-payment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("VERIFICADO"))
                .andExpect(jsonPath("$.data.transferReference").value("NEQ-987654321"));

        // Verify order status is PAGADO
        Order orderAfterFirstConfirm = orderRepository.findById(orderId).orElseThrow();
        assertThat(orderAfterFirstConfirm.getStatus()).isEqualTo(OrderStatus.PAGADO);
        assertThat(orderAfterFirstConfirm.getPaidAt()).isNotNull();

        // Verify stock: stockOnHand reduced to 8, stockReserved reduced to 0
        ProductVariant variantAfterFirstConfirm = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(variantAfterFirstConfirm.getStockOnHand()).isEqualTo(8);
        assertThat(variantAfterFirstConfirm.getStockReserved()).isEqualTo(0);

        // Verify reservations marked as CONSUMED
        List<StockReservation> reservations = stockReservationRepository.findByOrderId(orderId);
        assertThat(reservations).allMatch(r -> r.getStatus() == ReservationStatus.CONSUMED);

        // Verify exactly 1 VENTA movement
        List<InventoryMovement> movements = inventoryMovementRepository.findByVariantIdOrderByCreatedAtDesc(variant.getId());
        List<InventoryMovement> ventaMovements = movements.stream()
                .filter(m -> m.getMovementType() == MovementType.VENTA)
                .toList();
        assertThat(ventaMovements).hasSize(1);
        assertThat(ventaMovements.get(0).getQuantityDelta()).isEqualTo(-2);
        assertThat(ventaMovements.get(0).getQtyBefore()).isEqualTo(10);
        assertThat(ventaMovements.get(0).getQtyAfter()).isEqualTo(8);

        // 2. Second confirmation (Idempotent replay)
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/confirm-payment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("VERIFICADO"));

        // Verify stock is STILL 8 (NOT 6!)
        ProductVariant variantAfterSecondConfirm = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(variantAfterSecondConfirm.getStockOnHand()).isEqualTo(8);
        assertThat(variantAfterSecondConfirm.getStockReserved()).isEqualTo(0);

        // Verify still exactly 1 VENTA movement
        List<InventoryMovement> movementsAfterSecond = inventoryMovementRepository.findByVariantIdOrderByCreatedAtDesc(variant.getId());
        List<InventoryMovement> ventaMovementsAfterSecond = movementsAfterSecond.stream()
                .filter(m -> m.getMovementType() == MovementType.VENTA)
                .toList();
        assertThat(ventaMovementsAfterSecond).hasSize(1);

        // Verify single verified payment record
        List<Payment> payments = paymentRepository.findByOrderId(orderId);
        assertThat(payments).hasSize(1);
        assertThat(payments.get(0).getStatus()).isEqualTo(PaymentStatus.VERIFICADO);
    }

    @Test
    @DisplayName("P16 Matrix: Concurrent payment confirmation on same order -> both succeed without duplicating inventory consumption or throwing uncaught 500")
    void testP16_concurrentPaymentConfirmation_doesNotDuplicateInventoryConsumption() throws Exception {
        ProductVariant variant = createTestVariant("P16-" + UUID.randomUUID().toString().substring(0, 8), 10, 0);
        OrderCheckoutResponse checkout = createTestOrder(variant, 3);
        UUID orderId = checkout.getOrderId();

        ConfirmPaymentRequest confirmRequest = ConfirmPaymentRequest.builder()
                .paymentMethod(PaymentMethod.BANCOLOMBIA)
                .transferReference("BCOL-CONCURRENT-001")
                .amount(checkout.getTotal())
                .notes("Pago concurrente")
                .build();

        int threadCount = 2;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        List<Future<Integer>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            futures.add(executor.submit(() -> {
                startLatch.await();
                MvcResult result = mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/confirm-payment")
                                .header("Authorization", "Bearer " + adminToken)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(confirmRequest)))
                        .andReturn();
                return result.getResponse().getStatus();
            }));
        }

        startLatch.countDown();
        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        for (Future<Integer> f : futures) {
            int statusCode = f.get();
            if (statusCode == 200) {
                successCount.incrementAndGet();
            }
        }

        // Both requests succeed gracefully (200 OK)
        assertThat(successCount.get()).isEqualTo(threadCount);

        // Inventory consumed EXACTLY once: 10 - 3 = 7
        ProductVariant finalVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(finalVariant.getStockOnHand()).isEqualTo(7);
        assertThat(finalVariant.getStockReserved()).isEqualTo(0);

        // Exactly 1 VENTA movement recorded
        List<InventoryMovement> movements = inventoryMovementRepository.findByVariantIdOrderByCreatedAtDesc(variant.getId());
        long ventaCount = movements.stream().filter(m -> m.getMovementType() == MovementType.VENTA).count();
        assertThat(ventaCount).isEqualTo(1);
    }

    @Test
    @DisplayName("P17 Matrix: Attempt to confirm payment on EXPIRED order -> throws 409 Conflict, inventory untouched")
    void testP17_confirmPaymentOnExpiredOrder_throwsConflict() throws Exception {
        ProductVariant variant = createTestVariant("P17-" + UUID.randomUUID().toString().substring(0, 8), 10, 0);
        OrderCheckoutResponse checkout = createTestOrder(variant, 2);
        UUID orderId = checkout.getOrderId();

        // Expire the order
        orderCheckoutService.expireOrder(orderId);

        Order expiredOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(expiredOrder.getStatus()).isEqualTo(OrderStatus.EXPIRADO);

        // Reserved stock is now released (onHand=10, reserved=0)
        ProductVariant variantAfterExpiry = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(variantAfterExpiry.getStockOnHand()).isEqualTo(10);
        assertThat(variantAfterExpiry.getStockReserved()).isEqualTo(0);

        ConfirmPaymentRequest confirmRequest = ConfirmPaymentRequest.builder()
                .paymentMethod(PaymentMethod.DAVIPLATA)
                .transferReference("DVP-EXPIRED-TEST")
                .amount(checkout.getTotal())
                .build();

        // Attempt confirm payment on EXPIRED order
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/confirm-payment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("EXPIRADO")));

        // Stock MUST remain untouched (onHand=10, reserved=0)
        ProductVariant variantAfterAttempt = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(variantAfterAttempt.getStockOnHand()).isEqualTo(10);
        assertThat(variantAfterAttempt.getStockReserved()).isEqualTo(0);

        // Zero VENTA movements
        List<InventoryMovement> movements = inventoryMovementRepository.findByVariantIdOrderByCreatedAtDesc(variant.getId());
        assertThat(movements).noneMatch(m -> m.getMovementType() == MovementType.VENTA);
    }

    @Test
    @DisplayName("P18 Matrix: Cancel unpaid order -> releases stockReserved, preserves stockOnHand")
    void testP18_cancelUnpaidOrder_releasesReservedStockAndPreservesOnHand() throws Exception {
        ProductVariant variant = createTestVariant("P18-" + UUID.randomUUID().toString().substring(0, 8), 15, 0);
        OrderCheckoutResponse checkout = createTestOrder(variant, 4);
        UUID orderId = checkout.getOrderId();

        ProductVariant variantAfterCheckout = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(variantAfterCheckout.getStockOnHand()).isEqualTo(15);
        assertThat(variantAfterCheckout.getStockReserved()).isEqualTo(4);

        OrderStatusTransitionRequest cancelRequest = OrderStatusTransitionRequest.builder()
                .newStatus(OrderStatus.CANCELADO)
                .notes("Cliente no respondió confirmación en WhatsApp")
                .build();

        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/transition")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(cancelRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("CANCELADO"));

        // Order is CANCELADO
        Order cancelledOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(cancelledOrder.getStatus()).isEqualTo(OrderStatus.CANCELADO);

        // Stock reserved is RELEASED (0), stock on hand is PRESERVED (15)
        ProductVariant variantAfterCancel = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(variantAfterCancel.getStockOnHand()).isEqualTo(15);
        assertThat(variantAfterCancel.getStockReserved()).isEqualTo(0);

        // Reservations are marked RELEASED
        List<StockReservation> reservations = stockReservationRepository.findByOrderId(orderId);
        assertThat(reservations).allMatch(r -> r.getStatus() == ReservationStatus.RELEASED);

        // No VENTA movements were generated
        List<InventoryMovement> movements = inventoryMovementRepository.findByVariantIdOrderByCreatedAtDesc(variant.getId());
        assertThat(movements).noneMatch(m -> m.getMovementType() == MovementType.VENTA);
    }

    @Test
    @DisplayName("Order Status Lifecycle: PAGADO -> PREPARANDO -> DESPACHADO -> ENTREGADO")
    void testOrderStatusLifecycle_successfulFlow() throws Exception {
        ProductVariant variant = createTestVariant("FLOW-" + UUID.randomUUID().toString().substring(0, 8), 10, 0);
        OrderCheckoutResponse checkout = createTestOrder(variant, 1);
        UUID orderId = checkout.getOrderId();

        // 1. Verify Payment -> PAGADO
        ConfirmPaymentRequest confirmRequest = ConfirmPaymentRequest.builder()
                .paymentMethod(PaymentMethod.EFECTIVO)
                .transferReference("EF-DIRECTO-01")
                .amount(checkout.getTotal())
                .build();

        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/confirm-payment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(confirmRequest)))
                .andExpect(status().isOk());

        // 2. PAGADO -> PREPARANDO
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/transition")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderStatusTransitionRequest(OrderStatus.PREPARANDO, "Empacando pedido"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PREPARANDO"));

        // 3. PREPARANDO -> DESPACHADO
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/transition")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderStatusTransitionRequest(OrderStatus.DESPACHADO, "Enviado con mensajero"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("DESPACHADO"));

        // 4. DESPACHADO -> ENTREGADO
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/transition")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderStatusTransitionRequest(OrderStatus.ENTREGADO, "Entregado a satisfacción"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("ENTREGADO"));
    }

    @Test
    @DisplayName("Invalid State Transition: PENDIENTE_CONTACTO directly to ENTREGADO throws 409 Conflict")
    void testInvalidTransition_throwsConflict() throws Exception {
        ProductVariant variant = createTestVariant("INV-" + UUID.randomUUID().toString().substring(0, 8), 10, 0);
        OrderCheckoutResponse checkout = createTestOrder(variant, 1);
        UUID orderId = checkout.getOrderId();

        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/transition")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new OrderStatusTransitionRequest(OrderStatus.ENTREGADO, "Intento inválido"))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("Transición de estado no permitida")));
    }

    @Test
    @DisplayName("Extend Reservation: Extends expiresAt on order and reservations")
    void testExtendReservation_success() throws Exception {
        ProductVariant variant = createTestVariant("EXT-" + UUID.randomUUID().toString().substring(0, 8), 10, 0);
        OrderCheckoutResponse checkout = createTestOrder(variant, 1);
        UUID orderId = checkout.getOrderId();

        Order orderBefore = orderRepository.findById(orderId).orElseThrow();
        Instant originalExpiresAt = orderBefore.getExpiresAt();

        ExtendReservationRequest extendReq = ExtendReservationRequest.builder()
                .additionalMinutes(30)
                .notes("Cliente solicitó 30 minutos extra para ir al banco")
                .build();

        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/extend-reservation")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(extendReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        Order orderAfter = orderRepository.findById(orderId).orElseThrow();
        assertThat(orderAfter.getExpiresAt()).isAfter(originalExpiresAt);

        List<StockReservation> reservations = stockReservationRepository.findByOrderId(orderId);
        assertThat(reservations).allMatch(r -> r.getExpiresAt().equals(orderAfter.getExpiresAt()));
    }

    @Test
    @DisplayName("Admin Order Queries: List and detail endpoints")
    void testAdminOrderListingAndDetail() throws Exception {
        ProductVariant variant = createTestVariant("LST-" + UUID.randomUUID().toString().substring(0, 8), 10, 0);
        OrderCheckoutResponse checkout = createTestOrder(variant, 1);

        // List orders
        mockMvc.perform(get("/api/v1/admin/orders")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());

        // Get single order detail
        mockMvc.perform(get("/api/v1/admin/orders/" + checkout.getOrderId())
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reference").value(checkout.getReference()))
                .andExpect(jsonPath("$.data.items").isArray())
                .andExpect(jsonPath("$.data.reservations").isArray())
                .andExpect(jsonPath("$.data.statusHistory").isArray());
    }
}
