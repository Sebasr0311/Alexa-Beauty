package com.alexabeauty.reporting.excel;

import com.alexabeauty.catalog.domain.ProductVariant;
import com.alexabeauty.security.UserPrincipal;
import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.VerticalAlignment;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.DefaultIndexedColorMap;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFDataFormat;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Generates the official Excel Inventory & Product Catalog Report (ALEXA_INVENTORY_V1)
 * with complete purchase investment costs, retail prices, and stock counts.
 */
@Component
public class ExcelInventoryReportGenerator {

    public static final String REPORT_VERSION = "ALEXA_INVENTORY_V1";
    public static final String TIMEZONE = "America/Bogota";
    private static final ZoneId BOGOTA_ZONE = ZoneId.of(TIMEZONE);
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private static final byte[] WINE_RGB = new byte[]{(byte) 0x71, (byte) 0x1A, (byte) 0x38}; // #711A38
    private static final byte[] LIGHT_ROSE_RGB = new byte[]{(byte) 0xF5, (byte) 0xE5, (byte) 0xE8}; // #F5E5E8
    private static final byte[] ZEBRA_ROSE_RGB = new byte[]{(byte) 0xFC, (byte) 0xF7, (byte) 0xF8}; // #FCF7F8
    private static final byte[] DARK_WINE_RGB = new byte[]{(byte) 0x4C, (byte) 0x11, (byte) 0x26}; // #4C1126
    private static final byte[] WHITE_RGB = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

    public byte[] generateInventoryReport(List<ProductVariant> variants, UserPrincipal actor) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Styles styles = createStyles(workbook);

            createInventarioSheet(workbook, variants, actor, styles);
            createResumenSheet(workbook, variants, actor, styles);

