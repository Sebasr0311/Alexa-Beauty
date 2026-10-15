package com.alexabeauty.reporting;

import com.alexabeauty.auth.domain.User;
import com.alexabeauty.auth.repository.UserRepository;
import com.alexabeauty.catalog.domain.Product;
import com.alexabeauty.catalog.repository.BrandRepository;
import com.alexabeauty.catalog.repository.CategoryRepository;
import com.alexabeauty.catalog.repository.ProductRepository;
import com.alexabeauty.catalog.repository.ProductVariantRepository;
import com.alexabeauty.inventory.domain.InventoryMovement;
import com.alexabeauty.inventory.domain.MovementType;
import com.alexabeauty.inventory.repository.InventoryMovementRepository;
import com.alexabeauty.reporting.excel.ExcelProductTemplateGenerator;
import com.alexabeauty.reporting.excel.ExcelSanitizer;
import com.alexabeauty.reporting.repository.ExcelImportJobRepository;
import com.alexabeauty.security.JwtTokenProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Row;
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
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ExcelImportIntegrationTest {

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
    private ExcelImportJobRepository excelImportJobRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String adminToken;

    @BeforeEach
    void setUp() {
        User adminUser = userRepository.findByEmail("admin@alexabeauty.com").orElseThrow();
        adminToken = jwtTokenProvider.generateToken(adminUser);
    }

    @Test
    @DisplayName("Download template returns ALEXA_PRODUCTS_V1 workbook with 3 sheets and styled headers")
    void downloadTemplate_returnsValidWorkbook() throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/admin/products/import/template")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + ExcelProductTemplateGenerator.TEMPLATE_FILENAME + "\""))
                .andReturn();

        byte[] content = result.getResponse().getContentAsByteArray();
        assertThat(content).isNotEmpty();

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(3);
            assertThat(workbook.getSheetName(0)).isEqualTo("PRODUCTOS");
            assertThat(workbook.getSheetName(1)).isEqualTo("INSTRUCCIONES");
            assertThat(workbook.getSheetName(2)).isEqualTo("EJEMPLOS_NO_IMPORTAR");

            XSSFSheet sheet1 = workbook.getSheet("PRODUCTOS");
            Row headerRow = sheet1.getRow(0);
            assertThat(headerRow).isNotNull();

            for (int i = 0; i < ExcelProductTemplateGenerator.HEADERS.length; i++) {
                assertThat(headerRow.getCell(i).getStringCellValue())
                        .isEqualTo(ExcelProductTemplateGenerator.HEADERS[i]);
            }
        }
    }

    @Test
    @DisplayName("P04: Upload valid 5-row Excel -> preview reports 5 valid -> commit imports 5 products and creates ENTRADA_INICIAL movements")
    void p04_uploadValid5RowExcel_previewAndCommit_createsProductsAndKardexMovements() throws Exception {
        String batchSuffix = UUID.randomUUID().toString().substring(0, 6);
        List<List<Object>> rows = new ArrayList<>();
        int expectedTotalStock = 0;

        for (int i = 1; i <= 5; i++) {
            String sku = "SKU-P04-" + batchSuffix + "-" + i;
            int stock = 10 * i;
            expectedTotalStock += stock;
            rows.add(List.of(
                    "Producto P04 " + batchSuffix + " " + i,
                    "Marca P04 " + batchSuffix,
                    stock,
                    25000 + (i * 1000),
                    sku,
                    "Categoria P04",
                    "Tono " + i,
                    "30ml",
                    15000,
                    3,
                    "Descripción P04 " + i,
                    "SI"
            ));
        }

        byte[] excelBytes = createTestWorkbook(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "productos_p04.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
        );

        // 1. Preview
        MvcResult previewResult = mockMvc.perform(multipart("/api/v1/admin/products/imports/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRows").value(5))
                .andExpect(jsonPath("$.data.validRows").value(5))
                .andExpect(jsonPath("$.data.rejectedRows").value(0))
                .andExpect(jsonPath("$.data.isDuplicateFile").value(false))
                .andExpect(jsonPath("$.data.previewItems.length()").value(5))
                .andReturn();

        JsonNode previewJson = objectMapper.readTree(previewResult.getResponse().getContentAsString());
        UUID jobId = UUID.fromString(previewJson.path("data").path("jobId").asText());

        // 2. Commit
        MvcResult commitResult = mockMvc.perform(post("/api/v1/admin/products/imports/{jobId}/commit", jobId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.jobId").value(jobId.toString()))
                .andExpect(jsonPath("$.data.importedProductsCount").value(5))
                .andExpect(jsonPath("$.data.totalInitialStockAdded").value(expectedTotalStock))
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andReturn();

        // 3. Verify Database entities
        for (int i = 1; i <= 5; i++) {
            String sku = "SKU-P04-" + batchSuffix + "-" + i;
            Product product = productRepository.findBySku(sku).orElseThrow();
            assertThat(product.getName()).isEqualTo("Producto P04 " + batchSuffix + " " + i);
            com.alexabeauty.catalog.domain.ProductVariant variant = productVariantRepository.findBySku(sku).orElseThrow();
            assertThat(variant.getStockOnHand()).isEqualTo(10 * i);
        }

        // 4. Verify Kardex initial inventory movements
        List<InventoryMovement> movements = inventoryMovementRepository.findAll().stream()
                .filter(m -> ("IMPORT_" + jobId).equals(m.getReference()))
                .toList();
        assertThat(movements).hasSize(5);
        for (InventoryMovement m : movements) {
            assertThat(m.getMovementType()).isEqualTo(MovementType.ENTRADA_INICIAL);
            assertThat(m.getQtyBefore()).isEqualTo(0);
            assertThat(m.getQtyAfter()).isEqualTo(m.getQuantityDelta());
            assertThat(m.getReason()).isEqualTo("Importación masiva Excel");
        }
    }

    @Test
    @DisplayName("P05: Upload Excel with negative stock in row 3 -> preview reports error in row 3 -> commit with CANCEL_ON_ERROR fails and rollback ensures 0 products created")
    void p05_negativeStockInRow3_failsCommitAndRollsBack() throws Exception {
        String batchSuffix = UUID.randomUUID().toString().substring(0, 6);
        List<List<Object>> rows = new ArrayList<>();

        // Row 2 (first data row) - valid
        rows.add(List.of("Producto 1", "MarcaTest", 10, 20000, "SKU-P05-" + batchSuffix + "-1", "Cat", "Tono", "Pres", 10000, 3, "Desc", "SI"));
        // Row 3 (second data row) - INVALID negative stock
        rows.add(List.of("Producto 2 Invalido", "MarcaTest", -5, 20000, "SKU-P05-" + batchSuffix + "-2", "Cat", "Tono", "Pres", 10000, 3, "Desc", "SI"));
        // Row 4 (third data row) - valid
        rows.add(List.of("Producto 3", "MarcaTest", 8, 20000, "SKU-P05-" + batchSuffix + "-3", "Cat", "Tono", "Pres", 10000, 3, "Desc", "SI"));
        // Row 5 (fourth data row) - valid
        rows.add(List.of("Producto 4", "MarcaTest", 12, 20000, "SKU-P05-" + batchSuffix + "-4", "Cat", "Tono", "Pres", 10000, 3, "Desc", "SI"));
        // Row 6 (fifth data row) - valid
        rows.add(List.of("Producto 5", "MarcaTest", 4, 20000, "SKU-P05-" + batchSuffix + "-5", "Cat", "Tono", "Pres", 10000, 3, "Desc", "SI"));

        byte[] excelBytes = createTestWorkbook(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "productos_error_row3.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
        );

        // 1. Preview reports error in row 3
        MvcResult previewResult = mockMvc.perform(multipart("/api/v1/admin/products/imports/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRows").value(5))
                .andExpect(jsonPath("$.data.validRows").value(4))
                .andExpect(jsonPath("$.data.rejectedRows").value(1))
                .andExpect(jsonPath("$.data.errors[0].rowNumber").value(3))
                .andExpect(jsonPath("$.data.errors[0].columnName").value("stock_inicial"))
                .andExpect(jsonPath("$.data.errors[0].errorMessage").value("El stock inicial no puede ser negativo"))
                .andReturn();

        JsonNode previewJson = objectMapper.readTree(previewResult.getResponse().getContentAsString());
        UUID jobId = UUID.fromString(previewJson.path("data").path("jobId").asText());

        // 2. Commit with default CANCEL_ON_ERROR fails with 400 Bad Request
        mockMvc.perform(post("/api/v1/admin/products/imports/{jobId}/commit", jobId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("VALIDATION_ERROR"));

        // 3. Rollback guarantees 0 products from this file are created
        for (int i = 1; i <= 5; i++) {
            String sku = "SKU-P05-" + batchSuffix + "-" + i;
            assertThat(productRepository.findBySku(sku)).isEmpty();
        }

        // 4. Test download rejected rows Excel
        MvcResult rejectedResult = mockMvc.perform(get("/api/v1/admin/products/imports/{jobId}/rejected.xlsx", jobId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(header().string(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rechazados_import_" + jobId + ".xlsx\""))
                .andReturn();

        byte[] rejectedBytes = rejectedResult.getResponse().getContentAsByteArray();
        try (XSSFWorkbook rejectedWb = new XSSFWorkbook(new ByteArrayInputStream(rejectedBytes))) {
            XSSFSheet sheet = rejectedWb.getSheet("ERRORES_IMPORTACION");
            assertThat(sheet).isNotNull();
            Row row1 = sheet.getRow(1);
            assertThat(row1.getCell(0).getStringCellValue()).isEqualTo("Producto 2 Invalido");
            assertThat(row1.getCell(12).getStringCellValue()).contains("El stock inicial no puede ser negativo");
        }
    }

    @Test
    @DisplayName("P06: Uploading/committing same file twice -> rejected as duplicate / idempotent, inventory is NOT duplicated")
    void p06_uploadAndCommitTwice_isIdempotentAndRejectsDuplicates() throws Exception {
        String batchSuffix = UUID.randomUUID().toString().substring(0, 6);
        List<List<Object>> rows = List.of(
                List.of("Producto P06", "MarcaP06", 15, 35000, "SKU-P06-" + batchSuffix, "Rostro", "Def", "50g", 20000, 2, "Desc", "SI")
        );

        byte[] excelBytes = createTestWorkbook(rows);
        MockMultipartFile file1 = new MockMultipartFile(
                "file",
                "productos_p06.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
        );

        // Upload and Preview
        MvcResult previewResult1 = mockMvc.perform(multipart("/api/v1/admin/products/imports/preview")
                        .file(file1)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDuplicateFile").value(false))
                .andReturn();

        UUID jobId1 = UUID.fromString(objectMapper.readTree(previewResult1.getResponse().getContentAsString())
                .path("data").path("jobId").asText());

        // Commit first time
        mockMvc.perform(post("/api/v1/admin/products/imports/{jobId}/commit", jobId1)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"));

        // 1. Committing the same job a second time throws 409 CONFLICT
        mockMvc.perform(post("/api/v1/admin/products/imports/{jobId}/commit", jobId1)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));

        // 2. Uploading the exact same file bytes again flags isDuplicateFile = true
        MockMultipartFile fileDuplicate = new MockMultipartFile(
                "file",
                "productos_p06_duplicate.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
        );

        mockMvc.perform(multipart("/api/v1/admin/products/imports/preview")
                        .file(fileDuplicate)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.isDuplicateFile").value(true));

        // 3. Verify stock is NOT duplicated
        Product product = productRepository.findBySku("SKU-P06-" + batchSuffix).orElseThrow();
        com.alexabeauty.catalog.domain.ProductVariant variant = productVariantRepository.findBySku("SKU-P06-" + batchSuffix).orElseThrow();
        assertThat(variant.getStockOnHand()).isEqualTo(15);

        List<InventoryMovement> movements = inventoryMovementRepository.findByVariantIdOrderByCreatedAtDesc(variant.getId());
        assertThat(movements).hasSize(1);
    }

    @Test
    @DisplayName("P07: Excel cell starting with =CMD('calc') or +SUM(A1) is sanitized by ExcelSanitizer")
    void p07_formulaInjectionSanitized() throws Exception {
        // Direct unit verification of ExcelSanitizer
        assertThat(ExcelSanitizer.isDangerous("=CMD('calc')")).isTrue();
        assertThat(ExcelSanitizer.isDangerous("+SUM(A1)")).isTrue();
        assertThat(ExcelSanitizer.isDangerous("-10% Descuento")).isTrue();
        assertThat(ExcelSanitizer.isDangerous("@admin")).isTrue();
        assertThat(ExcelSanitizer.isDangerous("Texto Normal")).isFalse();

        assertThat(ExcelSanitizer.sanitize("=CMD('calc')")).isEqualTo("'=CMD('calc')");
        assertThat(ExcelSanitizer.sanitize("+SUM(A1)")).isEqualTo("'+SUM(A1)");
        assertThat(ExcelSanitizer.sanitize("-10% Descuento")).isEqualTo("'-10% Descuento");
        assertThat(ExcelSanitizer.sanitize("@admin")).isEqualTo("'@admin");
        assertThat(ExcelSanitizer.sanitize("Labial Normal")).isEqualTo("Labial Normal");

        // End-to-end import verification with formula values
        String batchSuffix = UUID.randomUUID().toString().substring(0, 6);
        List<List<Object>> rows = List.of(
                List.of("=CMD('calc')", "+SUM(A1)", 5, 20000, "SKU-P07-" + batchSuffix, "Labios", "Rojo", "5ml", 10000, 2, "@SUM(A1..A10)", "SI")
        );

        byte[] excelBytes = createTestWorkbook(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "formula_test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
        );

        MvcResult previewResult = mockMvc.perform(multipart("/api/v1/admin/products/imports/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.validRows").value(1))
                .andReturn();

        UUID jobId = UUID.fromString(objectMapper.readTree(previewResult.getResponse().getContentAsString())
                .path("data").path("jobId").asText());

        mockMvc.perform(post("/api/v1/admin/products/imports/{jobId}/commit", jobId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        Product product = productRepository.findBySku("SKU-P07-" + batchSuffix).orElseThrow();
        assertThat(product.getName()).startsWith("'");
        assertThat(product.getName()).doesNotStartWith("=");
        com.alexabeauty.catalog.domain.Brand brand = brandRepository.findById(product.getBrand().getId()).orElseThrow();
        assertThat(brand.getName()).startsWith("'");
        assertThat(product.getDescription()).startsWith("'");
    }

    @Test
    @DisplayName("AB-AUD-02: Import V2 Excel with 1 product and 4 shades -> creates 1 product, 4 variants, and 4 ENTRADA_INICIAL Kardex movements")
    void v2_importOneProductWithFourShades_createsOneProductFourVariantsAndFourMovements() throws Exception {
        String batchSuffix = UUID.randomUUID().toString().substring(0, 6).toUpperCase(java.util.Locale.ROOT);
        String productCode = "PRD-V2-" + batchSuffix;

        List<List<Object>> rows = List.of(
                List.of(productCode, "Base Liquida Fit Me " + batchSuffix, "Maybelline", "Rostro", "SKU-V2-" + batchSuffix + "-110", "110", "Porcelain", "#F5E0D0", "30ml", 45000, 22000, 10, 3, "Base acabado mate", "SI"),
                List.of(productCode, "Base Liquida Fit Me " + batchSuffix, "Maybelline", "Rostro", "SKU-V2-" + batchSuffix + "-120", "120", "Classic Ivory", "#ECCBB0", "30ml", 45000, 22000, 15, 3, "Base acabado mate", "SI"),
                List.of(productCode, "Base Liquida Fit Me " + batchSuffix, "Maybelline", "Rostro", "SKU-V2-" + batchSuffix + "-220", "220", "Natural Beige", "#DFB28A", "30ml", 45000, 22000, 8, 3, "Base acabado mate", "SI"),
                List.of(productCode, "Base Liquida Fit Me " + batchSuffix, "Maybelline", "Rostro", "SKU-V2-" + batchSuffix + "-310", "310", "Sun Beige", "#C69065", "30ml", 45000, 22000, 12, 3, "Base acabado mate", "SI")
        );

        byte[] excelBytes = createTestWorkbookV2(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "fit_me_4_tonos.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
        );

        // 1. Preview
        MvcResult previewResult = mockMvc.perform(multipart("/api/v1/admin/products/imports/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.totalRows").value(4))
                .andExpect(jsonPath("$.data.validRows").value(4))
                .andExpect(jsonPath("$.data.rejectedRows").value(0))
                .andExpect(jsonPath("$.data.totalProducts").value(1))
                .andExpect(jsonPath("$.data.groupedProducts[0].productCode").value(productCode))
                .andExpect(jsonPath("$.data.groupedProducts[0].variantCount").value(4))
                .andExpect(jsonPath("$.data.groupedProducts[0].totalStock").value(45))
                .andReturn();

        UUID jobId = UUID.fromString(objectMapper.readTree(previewResult.getResponse().getContentAsString())
                .path("data").path("jobId").asText());

        // 2. Commit
        mockMvc.perform(post("/api/v1/admin/products/imports/{jobId}/commit", jobId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("COMPLETED"))
                .andExpect(jsonPath("$.data.importedProductsCount").value(1))
                .andExpect(jsonPath("$.data.totalInitialStockAdded").value(45));

        // 3. Database assertions
        Product product = productRepository.findBySku(productCode).orElseThrow();
        assertThat(product.getName()).isEqualTo("Base Liquida Fit Me " + batchSuffix);
        List<com.alexabeauty.catalog.domain.ProductVariant> variants = productVariantRepository.findByProductId(product.getId());
        assertThat(variants).hasSize(4);

        for (int i = 0; i < 4; i++) {
            com.alexabeauty.catalog.domain.ProductVariant variant = variants.get(i);
            assertThat(variant.getShadeCode()).isNotBlank();
            assertThat(variant.getShadeName()).isNotBlank();
            assertThat(variant.getShadeHex()).startsWith("#");
            assertThat(variant.getStockOnHand()).isGreaterThan(0);
        }

        // 4. Kardex movements: exactly 4 ENTRADA_INICIAL
        List<InventoryMovement> movements = inventoryMovementRepository.findAll().stream()
                .filter(m -> ("IMPORT_" + jobId).equals(m.getReference()))
                .toList();
        assertThat(movements).hasSize(4);
        int totalMovementStock = movements.stream().mapToInt(InventoryMovement::getQuantityDelta).sum();
        assertThat(totalMovementStock).isEqualTo(45);
    }

    @Test
    @DisplayName("AB-AUD-02: V2 scale test with 500 rows processes correctly in preview")
    void v2_scale500Rows_previewValidatesEfficiently() throws Exception {
        String batchSuffix = UUID.randomUUID().toString().substring(0, 6);
        List<List<Object>> rows = new ArrayList<>();

        // 50 products x 10 shades each = 500 rows
        for (int p = 1; p <= 50; p++) {
            String pCode = "PRD-SCALE-" + batchSuffix + "-" + p;
            for (int s = 1; s <= 10; s++) {
                String sku = "SKU-SCALE-" + batchSuffix + "-" + p + "-" + s;
                rows.add(List.of(
                        pCode,
                        "Producto Escala " + p,
                        "MarcaEscala",
                        "Maquillaje",
                        sku,
                        "T" + s,
                        "Tono " + s,
                        "#AABBCC",
                        "Unidad",
                        30000,
                        15000,
                        5,
                        2,
                        "Descripcion escala",
                        "SI"
                ));
            }
        }

        byte[] excelBytes = createTestWorkbookV2(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "scale_500.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
        );

        long start = System.currentTimeMillis();
        MvcResult previewResult = mockMvc.perform(multipart("/api/v1/admin/products/imports/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRows").value(500))
                .andExpect(jsonPath("$.data.validRows").value(500))
                .andExpect(jsonPath("$.data.totalProducts").value(50))
                .andReturn();
        long duration = System.currentTimeMillis() - start;
        assertThat(duration).isLessThan(15000); // Must complete within 15s
    }

    @Test
    @DisplayName("AB-AUD-02: V2 preview rejects missing producto_codigo and invalid tono_hex")
    void v2_invalidHexAndMissingProductCode_reportedInPreview() throws Exception {
        List<List<Object>> rows = List.of(
                // Missing product code
                List.of("", "Labial Sin Codigo", "Vogue", "Labios", "SKU-BAD-1", "01", "Rojo", "#FFFFFF", "4g", 20000, 10000, 5, 2, "Desc", "SI"),
                // Invalid hex color
                List.of("PRD-BAD-HEX", "Labial Color Raro", "Vogue", "Labios", "SKU-BAD-2", "02", "Azul", "NO-HEX", "4g", 20000, 10000, 5, 2, "Desc", "SI")
        );

        byte[] excelBytes = createTestWorkbookV2(rows);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "invalid_v2.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                excelBytes
        );

        mockMvc.perform(multipart("/api/v1/admin/products/imports/preview")
                        .file(file)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.totalRows").value(2))
                .andExpect(jsonPath("$.data.rejectedRows").value(2))
                .andExpect(jsonPath("$.data.errors[0].columnName").value("producto_codigo"))
                .andExpect(jsonPath("$.data.errors[1].columnName").value("tono_hex"));
    }

    private byte[] createTestWorkbook(List<List<Object>> rows) throws IOException {
        return createTestWorkbook(ExcelProductTemplateGenerator.HEADERS_V1, rows);
    }

    private byte[] createTestWorkbookV2(List<List<Object>> rows) throws IOException {
        return createTestWorkbook(ExcelProductTemplateGenerator.HEADERS, rows);
    }

    private byte[] createTestWorkbook(String[] headers, List<List<Object>> rows) throws IOException {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("PRODUCTOS");
            Row headerRow = sheet.createRow(0);

            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
            }

            for (int r = 0; r < rows.size(); r++) {
                Row row = sheet.createRow(r + 1);
                List<Object> rowData = rows.get(r);
                for (int c = 0; c < rowData.size(); c++) {
                    Object val = rowData.get(c);
                    Cell cell = row.createCell(c);
                    if (val instanceof Number n) {
                        cell.setCellValue(n.doubleValue());
                    } else if (val != null) {
                        cell.setCellValue(val.toString());
                    }
                }
            }

            workbook.write(out);
            return out.toByteArray();
        }
    }
}
