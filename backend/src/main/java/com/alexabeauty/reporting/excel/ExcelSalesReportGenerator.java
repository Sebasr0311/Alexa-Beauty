package com.alexabeauty.reporting.excel;

import com.alexabeauty.reporting.dto.SaleOrderItemResponse;
import com.alexabeauty.reporting.dto.SaleOrderResponse;
import com.alexabeauty.reporting.dto.SalesSummaryDto;
import com.alexabeauty.reporting.dto.TopProductDto;
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
// POI formula evaluation deferred to Excel viewer
import org.apache.poi.xssf.usermodel.XSSFSheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Generates official 5-sheet Apache POI Excel Sales Report (ALEXA_SALES_V1):
 *  1. VENTAS
 *  2. DETALLE_PRODUCTOS
 *  3. TOP_PRODUCTOS
 *  4. RESUMEN
 *  5. METADATOS
 */
@Component
public class ExcelSalesReportGenerator {

    public static final String REPORT_VERSION = "ALEXA_SALES_V1";
    public static final String CURRENCY_CODE = "COP";
    public static final String TIMEZONE = "America/Bogota";

    private static final byte[] WINE_RGB = new byte[]{(byte) 0x71, (byte) 0x1A, (byte) 0x38}; // #711A38
    private static final byte[] LIGHT_ROSE_RGB = new byte[]{(byte) 0xF5, (byte) 0xE5, (byte) 0xE8}; // #F5E5E8
    private static final byte[] ZEBRA_ROSE_RGB = new byte[]{(byte) 0xFC, (byte) 0xF7, (byte) 0xF8}; // #FCF7F8
    private static final byte[] DARK_WINE_RGB = new byte[]{(byte) 0x4C, (byte) 0x11, (byte) 0x26}; // #4C1126
    private static final byte[] WHITE_RGB = new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF};

    public byte[] generateSalesReport(
            List<SaleOrderResponse> orders,
            SalesSummaryDto summary,
            List<TopProductDto> topProducts,
            LocalDate startDate,
            LocalDate endDate,
            UserPrincipal actor
    ) {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            Styles styles = createStyles(workbook);

            createVentasSheet(workbook, orders, styles);
            createDetalleProductosSheet(workbook, orders, styles);
            createTopProductosSheet(workbook, topProducts, styles);
            createResumenSheet(workbook, orders, summary, startDate, endDate, styles);
            createMetadatosSheet(workbook, orders, startDate, endDate, actor, styles);

            try {
                // Request Excel/Sheets to recalculate formula cells upon opening without POI heap overhead
                workbook.setForceFormulaRecalculation(true);
            } catch (Exception ignored) {
                // If any formula can't be evaluated statically in POI, continue
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error generating Excel sales report", e);
        }
    }

    private String formatChannelLabel(String channel) {
        if (channel == null || channel.isBlank()) {
            return "Tienda Online";
        }
        return switch (channel.trim().toUpperCase()) {
            case "PUNTO_FISICO" -> "Punto Físico (Mostrador)";
            case "WHATSAPP" -> "WhatsApp / Tienda";
            case "WEB", "TIENDA_ONLINE" -> "Tienda Online";
            default -> channel;
        };
    }

    private void createVentasSheet(XSSFWorkbook workbook, List<SaleOrderResponse> orders, Styles styles) {
        XSSFSheet sheet = workbook.createSheet("VENTAS");
        sheet.setDisplayGridlines(true);
        int numCols = 11;

        createBannerRows(sheet, numCols - 1, styles);

        // Header Row 5 (index 4)
        Row headerRow = sheet.createRow(4);
        headerRow.setHeightInPoints(34);
        String[] headers = {
                "Fecha de pago", "Referencia", "Cliente", "Método",
                "Subtotal COP", "Descuento COP", "Envío COP", "Total pagado COP",
                "Verificado por", "Estado", "Canal"
        };
        for (int c = 0; c < headers.length; c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(styles.columnHeaderStyle);
        }

        sheet.createFreezePane(0, 5);

        // Data rows (from row 6, index 5)
        int rowIdx = 5;
        for (int idx = 0; idx < orders.size(); idx++) {
            SaleOrderResponse order = orders.get(idx);
            boolean isAlt = idx % 2 == 1;
            Row row = sheet.createRow(rowIdx++);
            row.setHeightInPoints(22);

            XSSFCellStyle txtStyle = isAlt ? styles.zebraTextStyle : styles.textStyle;
            XSSFCellStyle txtCenterStyle = isAlt ? styles.zebraTextCenterStyle : styles.textCenterStyle;
            XSSFCellStyle currStyle = isAlt ? styles.zebraCurrencyStyle : styles.currencyStyle;

            // Col 0: Fecha de pago
            Cell c0 = row.createCell(0);
            c0.setCellValue(order.localPaidAt() != null ? order.localPaidAt() : "");
            c0.setCellStyle(txtCenterStyle);

            // Col 1: Referencia
            Cell c1 = row.createCell(1);
            c1.setCellValue(order.reference());
            c1.setCellStyle(txtStyle);

            // Col 2: Cliente
            Cell c2 = row.createCell(2);
            c2.setCellValue(order.customerName());
            c2.setCellStyle(txtStyle);

            // Col 3: Método
            Cell c3 = row.createCell(3);
            c3.setCellValue(order.paymentMethod() != null ? order.paymentMethod() : "");
            c3.setCellStyle(txtCenterStyle);

            // Col 4: Subtotal COP
            Cell c4 = row.createCell(4);
            c4.setCellValue(order.subtotal() != null ? order.subtotal().doubleValue() : 0.0);
            c4.setCellStyle(currStyle);

            // Col 5: Descuento COP
            Cell c5 = row.createCell(5);
            c5.setCellValue(order.discount() != null ? order.discount().doubleValue() : 0.0);
            c5.setCellStyle(currStyle);

            // Col 6: Envío COP
            Cell c6 = row.createCell(6);
            c6.setCellValue(order.shippingFee() != null ? order.shippingFee().doubleValue() : 0.0);
            c6.setCellStyle(currStyle);

            // Col 7: Total pagado COP
            Cell c7 = row.createCell(7);
            c7.setCellValue(order.total() != null ? order.total().doubleValue() : 0.0);
            c7.setCellStyle(currStyle);

            // Col 8: Verificado por
            Cell c8 = row.createCell(8);
            c8.setCellValue(order.verifiedBy() != null ? order.verifiedBy() : "Sistema");
            c8.setCellStyle(txtStyle);

            // Col 9: Estado
            Cell c9 = row.createCell(9);
            c9.setCellValue(order.status());
            c9.setCellStyle(txtCenterStyle);

            // Col 10: Canal
            Cell c10 = row.createCell(10);
            c10.setCellValue(formatChannelLabel(order.channel()));
            c10.setCellStyle(txtCenterStyle);
        }

        int lastDataRowNum = orders.isEmpty() ? 5 : (5 + orders.size());
        sheet.setAutoFilter(new CellRangeAddress(4, Math.max(4, lastDataRowNum - 1), 0, numCols - 1));

        // Row N+1 empty spacer
        sheet.createRow(rowIdx++);

        // Row N+2 total row
        Row totalRow = sheet.createRow(rowIdx);
        totalRow.setHeightInPoints(26);
        for (int c = 0; c < numCols; c++) {
            Cell cell = totalRow.createCell(c);
            cell.setCellStyle(styles.totalBarTextStyle);
        }

        Cell totalLabelA = totalRow.getCell(0);
        totalLabelA.setCellValue("TOTAL");
        totalLabelA.setCellStyle(styles.totalBarCenterStyle);

        Cell totalValueE = totalRow.getCell(4);
        if (!orders.isEmpty()) {
            totalValueE.setCellFormula(String.format("SUM(E6:E%d)", lastDataRowNum));
        } else {
            totalValueE.setCellValue(0.0);
        }
        totalValueE.setCellStyle(styles.totalBarCurrencyStyle);

        Cell totalValueF = totalRow.getCell(5);
        if (!orders.isEmpty()) {
            totalValueF.setCellFormula(String.format("SUM(F6:F%d)", lastDataRowNum));
        } else {
            totalValueF.setCellValue(0.0);
        }
        totalValueF.setCellStyle(styles.totalBarCurrencyStyle);

        Cell totalLabelG = totalRow.getCell(6);
        totalLabelG.setCellValue("TOTAL");
        totalLabelG.setCellStyle(styles.totalBarRightStyle);

        Cell totalValueH = totalRow.getCell(7);
        if (!orders.isEmpty()) {
            totalValueH.setCellFormula(String.format("SUM(H6:H%d)", lastDataRowNum));
        } else {
            totalValueH.setCellValue(0.0);
        }
        totalValueH.setCellStyle(styles.totalBarCurrencyStyle);

        int[] widths = {6200, 5800, 6800, 4800, 5200, 5200, 4600, 5800, 5800, 4600, 6200};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i]);
        }
    }

    private void createDetalleProductosSheet(XSSFWorkbook workbook, List<SaleOrderResponse> orders, Styles styles) {
        XSSFSheet sheet = workbook.createSheet("DETALLE_PRODUCTOS");
        sheet.setDisplayGridlines(true);
        int numCols = 12;

        createBannerRows(sheet, numCols - 1, styles);

        // Header Row 5 (index 4)
        Row headerRow = sheet.createRow(4);
        headerRow.setHeightInPoints(34);
        String[] headers = {
                "Referencia", "SKU", "Producto", "Marca", "Tono",
                "Cantidad", "Precio unitario COP", "Total línea COP", "Costo unitario COP",
                "Costo total línea COP", "Utilidad bruta línea COP", "Canal"
        };
        for (int c = 0; c < headers.length; c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(styles.columnHeaderStyle);
        }

        sheet.createFreezePane(0, 5);

        int rowIdx = 5;
        int totalItemCount = 0;

        for (SaleOrderResponse order : orders) {
            if (order.items() == null) continue;
            for (SaleOrderItemResponse item : order.items()) {
                boolean isAlt = totalItemCount % 2 == 1;
                totalItemCount++;
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(22);

                int excelRow = rowIdx; // 1-based row number
                XSSFCellStyle txtStyle = isAlt ? styles.zebraTextStyle : styles.textStyle;
                XSSFCellStyle txtCenterStyle = isAlt ? styles.zebraTextCenterStyle : styles.textCenterStyle;
                XSSFCellStyle intCenterStyle = isAlt ? styles.zebraIntegerCenterStyle : styles.integerCenterStyle;
                XSSFCellStyle currStyle = isAlt ? styles.zebraCurrencyStyle : styles.currencyStyle;

                // Col 0: Referencia
                Cell c0 = row.createCell(0);
                c0.setCellValue(order.reference());
                c0.setCellStyle(txtStyle);

                // Col 1: SKU
                Cell c1 = row.createCell(1);
                c1.setCellValue(item.sku());
                c1.setCellStyle(txtStyle);

                // Col 2: Producto
                Cell c2 = row.createCell(2);
                c2.setCellValue(item.productName());
                c2.setCellStyle(txtStyle);

                // Col 3: Marca
                Cell c3 = row.createCell(3);
                c3.setCellValue(item.brandName() != null ? item.brandName() : "");
                c3.setCellStyle(txtStyle);

                // Col 4: Tono
                Cell c4 = row.createCell(4);
                String shade = item.shadeName() != null && !item.shadeName().trim().equalsIgnoreCase("Default")
                        ? item.shadeName()
                        : "Estándar";
                c4.setCellValue(shade);
                c4.setCellStyle(txtStyle);

                // Col 5: Cantidad
                Cell c5 = row.createCell(5);
                c5.setCellValue(item.quantity());
                c5.setCellStyle(intCenterStyle);

                // Col 6: Precio unitario COP
                Cell c6 = row.createCell(6);
                c6.setCellValue(item.unitPrice() != null ? item.unitPrice().doubleValue() : 0.0);
                c6.setCellStyle(currStyle);

                // Col 7: Total línea COP (formula =F{r}*G{r})
                Cell c7 = row.createCell(7);
                c7.setCellFormula(String.format("F%d*G%d", excelRow, excelRow));
                c7.setCellStyle(currStyle);

                // Col 8: Costo unitario COP
                Cell c8 = row.createCell(8);
                if (item.costSnapshot() != null) {
                    c8.setCellValue(item.costSnapshot().doubleValue());
                } else {
                    c8.setCellValue("Sin costo histórico");
                }
                c8.setCellStyle(currStyle);

                // Col 9: Costo total línea COP
                Cell c9 = row.createCell(9);
                if (item.costSnapshot() != null) {
                    c9.setCellValue(item.costSnapshot().multiply(BigDecimal.valueOf(item.quantity())).doubleValue());
                } else {
                    c9.setCellValue("Sin costo histórico");
                }
                c9.setCellStyle(currStyle);

                // Col 10: Utilidad bruta línea COP
                Cell c10 = row.createCell(10);
                if (item.costSnapshot() != null && item.unitPrice() != null) {
                    BigDecimal lineProfit = item.unitPrice().subtract(item.costSnapshot())
                            .multiply(BigDecimal.valueOf(item.quantity()));
                    c10.setCellValue(lineProfit.doubleValue());
                } else {
                    c10.setCellValue("Sin costo histórico");
                }
                c10.setCellStyle(currStyle);

                // Col 11: Canal
                Cell c11 = row.createCell(11);
                c11.setCellValue(formatChannelLabel(order.channel()));
                c11.setCellStyle(txtCenterStyle);
            }
        }

        int lastDataRowNum = totalItemCount == 0 ? 5 : (5 + totalItemCount);
        sheet.setAutoFilter(new CellRangeAddress(4, Math.max(4, lastDataRowNum - 1), 0, numCols - 1));

        // Empty spacer row
        sheet.createRow(rowIdx++);

        // Total row M+2
        Row totalRow = sheet.createRow(rowIdx);
        totalRow.setHeightInPoints(26);
        for (int c = 0; c < numCols; c++) {
            Cell cell = totalRow.createCell(c);
            cell.setCellStyle(styles.totalBarTextStyle);
        }

        Cell totalLabelA = totalRow.getCell(0);
        totalLabelA.setCellValue("TOTAL");
        totalLabelA.setCellStyle(styles.totalBarCenterStyle);

        Cell totalQtyF = totalRow.getCell(5);
        if (totalItemCount > 0) {
            totalQtyF.setCellFormula(String.format("SUM(F6:F%d)", lastDataRowNum));
        } else {
            totalQtyF.setCellValue(0);
        }
        totalQtyF.setCellStyle(styles.totalBarIntegerCenterStyle);

        Cell totalLabelG = totalRow.getCell(6);
        totalLabelG.setCellValue("TOTAL");
        totalLabelG.setCellStyle(styles.totalBarRightStyle);

        Cell totalValueH = totalRow.getCell(7);
        if (totalItemCount > 0) {
            totalValueH.setCellFormula(String.format("SUM(H6:H%d)", lastDataRowNum));
        } else {
            totalValueH.setCellValue(0.0);
        }
        totalValueH.setCellStyle(styles.totalBarCurrencyStyle);

        int[] widths = {5800, 5800, 7800, 5500, 4800, 3800, 5800, 5800, 5800, 5800, 5800, 6000};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i]);
        }
    }

    private void createTopProductosSheet(XSSFWorkbook workbook, List<TopProductDto> topProducts, Styles styles) {
        XSSFSheet sheet = workbook.createSheet("TOP_PRODUCTOS");
        sheet.setDisplayGridlines(true);
        int numCols = 6;

        createBannerRows(sheet, numCols - 1, styles);

        // Header Row 5 (index 4)
        Row headerRow = sheet.createRow(4);
        headerRow.setHeightInPoints(34);
        String[] headers = {
                "Rango", "SKU", "Producto", "Unidades", "Ingresos COP", "Participación ingresos"
        };
        for (int c = 0; c < headers.length; c++) {
            Cell cell = headerRow.createCell(c);
            cell.setCellValue(headers[c]);
            cell.setCellStyle(styles.columnHeaderStyle);
        }

        sheet.createFreezePane(0, 5);

        int rowIdx = 5;
        for (int idx = 0; idx < topProducts.size(); idx++) {
            TopProductDto item = topProducts.get(idx);
            boolean isAlt = idx % 2 == 1;
            Row row = sheet.createRow(rowIdx++);
            row.setHeightInPoints(22);

            XSSFCellStyle txtStyle = isAlt ? styles.zebraTextStyle : styles.textStyle;
            XSSFCellStyle intCenterStyle = isAlt ? styles.zebraIntegerCenterStyle : styles.integerCenterStyle;
            XSSFCellStyle currStyle = isAlt ? styles.zebraCurrencyStyle : styles.currencyStyle;
            XSSFCellStyle pctStyle = isAlt ? styles.zebraPercentStyle : styles.percentStyle;

            // Col 0: Rango
            Cell c0 = row.createCell(0);
            c0.setCellValue(item.rank());
            c0.setCellStyle(intCenterStyle);

            // Col 1: SKU
            Cell c1 = row.createCell(1);
            c1.setCellValue(item.sku());
            c1.setCellStyle(txtStyle);

            // Col 2: Producto
            Cell c2 = row.createCell(2);
            c2.setCellValue(item.productName());
            c2.setCellStyle(txtStyle);

            // Col 3: Unidades
            Cell c3 = row.createCell(3);
            c3.setCellValue(item.unitsSold());
            c3.setCellStyle(intCenterStyle);

            // Col 4: Ingresos COP
            Cell c4 = row.createCell(4);
            c4.setCellValue(item.grossRevenue() != null ? item.grossRevenue().doubleValue() : 0.0);
            c4.setCellStyle(currStyle);

            // Col 5: Participación ingresos (percentage)
            Cell c5 = row.createCell(5);
            double share = item.revenueSharePercentage() != null
                    ? item.revenueSharePercentage().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP).doubleValue()
                    : 0.0;
            c5.setCellValue(share);
            c5.setCellStyle(pctStyle);
        }

        int[] widths = {3800, 6500, 8800, 4800, 5800, 6800};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i]);
        }
    }

    private void createResumenSheet(
            XSSFWorkbook workbook,
            List<SaleOrderResponse> orders,
            SalesSummaryDto summary,
            LocalDate startDate,
            LocalDate endDate,
            Styles styles
    ) {
        XSSFSheet sheet = workbook.createSheet("RESUMEN");
        sheet.setDisplayGridlines(true);
        int numCols = 2;

        createBannerRows(sheet, numCols - 1, styles);

        // Header Row 5 (index 4)
        Row headerRow = sheet.createRow(4);
        headerRow.setHeightInPoints(29);

        Cell h0 = headerRow.createCell(0);
        h0.setCellValue("INDICADOR");
        h0.setCellStyle(styles.columnHeaderStyle);

        Cell h1 = headerRow.createCell(1);
        h1.setCellValue("RESULTADO");
        h1.setCellStyle(styles.columnHeaderStyle);

        sheet.createFreezePane(0, 5);

        // Row 6: Ventas pagadas (COP)
        Row r6 = sheet.createRow(5);
        r6.setHeightInPoints(28);
        Cell r6a = r6.createCell(0);
        r6a.setCellValue("Ventas pagadas (COP)");
        r6a.setCellStyle(styles.boldTextStyle);
        Cell r6b = r6.createCell(1);
        r6b.setCellValue(summary.totalSales() != null ? summary.totalSales().doubleValue() : 0.0);
        r6b.setCellStyle(styles.boldCurrencyStyle);

        // Row 7: Pedidos pagados
        Row r7 = sheet.createRow(6);
        r7.setHeightInPoints(28);
        Cell r7a = r7.createCell(0);
        r7a.setCellValue("Pedidos pagados");
        r7a.setCellStyle(styles.boldTextStyle);
        Cell r7b = r7.createCell(1);
        r7b.setCellValue(summary.paidOrdersCount());
        r7b.setCellStyle(styles.boldIntegerStyle);

        // Row 8: Unidades vendidas
        Row r8 = sheet.createRow(7);
        r8.setHeightInPoints(28);
        Cell r8a = r8.createCell(0);
        r8a.setCellValue("Unidades vendidas");
        r8a.setCellStyle(styles.boldTextStyle);
        Cell r8b = r8.createCell(1);
        r8b.setCellValue(summary.totalUnitsSold());
        r8b.setCellStyle(styles.boldIntegerStyle);

        // Row 9: Ticket promedio (COP)
        Row r9 = sheet.createRow(8);
        r9.setHeightInPoints(28);
        Cell r9a = r9.createCell(0);
        r9a.setCellValue("Ticket promedio (COP)");
        r9a.setCellStyle(styles.boldTextStyle);
        Cell r9b = r9.createCell(1);
        r9b.setCellValue(summary.averageTicket() != null ? summary.averageTicket().doubleValue() : 0.0);
        r9b.setCellStyle(styles.boldCurrencyStyle);

        // Empty spacers (rows 10 and 11)
        sheet.createRow(9);
        sheet.createRow(10);

        // Row 12: PERÍODO
        Row r12 = sheet.createRow(11);
        r12.setHeightInPoints(24);
        Cell r12a = r12.createCell(0);
        r12a.setCellValue("PERÍODO");
        r12a.setCellStyle(styles.boldTextStyle);
        Cell r12b = r12.createCell(1);
        r12b.setCellValue(startDate + " a " + endDate);
        r12b.setCellStyle(styles.textStyle);

        // Row 13: Estado incluido
        Row r13 = sheet.createRow(12);
        r13.setHeightInPoints(24);
        Cell r13a = r13.createCell(0);
        r13a.setCellValue("Estado incluido");
        r13a.setCellStyle(styles.boldTextStyle);
        Cell r13b = r13.createCell(1);
        r13b.setCellValue("Únicamente PAGADO / ENTREGADO (Tienda y Punto Físico)");
        r13b.setCellStyle(styles.textStyle);

        // Row 14: Zona horaria
        Row r14 = sheet.createRow(13);
        r14.setHeightInPoints(24);
        Cell r14a = r14.createCell(0);
        r14a.setCellValue("Zona horaria");
        r14a.setCellStyle(styles.boldTextStyle);
        Cell r14b = r14.createCell(1);
        r14b.setCellValue(TIMEZONE);
        r14b.setCellStyle(styles.textStyle);

        // Row 15 (index 14): Section header for profitability
        Row r15Header = sheet.createRow(14);
        r15Header.setHeightInPoints(26);
        Cell pH0 = r15Header.createCell(0);
        pH0.setCellValue("RENTABILIDAD Y COSTOS CONSOLIDADOS");
        pH0.setCellStyle(styles.columnHeaderStyle);
        Cell pH1 = r15Header.createCell(1);
        pH1.setCellValue("VALOR");
        pH1.setCellStyle(styles.columnHeaderStyle);

        String[] profitLabels = {
                "Ingresos por productos (COP)",
                "Ingresos con costo conocido (COP)",
                "Costo histórico conocido (COP)",
                "Utilidad bruta parcial (COP)",
                "Margen sobre ingresos cubiertos (%)",
                "Unidades sin costo histórico"
        };
        BigDecimal[] profitValues = {
                summary.merchandiseRevenue(),
                summary.coveredMerchandiseRevenue(),
                summary.knownCostTotal(),
                summary.grossProfit(),
                summary.grossMarginPercentage(),
                BigDecimal.valueOf(summary.unitsWithoutCost())
        };
        for (int i = 0; i < profitLabels.length; i++) {
            Row row = sheet.createRow(15 + i);
            row.setHeightInPoints(25);
            Cell labelCell = row.createCell(0);
            labelCell.setCellValue(profitLabels[i]);
            labelCell.setCellStyle(i == 3 ? styles.totalBarTextStyle : styles.boldTextStyle);

            Cell value = row.createCell(1);
            if (profitValues[i] == null) {
                value.setCellValue("No disponible");
                value.setCellStyle(styles.textStyle);
            } else {
                value.setCellValue(profitValues[i].doubleValue());
                if (i == 3) {
                    value.setCellStyle(styles.totalBarCurrencyStyle);
                } else if (i == 4) {
                    value.setCellStyle(styles.boldTextRightStyle);
                } else if (i == 5) {
                    value.setCellStyle(styles.boldIntegerStyle);
                } else {
                    value.setCellStyle(styles.boldCurrencyStyle);
                }
            }
        }

        Row noteRow = sheet.createRow(22);
        noteRow.setHeightInPoints(22);
        Cell noteCell = noteRow.createCell(0);
        noteCell.setCellValue("Utilidad bruta solo sobre ventas con costo histórico; incluye descuentos aplicados y excluye envío.");
        noteCell.setCellStyle(styles.textStyle);
        Cell noteCellB = noteRow.createCell(1);
        noteCellB.setCellValue("Precios predeterminados y costos congelados al momento de la venta.");
        noteCellB.setCellStyle(styles.textStyle);

        // Channel & Discount Breakdown starting at Row 25 (index 24)
        long posCount = orders.stream().filter(o -> "PUNTO_FISICO".equalsIgnoreCase(o.channel())).count();
        BigDecimal posTotal = orders.stream()
                .filter(o -> "PUNTO_FISICO".equalsIgnoreCase(o.channel()))
                .map(o -> o.total() != null ? o.total() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long onlineCount = orders.size() - posCount;
        BigDecimal onlineTotal = orders.stream()
                .filter(o -> !"PUNTO_FISICO".equalsIgnoreCase(o.channel()))
                .map(o -> o.total() != null ? o.total() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalDiscounts = orders.stream()
                .map(o -> o.discount() != null ? o.discount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Row chHeader = sheet.createRow(24);
        chHeader.setHeightInPoints(26);
        Cell ch0 = chHeader.createCell(0);
        ch0.setCellValue("DESGLOSE POR CANAL Y DESCUENTOS");
        ch0.setCellStyle(styles.columnHeaderStyle);
        Cell ch1 = chHeader.createCell(1);
        ch1.setCellValue("CONSOLIDADO");
        ch1.setCellStyle(styles.columnHeaderStyle);

        Object[][] channelRows = {
                {"Ventas en Punto Físico (Mostrador) — Cantidad", posCount, false},
                {"Ventas en Punto Físico (Mostrador) — Total COP", posTotal.doubleValue(), true},
                {"Ventas en Tienda Online / WhatsApp — Cantidad", onlineCount, false},
                {"Ventas en Tienda Online / WhatsApp — Total COP", onlineTotal.doubleValue(), true},
                {"Total descuentos aplicados en el período (COP)", totalDiscounts.doubleValue(), true}
        };

        for (int i = 0; i < channelRows.length; i++) {
            Row r = sheet.createRow(25 + i);
            r.setHeightInPoints(24);
            Cell cA = r.createCell(0);
            cA.setCellValue((String) channelRows[i][0]);
            cA.setCellStyle(styles.boldTextStyle);

            Cell cB = r.createCell(1);
            boolean isCurr = (Boolean) channelRows[i][2];
            if (isCurr) {
                cB.setCellValue((Double) channelRows[i][1]);
                cB.setCellStyle(styles.boldCurrencyStyle);
            } else {
                cB.setCellValue((Long) channelRows[i][1]);
                cB.setCellStyle(styles.boldIntegerStyle);
            }
        }

        sheet.setColumnWidth(0, 11500);
        sheet.setColumnWidth(1, 14500);
    }

    private void createMetadatosSheet(
            XSSFWorkbook workbook,
            List<SaleOrderResponse> orders,
            LocalDate startDate,
            LocalDate endDate,
            UserPrincipal actor,
            Styles styles
    ) {
        XSSFSheet sheet = workbook.createSheet("METADATOS");
        sheet.setDisplayGridlines(true);
        int numCols = 2;

        createBannerRows(sheet, numCols - 1, styles);

        // Header Row 5 (index 4)
        Row headerRow = sheet.createRow(4);
        headerRow.setHeightInPoints(34);

        Cell h0 = headerRow.createCell(0);
        h0.setCellValue("Campo");
        h0.setCellStyle(styles.columnHeaderStyle);

        Cell h1 = headerRow.createCell(1);
        h1.setCellValue("Valor");
        h1.setCellStyle(styles.columnHeaderStyle);

        sheet.createFreezePane(0, 5);

        String actorDesc = "Sistema";
        if (actor != null) {
            if (actor.getFullName() != null && !actor.getFullName().isBlank()) {
                actorDesc = actor.getFullName() + " (" + actor.getEmail() + ")";
            } else if (actor.getEmail() != null) {
                actorDesc = actor.getEmail();
            }
        }

        long posOrders = orders.stream().filter(o -> "PUNTO_FISICO".equalsIgnoreCase(o.channel())).count();
        long webOrders = orders.size() - posOrders;

        String[][] metadata = {
                {"Versión", REPORT_VERSION},
                {"Moneda", CURRENCY_CODE},
                {"Zona horaria", TIMEZONE},
                {"Inicio (local)", startDate.toString() + " 00:00"},
                {"Fin (local)", endDate.toString() + " 23:59:59"},
                {"Estado filtrado", "PAGADO"},
                {"Canales incluidos", "Tienda Online / WhatsApp (" + webOrders + ") y Punto Físico (" + posOrders + ")"},
                {"Generado por", actorDesc},
                {"Fecha generación (UTC)", Instant.now().toString()}
        };

        int r = 5;
        for (String[] pair : metadata) {
            Row row = sheet.createRow(r++);
            row.setHeightInPoints(22);

            Cell c0 = row.createCell(0);
            c0.setCellValue(pair[0]);
            c0.setCellStyle(styles.boldTextStyle);

            Cell c1 = row.createCell(1);
            c1.setCellValue(pair[1]);
            c1.setCellStyle(styles.textStyle);
        }

        sheet.setColumnWidth(0, 8500);
        sheet.setColumnWidth(1, 18500);
    }

    private void createBannerRows(XSSFSheet sheet, int endCol, Styles styles) {
        // Row 1 & 2 merged: ALEXA BEAUTY   |   REPORTE DE VENTAS
        Row row1 = sheet.createRow(0);
        row1.setHeightInPoints(24);
        Row row2 = sheet.createRow(1);
        row2.setHeightInPoints(24);

        for (int r = 0; r <= 1; r++) {
            Row row = sheet.getRow(r);
            for (int c = 0; c <= endCol; c++) {
                Cell cell = row.createCell(c);
                cell.setCellStyle(styles.bannerTitleStyle);
            }
        }
        sheet.getRow(0).getCell(0).setCellValue("ALEXA BEAUTY   |   REPORTE DE VENTAS");
        sheet.addMergedRegion(new CellRangeAddress(0, 1, 0, endCol));

        // Row 3 (index 2): REPORTE OFICIAL DE VENTAS CONFIRMADAS
        Row row3 = sheet.createRow(2);
        row3.setHeightInPoints(25);
        for (int c = 0; c <= endCol; c++) {
            Cell cell = row3.createCell(c);
            cell.setCellStyle(styles.bannerSubtitleStyle);
        }
        row3.getCell(0).setCellValue("REPORTE OFICIAL DE VENTAS CONFIRMADAS");
        sheet.addMergedRegion(new CellRangeAddress(2, 2, 0, endCol));

        // Row 4 (index 3) is spacer
        sheet.createRow(3);
    }

    private Styles createStyles(XSSFWorkbook workbook) {
        Styles s = new Styles();
        XSSFDataFormat dataFormat = workbook.createDataFormat();

        // Colors
        XSSFColor wine = new XSSFColor(WINE_RGB, new DefaultIndexedColorMap());
        XSSFColor lightRose = new XSSFColor(LIGHT_ROSE_RGB, new DefaultIndexedColorMap());
        XSSFColor zebraRose = new XSSFColor(ZEBRA_ROSE_RGB, new DefaultIndexedColorMap());
        XSSFColor darkWine = new XSSFColor(DARK_WINE_RGB, new DefaultIndexedColorMap());
        XSSFColor white = new XSSFColor(WHITE_RGB, new DefaultIndexedColorMap());

        // Fonts
        XSSFFont titleFont = workbook.createFont();
        titleFont.setBold(true);
        titleFont.setColor(white);
        titleFont.setFontHeightInPoints((short) 16);

        XSSFFont subtitleFont = workbook.createFont();
        subtitleFont.setBold(true);
        subtitleFont.setColor(darkWine);
        subtitleFont.setFontHeightInPoints((short) 11);

        XSSFFont headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setColor(white);
        headerFont.setFontHeightInPoints((short) 10);

        XSSFFont regularFont = workbook.createFont();
        regularFont.setFontHeightInPoints((short) 10);

        XSSFFont boldFont = workbook.createFont();
        boldFont.setBold(true);
        boldFont.setFontHeightInPoints((short) 10);

        XSSFFont totalFont = workbook.createFont();
        totalFont.setBold(true);
        totalFont.setColor(darkWine);
        totalFont.setFontHeightInPoints((short) 10);

        // Formats
        short currencyFmt = dataFormat.getFormat("$ #,##0");
        short percentFmt = dataFormat.getFormat("0.0%");
        short integerFmt = dataFormat.getFormat("#,##0");

        // 1. Banner Title
        s.bannerTitleStyle = workbook.createCellStyle();
        s.bannerTitleStyle.setFillForegroundColor(wine);
        s.bannerTitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.bannerTitleStyle.setAlignment(HorizontalAlignment.CENTER);
        s.bannerTitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        s.bannerTitleStyle.setFont(titleFont);

        // 2. Banner Subtitle
        s.bannerSubtitleStyle = workbook.createCellStyle();
        s.bannerSubtitleStyle.setFillForegroundColor(lightRose);
        s.bannerSubtitleStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.bannerSubtitleStyle.setAlignment(HorizontalAlignment.CENTER);
        s.bannerSubtitleStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        s.bannerSubtitleStyle.setFont(subtitleFont);

        // 3. Column Header
        s.columnHeaderStyle = workbook.createCellStyle();
        s.columnHeaderStyle.setFillForegroundColor(wine);
        s.columnHeaderStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        s.columnHeaderStyle.setAlignment(HorizontalAlignment.CENTER);
        s.columnHeaderStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        s.columnHeaderStyle.setWrapText(true);
        s.columnHeaderStyle.setFont(headerFont);
        addThinBorders(s.columnHeaderStyle);

        // 4. Text styles
        s.textStyle = workbook.createCellStyle();
        s.textStyle.setFont(regularFont);
        s.textStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        addThinBorders(s.textStyle);

        s.zebraTextStyle = workbook.createCellStyle();
        s.zebraTextStyle.setFont(regularFont);
        s.zebraTextStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        s.zebraTextStyle.setFillForegroundColor(zebraRose);
        s.zebraTextStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        addThinBorders(s.zebraTextStyle);

        s.textCenterStyle = workbook.createCellStyle();
        s.textCenterStyle.setFont(regularFont);
        s.textCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        s.textCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        addThinBorders(s.textCenterStyle);

        s.zebraTextCenterStyle = workbook.createCellStyle();
        s.zebraTextCenterStyle.setFont(regularFont);
        s.zebraTextCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        s.zebraTextCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        s.zebraTextCenterStyle.setFillForegroundColor(zebraRose);
        s.zebraTextCenterStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        addThinBorders(s.zebraTextCenterStyle);

        s.boldTextStyle = workbook.createCellStyle();
        s.boldTextStyle.setFont(boldFont);
        s.boldTextStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        addThinBorders(s.boldTextStyle);

        s.boldTextCenterStyle = workbook.createCellStyle();
        s.boldTextCenterStyle.setFont(boldFont);
        s.boldTextCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        s.boldTextCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        addThinBorders(s.boldTextCenterStyle);

        s.boldTextRightStyle = workbook.createCellStyle();
        s.boldTextRightStyle.setFont(boldFont);
        s.boldTextRightStyle.setAlignment(HorizontalAlignment.RIGHT);
        s.boldTextRightStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        addThinBorders(s.boldTextRightStyle);

        // 5. Currency styles
        s.currencyStyle = workbook.createCellStyle();
        s.currencyStyle.setFont(regularFont);
        s.currencyStyle.setDataFormat(currencyFmt);
        s.currencyStyle.setAlignment(HorizontalAlignment.RIGHT);
        s.currencyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        addThinBorders(s.currencyStyle);

        s.zebraCurrencyStyle = workbook.createCellStyle();
        s.zebraCurrencyStyle.setFont(regularFont);
        s.zebraCurrencyStyle.setDataFormat(currencyFmt);
        s.zebraCurrencyStyle.setAlignment(HorizontalAlignment.RIGHT);
        s.zebraCurrencyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        s.zebraCurrencyStyle.setFillForegroundColor(zebraRose);
        s.zebraCurrencyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        addThinBorders(s.zebraCurrencyStyle);

        s.boldCurrencyStyle = workbook.createCellStyle();
        s.boldCurrencyStyle.setFont(boldFont);
        s.boldCurrencyStyle.setDataFormat(currencyFmt);
        s.boldCurrencyStyle.setAlignment(HorizontalAlignment.RIGHT);
        s.boldCurrencyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        addThinBorders(s.boldCurrencyStyle);

        // 6. Percent style
        s.percentStyle = workbook.createCellStyle();
        s.percentStyle.setFont(regularFont);
        s.percentStyle.setDataFormat(percentFmt);
        s.percentStyle.setAlignment(HorizontalAlignment.RIGHT);
        s.percentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        addThinBorders(s.percentStyle);

        s.zebraPercentStyle = workbook.createCellStyle();
        s.zebraPercentStyle.setFont(regularFont);
        s.zebraPercentStyle.setDataFormat(percentFmt);
        s.zebraPercentStyle.setAlignment(HorizontalAlignment.RIGHT);
        s.zebraPercentStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        s.zebraPercentStyle.setFillForegroundColor(zebraRose);
        s.zebraPercentStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        addThinBorders(s.zebraPercentStyle);

        // 7. Integer styles
        s.integerCenterStyle = workbook.createCellStyle();
        s.integerCenterStyle.setFont(regularFont);
        s.integerCenterStyle.setDataFormat(integerFmt);
        s.integerCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        s.integerCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        addThinBorders(s.integerCenterStyle);

        s.zebraIntegerCenterStyle = workbook.createCellStyle();
        s.zebraIntegerCenterStyle.setFont(regularFont);
        s.zebraIntegerCenterStyle.setDataFormat(integerFmt);
        s.zebraIntegerCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        s.zebraIntegerCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        s.zebraIntegerCenterStyle.setFillForegroundColor(zebraRose);
        s.zebraIntegerCenterStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        addThinBorders(s.zebraIntegerCenterStyle);

        s.boldIntegerStyle = workbook.createCellStyle();
        s.boldIntegerStyle.setFont(boldFont);
        s.boldIntegerStyle.setDataFormat(integerFmt);
        s.boldIntegerStyle.setAlignment(HorizontalAlignment.RIGHT);
        s.boldIntegerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        addThinBorders(s.boldIntegerStyle);

        // 8. Total Bar styles (Light Rose background + Dark Wine bold font)
        s.totalBarTextStyle = workbook.createCellStyle();
        s.totalBarTextStyle.setFont(totalFont);
        s.totalBarTextStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        s.totalBarTextStyle.setFillForegroundColor(lightRose);
        s.totalBarTextStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        addThinBorders(s.totalBarTextStyle);

        s.totalBarCenterStyle = workbook.createCellStyle();
        s.totalBarCenterStyle.setFont(totalFont);
        s.totalBarCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        s.totalBarCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        s.totalBarCenterStyle.setFillForegroundColor(lightRose);
        s.totalBarCenterStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        addThinBorders(s.totalBarCenterStyle);

        s.totalBarRightStyle = workbook.createCellStyle();
        s.totalBarRightStyle.setFont(totalFont);
        s.totalBarRightStyle.setAlignment(HorizontalAlignment.RIGHT);
        s.totalBarRightStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        s.totalBarRightStyle.setFillForegroundColor(lightRose);
        s.totalBarRightStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        addThinBorders(s.totalBarRightStyle);

        s.totalBarCurrencyStyle = workbook.createCellStyle();
        s.totalBarCurrencyStyle.setFont(totalFont);
        s.totalBarCurrencyStyle.setDataFormat(currencyFmt);
        s.totalBarCurrencyStyle.setAlignment(HorizontalAlignment.RIGHT);
        s.totalBarCurrencyStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        s.totalBarCurrencyStyle.setFillForegroundColor(lightRose);
        s.totalBarCurrencyStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        addThinBorders(s.totalBarCurrencyStyle);

        s.totalBarIntegerCenterStyle = workbook.createCellStyle();
        s.totalBarIntegerCenterStyle.setFont(totalFont);
        s.totalBarIntegerCenterStyle.setDataFormat(integerFmt);
        s.totalBarIntegerCenterStyle.setAlignment(HorizontalAlignment.CENTER);
        s.totalBarIntegerCenterStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        s.totalBarIntegerCenterStyle.setFillForegroundColor(lightRose);
        s.totalBarIntegerCenterStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        addThinBorders(s.totalBarIntegerCenterStyle);

        return s;
    }

    private void addThinBorders(XSSFCellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }

    private static class Styles {
        XSSFCellStyle bannerTitleStyle;
        XSSFCellStyle bannerSubtitleStyle;
        XSSFCellStyle columnHeaderStyle;
        XSSFCellStyle textStyle;
        XSSFCellStyle zebraTextStyle;
        XSSFCellStyle textCenterStyle;
        XSSFCellStyle zebraTextCenterStyle;
        XSSFCellStyle boldTextStyle;
        XSSFCellStyle boldTextCenterStyle;
        XSSFCellStyle boldTextRightStyle;
        XSSFCellStyle currencyStyle;
        XSSFCellStyle zebraCurrencyStyle;
        XSSFCellStyle boldCurrencyStyle;
        XSSFCellStyle percentStyle;
        XSSFCellStyle zebraPercentStyle;
        XSSFCellStyle integerCenterStyle;
        XSSFCellStyle zebraIntegerCenterStyle;
        XSSFCellStyle boldIntegerStyle;
        XSSFCellStyle totalBarTextStyle;
        XSSFCellStyle totalBarCenterStyle;
        XSSFCellStyle totalBarRightStyle;
        XSSFCellStyle totalBarCurrencyStyle;
        XSSFCellStyle totalBarIntegerCenterStyle;
    }
}
