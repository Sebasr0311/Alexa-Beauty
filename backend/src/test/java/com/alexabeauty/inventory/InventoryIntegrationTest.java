package com.alexabeauty.inventory;

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
import com.alexabeauty.common.audit.AuditLog;
import com.alexabeauty.common.audit.AuditLogRepository;
import com.alexabeauty.inventory.domain.InventoryMovement;
import com.alexabeauty.inventory.domain.MovementType;
import com.alexabeauty.inventory.domain.StockReceipt;
import com.alexabeauty.inventory.dto.CreateStockReceiptRequest;
import com.alexabeauty.inventory.dto.StockAdjustmentRequest;
import com.alexabeauty.inventory.dto.StockReceiptItemRequest;
import com.alexabeauty.inventory.repository.InventoryMovementRepository;
import com.alexabeauty.inventory.repository.StockReceiptRepository;
import com.alexabeauty.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
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
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class InventoryIntegrationTest {

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
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private StockReceiptRepository stockReceiptRepository;

    @Autowired
    private AuditLogRepository auditLogRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private Brand testBrand;
    private Category testCategory;

    @BeforeEach
    void setUp() {
        User adminUser = userRepository.findByEmail("admin@alexabeauty.com").orElseThrow();
        adminToken = jwtTokenProvider.generateToken(adminUser);

        testBrand = brandRepository.findBySlug("bella-cosmetics").orElseThrow();
        testCategory = categoryRepository.findBySlug("rostro").orElseThrow();
    }

    private ProductVariant createTestVariant(String sku, int stockOnHand, int stockReserved, int minStock) {
        Product product = Product.builder()
                .sku(sku + "-PROD")
                .name("Producto Test " + sku)
                .slug("prod-test-" + sku.toLowerCase())
                .brand(testBrand)
                .category(testCategory)
                .description("Descripción test")
                .active(true)
                .build();
        Product savedProduct = productRepository.save(product);

        ProductVariant variant = ProductVariant.builder()
                .product(savedProduct)
                .sku(sku)
                .shadeName("Tono Natural")
                .presentation("50ml")
                .salePrice(new BigDecimal("35000.00"))
                .referenceCost(new BigDecimal("18000.00"))
                .stockOnHand(stockOnHand)
                .stockReserved(stockReserved)
                .minStock(minStock)
                .active(true)
                .build();
        return productVariantRepository.save(variant);
    }

    @Test
    @DisplayName("P09: Receipt adds to stock_on_hand and preserves stock_reserved")
    void createReceipt_addsToStockOnHand_andPreservesStockReserved() throws Exception {
        String uniqueSku = "SKU-REC-" + UUID.randomUUID().toString().substring(0, 8);
        ProductVariant variant = createTestVariant(uniqueSku, 10, 4, 3);

        StockReceiptItemRequest itemRequest = new StockReceiptItemRequest(
                variant.getId(),
                5,
                new BigDecimal("18500.00"),
                "Lote A1"
        );

        CreateStockReceiptRequest receiptRequest = new CreateStockReceiptRequest(
                null,
                "Distribuidora Belleza SAS",
                "FAC-2026-001",
                "Recepción inicial mensual",
                List.of(itemRequest)
        );

        MvcResult result = mockMvc.perform(post("/api/v1/admin/inventory/receipts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiptRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.receiptNumber").isNotEmpty())
                .andExpect(jsonPath("$.data.totalUnits").value(5))
                .andExpect(jsonPath("$.data.totalCost").value(92500.00))
                .andExpect(jsonPath("$.data.items.length()").value(1))
                .andExpect(jsonPath("$.data.items[0].quantity").value(5))
                .andReturn();

        // Verify variant in database: stockOnHand = 10 + 5 = 15; stockReserved stays 4
        ProductVariant updatedVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(updatedVariant.getStockOnHand()).isEqualTo(15);
        assertThat(updatedVariant.getStockReserved()).isEqualTo(4);

        // Verify Kardex: movement ENTRADA created
        List<InventoryMovement> movements = inventoryMovementRepository.findByVariantIdOrderByCreatedAtDesc(variant.getId());
        assertThat(movements).isNotEmpty();
        InventoryMovement entrada = movements.stream()
                .filter(m -> m.getMovementType() == MovementType.ENTRADA)
                .findFirst()
                .orElseThrow();
        assertThat(entrada.getQuantityDelta()).isEqualTo(5);
        assertThat(entrada.getQtyBefore()).isEqualTo(10);
        assertThat(entrada.getQtyAfter()).isEqualTo(15);
        assertThat(entrada.getReason()).isEqualTo("Recepción de mercancía");
    }

    @Test
    @DisplayName("P10: Duplicate receipt with same idempotencyKey / receiptNumber does NOT duplicate stock")
    void createReceipt_withDuplicateIdempotencyKey_doesNotDuplicateStock() throws Exception {
        String uniqueSku = "SKU-IDEM-" + UUID.randomUUID().toString().substring(0, 8);
        ProductVariant variant = createTestVariant(uniqueSku, 20, 0, 5);

        String idempotencyKey = "IDEM-KEY-" + UUID.randomUUID().toString().substring(0, 8);
        StockReceiptItemRequest itemRequest = new StockReceiptItemRequest(
                variant.getId(),
                8,
                new BigDecimal("15000.00"),
                "Ítem 1"
        );

        CreateStockReceiptRequest receiptRequest = new CreateStockReceiptRequest(
                null,
                "Proveedor Confiable",
                "FAC-DUP-100",
                "Notas idempotentes",
                List.of(itemRequest)
        );

        // First call
        MvcResult firstResult = mockMvc.perform(post("/api/v1/admin/inventory/receipts")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiptRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode firstJson = objectMapper.readTree(firstResult.getResponse().getContentAsString());
        String receiptIdFirst = firstJson.path("data").path("id").asText();

        // Verify stock is now 28 (20 + 8)
        ProductVariant variantAfterFirst = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(variantAfterFirst.getStockOnHand()).isEqualTo(28);

        // Second call with identical Idempotency-Key
        MvcResult secondResult = mockMvc.perform(post("/api/v1/admin/inventory/receipts")
                        .header("Authorization", "Bearer " + adminToken)
                        .header("Idempotency-Key", idempotencyKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiptRequest)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andReturn();

        JsonNode secondJson = objectMapper.readTree(secondResult.getResponse().getContentAsString());
        String receiptIdSecond = secondJson.path("data").path("id").asText();

        // Must return the SAME receipt
        assertThat(receiptIdSecond).isEqualTo(receiptIdFirst);

        // Crucial verification: stockOnHand MUST STILL BE 28 (NOT 36!)
        ProductVariant variantAfterSecond = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(variantAfterSecond.getStockOnHand()).isEqualTo(28);

        // Check movements: only ONE ENTRADA movement exists
        List<InventoryMovement> movements = inventoryMovementRepository.findByVariantIdOrderByCreatedAtDesc(variant.getId());
        long entradaCount = movements.stream().filter(m -> m.getMovementType() == MovementType.ENTRADA).count();
        assertThat(entradaCount).isEqualTo(1);
    }

    @Test
    @DisplayName("Manual adjustment rejects newStockOnHand < stock_reserved with 422 Unprocessable Entity")
    void adjustStock_whenNewStockLessThanReserved_rejectsWith422UnprocessableEntity() throws Exception {
        String uniqueSku = "SKU-ADJ-FAIL-" + UUID.randomUUID().toString().substring(0, 8);
        ProductVariant variant = createTestVariant(uniqueSku, 10, 6, 2);

        StockAdjustmentRequest request = new StockAdjustmentRequest(
                variant.getId(),
                4, // Invalid: 4 < 6 reserved!
                "Intento de ajuste inferior a reservas",
                "Nota adicional"
        );

        mockMvc.perform(post("/api/v1/admin/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("no puede ser inferior a las unidades actualmente reservadas")));

        // Verify variant stockOnHand was NOT changed
        ProductVariant unchangedVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(unchangedVariant.getStockOnHand()).isEqualTo(10);
    }

    @Test
    @DisplayName("Valid manual adjustment creates AJUSTE movement in Kardex and logs to AuditLog")
    void adjustStock_validAdjustment_createsAjusteMovementAndAuditLog() throws Exception {
        String uniqueSku = "SKU-ADJ-OK-" + UUID.randomUUID().toString().substring(0, 8);
        ProductVariant variant = createTestVariant(uniqueSku, 12, 3, 2);

        StockAdjustmentRequest request = new StockAdjustmentRequest(
                variant.getId(),
                18, // Valid: 18 >= 3 reserved, delta = +6
                "Conteo físico trimestral de inventario",
                "Ajuste verificado por supervisor"
        );

        mockMvc.perform(post("/api/v1/admin/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.previousStockOnHand").value(12))
                .andExpect(jsonPath("$.data.newStockOnHand").value(18))
                .andExpect(jsonPath("$.data.stockReserved").value(3))
                .andExpect(jsonPath("$.data.availableStock").value(15))
                .andExpect(jsonPath("$.data.quantityDelta").value(6))
                .andExpect(jsonPath("$.data.reason").value("Conteo físico trimestral de inventario"));

        // Check variant persisted
        ProductVariant updatedVariant = productVariantRepository.findById(variant.getId()).orElseThrow();
        assertThat(updatedVariant.getStockOnHand()).isEqualTo(18);

        // Check Kardex movement
        List<InventoryMovement> movements = inventoryMovementRepository.findByVariantIdOrderByCreatedAtDesc(variant.getId());
        InventoryMovement ajusteMov = movements.stream()
                .filter(m -> m.getMovementType() == MovementType.AJUSTE)
                .findFirst()
                .orElseThrow();
        assertThat(ajusteMov.getQuantityDelta()).isEqualTo(6);
        assertThat(ajusteMov.getQtyBefore()).isEqualTo(12);
        assertThat(ajusteMov.getQtyAfter()).isEqualTo(18);
        assertThat(ajusteMov.getReference()).isEqualTo("AJUSTE_MANUAL");
        assertThat(ajusteMov.getReason()).isEqualTo("Conteo físico trimestral de inventario");

        // Check AuditLog
        List<AuditLog> auditLogs = auditLogRepository.findByEntityTypeAndEntityId("ProductVariant", variant.getId().toString());
        assertThat(auditLogs).isNotEmpty();
        AuditLog log = auditLogs.get(0);
        assertThat(log.getAction()).isEqualTo("STOCK_ADJUSTMENT");
        assertThat(log.getActorEmail()).isEqualTo("admin@alexabeauty.com");
    }

    @Test
    @DisplayName("Inventory summary calculates availableStock = on_hand - reserved and filters lowStock")
    void getInventorySummary_calculatesAvailableAndFiltersLowStock() throws Exception {
        String lowStockSku = "SKU-LOW-" + UUID.randomUUID().toString().substring(0, 8);
        // onHand=6, reserved=3 -> available=3 <= minStock=4 -> isLowStock = true
        ProductVariant lowStockVariant = createTestVariant(lowStockSku, 6, 3, 4);

        String okStockSku = "SKU-OK-" + UUID.randomUUID().toString().substring(0, 8);
        // onHand=25, reserved=2 -> available=23 > minStock=5 -> isLowStock = false
        ProductVariant okStockVariant = createTestVariant(okStockSku, 25, 2, 5);

        // Query summary with lowStockOnly=true
        mockMvc.perform(get("/api/v1/admin/inventory/summary")
                        .param("lowStockOnly", "true")
                        .param("query", lowStockSku)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].sku").value(lowStockSku))
                .andExpect(jsonPath("$.data.content[0].stockOnHand").value(6))
                .andExpect(jsonPath("$.data.content[0].stockReserved").value(3))
                .andExpect(jsonPath("$.data.content[0].availableStock").value(3))
                .andExpect(jsonPath("$.data.content[0].isLowStock").value(true));

        // Query summary with okStockSku and lowStockOnly=true should return empty
        mockMvc.perform(get("/api/v1/admin/inventory/summary")
                        .param("lowStockOnly", "true")
                        .param("query", okStockSku)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content.length()").value(0));
    }

    @Test
    @DisplayName("Kardex movements endpoint filters by variantId and movementType")
    void getMovements_filtersByVariantAndType() throws Exception {
        String sku = "SKU-KARDEX-" + UUID.randomUUID().toString().substring(0, 8);
        ProductVariant variant = createTestVariant(sku, 15, 0, 3);

        // Create adjustment
        StockAdjustmentRequest adjRequest = new StockAdjustmentRequest(
                variant.getId(),
                20,
                "Ajuste para test kardex",
                null
        );
        mockMvc.perform(post("/api/v1/admin/inventory/adjustments")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adjRequest)))
                .andExpect(status().isOk());

        // Query Kardex movements for this variant and type AJUSTE
        mockMvc.perform(get("/api/v1/admin/inventory/movements")
                        .param("variantId", variant.getId().toString())
                        .param("type", "AJUSTE")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content.length()").value(1))
                .andExpect(jsonPath("$.data.content[0].movementType").value("AJUSTE"))
                .andExpect(jsonPath("$.data.content[0].quantityDelta").value(5))
                .andExpect(jsonPath("$.data.content[0].sku").value(sku));
    }

    @Test
    @DisplayName("List and get receipt by ID returns accurate receipt header and items")
    void listAndGetReceipts_returnsExpectedDetails() throws Exception {
        String sku = "SKU-DETAIL-" + UUID.randomUUID().toString().substring(0, 8);
        ProductVariant variant = createTestVariant(sku, 5, 0, 2);

        StockReceiptItemRequest item = new StockReceiptItemRequest(
                variant.getId(),
                12,
                new BigDecimal("10000.00"),
                "Prueba detalle"
        );
        CreateStockReceiptRequest receiptReq = new CreateStockReceiptRequest(
                "REC-DETAIL-001",
                "Proveedor Mayorista",
                "FAC-9999",
                "Notas del recibo",
                List.of(item)
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/inventory/receipts")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(receiptReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID receiptId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString())
                .path("data").path("id").asText());

        // Get receipt by ID
        mockMvc.perform(get("/api/v1/admin/inventory/receipts/{id}", receiptId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.receiptNumber").value("REC-DETAIL-001"))
                .andExpect(jsonPath("$.data.providerName").value("Proveedor Mayorista"))
                .andExpect(jsonPath("$.data.totalUnits").value(12))
                .andExpect(jsonPath("$.data.totalCost").value(120000.00))
                .andExpect(jsonPath("$.data.items.length()").value(1));

        // List receipts
        mockMvc.perform(get("/api/v1/admin/inventory/receipts")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray());
    }
}
