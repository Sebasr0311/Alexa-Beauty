package com.alexabeauty.order;

import com.alexabeauty.catalog.domain.Brand;
import com.alexabeauty.catalog.domain.Category;
import com.alexabeauty.catalog.domain.Product;
import com.alexabeauty.catalog.domain.ProductVariant;
import com.alexabeauty.catalog.repository.BrandRepository;
import com.alexabeauty.catalog.repository.CategoryRepository;
import com.alexabeauty.catalog.repository.ProductRepository;
import com.alexabeauty.catalog.repository.ProductVariantRepository;
import com.alexabeauty.order.domain.Order;
import com.alexabeauty.order.domain.OrderStatus;
import com.alexabeauty.order.domain.OrderStatusHistory;
import com.alexabeauty.order.domain.ReservationStatus;
import com.alexabeauty.order.domain.StockReservation;
import com.alexabeauty.order.dto.CreateOrderRequest;
import com.alexabeauty.order.dto.OrderItemRequest;
import com.alexabeauty.order.repository.OrderRepository;
import com.alexabeauty.order.repository.OrderStatusHistoryRepository;
import com.alexabeauty.order.repository.StockReservationRepository;
import com.alexabeauty.order.service.ReservationExpirationJob;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.time.Duration;
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
class OrderCheckoutIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private ProductVariantRepository productVariantRepository;

    @Autowired
    private BrandRepository brandRepository;

    @Autowired
    private CategoryRepository categoryRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private StockReservationRepository stockReservationRepository;

    @Autowired
    private OrderStatusHistoryRepository orderStatusHistoryRepository;

    @Autowired
    private ReservationExpirationJob reservationExpirationJob;

    private Product createProduct(String name, String sku) {
        Brand brand = brandRepository.findAll().stream().findFirst().orElseThrow();
        Category category = categoryRepository.findAll().stream().findFirst().orElseThrow();
        Product product = Product.builder()
                .name(name)
                .sku(sku)
                .slug("prod-" + UUID.randomUUID())
                .brand(brand)
                .category(category)
                .active(true)
                .build();
        return productRepository.save(product);
    }

    private ProductVariant createVariant(Product product, String sku, int stockOnHand, int stockReserved, BigDecimal price) {
        ProductVariant variant = ProductVariant.builder()
                .product(product)
                .sku(sku)
                .shadeName("Tono 01")
                .salePrice(price)
                .minStock(2)
                .stockOnHand(stockOnHand)
                .stockReserved(stockReserved)
                .active(true)
                .build();
        return productVariantRepository.save(variant);
    }

    @Test
    @DisplayName("P11: Competing buyers for last stock: 1 unit available, 2 concurrent order requests -> first succeeds, second gets 409 Conflict, stock never goes negative")
    void checkout_P11_competingBuyersForLastStock_oneSucceedsOneFailsWith409() throws Exception {
        // Setup variant with stockOnHand = 1, stockReserved = 0 (exactly 1 unit available)
        Product product = createProduct("Labial Edición Limitada", "SKU-LIMIT-01");
        ProductVariant variant = createVariant(product, "VAR-LIMIT-01", 1, 0, new BigDecimal("35000.00"));

        CreateOrderRequest request1 = CreateOrderRequest.builder()
                .customerName("Comprador 1")
                .customerPhone("3001111111")
                .deliveryType("ENVIO_DOMICILIO")
                .city("Bogotá")
                .address("Calle 1 # 2-3")
                .items(List.of(new OrderItemRequest(variant.getId(), 1)))
                .build();

        CreateOrderRequest request2 = CreateOrderRequest.builder()
                .customerName("Comprador 2")
                .customerPhone("3002222222")
                .deliveryType("RETIRO_TIENDA")
                .items(List.of(new OrderItemRequest(variant.getId(), 1)))
                .build();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger conflictCount = new AtomicInteger(0);
        List<String> conflictMessages = Collections.synchronizedList(new ArrayList<>());

        Future<?> f1 = executor.submit(() -> {
            try {
                startLatch.await();
                MvcResult res = mockMvc.perform(post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request1)))
                        .andReturn();
                int httpStatus = res.getResponse().getStatus();
                if (httpStatus == 201) {
                    successCount.incrementAndGet();
                } else if (httpStatus == 409) {
                    conflictCount.incrementAndGet();
                    conflictMessages.add(res.getResponse().getContentAsString());
                }
            } catch (Exception e) {
                // ignore
            }
        });

        Future<?> f2 = executor.submit(() -> {
            try {
                startLatch.await();
                MvcResult res = mockMvc.perform(post("/api/v1/orders")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(objectMapper.writeValueAsString(request2)))
                        .andReturn();
                int httpStatus = res.getResponse().getStatus();
                if (httpStatus == 201) {
                    successCount.incrementAndGet();
                } else if (httpStatus == 409) {
                    conflictCount.incrementAndGet();
                    conflictMessages.add(res.getResponse().getContentAsString());
                }
            } catch (Exception e) {
                // ignore
            }
        });

        // Trigger concurrent execution
        startLatch.countDown();
        f1.get(10, TimeUnit.SECONDS);
        f2.get(10, TimeUnit.SECONDS);
        executor.shutdown();

        // Exactly one succeeded (201) and one conflicted (409)
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(conflictCount.get()).isEqualTo(1);
        assertThat(conflictMessages.get(0)).contains("Stock insuficiente para el producto");

        // Stock invariant: stock_on_hand must still be 1, stock_reserved must be 1, available = 0 (never negative!)
        ProductVariant refreshed = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(refreshed.getStockOnHand()).isEqualTo(1);
        assertThat(refreshed.getStockReserved()).isEqualTo(1);
        assertThat(refreshed.getStockOnHand() - refreshed.getStockReserved()).isEqualTo(0);
    }

    @Test
    @DisplayName("P12: Valid checkout creates order with 45m expiration and wa.me URL")
    void checkout_P12_validCheckout_createsOrderWith45mExpirationAndWaMeUrl() throws Exception {
        Product product = createProduct("Rubor Mineral Glow", "SKU-RUBOR-01");
        ProductVariant variant = createVariant(product, "VAR-RUBOR-01", 10, 0, new BigDecimal("25000.00"));

        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerName("Camila Morales")
                .customerPhone("3001234567")
                .deliveryType("ENVIO_DOMICILIO")
                .city("Medellín")
                .address("Carrera 43A # 1-50")
                .customerNotes("Por favor enviar después de las 2pm")
                .items(List.of(new OrderItemRequest(variant.getId(), 2)))
                .build();

        Instant beforeCheckout = Instant.now();

        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reference").value(org.hamcrest.Matchers.matchesPattern("^AB-\\d{4}-\\d{6}$")))
                .andExpect(jsonPath("$.data.status").value("PENDIENTE_CONTACTO"))
                .andExpect(jsonPath("$.data.subtotal").value(50000.00))
                .andExpect(jsonPath("$.data.total").value(50000.00))
                .andExpect(jsonPath("$.data.trackingToken").isNotEmpty())
                .andExpect(jsonPath("$.data.whatsappUrl").value(org.hamcrest.Matchers.startsWith("https://wa.me/")))
                .andExpect(jsonPath("$.data.whatsappMessageText").value(org.hamcrest.Matchers.containsString("Hola, Alexa Beauty 🌷")))
                .andExpect(jsonPath("$.data.whatsappMessageText").value(org.hamcrest.Matchers.containsString("Camila Morales")))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        String reference = json.path("reference").asText();
        String trackingToken = json.path("trackingToken").asText();
        Instant expiresAt = Instant.parse(json.path("expiresAt").asText());

        // Verify ~45m expiration
        long diffMinutes = Duration.between(beforeCheckout, expiresAt).toMinutes();
        assertThat(diffMinutes).isBetween(44L, 46L);

        // Verify stock reserved
        ProductVariant refreshed = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(refreshed.getStockReserved()).isEqualTo(2);

        // Verify public tracking endpoint
        mockMvc.perform(get("/api/v1/orders/track")
                        .param("reference", reference)
                        .param("token", trackingToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.reference").value(reference))
                .andExpect(jsonPath("$.data.status").value("PENDIENTE_CONTACTO"))
                .andExpect(jsonPath("$.data.total").value(50000.00))
                .andExpect(jsonPath("$.data.itemCount").value(2));
    }

    @Test
    @DisplayName("P13: Duplicate checkout with same Idempotency-Key returns existing order without creating second reservation")
    void checkout_P13_duplicateCheckoutWithIdempotencyKey_returnsExistingOrderWithoutSecondReservation() throws Exception {
        Product product = createProduct("Base Líquida HD", "SKU-BASE-01");
        ProductVariant variant = createVariant(product, "VAR-BASE-01", 5, 0, new BigDecimal("45000.00"));

        String idempotencyKey = "IDEM-ORDER-" + UUID.randomUUID();

        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerName("Ana Sofía")
                .customerPhone("3109876543")
                .deliveryType("RETIRO_TIENDA")
                .items(List.of(new OrderItemRequest(variant.getId(), 2)))
                .build();

        // 1st request with Idempotency-Key
        MvcResult firstResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode firstData = objectMapper.readTree(firstResult.getResponse().getContentAsString()).path("data");
        String firstReference = firstData.path("reference").asText();
        String firstOrderId = firstData.path("orderId").asText();

        // Stock reserved should be 2
        ProductVariant afterFirst = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(afterFirst.getStockReserved()).isEqualTo(2);

        // 2nd request with IDENTICAL Idempotency-Key
        MvcResult secondResult = mockMvc.perform(post("/api/v1/orders")
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode secondData = objectMapper.readTree(secondResult.getResponse().getContentAsString()).path("data");
        String secondReference = secondData.path("reference").asText();
        String secondOrderId = secondData.path("orderId").asText();

        // Must return identical order reference and ID
        assertThat(secondReference).isEqualTo(firstReference);
        assertThat(secondOrderId).isEqualTo(firstOrderId);

        // Stock reserved MUST STILL BE 2 (NOT 4!)
        ProductVariant afterSecond = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(afterSecond.getStockReserved()).isEqualTo(2);

        // Total reservations for order must be exactly 1
        List<StockReservation> reservations = stockReservationRepository.findByOrderId(UUID.fromString(firstOrderId));
        assertThat(reservations).hasSize(1);
    }

    @Test
    @DisplayName("P14: Reservation expiration releases stock_reserved and leaves stock_on_hand intact")
    void expiration_P14_reservationExpiration_releasesStockReservedAndLeavesStockOnHandIntact() throws Exception {
        Product product = createProduct("Paleta Glow 9 Colores", "SKU-PALETA-01");
        ProductVariant variant = createVariant(product, "VAR-PALETA-01", 10, 0, new BigDecimal("60000.00"));

        CreateOrderRequest request = CreateOrderRequest.builder()
                .customerName("Laura Gómez")
                .customerPhone("3155555555")
                .deliveryType("ENVIO_DOMICILIO")
                .city("Cali")
                .address("Avenida 6N # 20-30")
                .items(List.of(new OrderItemRequest(variant.getId(), 3)))
                .build();

        MvcResult result = mockMvc.perform(post("/api/v1/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString()).path("data");
        UUID orderId = UUID.fromString(json.path("orderId").asText());

        // Stock reserved is now 3, on_hand is 10
        ProductVariant variantAfterOrder = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(variantAfterOrder.getStockReserved()).isEqualTo(3);
        assertThat(variantAfterOrder.getStockOnHand()).isEqualTo(10);

        // Artificially expire the order in database (set expires_at to 5 minutes in the past)
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setExpiresAt(Instant.now().minus(Duration.ofMinutes(5)));
        orderRepository.saveAndFlush(order);

        // Trigger reservation expiration job
        int expiredCount = reservationExpirationJob.expireReservations();
        assertThat(expiredCount).isGreaterThanOrEqualTo(1);

        // Verify order transitioned to EXPIRADO
        Order expiredOrder = orderRepository.findById(orderId).orElseThrow();
        assertThat(expiredOrder.getStatus()).isEqualTo(OrderStatus.EXPIRADO);

        // Verify stock reservations transitioned to RELEASED
        List<StockReservation> reservations = stockReservationRepository.findByOrderId(orderId);
        assertThat(reservations).isNotEmpty();
        for (StockReservation res : reservations) {
            assertThat(res.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        }

        // Verify OrderStatusHistory record
        List<OrderStatusHistory> historyList = orderStatusHistoryRepository.findByOrderIdOrderByCreatedAtDesc(orderId);
        assertThat(historyList).isNotEmpty();
        OrderStatusHistory latestHistory = historyList.get(0);
        assertThat(latestHistory.getNewStatus()).isEqualTo(OrderStatus.EXPIRADO);
        assertThat(latestHistory.getNotes()).contains("Reservation expired");

        // Crucial P14 invariant: stock_reserved is released back to 0, and stock_on_hand is UNTOUCHED (10)!
        ProductVariant variantAfterExpiration = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(variantAfterExpiration.getStockReserved()).isEqualTo(0);
        assertThat(variantAfterExpiration.getStockOnHand()).isEqualTo(10);
    }
}
