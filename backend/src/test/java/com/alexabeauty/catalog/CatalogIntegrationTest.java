package com.alexabeauty.catalog;

import com.alexabeauty.auth.domain.User;
import com.alexabeauty.auth.repository.UserRepository;
import com.alexabeauty.catalog.domain.Brand;
import com.alexabeauty.catalog.domain.Category;
import com.alexabeauty.catalog.domain.Product;
import com.alexabeauty.catalog.domain.ProductVariant;
import com.alexabeauty.catalog.dto.CreateProductRequest;
import com.alexabeauty.catalog.dto.ProductVariantRequest;
import com.alexabeauty.catalog.repository.BrandRepository;
import com.alexabeauty.catalog.repository.CategoryRepository;
import com.alexabeauty.catalog.repository.ProductImageRepository;
import com.alexabeauty.catalog.repository.ProductRepository;
import com.alexabeauty.catalog.repository.ProductVariantRepository;
import com.alexabeauty.inventory.domain.InventoryMovement;
import com.alexabeauty.inventory.domain.MovementType;
import com.alexabeauty.inventory.repository.InventoryMovementRepository;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class CatalogIntegrationTest {

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
    private ProductImageRepository productImageRepository;

    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;
    private Brand defaultBrand;
    private Category defaultCategory;
    private Category lipsCategory;

    @BeforeEach
    void setUp() {
        User adminUser = userRepository.findByEmail("admin@alexabeauty.com").orElseThrow();
        adminToken = jwtTokenProvider.generateToken(adminUser);

        defaultBrand = brandRepository.findBySlug("bella-cosmetics").orElseThrow();
        defaultCategory = categoryRepository.findBySlug("rostro").orElseThrow();
        lipsCategory = categoryRepository.findBySlug("labios").orElseThrow();
    }

    @Test
    @DisplayName("Create product without variants creates DEFAULT variant and ENTRADA_INICIAL inventory movement")
    void createProduct_withoutVariants_createsDefaultVariantAndInventoryMovement() throws Exception {
        String uniqueSku = "SKU-DEFAULT-" + UUID.randomUUID().toString().substring(0, 8);
        CreateProductRequest request = new CreateProductRequest(
                "Base Líquida Cobertura Total",
                defaultBrand.getId(),
                defaultCategory.getId(),
                "Base de alta cobertura con acabado mate.",
                uniqueSku,
                new BigDecimal("45000.00"),
                15,
                3,
                null,
                true,
                true
        );

        MvcResult result = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.sku").value(uniqueSku))
                .andExpect(jsonPath("$.data.variants.length()").value(1))
                .andExpect(jsonPath("$.data.variants[0].shadeName").value("Default"))
                .andExpect(jsonPath("$.data.variants[0].sku").value(uniqueSku))
                .andExpect(jsonPath("$.data.variants[0].stockOnHand").value(15))
                .andExpect(jsonPath("$.data.variants[0].stockReserved").value(0))
                .andExpect(jsonPath("$.data.variants[0].availableStock").value(15))
                .andReturn();

        JsonNode jsonNode = objectMapper.readTree(result.getResponse().getContentAsString());
        UUID variantId = UUID.fromString(jsonNode.path("data").path("variants").get(0).path("id").asText());

        // Verify inventory movements table
        List<InventoryMovement> movements = inventoryMovementRepository.findByVariantIdOrderByCreatedAtDesc(variantId);
        assertThat(movements).hasSize(1);
        InventoryMovement movement = movements.get(0);
        assertThat(movement.getMovementType()).isEqualTo(MovementType.ENTRADA_INICIAL);
        assertThat(movement.getQuantityDelta()).isEqualTo(15);
        assertThat(movement.getQtyBefore()).isEqualTo(0);
        assertThat(movement.getQtyAfter()).isEqualTo(15);
        assertThat(movement.getReference()).isEqualTo("ALTA_PRODUCTO");
        assertThat(movement.getReason()).isEqualTo("Stock inicial al registrar producto");
    }

    @Test
    @DisplayName("Create product with duplicate SKU throws 409 Conflict")
    void createProduct_withDuplicateSku_throws409Conflict() throws Exception {
        String duplicateSku = "SKU-DUP-" + UUID.randomUUID().toString().substring(0, 8);
        CreateProductRequest request1 = new CreateProductRequest(
                "Polvo Compacto Mate",
                defaultBrand.getId(),
                defaultCategory.getId(),
                "Polvo para sellar maquillaje.",
                duplicateSku,
                new BigDecimal("30000.00"),
                10,
                2,
                null,
                false,
                true
        );

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request1)))
                .andExpect(status().isCreated());

        CreateProductRequest request2 = new CreateProductRequest(
                "Polvo Compacto Otra Marca",
                defaultBrand.getId(),
                defaultCategory.getId(),
                "Descripción alternativa.",
                duplicateSku,
                new BigDecimal("32000.00"),
                5,
                2,
                null,
                false,
                true
        );

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request2)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("CONFLICT"));
    }

    @Test
    @DisplayName("Create product with negative price throws 400 Bad Request")
    void createProduct_withNegativePrice_throws400BadRequest() throws Exception {
        String sku = "SKU-NEG-" + UUID.randomUUID().toString().substring(0, 8);
        CreateProductRequest request = new CreateProductRequest(
                "Producto Precio Inválido",
                defaultBrand.getId(),
                defaultCategory.getId(),
                "Producto de prueba con precio negativo.",
                sku,
                new BigDecimal("-5000.00"),
                10,
                2,
                null,
                false,
                true
        );

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("Public catalog lists only active products and respects category filter")
    void listPublicProducts_filtersOnlyActiveAndByCategory() throws Exception {
        String activeSkuRostro = "SKU-ACT-R-" + UUID.randomUUID().toString().substring(0, 6);
        CreateProductRequest reqActiveRostro = new CreateProductRequest(
                "Rubor Rosa Activo",
                defaultBrand.getId(),
                defaultCategory.getId(),
                "Rubor en polvo para el rostro.",
                activeSkuRostro,
                new BigDecimal("25000.00"),
                8,
                2,
                null,
                false,
                true
        );

        MvcResult res1 = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqActiveRostro)))
                .andExpect(status().isCreated())
                .andReturn();
        UUID activeProductId = UUID.fromString(objectMapper.readTree(res1.getResponse().getContentAsString()).path("data").path("id").asText());

        String inactiveSkuRostro = "SKU-INACT-R-" + UUID.randomUUID().toString().substring(0, 6);
        CreateProductRequest reqInactiveRostro = new CreateProductRequest(
                "Rubor Inactivo",
                defaultBrand.getId(),
                defaultCategory.getId(),
                "Rubor inactivo.",
                inactiveSkuRostro,
                new BigDecimal("22000.00"),
                5,
                1,
                null,
                false,
                false
        );

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqInactiveRostro)))
                .andExpect(status().isCreated());

        String activeSkuLabios = "SKU-ACT-L-" + UUID.randomUUID().toString().substring(0, 6);
        CreateProductRequest reqActiveLabios = new CreateProductRequest(
                "Labial Rojo Pasión",
                defaultBrand.getId(),
                lipsCategory.getId(),
                "Labial cremoso.",
                activeSkuLabios,
                new BigDecimal("18000.00"),
                20,
                5,
                null,
                false,
                true
        );

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reqActiveLabios)))
                .andExpect(status().isCreated());

        // Now query public catalog filtered by category 'rostro'
        mockMvc.perform(get("/api/v1/catalog/products")
                        .param("category", "rostro")
                        .param("size", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content[?(@.sku == '" + activeSkuRostro + "')]").isNotEmpty())
                .andExpect(jsonPath("$.data.content[?(@.sku == '" + inactiveSkuRostro + "')]").isEmpty())
                .andExpect(jsonPath("$.data.content[?(@.sku == '" + activeSkuLabios + "')]").isEmpty());
    }

    @Test
    @DisplayName("Public product detail returns available stock as stock_on_hand - stock_reserved")
    void getProductDetail_returnsAvailableStockFormula() throws Exception {
        String sku = "SKU-STOCK-" + UUID.randomUUID().toString().substring(0, 8);
        ProductVariantRequest variantReq = new ProductVariantRequest(
                sku + "-V1",
                "Tono Canela",
                "30ml",
                new BigDecimal("52000.00"),
                new BigDecimal("30000.00"),
                2,
                10,
                true
        );

        CreateProductRequest request = new CreateProductRequest(
                "Base Hidratante Acabado Natural",
                defaultBrand.getId(),
                defaultCategory.getId(),
                "Base fluida de larga duración.",
                sku,
                new BigDecimal("52000.00"),
                10,
                2,
                List.of(variantReq),
                false,
                true
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString());
        String slug = createdJson.path("data").path("slug").asText();
        UUID variantId = UUID.fromString(createdJson.path("data").path("variants").get(0).path("id").asText());

        // Simulate a reservation: update stockReserved = 4 directly in database
        ProductVariant variant = productVariantRepository.findById(variantId).orElseThrow();
        variant.setStockReserved(4);
        productVariantRepository.save(variant);

        // Call public detail endpoint: internal stockOnHand/stockReserved are omitted, availableStock is verified
        mockMvc.perform(get("/api/v1/catalog/products/" + slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.variants[0].stockOnHand").doesNotExist())
                .andExpect(jsonPath("$.data.variants[0].stockReserved").doesNotExist())
                .andExpect(jsonPath("$.data.variants[0].availableStock").value(6))
                .andExpect(jsonPath("$.data.totalStockOnHand").doesNotExist())
                .andExpect(jsonPath("$.data.totalAvailableStock").value(6));
    }

    @Test
    @DisplayName("Image upload validates MIME type (rejects text/plain, accepts image/png)")
    void uploadImage_validatesMimeType() throws Exception {
        // Step 1: Create a product to attach images to
        String sku = "SKU-IMG-" + UUID.randomUUID().toString().substring(0, 8);
        CreateProductRequest request = new CreateProductRequest(
                "Iluminador Glow Dorado",
                defaultBrand.getId(),
                defaultCategory.getId(),
                "Iluminador en barra con partículas perladas.",
                sku,
                new BigDecimal("35000.00"),
                12,
                3,
                null,
                false,
                true
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID productId = UUID.fromString(objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data").path("id").asText());

        // Step 2: Upload invalid file type (text/plain)
        MockMultipartFile textFile = new MockMultipartFile(
                "file",
                "test.txt",
                "text/plain",
                "Esto no es una imagen".getBytes()
        );

        mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", productId)
                        .file(textFile)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));

        // Step 3: Upload valid file type (image/png)
        MockMultipartFile pngFile = new MockMultipartFile(
                "file",
                "glow.png",
                "image/png",
                validPng()
        );

        MvcResult uploadResult = mockMvc.perform(multipart("/api/v1/admin/products/{id}/images", productId)
                        .file(pngFile)
                        .param("altText", "Foto principal del iluminador glow")
                        .param("isCover", "true")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.isCover").value(true))
                .andExpect(jsonPath("$.data.altText").value("Foto principal del iluminador glow"))
                .andExpect(jsonPath("$.data.url").isNotEmpty())
                .andReturn();

        UUID imageId = UUID.fromString(objectMapper.readTree(uploadResult.getResponse().getContentAsString()).path("data").path("id").asText());

        // Step 4: Verify set cover image and delete image endpoints
        mockMvc.perform(patch("/api/v1/admin/products/{id}/images/{imageId}/cover", productId, imageId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isCover").value(true));

        mockMvc.perform(delete("/api/v1/admin/products/{id}/images/{imageId}", productId, imageId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("Public catalog endpoints do not leak referenceCost, minStock, stockOnHand or stockReserved")
    void publicCatalogDoesNotLeakInternalCostsOrPhysicalStock() throws Exception {
        String sku = "SKU-PUBSEC-" + UUID.randomUUID().toString().substring(0, 8);
        ProductVariantRequest variantReq = new ProductVariantRequest(
                sku + "-V1",
                "Rojo Fuego",
                "3.5g",
                new BigDecimal("28000.00"),
                new BigDecimal("15000.00"),
                3,
                10,
                true,
                "01",
                "#FF0000"
        );

        CreateProductRequest createReq = new CreateProductRequest(
                "Labial Seguro Publico " + sku,
                defaultBrand.getId(),
                defaultCategory.getId(),
                "Descripcion publica",
                sku,
                new BigDecimal("28000.00"),
                10,
                3,
                List.of(variantReq),
                false,
                true
        );

        MvcResult createResult = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated())
                .andReturn();

        JsonNode createdJson = objectMapper.readTree(createResult.getResponse().getContentAsString()).path("data");
        UUID productId = UUID.fromString(createdJson.path("id").asText());
        String slug = createdJson.path("slug").asText();

        // 1. Verify PUBLIC LIST endpoint (/api/v1/catalog/products): internal costs & physical stock MUST NOT exist
        mockMvc.perform(get("/api/v1/catalog/products")
                        .param("query", sku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.content[0].sku").value(sku))
                .andExpect(jsonPath("$.data.content[0].variants[0].referenceCost").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].variants[0].stockOnHand").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].variants[0].stockReserved").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].variants[0].minStock").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].totalStockOnHand").doesNotExist())
                .andExpect(jsonPath("$.data.content[0].variants[0].availableStock").value(10))
                .andExpect(jsonPath("$.data.content[0].totalAvailableStock").value(10));

        // 2. Verify PUBLIC DETAIL endpoint (/api/v1/catalog/products/{slug}): internal costs & physical stock MUST NOT exist
        mockMvc.perform(get("/api/v1/catalog/products/{slug}", slug))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.sku").value(sku))
                .andExpect(jsonPath("$.data.variants[0].referenceCost").doesNotExist())
                .andExpect(jsonPath("$.data.variants[0].stockOnHand").doesNotExist())
                .andExpect(jsonPath("$.data.variants[0].stockReserved").doesNotExist())
                .andExpect(jsonPath("$.data.variants[0].minStock").doesNotExist())
                .andExpect(jsonPath("$.data.totalStockOnHand").doesNotExist())
                .andExpect(jsonPath("$.data.variants[0].availableStock").value(10))
                .andExpect(jsonPath("$.data.totalAvailableStock").value(10));

        // 3. Verify ADMIN DETAIL endpoint (/api/v1/admin/products/{id}): internal costs & physical stock MUST exist for admin
        mockMvc.perform(get("/api/v1/admin/products/{id}", productId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.variants[0].referenceCost").value(15000.00))
                .andExpect(jsonPath("$.data.variants[0].stockOnHand").value(10))
                .andExpect(jsonPath("$.data.variants[0].minStock").value(3))
                .andExpect(jsonPath("$.data.totalStockOnHand").value(10));
    }

    @Test
    @DisplayName("AB-AUD-04: Public catalog searches by shadeCode, shadeName, variant SKU and brand without duplicating products")
    void searchByShadeCodeNameVariantSkuAndBrandWithoutDuplication() throws Exception {
        String baseSku = "SKU-SEARCH-" + UUID.randomUUID().toString().substring(0, 8);
        String uniqueShadeCode = "SHD-" + UUID.randomUUID().toString().substring(0, 4);
        String uniqueShadeName = "Classic Ivory " + UUID.randomUUID().toString().substring(0, 4);
        String uniqueVariantSku = baseSku + "-V10";

        ProductVariantRequest v1 = new ProductVariantRequest(
                uniqueVariantSku,
                uniqueShadeName,
                "30ml",
                new BigDecimal("45000.00"),
                new BigDecimal("20000.00"),
                3,
                15,
                true,
                uniqueShadeCode,
                "#E5B899"
        );
        ProductVariantRequest v2 = new ProductVariantRequest(
                baseSku + "-V20",
                "Warm Beige",
                "30ml",
                new BigDecimal("45000.00"),
                new BigDecimal("20000.00"),
                3,
                10,
                true,
                "SHD-WB",
                "#C68B59"
        );

        CreateProductRequest createReq = new CreateProductRequest(
                "Base Fit Me Search " + baseSku,
                defaultBrand.getId(),
                defaultCategory.getId(),
                "Base fluida mate",
                baseSku,
                new BigDecimal("45000.00"),
                25,
                3,
                List.of(v1, v2),
                false,
                true
        );

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isCreated());

        // A. Search by shade code -> exactly 1 product returned (not duplicated despite 2 variants)
        mockMvc.perform(get("/api/v1/catalog/products")
                        .param("query", uniqueShadeCode))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].sku").value(baseSku));

        // B. Search by shade name -> exactly 1 product returned
        mockMvc.perform(get("/api/v1/catalog/products")
                        .param("query", uniqueShadeName))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].sku").value(baseSku));

        // C. Search by variant SKU -> exactly 1 product returned
        mockMvc.perform(get("/api/v1/catalog/products")
                        .param("query", uniqueVariantSku))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(1))
                .andExpect(jsonPath("$.data.content[0].sku").value(baseSku));

        // D. Search by brand name
        mockMvc.perform(get("/api/v1/catalog/products")
                        .param("query", defaultBrand.getName()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalElements").value(org.hamcrest.Matchers.greaterThanOrEqualTo(1)));
    }

    @Test
    @DisplayName("AB-AUD-05: Creating additional variant without commercial identity is rejected with 400 ValidationException")
    void additionalVariantRequiresCommercialIdentity() throws Exception {
        String baseSku = "SKU-NOID-" + UUID.randomUUID().toString().substring(0, 8);

        // 1. Create product with empty code, name, and presentation in variants list -> rejected
        ProductVariantRequest emptyVariant = new ProductVariantRequest(
                baseSku + "-V1",
                null,
                null,
                new BigDecimal("30000.00"),
                new BigDecimal("10000.00"),
                3,
                5,
                true,
                null,
                null
        );

        CreateProductRequest createReq = new CreateProductRequest(
                "Producto Invalido " + baseSku,
                defaultBrand.getId(),
                defaultCategory.getId(),
                "Descripcion",
                baseSku,
                new BigDecimal("30000.00"),
                5,
                3,
                List.of(emptyVariant),
                false,
                true
        );

        mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(createReq)))
                .andExpect(status().isBadRequest());

        // 2. Create simple product (default variant) -> succeeds
        CreateProductRequest simpleReq = new CreateProductRequest(
                "Producto Simple " + baseSku,
                defaultBrand.getId(),
                defaultCategory.getId(),
                "Descripcion",
                baseSku,
                new BigDecimal("30000.00"),
                5,
                3,
                null,
                false,
                true
        );

        MvcResult createOk = mockMvc.perform(post("/api/v1/admin/products")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(simpleReq)))
                .andExpect(status().isCreated())
                .andReturn();

        UUID productId = UUID.fromString(objectMapper.readTree(createOk.getResponse().getContentAsString()).path("data").path("id").asText());

        // 3. Adding an additional variant with no shadeCode, no shadeName, and no presentation -> rejected with 400
        mockMvc.perform(post("/api/v1/admin/products/{productId}/variants", productId)
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(emptyVariant)))
                .andExpect(status().isBadRequest());
    }

    private byte[] validPng() throws java.io.IOException {
        var bytes = new java.io.ByteArrayOutputStream();
        javax.imageio.ImageIO.write(new java.awt.image.BufferedImage(2, 2, java.awt.image.BufferedImage.TYPE_INT_RGB), "png", bytes);
        return bytes.toByteArray();
    }
}
