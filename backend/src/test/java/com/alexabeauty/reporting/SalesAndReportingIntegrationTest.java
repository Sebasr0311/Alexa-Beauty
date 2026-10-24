package com.alexabeauty.reporting;

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
import com.alexabeauty.inventory.domain.MovementType;
import com.alexabeauty.inventory.repository.InventoryMovementRepository;
import com.alexabeauty.order.domain.Order;
import com.alexabeauty.order.domain.OrderItem;
import com.alexabeauty.order.domain.OrderStatus;
import com.alexabeauty.order.repository.OrderRepository;
import com.alexabeauty.payment.domain.Payment;
import com.alexabeauty.payment.domain.PaymentMethod;
import com.alexabeauty.payment.domain.PaymentStatus;
import com.alexabeauty.payment.repository.PaymentRepository;
import com.alexabeauty.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SalesAndReportingIntegrationTest {

    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private UserRepository userRepository;

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
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private User adminUser;
    private Brand testBrand;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        adminUser = userRepository.findByEmail("admin@alexabeauty.com").orElseThrow();
        adminToken = jwtTokenProvider.generateToken(adminUser);

        testBrand = brandRepository.findAll().stream().findFirst().orElseThrow();
        testCategory = categoryRepository.findAll().stream().findFirst().orElseThrow();
    }

    private ProductVariant createTestVariant(String sku, String name, BigDecimal price, BigDecimal cost) {
        Product product = Product.builder()
                .name(name)
                .sku("PROD-" + sku)
                .slug("prod-slug-" + UUID.randomUUID())
                .brand(testBrand)
                .category(testCategory)
                .active(true)
                .build();
        Product savedProduct = productRepository.save(product);

        ProductVariant variant = ProductVariant.builder()
                .product(savedProduct)
                .sku(sku)
                .shadeName("Tono " + sku)
                .salePrice(price)
                .referenceCost(cost)
                .stockOnHand(50)
                .stockReserved(0)
                .minStock(3)
                .active(true)
                .build();
        ProductVariant savedVariant = productVariantRepository.save(variant);
        savedVariant.setProduct(savedProduct);
        return savedVariant;
    }

    private Order createPaidOrder(
            String reference,
            ProductVariant variant,
            String productName,
            int quantity,
            BigDecimal unitPrice,
            Instant paidAt,
            PaymentMethod method,
            OrderStatus status
    ) {
        BigDecimal total = unitPrice.multiply(BigDecimal.valueOf(quantity));

        Order order = Order.builder()
                .reference(reference)
                .customerName("Cliente " + reference)
                .customerPhone("300" + (1000000 + (int)(Math.random() * 8999999)))
                .deliveryType("LOCAL_PICKUP")
                .subtotal(total)
                .discount(BigDecimal.ZERO)
                .shippingFee(BigDecimal.ZERO)
                .total(total)
                .status(status)
                .channel("WHATSAPP")
                .expiresAt(Instant.now().plusSeconds(7200))
                .paidAt(paidAt)
                .trackingTokenHash(UUID.randomUUID().toString())
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .variant(variant)
                .sku(variant.getSku())
                .productName(productName)
                .variantName(variant.getShadeName())
                .unitPrice(unitPrice)
                .quantity(quantity)
                .subtotal(total)
                .costSnapshot(variant.getReferenceCost())
                .build();
        order.getItems().add(item);

        Order savedOrder = orderRepository.save(order);

        if (paidAt != null && method != null) {
            Payment payment = Payment.builder()
                    .order(savedOrder)
                    .paymentMethod(method)
                    .transferReference("TR-" + reference)
                    .amount(total)
                    .status(PaymentStatus.VERIFICADO)
                    .verifiedBy(adminUser)
                    .verifiedAt(paidAt)
                    .build();
            paymentRepository.save(payment);
            savedOrder.getPayments().add(payment);
        }

        return savedOrder;
    }


    @Test
    @DisplayName("P19 Matrix: Strict America/Bogota boundary test between 2026-10-10 23:59 and 2026-10-11 00:01")
    void testP19_boundaryTimezoneFiltering() throws Exception {
        ProductVariant variant = createTestVariant("SKU-P19-" + UUID.randomUUID().toString().substring(0, 6),
                "Base Alta Cobertura P19", new BigDecimal("50000.00"), new BigDecimal("25000.00"));

        // Order 1 paid at 2026-10-10 23:59:00 Bogota time (UTC: 2026-10-11T04:59:00Z)
        Instant paidAtDay1 = LocalDateTime.of(2026, 10, 10, 23, 59, 0)
                .atZone(BOGOTA_ZONE).toInstant();
        Order order1 = createPaidOrder("REF-P19-DAY1-" + UUID.randomUUID().toString().substring(0, 6),
                variant, "Base Alta Cobertura P19", 1, new BigDecimal("50000.00"), paidAtDay1, PaymentMethod.NEQUI, OrderStatus.PAGADO);

        // Order 2 paid at 2026-10-11 00:01:00 Bogota time (UTC: 2026-10-11T05:01:00Z)
        Instant paidAtDay2 = LocalDateTime.of(2026, 10, 11, 0, 1, 0)
                .atZone(BOGOTA_ZONE).toInstant();
        Order order2 = createPaidOrder("REF-P19-DAY2-" + UUID.randomUUID().toString().substring(0, 6),
                variant, "Base Alta Cobertura P19", 2, new BigDecimal("50000.00"), paidAtDay2, PaymentMethod.BANCOLOMBIA, OrderStatus.PAGADO);

        // 1. Query strictly for 2026-10-10: MUST return ONLY Order 1
        MvcResult resultDay1 = mockMvc.perform(get("/api/v1/admin/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", "2026-10-10")
                        .param("to", "2026-10-10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.paidOrdersCount").value(1))
                .andExpect(jsonPath("$.data.summary.totalSales").value(50000.00))
                .andReturn();

        JsonNode jsonDay1 = objectMapper.readTree(resultDay1.getResponse().getContentAsString());
        List<String> refsDay1 = jsonDay1.path("data").path("orders").findValuesAsText("reference");
        assertThat(refsDay1).contains(order1.getReference());
        assertThat(refsDay1).doesNotContain(order2.getReference());

        // 2. Query strictly for 2026-10-11: MUST return ONLY Order 2
        MvcResult resultDay2 = mockMvc.perform(get("/api/v1/admin/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", "2026-10-11")
                        .param("to", "2026-10-11"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.paidOrdersCount").value(1))
                .andExpect(jsonPath("$.data.summary.totalSales").value(100000.00))
                .andReturn();

        JsonNode jsonDay2 = objectMapper.readTree(resultDay2.getResponse().getContentAsString());
        List<String> refsDay2 = jsonDay2.path("data").path("orders").findValuesAsText("reference");
        assertThat(refsDay2).contains(order2.getReference());
        assertThat(refsDay2).doesNotContain(order1.getReference());
    }

    @Test
    @DisplayName("Test Unpaid/Pending/Canceled/Expired orders are 100% excluded from sales and analytics")
    void testUnpaidAndNonVerifiedOrders_areExcluded() throws Exception {
        ProductVariant variant = createTestVariant("SKU-EXCL-" + UUID.randomUUID().toString().substring(0, 6),
                "Labial Exclusión", new BigDecimal("30000.00"), new BigDecimal("15000.00"));

        Instant testInstant = LocalDateTime.of(2026, 10, 15, 12, 0, 0)
                .atZone(BOGOTA_ZONE).toInstant();

        // 1. Pending payment order
        Order pendingOrder = createPaidOrder("REF-PENDING-" + UUID.randomUUID().toString().substring(0, 6),
                variant, "Labial Exclusión", 1, new BigDecimal("30000.00"), null, null, OrderStatus.PENDIENTE_PAGO);

        // 2. Expired order
        Order expiredOrder = createPaidOrder("REF-EXPIRED-" + UUID.randomUUID().toString().substring(0, 6),
                variant, "Labial Exclusión", 1, new BigDecimal("30000.00"), null, null, OrderStatus.EXPIRADO);

        // 3. Canceled order
        Order canceledOrder = createPaidOrder("REF-CANCEL-" + UUID.randomUUID().toString().substring(0, 6),
                variant, "Labial Exclusión", 1, new BigDecimal("30000.00"), testInstant, PaymentMethod.NEQUI, OrderStatus.CANCELADO);

        // 4. Paid order (should be included)
        Order paidOrder = createPaidOrder("REF-INCLUDED-" + UUID.randomUUID().toString().substring(0, 6),
                variant, "Labial Exclusión", 1, new BigDecimal("30000.00"), testInstant, PaymentMethod.NEQUI, OrderStatus.PAGADO);

        MvcResult result = mockMvc.perform(get("/api/v1/admin/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", "2026-10-15")
                        .param("to", "2026-10-15"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode json = objectMapper.readTree(result.getResponse().getContentAsString());
        List<String> references = json.path("data").path("orders").findValuesAsText("reference");

        assertThat(references).contains(paidOrder.getReference());
        assertThat(references).doesNotContain(pendingOrder.getReference());
        assertThat(references).doesNotContain(expiredOrder.getReference());
        assertThat(references).doesNotContain(canceledOrder.getReference());
    }

    @Test
    @DisplayName("P20: Excel Export matches 100% with API data across all 5 sheets")
    void testP20_excelExport_matchesApiDataExactly() throws Exception {
        String testSuffix = UUID.randomUUID().toString().substring(0, 6);
        ProductVariant varA = createTestVariant("SKU-P20-A-" + testSuffix, "Rubor Compacto P20",
                new BigDecimal("24000.00"), new BigDecimal("12000.00"));
        ProductVariant varB = createTestVariant("SKU-P20-B-" + testSuffix, "Iluminador Glow P20",
                new BigDecimal("35000.00"), new BigDecimal("18000.00"));

        Instant paidTime1 = LocalDateTime.of(2026, 10, 20, 10, 30, 0).atZone(BOGOTA_ZONE).toInstant();
        Instant paidTime2 = LocalDateTime.of(2026, 10, 20, 15, 45, 0).atZone(BOGOTA_ZONE).toInstant();

        // Order 1: 2 units varA (48000)
        Order order1 = createPaidOrder("REF-P20-1-" + testSuffix, varA, "Rubor Compacto P20", 2, new BigDecimal("24000.00"),
                paidTime1, PaymentMethod.NEQUI, OrderStatus.PAGADO);

        // Order 2: 1 unit varB (35000)
        Order order2 = createPaidOrder("REF-P20-2-" + testSuffix, varB, "Iluminador Glow P20", 1, new BigDecimal("35000.00"),
                paidTime2, PaymentMethod.DAVIPLATA, OrderStatus.ENTREGADO);

        // 1. Fetch JSON API
        MvcResult apiResult = mockMvc.perform(get("/api/v1/admin/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", "2026-10-20")
                        .param("to", "2026-10-20"))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode apiData = objectMapper.readTree(apiResult.getResponse().getContentAsString()).path("data");
        double expectedTotalSales = apiData.path("summary").path("totalSales").asDouble();
        long expectedPaidOrders = apiData.path("summary").path("paidOrdersCount").asLong();
        long expectedUnitsSold = apiData.path("summary").path("totalUnitsSold").asLong();

        assertThat(expectedTotalSales).isEqualTo(83000.00);
        assertThat(expectedPaidOrders).isEqualTo(2);
        assertThat(expectedUnitsSold).isEqualTo(3);

        // 2. Fetch Excel Export
        MvcResult excelResult = mockMvc.perform(get("/api/v1/admin/sales/export.xlsx")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", "2026-10-20")
                        .param("to", "2026-10-20"))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"AlexaBeauty_Reporte_Ventas_2026-10-20_2026-10-20.xlsx\""))
                .andReturn();

        byte[] bytes = excelResult.getResponse().getContentAsByteArray();
        assertThat(bytes).isNotEmpty();

        // 3. Inspect with Apache POI
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(bytes))) {
            // Confirm all 5 sheets exist
            assertThat(workbook.getSheet("VENTAS")).isNotNull();
            assertThat(workbook.getSheet("DETALLE_PRODUCTOS")).isNotNull();
            assertThat(workbook.getSheet("TOP_PRODUCTOS")).isNotNull();
            assertThat(workbook.getSheet("RESUMEN")).isNotNull();
            assertThat(workbook.getSheet("METADATOS")).isNotNull();

            // Validate Sheet RESUMEN
            XSSFSheet resumen = workbook.getSheet("RESUMEN");
            assertThat(resumen.getRow(4).getCell(0).getStringCellValue()).isEqualTo("INDICADOR");
            assertThat(resumen.getRow(4).getCell(1).getStringCellValue()).isEqualTo("RESULTADO");

            // Ventas pagadas (COP)
            assertThat(resumen.getRow(5).getCell(0).getStringCellValue()).isEqualTo("Ventas pagadas (COP)");
            assertThat(resumen.getRow(5).getCell(1).getNumericCellValue()).isEqualTo(expectedTotalSales);

            // Pedidos pagados
            assertThat(resumen.getRow(6).getCell(0).getStringCellValue()).isEqualTo("Pedidos pagados");
            assertThat((long) resumen.getRow(6).getCell(1).getNumericCellValue()).isEqualTo(expectedPaidOrders);

            // Unidades vendidas
            assertThat(resumen.getRow(7).getCell(0).getStringCellValue()).isEqualTo("Unidades vendidas");
            assertThat((long) resumen.getRow(7).getCell(1).getNumericCellValue()).isEqualTo(expectedUnitsSold);

            // Validate Sheet METADATOS
            XSSFSheet metadatos = workbook.getSheet("METADATOS");
            assertThat(metadatos.getRow(5).getCell(0).getStringCellValue()).isEqualTo("Versión");
            assertThat(metadatos.getRow(5).getCell(1).getStringCellValue()).isEqualTo("ALEXA_SALES_V1");
            assertThat(metadatos.getRow(6).getCell(0).getStringCellValue()).isEqualTo("Moneda");
            assertThat(metadatos.getRow(6).getCell(1).getStringCellValue()).isEqualTo("COP");
            assertThat(metadatos.getRow(7).getCell(0).getStringCellValue()).isEqualTo("Zona horaria");
            assertThat(metadatos.getRow(7).getCell(1).getStringCellValue()).isEqualTo("America/Bogota");

            // Validate Sheet VENTAS Total row
            XSSFSheet ventas = workbook.getSheet("VENTAS");
            assertThat(ventas.getRow(0).getCell(0).getStringCellValue()).contains("ALEXA BEAUTY");
            assertThat(ventas.getRow(2).getCell(0).getStringCellValue()).isEqualTo("REPORTE OFICIAL DE VENTAS CONFIRMADAS");
            assertThat(ventas.getRow(4).getCell(0).getStringCellValue()).isEqualTo("Fecha de pago");
            assertThat(ventas.getRow(4).getCell(7).getStringCellValue()).isEqualTo("Total pagado COP");

            // Total row formula =SUM(H6:H7)
            assertThat(ventas.getRow(8).getCell(7).getCellFormula()).isEqualTo("SUM(H6:H7)");
        }
    }

    @Test
    @DisplayName("Analytics overview and Top Products ranking calculate gross revenue and shares correctly")
    void testAnalytics_overviewAndTopProducts() throws Exception {
        String testSuffix = UUID.randomUUID().toString().substring(0, 6);
        ProductVariant top1 = createTestVariant("SKU-TOP-1-" + testSuffix, "Producto Top 1",
                new BigDecimal("60000.00"), new BigDecimal("30000.00"));
        ProductVariant top2 = createTestVariant("SKU-TOP-2-" + testSuffix, "Producto Top 2",
                new BigDecimal("40000.00"), new BigDecimal("20000.00"));

        Instant testTime = LocalDateTime.of(2026, 10, 25, 14, 0, 0).atZone(BOGOTA_ZONE).toInstant();

        // Order 1: 2 units of top1 -> 120000.00
        createPaidOrder("REF-TOP-1-" + testSuffix, top1, "Producto Top 1", 2, new BigDecimal("60000.00"),
                testTime, PaymentMethod.NEQUI, OrderStatus.PAGADO);

        // Order 2: 1 unit of top2 -> 40000.00
        createPaidOrder("REF-TOP-2-" + testSuffix, top2, "Producto Top 2", 1, new BigDecimal("40000.00"),
                testTime, PaymentMethod.BANCOLOMBIA, OrderStatus.PAGADO);

        // Call /api/v1/admin/analytics/overview
        MvcResult overviewResult = mockMvc.perform(get("/api/v1/admin/analytics/overview")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", "2026-10-25")
                        .param("to", "2026-10-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.totalSales").value(160000.00))
                .andExpect(jsonPath("$.data.summary.paidOrdersCount").value(2))
                .andExpect(jsonPath("$.data.summary.totalUnitsSold").value(3))
                .andExpect(jsonPath("$.data.topProducts[0].sku").value(top1.getSku()))
                .andExpect(jsonPath("$.data.topProducts[0].rank").value(1))
                .andExpect(jsonPath("$.data.topProducts[0].grossRevenue").value(120000.00))
                .andExpect(jsonPath("$.data.topProducts[0].revenueSharePercentage").value(75.00))
                .andExpect(jsonPath("$.data.topProducts[1].sku").value(top2.getSku()))
                .andExpect(jsonPath("$.data.topProducts[1].rank").value(2))
                .andExpect(jsonPath("$.data.topProducts[1].grossRevenue").value(40000.00))
                .andExpect(jsonPath("$.data.topProducts[1].revenueSharePercentage").value(25.00))
                .andReturn();

        // Call /api/v1/admin/analytics/top-products
        mockMvc.perform(get("/api/v1/admin/analytics/top-products")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", "2026-10-25")
                        .param("to", "2026-10-25"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].sku").value(top1.getSku()))
                .andExpect(jsonPath("$.data[0].grossRevenue").value(120000.00))
                .andExpect(jsonPath("$.data[1].sku").value(top2.getSku()))
                .andExpect(jsonPath("$.data[1].grossRevenue").value(40000.00));
    }

    @Test
    @DisplayName("Filter sales by PaymentMethod NEQUI vs BANCOLOMBIA")
    void testFilterByPaymentMethod() throws Exception {
        String testSuffix = UUID.randomUUID().toString().substring(0, 6);
        ProductVariant variant = createTestVariant("SKU-METH-" + testSuffix, "Producto Método",
                new BigDecimal("50000.00"), new BigDecimal("25000.00"));

        Instant testTime = LocalDateTime.of(2026, 10, 28, 11, 0, 0).atZone(BOGOTA_ZONE).toInstant();

        Order orderNequi = createPaidOrder("REF-NEQ-" + testSuffix, variant, "Producto Método", 1, new BigDecimal("50000.00"),
                testTime, PaymentMethod.NEQUI, OrderStatus.PAGADO);

        Order orderBancolombia = createPaidOrder("REF-BAN-" + testSuffix, variant, "Producto Método", 1, new BigDecimal("50000.00"),
                testTime, PaymentMethod.BANCOLOMBIA, OrderStatus.PAGADO);


        // Filter NEQUI
        MvcResult resNequi = mockMvc.perform(get("/api/v1/admin/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", "2026-10-28")
                        .param("to", "2026-10-28")
                        .param("method", "NEQUI"))
                .andExpect(status().isOk())
                .andReturn();

        List<String> nequiRefs = objectMapper.readTree(resNequi.getResponse().getContentAsString())
                .path("data").path("orders").findValuesAsText("reference");
        assertThat(nequiRefs).contains(orderNequi.getReference());
        assertThat(nequiRefs).doesNotContain(orderBancolombia.getReference());

        // Filter BANCOLOMBIA
        MvcResult resBan = mockMvc.perform(get("/api/v1/admin/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", "2026-10-28")
                        .param("to", "2026-10-28")
                        .param("method", "BANCOLOMBIA"))
                .andExpect(status().isOk())
                .andReturn();

        List<String> banRefs = objectMapper.readTree(resBan.getResponse().getContentAsString())
                .path("data").path("orders").findValuesAsText("reference");
        assertThat(banRefs).contains(orderBancolombia.getReference());
        assertThat(banRefs).doesNotContain(orderNequi.getReference());
    }

    @Test
    @DisplayName("POST /api/v1/admin/sales registers physical store sale, deducts stockOnHand, preserves stockReserved, and appears in sales")
    void testCreatePhysicalSale_deductsStockAndAppearsInSales() throws Exception {
        String testSuffix = UUID.randomUUID().toString().substring(0, 6);
        ProductVariant variant = createTestVariant("SKU-POS-" + testSuffix, "Sérum Punto Físico",
                new BigDecimal("45000.00"), new BigDecimal("20000.00"));
        variant.setStockOnHand(5);
        variant.setStockReserved(1);
        productVariantRepository.save(variant);

        String payload = """
                {
                  "customerName": "Camila Mostrador",
                  "customerPhone": "3159998877",
                  "paymentMethod": "NEQUI",
                  "transferReference": "NEQ-POS-001",
                  "discount": 5000.00,
                  "notes": "Venta presencial en mostrador",
                  "items": [
                    {
                      "variantId": "%s",
                      "quantity": 2
                    }
                  ]
                }
                """.formatted(variant.getId());

        MvcResult createRes = mockMvc.perform(post("/api/v1/admin/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.channel").value("PUNTO_FISICO"))
                .andExpect(jsonPath("$.data.status").value("ENTREGADO"))
                .andExpect(jsonPath("$.data.paymentMethod").value("NEQUI"))
                .andExpect(jsonPath("$.data.subtotal").value(90000.00))
                .andExpect(jsonPath("$.data.discount").value(5000.00))
                .andExpect(jsonPath("$.data.total").value(85000.00))
                .andExpect(jsonPath("$.data.items[0].costSnapshot").value(20000.00))
                .andReturn();

        String createdRef = objectMapper.readTree(createRes.getResponse().getContentAsString())
                .path("data").path("reference").asText();

        ProductVariant updatedVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(updatedVariant.getStockOnHand()).isEqualTo(3);
        assertThat(updatedVariant.getStockReserved()).isEqualTo(1);

        // Now attempt to sell 3 units when only 2 are available (stockOnHand=3, stockReserved=1) -> 409 Conflict
        String conflictPayload = """
                {
                  "paymentMethod": "EFECTIVO",
                  "items": [
                    {
                      "variantId": "%s",
                      "quantity": 3
                    }
                  ]
                }
                """.formatted(variant.getId());

        mockMvc.perform(post("/api/v1/admin/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(conflictPayload))
                .andExpect(status().isConflict());

        // Verify the created physical sale appears in GET /api/v1/admin/sales
        MvcResult listRes = mockMvc.perform(get("/api/v1/admin/sales")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        List<String> refs = objectMapper.readTree(listRes.getResponse().getContentAsString())
                .path("data").path("orders").findValuesAsText("reference");
        assertThat(refs).contains(createdRef);

        // Verify the created physical sale is exported in /api/v1/admin/sales/export.xlsx for today's date filter
        String todayStr = java.time.LocalDate.now(BOGOTA_ZONE).toString();
        MvcResult excelRes = mockMvc.perform(get("/api/v1/admin/sales/export.xlsx")
                        .header("Authorization", "Bearer " + adminToken)
                        .param("from", todayStr)
                        .param("to", todayStr))
                .andExpect(status().isOk())
                .andReturn();

        try (XSSFWorkbook wb = new XSSFWorkbook(new ByteArrayInputStream(excelRes.getResponse().getContentAsByteArray()))) {
            XSSFSheet ventas = wb.getSheet("VENTAS");
            boolean foundPosOrder = false;
            for (int r = 5; r <= ventas.getLastRowNum(); r++) {
                var row = ventas.getRow(r);
                if (row != null && row.getCell(1) != null && createdRef.equals(row.getCell(1).getStringCellValue())) {
                    foundPosOrder = true;
                    assertThat(row.getCell(4).getNumericCellValue()).isEqualTo(90000.0);
                    assertThat(row.getCell(5).getNumericCellValue()).isEqualTo(5000.0);
                    assertThat(row.getCell(7).getNumericCellValue()).isEqualTo(85000.0);
                    assertThat(row.getCell(10).getStringCellValue()).isEqualTo("Punto Físico (Mostrador)");
                }
            }
            assertThat(foundPosOrder).isTrue();

            XSSFSheet resumen = wb.getSheet("RESUMEN");
            assertThat(resumen.getRow(24).getCell(0).getStringCellValue()).isEqualTo("DESGLOSE POR CANAL Y DESCUENTOS");
            assertThat(resumen.getRow(25).getCell(1).getNumericCellValue()).isGreaterThanOrEqualTo(1.0);
        }
    }

    @Test
    @DisplayName("Admin can cancel a sale, restoring stockOnHand, logging RETURN movement, and excluding it from sales list")
    void testCancelSale_restoresStockAndKardexMovement() throws Exception {
        String testSuffix = UUID.randomUUID().toString().substring(0, 6);
        ProductVariant variant = createTestVariant("SKU-CAN-" + testSuffix, "Base Cancelable " + testSuffix,
                new BigDecimal("60000.00"), new BigDecimal("30000.00"));
        variant.setStockOnHand(10);
        variant.setStockReserved(0);
        productVariantRepository.save(variant);

        // 1. Create physical sale of 2 units
        String payload = """
                {
                  "customerName": "Cliente Cancelación",
                  "paymentMethod": "EFECTIVO",
                  "items": [
                    {
                      "variantId": "%s",
                      "quantity": 2
                    }
                  ]
                }
                """.formatted(variant.getId());

        MvcResult saleRes = mockMvc.perform(post("/api/v1/admin/sales")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode json = objectMapper.readTree(saleRes.getResponse().getContentAsString());
        UUID orderId = UUID.fromString(json.path("data").path("id").asText());
        String orderRef = json.path("data").path("reference").asText();

        // Verify stockOnHand was reduced to 8
        ProductVariant variantAfterSale = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(variantAfterSale.getStockOnHand()).isEqualTo(8);

        // 2. Cancel the sale via DELETE /api/v1/admin/sales/{id}
        mockMvc.perform(delete("/api/v1/admin/sales/{id}", orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        // 3. Verify stockOnHand is restored back to 10
        ProductVariant variantAfterCancel = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(variantAfterCancel.getStockOnHand()).isEqualTo(10);

        // 4. Verify Kardex has a RETURN movement
        var movements = inventoryMovementRepository.findAll().stream()
                .filter(m -> orderRef.equals(m.getReference()) && m.getMovementType() == MovementType.RETURN)
                .toList();
        assertThat(movements).hasSize(1);
        assertThat(movements.get(0).getQuantityDelta()).isEqualTo(2);

        // 5. Verify the order is excluded from GET /api/v1/admin/sales
        MvcResult listRes = mockMvc.perform(get("/api/v1/admin/sales")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andReturn();

        List<String> refs = objectMapper.readTree(listRes.getResponse().getContentAsString())
                .path("data").path("orders").findValuesAsText("reference");
        assertThat(refs).doesNotContain(orderRef);

        // 6. Attempting to cancel again returns 409 Conflict
        mockMvc.perform(delete("/api/v1/admin/sales/{id}", orderId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict());
    }
}
