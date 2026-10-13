package com.alexabeauty.reporting.service;

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
import com.alexabeauty.catalog.util.SlugUtils;
import com.alexabeauty.common.error.ConflictException;
import com.alexabeauty.common.error.ResourceNotFoundException;
import com.alexabeauty.common.error.ValidationException;
import com.alexabeauty.inventory.domain.InventoryMovement;
import com.alexabeauty.inventory.domain.MovementType;
import com.alexabeauty.inventory.repository.InventoryMovementRepository;
import com.alexabeauty.reporting.domain.ExcelImportError;
import com.alexabeauty.reporting.domain.ExcelImportJob;
import com.alexabeauty.reporting.domain.ImportJobStatus;
import com.alexabeauty.reporting.dto.ConflictPolicy;
import com.alexabeauty.reporting.dto.ExcelCommitResponse;
import com.alexabeauty.reporting.dto.ExcelImportPreviewItemDto;
import com.alexabeauty.reporting.dto.ExcelImportPreviewResponse;
import com.alexabeauty.reporting.dto.ExcelRowErrorResponse;
import com.alexabeauty.reporting.excel.ExcelProductTemplateGenerator;
import com.alexabeauty.reporting.excel.ExcelSanitizer;
import com.alexabeauty.reporting.repository.ExcelImportErrorRepository;
import com.alexabeauty.reporting.repository.ExcelImportJobRepository;
import com.alexabeauty.security.UserPrincipal;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExcelImportServiceImpl implements ExcelImportService {

    private static final Logger log = LoggerFactory.getLogger(ExcelImportServiceImpl.class);
    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB

    private final ExcelProductTemplateGenerator templateGenerator;
    private final ExcelImportJobRepository jobRepository;
    private final ExcelImportErrorRepository errorRepository;
    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public ExcelImportServiceImpl(
            ExcelProductTemplateGenerator templateGenerator,
            ExcelImportJobRepository jobRepository,
            ExcelImportErrorRepository errorRepository,
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            InventoryMovementRepository inventoryMovementRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper
    ) {
        this.templateGenerator = templateGenerator;
        this.jobRepository = jobRepository;
        this.errorRepository = errorRepository;
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    public byte[] generateTemplate() {
        return templateGenerator.generateTemplate();
    }

    @Override
    @Transactional
    public ExcelImportPreviewResponse processPreview(MultipartFile file, UserPrincipal principal) {
        // 1. File validation
        if (file == null || file.isEmpty()) {
            throw new ValidationException("El archivo está vacío");
        }

        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".xlsx")) {
            throw new ValidationException("Solo se permiten archivos Excel con extensión .xlsx");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException("El archivo excede el tamaño máximo permitido de 5MB");
        }

        byte[] fileBytes;
        try {
            fileBytes = file.getBytes();
        } catch (IOException e) {
            throw new ValidationException("No se pudieron leer los bytes del archivo");
        }

        // 2. SHA-256 hash calculation
        String fileHash = calculateSha256(fileBytes);

        // 3. Duplicate completed file check
        boolean isDuplicateFile = jobRepository.existsByFileHashAndStatus(fileHash, ImportJobStatus.COMPLETED);

        // 4. Parse Excel PRODUCTOS sheet
        List<ExcelImportPreviewItemDto> validItems = new ArrayList<>();
        List<RejectedRowData> rejectedRowsData = new ArrayList<>();
        List<ExcelImportError> allErrors = new ArrayList<>();
        Set<String> fileSeenSkus = new HashSet<>();
        int duplicateSkuCount = 0;

        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(fileBytes))) {
            XSSFSheet sheet = workbook.getSheet("PRODUCTOS");
            if (sheet == null) {
                sheet = workbook.getSheet("productos");
            }
            if (sheet == null) {
                throw new ValidationException("La hoja 'PRODUCTOS' no fue encontrada en el archivo Excel");
            }

            Row headerRow = sheet.getRow(0);
            if (headerRow == null) {
                throw new ValidationException("El archivo no contiene encabezados en la fila 1 de la hoja PRODUCTOS");
            }

            Map<String, Integer> colMap = new HashMap<>();
            for (int c = 0; c < headerRow.getLastCellNum(); c++) {
                Cell cell = headerRow.getCell(c);
                if (cell != null) {
                    String val = cell.getStringCellValue();
                    if (val != null) {
                        colMap.put(val.trim().toLowerCase(Locale.ROOT), c);
                    }
                }
            }

            // Verify mandatory headers
            if (!colMap.containsKey("nombre") || !colMap.containsKey("marca")
                    || !colMap.containsKey("stock_inicial") || !colMap.containsKey("precio_venta_cop")) {
                throw new ValidationException("Faltan encabezados obligatorios en la hoja PRODUCTOS (nombre, marca, stock_inicial, precio_venta_cop)");
            }

            // Iterate rows
            for (int r = 1; r <= sheet.getLastRowNum(); r++) {
                Row row = sheet.getRow(r);
                if (row == null || isRowEmpty(row)) {
                    continue;
                }

                int rowNumber = r + 1; // 1-based index (row 1 is header)

                String nombreRaw = getCellString(row, colMap.get("nombre"));
                String marcaRaw = getCellString(row, colMap.get("marca"));
                String stockInicialRaw = getCellString(row, colMap.get("stock_inicial"));
                String precioVentaRaw = getCellString(row, colMap.get("precio_venta_cop"));
                String skuRaw = getCellString(row, colMap.get("sku"));
                String categoriaRaw = getCellString(row, colMap.get("categoria"));
                String tonoRaw = getCellString(row, colMap.get("tono"));
                String presentacionRaw = getCellString(row, colMap.get("presentacion"));
                String costoUnitarioRaw = getCellString(row, colMap.get("costo_unitario_cop"));
                String stockMinimoRaw = getCellString(row, colMap.get("stock_minimo"));
                String descripcionRaw = getCellString(row, colMap.get("descripcion"));
                String publicadoRaw = getCellString(row, colMap.get("publicado"));

                List<RowErrorInternal> rowErrors = new ArrayList<>();

                // Validation: nombre (mandatory, 2–150 chars)
                String nombre = null;
                if (nombreRaw == null || nombreRaw.isBlank()) {
                    rowErrors.add(new RowErrorInternal("nombre", nombreRaw, "El nombre es obligatorio", "Ingrese un nombre de producto entre 2 y 150 caracteres"));
                } else {
                    String trimmed = nombreRaw.trim();
                    if (trimmed.length() < 2 || trimmed.length() > 150) {
                        rowErrors.add(new RowErrorInternal("nombre", nombreRaw, "El nombre debe tener entre 2 y 150 caracteres", "Ajuste la longitud del nombre"));
                    } else {
                        nombre = ExcelSanitizer.sanitize(trimmed);
                    }
                }

                // Validation: marca (mandatory, 2–100 chars)
                String marca = null;
                if (marcaRaw == null || marcaRaw.isBlank()) {
                    rowErrors.add(new RowErrorInternal("marca", marcaRaw, "La marca es obligatoria", "Ingrese la marca del producto"));
                } else {
                    String trimmed = marcaRaw.trim();
                    if (trimmed.length() < 2 || trimmed.length() > 100) {
                        rowErrors.add(new RowErrorInternal("marca", marcaRaw, "La marca debe tener entre 2 y 100 caracteres", "Ajuste la longitud de la marca"));
                    } else {
                        marca = ExcelSanitizer.sanitize(trimmed);
                    }
                }

                // Validation: stock_inicial (mandatory integer >= 0)
                Integer stockInicial = null;
                if (stockInicialRaw == null || stockInicialRaw.isBlank()) {
                    rowErrors.add(new RowErrorInternal("stock_inicial", stockInicialRaw, "El stock inicial es obligatorio y debe ser un número entero mayor o igual a 0", "Ingrese un número entero >= 0"));
                } else {
                    try {
                        double d = Double.parseDouble(stockInicialRaw.trim().replace(",", "."));
                        if (d < 0) {
                            rowErrors.add(new RowErrorInternal("stock_inicial", stockInicialRaw, "El stock inicial no puede ser negativo", "Ingrese un número mayor o igual a 0"));
                        } else if (d != (long) d) {
                            rowErrors.add(new RowErrorInternal("stock_inicial", stockInicialRaw, "El stock inicial debe ser un número entero sin decimales", "Ingrese un número entero"));
                        } else {
                            stockInicial = (int) d;
                        }
                    } catch (NumberFormatException e) {
                        rowErrors.add(new RowErrorInternal("stock_inicial", stockInicialRaw, "El stock inicial debe ser un número entero válido", "Ingrese un entero válido"));
                    }
                }

                // Validation: precio_venta_cop (mandatory numeric >= 0)
                BigDecimal precioVenta = null;
                if (precioVentaRaw == null || precioVentaRaw.isBlank()) {
                    rowErrors.add(new RowErrorInternal("precio_venta_cop", precioVentaRaw, "El precio de venta es obligatorio y debe ser numérico mayor o igual a 0", "Ingrese el precio de venta"));
                } else {
                    try {
                        precioVenta = new BigDecimal(precioVentaRaw.trim().replace(",", "."));
                        if (precioVenta.compareTo(BigDecimal.ZERO) < 0) {
                            rowErrors.add(new RowErrorInternal("precio_venta_cop", precioVentaRaw, "El precio de venta no puede ser negativo", "Ingrese un valor numérico mayor o igual a 0"));
                        }
                    } catch (Exception e) {
                        rowErrors.add(new RowErrorInternal("precio_venta_cop", precioVentaRaw, "El precio de venta debe ser un número válido", "Ingrese un valor numérico"));
                    }
                }

                // Validation: sku (optional, check duplicates)
                String sku = null;
                if (skuRaw != null && !skuRaw.isBlank()) {
                    String cleanSku = ExcelSanitizer.sanitize(skuRaw.trim());
                    if (!fileSeenSkus.add(cleanSku)) {
                        duplicateSkuCount++;
                        rowErrors.add(new RowErrorInternal("sku", skuRaw, "SKU duplicado dentro del archivo Excel: " + cleanSku, "Asegúrese de que el SKU no se repita en el archivo"));
                    } else if (productVariantRepository.existsBySku(cleanSku) || productRepository.existsBySku(cleanSku)) {
                        duplicateSkuCount++;
                        rowErrors.add(new RowErrorInternal("sku", skuRaw, "El SKU ya existe en la base de datos: " + cleanSku, "Use un SKU no registrado o déjelo vacío para autogeneración"));
                    } else {
                        sku = cleanSku;
                    }
                }

                // Validation: costo_unitario_cop (optional numeric >= 0)
                BigDecimal costoUnitario = null;
                if (costoUnitarioRaw != null && !costoUnitarioRaw.isBlank()) {
                    try {
                        costoUnitario = new BigDecimal(costoUnitarioRaw.trim().replace(",", "."));
                        if (costoUnitario.compareTo(BigDecimal.ZERO) < 0) {
                            rowErrors.add(new RowErrorInternal("costo_unitario_cop", costoUnitarioRaw, "El costo unitario no puede ser negativo", "Ingrese un valor numérico mayor o igual a 0"));
                        }
                    } catch (Exception e) {
                        rowErrors.add(new RowErrorInternal("costo_unitario_cop", costoUnitarioRaw, "El costo unitario debe ser numérico", "Ingrese un número válido"));
                    }
                }

                // Validation: stock_minimo (optional integer >= 0, default 3)
                int stockMinimo = 3;
                if (stockMinimoRaw != null && !stockMinimoRaw.isBlank()) {
                    try {
                        double d = Double.parseDouble(stockMinimoRaw.trim().replace(",", "."));
                        if (d < 0) {
                            rowErrors.add(new RowErrorInternal("stock_minimo", stockMinimoRaw, "El stock mínimo no puede ser negativo", "Ingrese un valor mayor o igual a 0"));
                        } else {
                            stockMinimo = (int) d;
                        }
                    } catch (Exception e) {
                        rowErrors.add(new RowErrorInternal("stock_minimo", stockMinimoRaw, "El stock mínimo debe ser un número entero", "Ingrese un entero mayor o igual a 0"));
                    }
                }

                // Validation: publicado (optional SI / NO, default true)
                boolean publicado = true;
                if (publicadoRaw != null && !publicadoRaw.isBlank()) {
                    String pub = publicadoRaw.trim().toUpperCase(Locale.ROOT);
                    if (pub.equals("SI") || pub.equals("SÍ") || pub.equals("TRUE") || pub.equals("1") || pub.equals("YES")) {
                        publicado = true;
                    } else if (pub.equals("NO") || pub.equals("FALSE") || pub.equals("0")) {
                        publicado = false;
                    } else {
                        rowErrors.add(new RowErrorInternal("publicado", publicadoRaw, "Valor inválido para publicado: debe ser 'SI' o 'NO'", "Ingrese SI o NO"));
                    }
                }

                String categoria = categoriaRaw != null && !categoriaRaw.isBlank() ? ExcelSanitizer.sanitize(categoriaRaw.trim()) : null;
                String tono = tonoRaw != null && !tonoRaw.isBlank() ? ExcelSanitizer.sanitize(tonoRaw.trim()) : null;
                String presentacion = presentacionRaw != null && !presentacionRaw.isBlank() ? ExcelSanitizer.sanitize(presentacionRaw.trim()) : null;
                String descripcion = descripcionRaw != null && !descripcionRaw.isBlank() ? ExcelSanitizer.sanitize(descripcionRaw.trim()) : null;

                if (rowErrors.isEmpty()) {
                    validItems.add(new ExcelImportPreviewItemDto(
                            rowNumber,
                            nombre,
                            marca,
                            stockInicial,
                            precioVenta,
                            sku,
                            categoria,
                            tono,
                            presentacion,
                            costoUnitario,
                            stockMinimo,
                            descripcion,
                            publicado
                    ));
                } else {
                    String errorSummary = rowErrors.stream()
                            .map(re -> (re.columnName() != null ? re.columnName() + ": " : "") + re.errorMessage())
                            .collect(Collectors.joining("; "));

                    rejectedRowsData.add(new RejectedRowData(
                            rowNumber,
                            nombreRaw,
                            marcaRaw,
                            stockInicialRaw,
                            precioVentaRaw,
                            skuRaw,
                            categoriaRaw,
                            tonoRaw,
                            presentacionRaw,
                            costoUnitarioRaw,
                            stockMinimoRaw,
                            descripcionRaw,
                            publicadoRaw,
                            errorSummary
                    ));

                    for (RowErrorInternal re : rowErrors) {
                        allErrors.add(ExcelImportError.builder()
                                .rowNumber(rowNumber)
                                .columnName(re.columnName())
                                .rawValue(ExcelSanitizer.sanitize(re.rawValue()))
                                .errorMessage(re.errorMessage())
                                .suggestion(re.suggestion())
                                .build());
                    }
                }
            }
        } catch (IOException e) {
            throw new ValidationException("Error al procesar el archivo Excel: " + e.getMessage());
        }

        // 5. Persist ExcelImportJob & ExcelImportErrors
        User actor = null;
        if (principal != null && principal.getId() != null) {
            actor = userRepository.findById(principal.getId()).orElse(null);
        }

        ImportSummaryPayload payload = new ImportSummaryPayload(validItems, rejectedRowsData, duplicateSkuCount, isDuplicateFile);
        String jsonSummary;
        try {
            jsonSummary = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            throw new IllegalStateException("Error al serializar el diagnóstico de importación", e);
        }

        ExcelImportJob job = ExcelImportJob.builder()
                .fileName(originalFilename)
                .fileHash(fileHash)
                .user(actor)
                .templateVersion("ALEXA_PRODUCTS_V1")
                .totalRows(validItems.size() + rejectedRowsData.size())
                .validRows(validItems.size())
                .rejectedRows(rejectedRowsData.size())
                .status(ImportJobStatus.PREVIEW)
                .resultSummary(jsonSummary)
                .build();

        ExcelImportJob savedJob = jobRepository.save(job);

        for (ExcelImportError err : allErrors) {
            err.setJob(savedJob);
        }
        if (!allErrors.isEmpty()) {
            errorRepository.saveAll(allErrors);
        }

        // 6. Build response
        List<ExcelRowErrorResponse> errorDtos = allErrors.stream()
                .map(ExcelRowErrorResponse::from)
                .toList();

        List<ExcelImportPreviewItemDto> previewItems = validItems.stream()
                .limit(10)
                .toList();

        return new ExcelImportPreviewResponse(
                savedJob.getId(),
                originalFilename,
                fileHash,
                savedJob.getTotalRows(),
                savedJob.getValidRows(),
                savedJob.getRejectedRows(),
                duplicateSkuCount,
                isDuplicateFile,
                errorDtos,
                previewItems
        );
    }

    @Override
    @Transactional
    public ExcelCommitResponse commitImport(UUID jobId, ConflictPolicy policy, UserPrincipal principal) {
        ExcelImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ExcelImportJob", jobId));

        // IDEMPOTENCY / P06 MATRIX:
        if (job.getStatus() == ImportJobStatus.COMPLETED) {
            throw new ConflictException("Este archivo ya fue importado exitosamente.");
        }

        ConflictPolicy effectivePolicy = policy != null ? policy : ConflictPolicy.CANCEL_ON_ERROR;
        if (effectivePolicy == ConflictPolicy.CANCEL_ON_ERROR && job.getRejectedRows() > 0) {
            throw new ValidationException("No se puede importar: el archivo contiene filas con errores.");
        }

        ImportSummaryPayload payload;
        try {
            payload = objectMapper.readValue(job.getResultSummary(), ImportSummaryPayload.class);
        } catch (Exception e) {
            throw new IllegalStateException("Error al leer el contenido del trabajo de importación", e);
        }

        User actor = null;
        if (principal != null && principal.getId() != null) {
            actor = userRepository.findById(principal.getId()).orElse(job.getUser());
        } else {
            actor = job.getUser();
        }

        Map<String, Brand> brandCache = new HashMap<>();
        Map<String, Category> categoryCache = new HashMap<>();
        Set<String> usedSlugs = new HashSet<>();
        Set<String> usedSkus = new HashSet<>();

        int importedCount = 0;
        int totalInitialStockAdded = 0;

        for (ExcelImportPreviewItemDto item : payload.validRows()) {
            String itemSku = item.sku();
            if (itemSku != null && !itemSku.isBlank()) {
                if (effectivePolicy == ConflictPolicy.SKIP_DUPLICATES) {
                    if (productRepository.existsBySku(itemSku) || productVariantRepository.existsBySku(itemSku) || usedSkus.contains(itemSku)) {
                        log.warn("Skipping duplicate SKU {} under policy SKIP_DUPLICATES", itemSku);
                        continue;
                    }
                }
            }

            // Find or create Brand
            String brandName = item.marca().trim();
            String brandSlug = SlugUtils.toSlug(brandName);
            Brand brand = brandCache.computeIfAbsent(brandSlug, slug ->
                    brandRepository.findBySlug(slug).orElseGet(() ->
                            brandRepository.save(Brand.builder()
                                    .name(brandName)
                                    .slug(slug)
                                    .active(true)
                                    .build())
                    )
            );

            // Find or create Category
            String catName = item.categoria() != null && !item.categoria().isBlank() ? item.categoria().trim() : "General";
            String catSlug = SlugUtils.toSlug(catName);
            Category category = categoryCache.computeIfAbsent(catSlug, slug ->
                    categoryRepository.findBySlug(slug).orElseGet(() ->
                            categoryRepository.save(Category.builder()
                                    .name(catName)
                                    .slug(slug)
                                    .sortOrder(0)
                                    .active(true)
                                    .build())
                    )
            );

            // Generate unique SKU if empty
            String sku = itemSku;
            if (sku == null || sku.isBlank()) {
                String cleanSlug = brand.getSlug().toUpperCase(Locale.ROOT).replaceAll("[^A-Z0-9]", "");
                String brandCode = cleanSlug.length() >= 3 ? cleanSlug.substring(0, 3) : (cleanSlug.isEmpty() ? "GEN" : cleanSlug);
                do {
                    sku = String.format("AB-%s-%s", brandCode, UUID.randomUUID().toString().substring(0, 8).toUpperCase(Locale.ROOT));
                } while (productRepository.existsBySku(sku) || productVariantRepository.existsBySku(sku) || usedSkus.contains(sku));
            }
            usedSkus.add(sku);

            // Generate unique product slug
            String productSlug = SlugUtils.generateUniqueSlug(item.nombre(), s -> productRepository.existsBySlug(s) || usedSlugs.contains(s));
            usedSlugs.add(productSlug);

            // Create Product
            Product product = Product.builder()
                    .sku(sku)
                    .name(item.nombre().trim())
                    .slug(productSlug)
                    .brand(brand)
                    .category(category)
                    .description(item.descripcion())
                    .active(item.publicado())
                    .featured(false)
                    .variants(new ArrayList<>())
                    .images(new ArrayList<>())
                    .build();

            // Create ProductVariant
            String shade = item.tono() != null && !item.tono().isBlank() ? item.tono().trim() : "Default";
            ProductVariant variant = ProductVariant.builder()
                    .product(product)
                    .sku(sku)
                    .shadeName(shade)
                    .presentation(item.presentacion())
                    .salePrice(item.precioVentaCop())
                    .referenceCost(item.costoUnitarioCop())
                    .stockOnHand(item.stockInicial())
                    .stockReserved(0)
                    .minStock(item.stockMinimo() > 0 ? item.stockMinimo() : 3)
                    .active(item.publicado())
                    .build();
            product.getVariants().add(variant);

            Product savedProduct = productRepository.save(product);
            ProductVariant savedVariant = savedProduct.getVariants().get(0);

            // KARDEX INITIAL MOVEMENT: If stock_inicial > 0
            if (item.stockInicial() > 0) {
                InventoryMovement movement = InventoryMovement.builder()
                        .variant(savedVariant)
                        .movementType(MovementType.ENTRADA_INICIAL)
                        .quantityDelta(item.stockInicial())
                        .qtyBefore(0)
                        .qtyAfter(item.stockInicial())
                        .reference("IMPORT_" + job.getId())
                        .reason("Importación masiva Excel")
                        .user(actor)
                        .build();
                inventoryMovementRepository.save(movement);
                totalInitialStockAdded += item.stockInicial();
            }

            importedCount++;
        }

        job.setStatus(ImportJobStatus.COMPLETED);
        job.setFinishedAt(Instant.now());
        jobRepository.save(job);

        return new ExcelCommitResponse(job.getId(), importedCount, totalInitialStockAdded, "COMPLETED");
    }

    @Override
    public byte[] generateRejectedRowsExcel(UUID jobId) {
        ExcelImportJob job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("ExcelImportJob", jobId));

        ImportSummaryPayload payload;
        try {
            payload = objectMapper.readValue(job.getResultSummary(), ImportSummaryPayload.class);
        } catch (Exception e) {
            throw new IllegalStateException("Error al leer resumen de filas rechazadas", e);
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFSheet sheet = workbook.createSheet("ERRORES_IMPORTACION");

            String[] headers = {
                    "nombre", "marca", "stock_inicial", "precio_venta_cop", "sku",
                    "categoria", "tono", "presentacion", "costo_unitario_cop",
                    "stock_minimo", "descripcion", "publicado", "ERROR_MOTIVO"
            };

            XSSFCellStyle headerStyle = workbook.createCellStyle();
            byte[] wineRgb = new byte[]{(byte) 0x71, (byte) 0x1A, (byte) 0x38};
            headerStyle.setFillForegroundColor(new XSSFColor(wineRgb, new DefaultIndexedColorMap()));
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);

            XSSFFont font = workbook.createFont();
            font.setBold(true);
            font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, new DefaultIndexedColorMap()));
            font.setFontHeightInPoints((short) 11);
            headerStyle.setFont(font);

            Row headerRow = sheet.createRow(0);
            headerRow.setHeightInPoints(28);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            int rIndex = 1;
            for (RejectedRowData rr : payload.rejectedRows()) {
                Row row = sheet.createRow(rIndex++);
                row.setHeightInPoints(20);
                row.createCell(0).setCellValue(ExcelSanitizer.sanitize(rr.nombre()));
                row.createCell(1).setCellValue(ExcelSanitizer.sanitize(rr.marca()));
                row.createCell(2).setCellValue(ExcelSanitizer.sanitize(rr.stockInicial()));
                row.createCell(3).setCellValue(ExcelSanitizer.sanitize(rr.precioVentaCop()));
                row.createCell(4).setCellValue(ExcelSanitizer.sanitize(rr.sku()));
                row.createCell(5).setCellValue(ExcelSanitizer.sanitize(rr.categoria()));
                row.createCell(6).setCellValue(ExcelSanitizer.sanitize(rr.tono()));
                row.createCell(7).setCellValue(ExcelSanitizer.sanitize(rr.presentacion()));
                row.createCell(8).setCellValue(ExcelSanitizer.sanitize(rr.costoUnitarioCop()));
                row.createCell(9).setCellValue(ExcelSanitizer.sanitize(rr.stockMinimo()));
                row.createCell(10).setCellValue(ExcelSanitizer.sanitize(rr.descripcion()));
                row.createCell(11).setCellValue(ExcelSanitizer.sanitize(rr.publicado()));
                row.createCell(12).setCellValue(ExcelSanitizer.sanitize(rr.errorMotivo()));
            }

            sheet.createFreezePane(0, 1);
            sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, headers.length - 1));

            for (int i = 0; i < headers.length; i++) {
                sheet.setColumnWidth(i, i == 12 ? 16000 : 5200);
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error al exportar filas rechazadas en formato Excel", e);
        }
    }

    private String getCellString(Row row, Integer colIndex) {
        if (colIndex == null || row == null) {
            return null;
        }
        Cell cell = row.getCell(colIndex);
        if (cell == null) {
            return null;
        }
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            try {
                return "=" + cell.getCellFormula();
            } catch (Exception e) {
                type = cell.getCachedFormulaResultType();
            }
        }
        return switch (type) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> {
                if (DateUtil.isCellDateFormatted(cell)) {
                    yield cell.getDateCellValue().toString();
                }
                double num = cell.getNumericCellValue();
                if (num == (long) num) {
                    yield String.valueOf((long) num);
                } else {
                    yield BigDecimal.valueOf(num).stripTrailingZeros().toPlainString();
                }
            }
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            case BLANK -> "";
            default -> "";
        };
    }

    private boolean isRowEmpty(Row row) {
        if (row == null) {
            return true;
        }
        for (int c = row.getFirstCellNum(); c < row.getLastCellNum(); c++) {
            Cell cell = row.getCell(c);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellString(row, c);
                if (val != null && !val.isBlank()) {
                    return false;
                }
            }
        }
        return true;
    }

    private String calculateSha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data);
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                hexString.append(String.format("%02x", b));
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record RejectedRowData(
            int rowNumber,
            String nombre,
            String marca,
            String stockInicial,
            String precioVentaCop,
            String sku,
            String categoria,
            String tono,
            String presentacion,
            String costoUnitarioCop,
            String stockMinimo,
            String descripcion,
            String publicado,
            String errorMotivo
    ) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ImportSummaryPayload(
            List<ExcelImportPreviewItemDto> validRows,
            List<RejectedRowData> rejectedRows,
            int duplicateSkuCount,
            boolean isDuplicateFile
    ) {}

    private record RowErrorInternal(
            String columnName,
            String rawValue,
            String errorMessage,
            String suggestion
    ) {}
}