            workbook.setForceFormulaRecalculation(true);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error al generar reporte Excel de inventario", e);
        }
    }

    private void createInventarioSheet(XSSFWorkbook workbook, List<ProductVariant> variants, UserPrincipal actor, Styles styles) {
        XSSFSheet sheet = workbook.createSheet("INVENTARIO_PRODUCTOS");
        sheet.setDisplayGridlines(true);
        int numCols = 17;

        createBannerRows(sheet, numCols - 1, "CONTROL DE INVENTARIO Y CATÁLOGO DE PRODUCTOS", styles);

        // Header Row 5 (index 4)
        Row headerRow = sheet.createRow(4);
        headerRow.setHeightInPoints(34);
        String[] headers = {
                "SKU", "Producto", "Marca", "Categoría", "Tono / Presentación",
                "Stock Físico", "Stock Reservado", "Stock Disponible", "Stock Mínimo",
                "Costo Inversión COP", "Precio Venta COP", "Margen Unit. COP", "Margen %",
                "Inversión Total Stock COP", "Valor Venta Stock COP", "Estado", "Alerta Stock"
        };
        for (int c = 0; c < headers.length; c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(styles.columnHeaderStyle);
        }

        sheet.createFreezePane(0, 5);

        int rowIdx = 5;
        BigDecimal totalInvestmentSum = BigDecimal.ZERO;
        BigDecimal totalRetailSum = BigDecimal.ZERO;
        int totalPhysicalSum = 0;
        int totalReservedSum = 0;
        int totalAvailableSum = 0;

        for (int idx = 0; idx < variants.size(); idx++) {
            ProductVariant v = variants.get(idx);
            boolean isAlt = idx % 2 == 1;
            Row row = sheet.createRow(rowIdx++);
            row.setHeightInPoints(22);

            int excelRow = rowIdx; // 1-based row number
            XSSFCellStyle txtStyle = isAlt ? styles.zebraTextStyle : styles.textStyle;
            XSSFCellStyle txtCenterStyle = isAlt ? styles.zebraTextCenterStyle : styles.textCenterStyle;
            XSSFCellStyle intCenterStyle = isAlt ? styles.zebraIntegerCenterStyle : styles.integerCenterStyle;
            XSSFCellStyle currStyle = isAlt ? styles.zebraCurrencyStyle : styles.currencyStyle;
            XSSFCellStyle pctStyle = isAlt ? styles.zebraPercentStyle : styles.percentStyle;

            int physical = v.getStockOnHand();
            int reserved = v.getStockReserved();
            int available = Math.max(0, physical - reserved);
            int minStock = v.getMinStock();

            totalPhysicalSum += physical;
            totalReservedSum += reserved;
            totalAvailableSum += available;

            BigDecimal cost = v.getReferenceCost() != null ? v.getReferenceCost() : BigDecimal.ZERO;
            BigDecimal price = v.getSalePrice() != null ? v.getSalePrice() : BigDecimal.ZERO;
            BigDecimal unitMargin = price.subtract(cost);
            double marginPct = price.signum() > 0
                    ? unitMargin.divide(price, 4, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;

            BigDecimal lineInvestment = cost.multiply(BigDecimal.valueOf(physical));
            BigDecimal lineRetail = price.multiply(BigDecimal.valueOf(physical));
            totalInvestmentSum = totalInvestmentSum.add(lineInvestment);
            totalRetailSum = totalRetailSum.add(lineRetail);

            String productName = (v.getProduct() != null) ? v.getProduct().getName() : "";
            String brandName = (v.getProduct() != null && v.getProduct().getBrand() != null)
                    ? v.getProduct().getBrand().getName() : "Alexa Beauty";
            String categoryName = (v.getProduct() != null && v.getProduct().getCategory() != null)
                    ? v.getProduct().getCategory().getName() : "General";

            String shade = v.getShadeName() != null && !v.getShadeName().trim().equalsIgnoreCase("Default")
                    ? v.getShadeName().trim()
                    : (v.getPresentation() != null ? v.getPresentation() : "Tono único");

            // Col 0: SKU
            Cell c0 = row.createCell(0);
            c0.setCellValue(v.getSku() != null ? v.getSku() : "");
            c0.setCellStyle(txtStyle);

            // Col 1: Producto
            Cell c1 = row.createCell(1);
            c1.setCellValue(productName);
            c1.setCellStyle(txtStyle);

            // Col 2: Marca
            Cell c2 = row.createCell(2);
            c2.setCellValue(brandName);
            c2.setCellStyle(txtStyle);

            // Col 3: Categoría
            Cell c3 = row.createCell(3);
            c3.setCellValue(categoryName);
            c3.setCellStyle(txtStyle);

            // Col 4: Tono / Presentación
            Cell c4 = row.createCell(4);
            c4.setCellValue(shade);
            c4.setCellStyle(txtStyle);

            // Col 5: Stock Físico (F)
            Cell c5 = row.createCell(5);
            c5.setCellValue(physical);
            c5.setCellStyle(intCenterStyle);

            // Col 6: Stock Reservado (G)
            Cell c6 = row.createCell(6);
            c6.setCellValue(reserved);
            c6.setCellStyle(intCenterStyle);

            // Col 7: Stock Disponible (H)
            Cell c7 = row.createCell(7);
            c7.setCellValue(available);
            c7.setCellStyle(intCenterStyle);

            // Col 8: Stock Mínimo (I)
            Cell c8 = row.createCell(8);
            c8.setCellValue(minStock);
            c8.setCellStyle(intCenterStyle);

            // Col 9: Costo Inversión COP (J)
            Cell c9 = row.createCell(9);
            c9.setCellValue(cost.doubleValue());
            c9.setCellStyle(currStyle);

            // Col 10: Precio Venta COP (K)
            Cell c10 = row.createCell(10);
            c10.setCellValue(price.doubleValue());
            c10.setCellStyle(currStyle);

            // Col 11: Margen Unitario COP (L) = K - J
            Cell c11 = row.createCell(11);
            c11.setCellFormula(String.format("K%d-J%d", excelRow, excelRow));
            c11.setCellValue(unitMargin.doubleValue());
            c11.setCellStyle(currStyle);

            // Col 12: Margen % (M) = IF(K>0, L/K, 0)
            Cell c12 = row.createCell(12);
            c12.setCellFormula(String.format("IF(K%d>0, L%d/K%d, 0)", excelRow, excelRow, excelRow));
            c12.setCellValue(marginPct);
            c12.setCellStyle(pctStyle);

            // Col 13: Inversión Total Stock COP (N) = F * J
            Cell c13 = row.createCell(13);
            c13.setCellFormula(String.format("F%d*J%d", excelRow, excelRow));
            c13.setCellValue(lineInvestment.doubleValue());
            c13.setCellStyle(currStyle);

            // Col 14: Valor Venta Stock COP (O) = F * K
            Cell c14 = row.createCell(14);
            c14.setCellFormula(String.format("F%d*K%d", excelRow, excelRow));
            c14.setCellValue(lineRetail.doubleValue());
            c14.setCellStyle(currStyle);

            // Col 15: Estado
            Cell c15 = row.createCell(15);
            c15.setCellValue(v.isActive() ? "ACTIVO" : "SUSPENDIDO");
            c15.setCellStyle(txtCenterStyle);

            // Col 16: Alerta Stock
            Cell c16 = row.createCell(16);
            String alertLabel = available <= 0
                    ? "AGOTADO"
                    : (available <= minStock ? "STOCK BAJO" : "NORMAL");
            c16.setCellValue(alertLabel);
            c16.setCellStyle(txtCenterStyle);
        }

        int lastDataRowNum = variants.isEmpty() ? 5 : (5 + variants.size());
        sheet.setAutoFilter(new CellRangeAddress(4, Math.max(4, lastDataRowNum - 1), 0, numCols - 1));

        // Spacer row
        sheet.createRow(rowIdx++);

        // Total row
        Row totalRow = sheet.createRow(rowIdx);
        totalRow.setHeightInPoints(26);
        for (int c = 0; c < numCols; c++) {
            Cell cell = totalRow.createCell(c);
            cell.setCellStyle(styles.totalBarTextStyle);
        }

        Cell totalLabelA = totalRow.getCell(0);
        totalLabelA.setCellValue("TOTALES");
        totalLabelA.setCellStyle(styles.totalBarCenterStyle);

        // F: Stock Físico
        Cell totalF = totalRow.getCell(5);
        if (!variants.isEmpty()) {
            totalF.setCellFormula(String.format("SUM(F6:F%d)", lastDataRowNum));
        }
        totalF.setCellValue(totalPhysicalSum);
        totalF.setCellStyle(styles.totalBarIntegerCenterStyle);

        // G: Stock Reservado
        Cell totalG = totalRow.getCell(6);
        if (!variants.isEmpty()) {
            totalG.setCellFormula(String.format("SUM(G6:G%d)", lastDataRowNum));
        }
        totalG.setCellValue(totalReservedSum);
        totalG.setCellStyle(styles.totalBarIntegerCenterStyle);

        // H: Stock Disponible
        Cell totalH = totalRow.getCell(7);
        if (!variants.isEmpty()) {
            totalH.setCellFormula(String.format("SUM(H6:H%d)", lastDataRowNum));
        }
        totalH.setCellValue(totalAvailableSum);
        totalH.setCellStyle(styles.totalBarIntegerCenterStyle);

        // N: Inversión Total Stock
        Cell totalN = totalRow.getCell(13);
        if (!variants.isEmpty()) {
            totalN.setCellFormula(String.format("SUM(N6:N%d)", lastDataRowNum));
        }
        totalN.setCellValue(totalInvestmentSum.doubleValue());
        totalN.setCellStyle(styles.totalBarCurrencyStyle);

        // O: Valor Venta Total Stock
        Cell totalO = totalRow.getCell(14);
        if (!variants.isEmpty()) {
            totalO.setCellFormula(String.format("SUM(O6:O%d)", lastDataRowNum));
        }
        totalO.setCellValue(totalRetailSum.doubleValue());
        totalO.setCellStyle(styles.totalBarCurrencyStyle);

        int[] widths = {
                5400, 8500, 5200, 5200, 5800,
                3800, 3800, 3800, 3400,
                5800, 5800, 5500, 4200,
                6800, 6800, 4200, 4200
        };
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i]);
        }
    }

    private void createResumenSheet(XSSFWorkbook workbook, List<ProductVariant> variants, UserPrincipal actor, Styles styles) {
        XSSFSheet sheet = workbook.createSheet("RESUMEN_EJECUTIVO");
        sheet.setDisplayGridlines(true);
        int numCols = 2;

        createBannerRows(sheet, numCols - 1, "RESUMEN CONSOLIDADO DE INVENTARIO", styles);

        Row headerRow = sheet.createRow(4);
        headerRow.setHeightInPoints(29);

        Cell h0 = headerRow.createCell(0);
        h0.setCellValue("MÉTRICA / INDICADOR");
        h0.setCellStyle(styles.columnHeaderStyle);

        Cell h1 = headerRow.createCell(1);
        h1.setCellValue("VALOR");
        h1.setCellStyle(styles.columnHeaderStyle);

        sheet.createFreezePane(0, 5);

        int totalPhysical = 0;
        int totalReserved = 0;
        int totalAvailable = 0;
        int lowStockCount = 0;
        int activeCount = 0;
        BigDecimal totalInvestment = BigDecimal.ZERO;
        BigDecimal totalRetail = BigDecimal.ZERO;

        for (ProductVariant v : variants) {
            int phys = v.getStockOnHand();
            int res = v.getStockReserved();
            int avail = Math.max(0, phys - res);
            totalPhysical += phys;
            totalReserved += res;
            totalAvailable += avail;
            if (avail <= v.getMinStock()) lowStockCount++;
            if (v.isActive()) activeCount++;

            BigDecimal cost = v.getReferenceCost() != null ? v.getReferenceCost() : BigDecimal.ZERO;
            BigDecimal price = v.getSalePrice() != null ? v.getSalePrice() : BigDecimal.ZERO;
            totalInvestment = totalInvestment.add(cost.multiply(BigDecimal.valueOf(phys)));
            totalRetail = totalRetail.add(price.multiply(BigDecimal.valueOf(phys)));
        }

        BigDecimal potentialGrossProfit = totalRetail.subtract(totalInvestment);
        double overallMargin = totalRetail.signum() > 0
                ? potentialGrossProfit.divide(totalRetail, 4, RoundingMode.HALF_UP).doubleValue() * 100
                : 0.0;

        String[] labels = {
                "Total referencias / variantes",
                "Variantes activas para venta",
                "Unidades en stock físico (bodega)",
                "Unidades reservadas (pedidos pendientes)",
                "Unidades disponibles para venta",
                "Variantes en alerta (stock bajo o agotadas)",
                "VALOR TOTAL DE INVERSIÓN EN INVENTARIO (COP)",
                "VALOR COMERCIAL ESTIMADO EN INVENTARIO (COP)",
                "UTILIDAD BRUTA POTENCIAL TOTAL (COP)",
                "MARGEN BRUTO POTENCIAL (%)",
                "Fecha y hora de corte (Colombia)",
                "Zona horaria oficial",
                "Generado por"
        };

        String actorName = actor != null ? (actor.getFullName() != null && !actor.getFullName().isBlank() ? actor.getFullName() : actor.getUsername()) : "Sistema";
        String nowStr = Instant.now().atZone(BOGOTA_ZONE).format(DATE_TIME_FORMATTER);

        for (int i = 0; i < labels.length; i++) {
            Row row = sheet.createRow(5 + i);
            row.setHeightInPoints(26);

            Cell cLabel = row.createCell(0);
            cLabel.setCellValue(labels[i]);
            cLabel.setCellStyle((i >= 6 && i <= 8) ? styles.totalBarTextStyle : styles.boldTextStyle);

            Cell cVal = row.createCell(1);
            switch (i) {
                case 0 -> {
                    cVal.setCellValue(variants.size());
                    cVal.setCellStyle(styles.boldIntegerStyle);
                }
                case 1 -> {
                    cVal.setCellValue(activeCount);
                    cVal.setCellStyle(styles.boldIntegerStyle);
                }
                case 2 -> {
                    cVal.setCellValue(totalPhysical);
                    cVal.setCellStyle(styles.boldIntegerStyle);
                }
                case 3 -> {
                    cVal.setCellValue(totalReserved);
                    cVal.setCellStyle(styles.boldIntegerStyle);
                }
                case 4 -> {
                    cVal.setCellValue(totalAvailable);
                    cVal.setCellStyle(styles.boldIntegerStyle);
                }
                case 5 -> {
                    cVal.setCellValue(lowStockCount);
                    cVal.setCellStyle(styles.boldIntegerStyle);
                }
                case 6 -> {
                    cVal.setCellValue(totalInvestment.doubleValue());
                    cVal.setCellStyle(styles.totalBarCurrencyStyle);
                }
                case 7 -> {
                    cVal.setCellValue(totalRetail.doubleValue());
                    cVal.setCellStyle(styles.totalBarCurrencyStyle);
                }
                case 8 -> {
                    cVal.setCellValue(potentialGrossProfit.doubleValue());
                    cVal.setCellStyle(styles.totalBarCurrencyStyle);
                }
                case 9 -> {
                    cVal.setCellValue(String.format("%.2f%%", overallMargin));
                    cVal.setCellStyle(styles.boldTextRightStyle);
                }
                case 10 -> {
                    cVal.setCellValue(nowStr);
                    cVal.setCellStyle(styles.textStyle);
                }
                case 11 -> {
                    cVal.setCellValue(TIMEZONE);
                    cVal.setCellStyle(styles.textStyle);
                }
                case 12 -> {
                    cVal.setCellValue(actorName);
                    cVal.setCellStyle(styles.textStyle);
                }
            }
        }

        sheet.setColumnWidth(0, 14000);
        sheet.setColumnWidth(1, 10000);
    }

    private void createBannerRows(XSSFSheet sheet, int lastColIdx, String title, Styles styles) {
        // Row 1: Brand title
        Row r0 = sheet.createRow(0);
        r0.setHeightInPoints(34);
        Cell c00 = r0.createCell(0);
        c00.setCellValue("ALEXA BEAUTY");
        c00.setCellStyle(styles.bannerTitleStyle);
        for (int c = 1; c <= lastColIdx; c++) {
            r0.createCell(c).setCellStyle(styles.bannerTitleStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(0, 0, 0, lastColIdx));

        // Row 2: Subtitle
        Row r1 = sheet.createRow(1);
        r1.setHeightInPoints(24);
        Cell c10 = r1.createCell(0);
        c10.setCellValue(title + " — " + REPORT_VERSION);
        c10.setCellStyle(styles.bannerSubtitleStyle);
        for (int c = 1; c <= lastColIdx; c++) {
            r1.createCell(c).setCellStyle(styles.bannerSubtitleStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(1, 1, 0, lastColIdx));

        // Row 3: Generation info
        Row r2 = sheet.createRow(2);
        r2.setHeightInPoints(20);
        Cell c20 = r2.createCell(0);
        String metaText = String.format("Corte generado el %s (%s) · Moneda: COP · Valores de inversión según costo de adquisición",
                Instant.now().atZone(BOGOTA_ZONE).format(DATE_TIME_FORMATTER), TIMEZONE);
        c20.setCellValue(metaText);
        c20.setCellStyle(styles.bannerMetaStyle);
        for (int c = 1; c <= lastColIdx; c++) {
            r2.createCell(c).setCellStyle(styles.bannerMetaStyle);
        }
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, lastColIdx));

        // Row 4: Empty spacer
        sheet.createRow(3).setHeightInPoints(10);
    }

    private Styles createStyles(XSSFWorkbook workbook) {
        DefaultIndexedColorMap colorMap = new DefaultIndexedColorMap();
        XSSFColor wine = new XSSFColor(WINE_RGB, colorMap);
        XSSFColor lightRose = new XSSFColor(LIGHT_ROSE_RGB, colorMap);
        XSSFColor zebraRose = new XSSFColor(ZEBRA_ROSE_RGB, colorMap);
        XSSFColor darkWine = new XSSFColor(DARK_WINE_RGB, colorMap);
        XSSFColor white = new XSSFColor(WHITE_RGB, colorMap);

        XSSFDataFormat df = workbook.createDataFormat();
        short currencyFormat = df.getFormat("$#,##0");
        short integerFormat = df.getFormat("#,##0");
        short percentFormat = df.getFormat("0.0%");

        XSSFFont fontBannerTitle = workbook.createFont();
        fontBannerTitle.setFontName("Calibri");
        fontBannerTitle.setFontHeightInPoints((short) 16);
        fontBannerTitle.setBold(true);
        fontBannerTitle.setColor(white);

        XSSFFont fontBannerSubtitle = workbook.createFont();
        fontBannerSubtitle.setFontName("Calibri");
        fontBannerSubtitle.setFontHeightInPoints((short) 11);
        fontBannerSubtitle.setBold(true);
        fontBannerSubtitle.setColor(white);

        XSSFFont fontBannerMeta = workbook.createFont();
        fontBannerMeta.setFontName("Calibri");
        fontBannerMeta.setFontHeightInPoints((short) 9);
        fontBannerMeta.setItalic(true);
        fontBannerMeta.setColor(lightRose);

        XSSFFont fontHeader = workbook.createFont();
        fontHeader.setFontName("Calibri");
        fontHeader.setFontHeightInPoints((short) 10);
        fontHeader.setBold(true);
        fontHeader.setColor(white);

        XSSFFont fontRegular = workbook.createFont();
        fontRegular.setFontName("Calibri");
        fontRegular.setFontHeightInPoints((short) 10);

        XSSFFont fontBold = workbook.createFont();
        fontBold.setFontName("Calibri");
        fontBold.setFontHeightInPoints((short) 10);
        fontBold.setBold(true);

        XSSFFont fontTotal = workbook.createFont();
        fontTotal.setFontName("Calibri");
        fontTotal.setFontHeightInPoints((short) 11);
        fontTotal.setBold(true);
        fontTotal.setColor(white);

        Styles s = new Styles();

        // Banner Title
        s.bannerTitleStyle = workbook.createCellStyle();
        s.bannerTitleStyle.setFont(fontBannerTitle);
        s.bannerTitleStyle.setFillForegroundColor(wine);
        s.bannerTitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.bannerTitleStyle.setAlignment(HorizontalAlignment.CENTER);
        s.bannerTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Banner Subtitle
        s.bannerSubtitleStyle = workbook.createCellStyle();
        s.bannerSubtitleStyle.setFont(fontBannerSubtitle);
        s.bannerSubtitleStyle.setFillForegroundColor(wine);
        s.bannerSubtitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.bannerSubtitleStyle.setAlignment(HorizontalAlignment.CENTER);
        s.bannerSubtitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Banner Meta
        s.bannerMetaStyle = workbook.createCellStyle();
        s.bannerMetaStyle.setFont(fontBannerMeta);
        s.bannerMetaStyle.setFillForegroundColor(wine);
        s.bannerMetaStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.bannerMetaStyle.setAlignment(HorizontalAlignment.CENTER);
        s.bannerMetaStyle.setVerticalAlignment(VerticalAlignment.CENTER);

        // Column Header
        s.columnHeaderStyle = workbook.createCellStyle();
        s.columnHeaderStyle.setFont(fontHeader);
        s.columnHeaderStyle.setFillForegroundColor(wine);
        s.columnHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.columnHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        s.columnHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        s.columnHeaderStyle.setWrapText(true);
        applyBorders(s.columnHeaderStyle, BorderStyle.THIN);

        // Text regular
        s.textStyle = workbook.createCellStyle();
        s.textStyle.setFont(fontRegular);
        s.textStyle.setAlignment(HorizontalAlignment.LEFT);
        s.textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(s.textStyle, BorderStyle.THIN);

        // Text center
        s.textCenterStyle = workbook.createCellStyle();
        s.textCenterStyle.setFont(fontRegular);
        s.textCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        s.textCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(s.textCenterStyle, BorderStyle.THIN);

        // Integer center
        s.integerCenterStyle = workbook.createCellStyle();
        s.integerCenterStyle.setFont(fontRegular);
        s.integerCenterStyle.setDataFormat(integerFormat);
        s.integerCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        s.integerCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(s.integerCenterStyle, BorderStyle.THIN);

        // Currency
        s.currencyStyle = workbook.createCellStyle();
        s.currencyStyle.setFont(fontRegular);
        s.currencyStyle.setDataFormat(currencyFormat);
        s.currencyStyle.setAlignment(HorizontalAlignment.RIGHT);
        s.currencyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(s.currencyStyle, BorderStyle.THIN);

        // Percent
        s.percentStyle = workbook.createCellStyle();
        s.percentStyle.setFont(fontRegular);
        s.percentStyle.setDataFormat(percentFormat);
        s.percentStyle.setAlignment(HorizontalAlignment.RIGHT);
        s.percentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(s.percentStyle, BorderStyle.THIN);

        // Zebra styles
        s.zebraTextStyle = cloneWithBg(workbook, s.textStyle, zebraRose);
        s.zebraTextCenterStyle = cloneWithBg(workbook, s.textCenterStyle, zebraRose);
        s.zebraIntegerCenterStyle = cloneWithBg(workbook, s.integerCenterStyle, zebraRose);
        s.zebraCurrencyStyle = cloneWithBg(workbook, s.currencyStyle, zebraRose);
        s.zebraPercentStyle = cloneWithBg(workbook, s.percentStyle, zebraRose);

        // Bold styles
        s.boldTextStyle = workbook.createCellStyle();
        s.boldTextStyle.setFont(fontBold);
        s.boldTextStyle.setAlignment(HorizontalAlignment.LEFT);
        s.boldTextStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(s.boldTextStyle, BorderStyle.THIN);

        s.boldIntegerStyle = workbook.createCellStyle();
        s.boldIntegerStyle.setFont(fontBold);
        s.boldIntegerStyle.setDataFormat(integerFormat);
        s.boldIntegerStyle.setAlignment(HorizontalAlignment.RIGHT);
        s.boldIntegerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(s.boldIntegerStyle, BorderStyle.THIN);

        s.boldCurrencyStyle = workbook.createCellStyle();
        s.boldCurrencyStyle.setFont(fontBold);
        s.boldCurrencyStyle.setDataFormat(currencyFormat);
        s.boldCurrencyStyle.setAlignment(HorizontalAlignment.RIGHT);
        s.boldCurrencyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(s.boldCurrencyStyle, BorderStyle.THIN);

        s.boldTextRightStyle = workbook.createCellStyle();
        s.boldTextRightStyle.setFont(fontBold);
        s.boldTextRightStyle.setAlignment(HorizontalAlignment.RIGHT);
        s.boldTextRightStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(s.boldTextRightStyle, BorderStyle.THIN);

        // Total bar (Dark Wine)
        s.totalBarTextStyle = workbook.createCellStyle();
        s.totalBarTextStyle.setFont(fontTotal);
        s.totalBarTextStyle.setFillForegroundColor(darkWine);
        s.totalBarTextStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.totalBarTextStyle.setAlignment(HorizontalAlignment.LEFT);
        s.totalBarTextStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(s.totalBarTextStyle, BorderStyle.MEDIUM);

        s.totalBarCenterStyle = workbook.createCellStyle();
        s.totalBarCenterStyle.setFont(fontTotal);
        s.totalBarCenterStyle.setFillForegroundColor(darkWine);
        s.totalBarCenterStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.totalBarCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        s.totalBarCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(s.totalBarCenterStyle, BorderStyle.MEDIUM);

        s.totalBarIntegerCenterStyle = workbook.createCellStyle();
        s.totalBarIntegerCenterStyle.setFont(fontTotal);
        s.totalBarIntegerCenterStyle.setDataFormat(integerFormat);
        s.totalBarIntegerCenterStyle.setFillForegroundColor(darkWine);
        s.totalBarIntegerCenterStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.totalBarIntegerCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        s.totalBarIntegerCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(s.totalBarIntegerCenterStyle, BorderStyle.MEDIUM);

        s.totalBarCurrencyStyle = workbook.createCellStyle();
        s.totalBarCurrencyStyle.setFont(fontTotal);
        s.totalBarCurrencyStyle.setDataFormat(currencyFormat);
        s.totalBarCurrencyStyle.setFillForegroundColor(darkWine);
        s.totalBarCurrencyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.totalBarCurrencyStyle.setAlignment(HorizontalAlignment.RIGHT);
        s.totalBarCurrencyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        applyBorders(s.totalBarCurrencyStyle, BorderStyle.MEDIUM);

        return s;
    }

    private static void applyBorders(XSSFCellStyle style, BorderStyle border) {
        style.setBorderTop(border);
        style.setBorderBottom(border);
        style.setBorderLeft(border);
        style.setBorderRight(border);
    }

    private static XSSFCellStyle cloneWithBg(XSSFWorkbook workbook, XSSFCellStyle original, XSSFColor color) {
        XSSFCellStyle clone = workbook.createCellStyle();
        clone.cloneStyleFrom(original);
        clone.setFillForegroundColor(color);
        clone.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return clone;
    }

    private static class Styles {
        XSSFCellStyle bannerTitleStyle;
        XSSFCellStyle bannerSubtitleStyle;
        XSSFCellStyle bannerMetaStyle;
        XSSFCellStyle columnHeaderStyle;
        XSSFCellStyle textStyle;
        XSSFCellStyle textCenterStyle;
        XSSFCellStyle integerCenterStyle;
        XSSFCellStyle currencyStyle;
        XSSFCellStyle percentStyle;
        XSSFCellStyle zebraTextStyle;
        XSSFCellStyle zebraTextCenterStyle;
        XSSFCellStyle zebraIntegerCenterStyle;
        XSSFCellStyle zebraCurrencyStyle;
        XSSFCellStyle zebraPercentStyle;
        XSSFCellStyle boldTextStyle;
        XSSFCellStyle boldIntegerStyle;
        XSSFCellStyle boldCurrencyStyle;
        XSSFCellStyle boldTextRightStyle;
        XSSFCellStyle totalBarTextStyle;
        XSSFCellStyle totalBarCenterStyle;
        XSSFCellStyle totalBarIntegerCenterStyle;
        XSSFCellStyle totalBarCurrencyStyle;
    }
}
