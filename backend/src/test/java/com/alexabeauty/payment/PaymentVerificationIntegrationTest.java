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

    @Autowired
    private org.springframework.transaction.PlatformTransactionManager transactionManager;

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
    void rejectsPartialManualPaymentWithoutConsumingStock() throws Exception {
        ProductVariant variant = createTestVariant("PARTIAL-" + UUID.randomUUID(), 10, 0);
        OrderCheckoutResponse checkout = createTestOrder(variant, 2);
        assertRejectedPayment(checkout, new BigDecimal("1.00"));
        assertThat(productVariantRepository.findById(variant.getId()).orElseThrow().getStockOnHand()).isEqualTo(10);
        assertThat(paymentRepository.findByOrderId(checkout.getOrderId())).isEmpty();
    }

    @Test
    void expirationDoesNotReleaseFutureReservation() {
        ProductVariant variant = createTestVariant("NOT-DUE-" + UUID.randomUUID(), 10, 0);
        var checkout = createTestOrder(variant, 2);
        orderCheckoutService.expireOrder(checkout.getOrderId());
        assertThat(orderRepository.findById(checkout.getOrderId()).orElseThrow().getStatus()).isNotEqualTo(OrderStatus.EXPIRADO);
        assertThat(productVariantRepository.findById(variant.getId()).orElseThrow().getStockReserved()).isEqualTo(2);
    }

    @Test
    void concurrentExpirationReleasesOnlyItsOwnActiveReservation() throws Exception {
        ProductVariant variant = createTestVariant("EXPIRE-RACE-" + UUID.randomUUID().toString().substring(0, 8), 10, 0);
        var expired = createTestOrder(variant, 2);
        var other = createTestOrder(variant, 2);
        Order due = orderRepository.findById(expired.getOrderId()).orElseThrow();
        due.setExpiresAt(Instant.now().minusSeconds(60)); orderRepository.save(due);
        CountDownLatch variantLocked = new CountDownLatch(1), releaseVariant = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(3);
        try {
            Future<?> holder = pool.submit(() -> new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                productVariantRepository.findByIdInOrderByIdForUpdate(List.of(variant.getId()));
                variantLocked.countDown();
                try { if (!releaseVariant.await(5, TimeUnit.SECONDS)) throw new AssertionError("release timeout"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new RuntimeException(e); }
            }));
            assertThat(variantLocked.await(5, TimeUnit.SECONDS)).isTrue();
            CountDownLatch started = new CountDownLatch(2);
            Future<?> first = pool.submit(() -> { started.countDown(); orderCheckoutService.expireOrder(expired.getOrderId()); });
            Future<?> second = pool.submit(() -> { started.countDown(); orderCheckoutService.expireOrder(expired.getOrderId()); });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(500); releaseVariant.countDown();
            holder.get(10, TimeUnit.SECONDS); first.get(10, TimeUnit.SECONDS); second.get(10, TimeUnit.SECONDS);
            assertThat(productVariantRepository.findById(variant.getId()).orElseThrow().getStockReserved()).isEqualTo(2);
            assertThat(orderRepository.findById(other.getOrderId()).orElseThrow().getStatus()).isNotEqualTo(OrderStatus.EXPIRADO);
            assertThat(stockReservationRepository.findByOrderId(other.getOrderId())).allMatch(r -> r.getStatus() == ReservationStatus.ACTIVE);
        } finally { releaseVariant.countDown(); pool.shutdownNow(); }
    }

    @Test
    void expirationWaitingForPaymentCommitCannotOverwritePaidOrder() throws Exception {
        ProductVariant variant = createTestVariant("PAY-WINS-" + UUID.randomUUID().toString().substring(0, 8), 10, 0);
        var checkout = createTestOrder(variant, 2);
        var payment = ConfirmPaymentRequest.builder().paymentMethod(PaymentMethod.NEQUI)
                .transferReference("RACE-PAYMENT").amount(checkout.getTotal()).build();
        Instant deadline = Instant.now().plusSeconds(2);
        Order boundaryOrder = orderRepository.findById(checkout.getOrderId()).orElseThrow();
        boundaryOrder.setExpiresAt(deadline); orderRepository.saveAndFlush(boundaryOrder);
        CountDownLatch paymentRecorded = new CountDownLatch(1), commitPayment = new CountDownLatch(1);
        ExecutorService pool = Executors.newFixedThreadPool(2);
        try {
            Future<?> paying = pool.submit(() -> new org.springframework.transaction.support.TransactionTemplate(transactionManager).executeWithoutResult(tx -> {
                try {
                    mockMvc.perform(post("/api/v1/admin/orders/" + checkout.getOrderId() + "/confirm-payment")
                            .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(payment))).andExpect(status().isOk());
                    paymentRecorded.countDown();
                    if (!commitPayment.await(5, TimeUnit.SECONDS)) throw new AssertionError("commit timeout");
                } catch (Exception e) { throw new RuntimeException(e); }
            }));
            assertThat(paymentRecorded.await(5, TimeUnit.SECONDS)).isTrue();
            while (Instant.now().isBefore(deadline)) Thread.sleep(25);
            assertThat(Instant.now()).isAfterOrEqualTo(deadline);
            CountDownLatch started = new CountDownLatch(1);
            Future<?> expiring = pool.submit(() -> { started.countDown(); orderCheckoutService.expireOrder(checkout.getOrderId()); });
            assertThat(started.await(5, TimeUnit.SECONDS)).isTrue();
            Thread.sleep(500); commitPayment.countDown();
            paying.get(10, TimeUnit.SECONDS); expiring.get(10, TimeUnit.SECONDS);
            assertThat(orderRepository.findById(checkout.getOrderId()).orElseThrow().getStatus()).isEqualTo(OrderStatus.PAGADO);
            assertThat(productVariantRepository.findById(variant.getId()).orElseThrow().getStockOnHand()).isEqualTo(8);
            assertThat(productVariantRepository.findById(variant.getId()).orElseThrow().getStockReserved()).isZero();
            assertThat(stockReservationRepository.findByOrderId(checkout.getOrderId())).allMatch(r -> r.getStatus() == ReservationStatus.CONSUMED);
        } finally { commitPayment.countDown(); pool.shutdownNow(); }
    }

    @Test
    void publicCheckoutReplayManualPaymentAndExcelReportUseRealInventory() throws Exception {
        ProductVariant variant = createTestVariant("FLOW-" + UUID.randomUUID(), 5, 0);
        String slug = productRepository.findById(variant.getProduct().getId()).orElseThrow().getSlug();
        mockMvc.perform(get("/api/v1/catalog/products/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variants[0].id").value(variant.getId().toString()));
        CreateOrderRequest request = CreateOrderRequest.builder().customerName("Integration Customer")
                .customerPhone("3001234567").deliveryType("RETIRO_TIENDA")
                .items(List.of(OrderItemRequest.builder().variantId(variant.getId()).quantity(2).build())).build();
        String key = UUID.randomUUID().toString();
        MvcResult created = mockMvc.perform(post("/api/v1/orders").header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andReturn();
        var data = objectMapper.readTree(created.getResponse().getContentAsString()).path("data");
        String orderId = data.path("orderId").asText();
        mockMvc.perform(post("/api/v1/orders").header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.data.orderId").value(orderId));
        assertThat(productVariantRepository.findById(variant.getId()).orElseThrow().getStockReserved()).isEqualTo(2);
        request.setCustomerName("Different Customer");
        mockMvc.perform(post("/api/v1/orders").header("Idempotency-Key", key)
                .contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
        var payment = ConfirmPaymentRequest.builder().paymentMethod(PaymentMethod.EFECTIVO)
                .transferReference("MANUAL-FLOW-" + key).amount(new BigDecimal("100000.00")).build();
        mockMvc.perform(post("/api/v1/admin/orders/" + orderId + "/confirm-payment")
                .header("Authorization", "Bearer " + adminToken).contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(payment))).andExpect(status().isOk());
        assertThat(productVariantRepository.findById(variant.getId()).orElseThrow().getStockOnHand()).isEqualTo(3);
        mockMvc.perform(get("/api/v1/orders/track").param("reference", data.path("reference").asText())
                .param("token", data.path("trackingToken").asText()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.data.status").value("PAGADO"));
        String date = java.time.LocalDate.now(java.time.ZoneId.of("America/Bogota")).toString();
        var report = mockMvc.perform(get("/api/v1/admin/sales/export.xlsx")
                .header("Authorization", "Bearer " + adminToken).param("from", date).param("to", date))
                .andExpect(status().isOk()).andReturn();
        try (var workbook = new org.apache.poi.xssf.usermodel.XSSFWorkbook(
                new java.io.ByteArrayInputStream(report.getResponse().getContentAsByteArray()))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(5);
            boolean found = false;
            for (var sheet : workbook) for (var row : sheet) for (var cell : row) {
                if (cell.toString().contains(data.path("reference").asText())) found = true;
            }
            assertThat(found).isTrue();
        }
    }

    @Test
    void rejectsElapsedReservationBeforeExpirationWorkerRuns() throws Exception {
        ProductVariant variant = createTestVariant("ELAPSED-" + UUID.randomUUID(), 10, 0);
        OrderCheckoutResponse checkout = createTestOrder(variant, 2);
        Order order = orderRepository.findById(checkout.getOrderId()).orElseThrow();
        order.setExpiresAt(Instant.now().minusSeconds(60));
        orderRepository.save(order);
        assertRejectedPayment(checkout, checkout.getTotal());
        assertThat(paymentRepository.findByOrderId(checkout.getOrderId())).isEmpty();
    }

    @Test
    void rejectsMissingReservedStockInsteadOfClampingIt() throws Exception {
        ProductVariant variant = createTestVariant("INV-" + UUID.randomUUID(), 10, 0);
        OrderCheckoutResponse checkout = createTestOrder(variant, 2);
        ProductVariant stored = productVariantRepository.findById(variant.getId()).orElseThrow();
        stored.setStockReserved(0);
        productVariantRepository.save(stored);
        assertRejectedPayment(checkout, checkout.getTotal());
        assertThat(productVariantRepository.findById(variant.getId()).orElseThrow().getStockOnHand()).isEqualTo(10);
    }

    private void assertRejectedPayment(OrderCheckoutResponse checkout, BigDecimal amount) throws Exception {
        ConfirmPaymentRequest request = ConfirmPaymentRequest.builder()
                .paymentMethod(PaymentMethod.NEQUI).transferReference("REGRESSION")
                .amount(amount).build();
        mockMvc.perform(post("/api/v1/admin/orders/" + checkout.getOrderId() + "/confirm-payment")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isConflict());
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
        Order due = orderRepository.findById(orderId).orElseThrow();
        due.setExpiresAt(Instant.now().minusSeconds(60));
        orderRepository.saveAndFlush(due);
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
