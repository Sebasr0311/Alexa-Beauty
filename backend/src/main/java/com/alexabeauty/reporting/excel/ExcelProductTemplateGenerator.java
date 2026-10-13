package com.alexabeauty.reporting.excel;

import org.apache.poi.ss.usermodel.Cell;
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
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * Generates the official Excel bulk product import template workbook (ALEXA_PRODUCTS_V1).
 * Comprises:
 *  - Sheet 1: PRODUCTOS with exact headers and wine styling (#711A38).
 *  - Sheet 2: INSTRUCCIONES detailing requirements, data types, and safety rules.
 *  - Sheet 3: EJEMPLOS_NO_IMPORTAR with 3 sample rows.
 */
@Component
public class ExcelProductTemplateGenerator {

    public static final String TEMPLATE_VERSION = "ALEXA_PRODUCTS_V1";
    public static final String TEMPLATE_FILENAME = "AlexaBeauty_Plantilla_Productos_v1.xlsx";

    public static final String[] HEADERS = {
            "nombre",
            "marca",
            "stock_inicial",
            "precio_venta_cop",
            "sku",
            "categoria",
            "tono",
            "presentacion",
            "costo_unitario_cop",
            "stock_minimo",
            "descripcion",
            "publicado"
    };

    private static final byte[] WINE_RGB = new byte[]{(byte) 0x71, (byte) 0x1A, (byte) 0x38}; // #711A38
    private static final byte[] GOLD_RGB = new byte[]{(byte) 0xC9, (byte) 0xA2, (byte) 0x27}; // #C9A227

    /**
     * Generates the XLSX template as a byte array.
     */
    public byte[] generateTemplate() {
        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            XSSFCellStyle wineHeaderStyle = createHeaderStyle(workbook, WINE_RGB);
            XSSFCellStyle instructionHeaderStyle = createHeaderStyle(workbook, GOLD_RGB);

            createProductosSheet(workbook, wineHeaderStyle);
            createInstruccionesSheet(workbook, instructionHeaderStyle);
            createEjemplosSheet(workbook, wineHeaderStyle);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new IllegalStateException("Error generating Excel product template", e);
        }
    }

    private void createProductosSheet(XSSFWorkbook workbook, XSSFCellStyle headerStyle) {
        XSSFSheet sheet = workbook.createSheet("PRODUCTOS");

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(28);

        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new CellRangeAddress(0, 0, 0, HEADERS.length - 1));

        int[] widths = {7000, 5000, 4000, 5000, 5000, 5000, 4500, 4500, 5000, 4000, 9000, 3500};
        for (int i = 0; i < widths.length; i++) {
            sheet.setColumnWidth(i, widths[i]);
        }
    }

    private void createInstruccionesSheet(XSSFWorkbook workbook, XSSFCellStyle headerStyle) {
        XSSFSheet sheet = workbook.createSheet("INSTRUCCIONES");

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(24);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("ALEXA BEAUTY - GUÍA DE IMPORTACIÓN MASIVA DE PRODUCTOS");

        Row versionRow = sheet.createRow(1);
        versionRow.createCell(0).setCellValue("Versión de Plantilla: " + TEMPLATE_VERSION);

        Row emptyRow = sheet.createRow(2);

        Row guideHeaderRow = sheet.createRow(3);
        guideHeaderRow.setHeightInPoints(24);
        String[] guideHeaders = {"Columna", "Obligatorio", "Tipo de Dato", "Descripción y Reglas de Validación"};
        for (int i = 0; i < guideHeaders.length; i++) {
            Cell cell = guideHeaderRow.createCell(i);
            cell.setCellValue(guideHeaders[i]);
            cell.setCellStyle(headerStyle);
        }

        String[][] columnRules = {
                {"nombre", "SÍ", "Texto (2 - 150)", "Nombre del producto. Obligatorio. Debe ser único o representativo."},
                {"marca", "SÍ", "Texto (2 - 100)", "Marca del producto. Si no existe en el sistema, será creada automáticamente."},
                {"stock_inicial", "SÍ", "Entero (>= 0)", "Stock físico disponible. Genera un movimiento de Kárdex tipo ENTRADA_INICIAL si es > 0."},
                {"precio_venta_cop", "SÍ", "Numérico (>= 0)", "Precio de venta en COP. No puede ser negativo."},
                {"sku", "NO", "Texto (<= 50)", "Código único del producto/variante. Si está vacío, se generará automáticamente (AB-MARCA-XXXX)."},
                {"categoria", "NO", "Texto (<= 100)", "Categoría del producto. Si no existe, se creará automáticamente. Por defecto 'General'."},
                {"tono", "NO", "Texto (<= 100)", "Nombre del tono o variante de color (ej. 'Tono 120', 'Rojo Pasión'). Por defecto 'Default'."},
                {"presentacion", "NO", "Texto (<= 100)", "Tamaño o presentación física (ej. '30ml', '50g', 'Unidad')."},
                {"costo_unitario_cop", "NO", "Numérico (>= 0)", "Costo de referencia de adquisición por unidad en COP."},
                {"stock_minimo", "NO", "Entero (>= 0)", "Umbral mínimo para generar alertas de inventario bajo. Por defecto es 3."},
                {"descripcion", "NO", "Texto", "Descripción detallada del producto para la tienda virtual."},
                {"publicado", "NO", "SI / NO", "Indica si el producto estará visible inmediatamente en el catálogo. Por defecto es SI."}
        };

        for (int i = 0; i < columnRules.length; i++) {
            Row row = sheet.createRow(4 + i);
            row.setHeightInPoints(20);
            for (int j = 0; j < columnRules[i].length; j++) {
                row.createCell(j).setCellValue(columnRules[i][j]);
            }
        }

        int noteStart = 4 + columnRules.length + 1;
        Row note1 = sheet.createRow(noteStart);
        note1.createCell(0).setCellValue("SEGURIDAD: Prohibido ingresar fórmulas de Excel (=, +, -, @). Cualquier celda con fórmulas será neutralizada.");
        Row note2 = sheet.createRow(noteStart + 1);
        note2.createCell(0).setCellValue("IMPORTANTE: Diligencie únicamente la hoja 'PRODUCTOS' a partir de la fila 2. No renombre ni elimine columnas.");

        sheet.setColumnWidth(0, 5000);
        sheet.setColumnWidth(1, 3500);
        sheet.setColumnWidth(2, 5000);
        sheet.setColumnWidth(3, 16000);
    }

    private void createEjemplosSheet(XSSFWorkbook workbook, XSSFCellStyle headerStyle) {
        XSSFSheet sheet = workbook.createSheet("EJEMPLOS_NO_IMPORTAR");

        Row headerRow = sheet.createRow(0);
        headerRow.setHeightInPoints(28);
        for (int i = 0; i < HEADERS.length; i++) {
            Cell cell = headerRow.createCell(i);
            cell.setCellValue(HEADERS[i]);
            cell.setCellStyle(headerStyle);
        }

        Object[][] sampleData = {
                {"Labial Líquido Mate Larga Duración", "Vogue", 12, 18000, "VOG-LIP-MATE-01", "Labios", "Rojo Pasión", "5ml", 9500, 3, "Labial líquido mate de alta fijación indeleble", "SI"},
                {"Base Líquida HD Acabado Natural", "Maybelline", 8, 48000, "MAY-BAS-HD-120", "Rostro", "Tono 120 Canela", "30ml", 26000, 2, "Base líquida mate con cobertura uniforme y textura ligera", "SI"},
                {"Serum Hidratante con Ácido Hialurónico", "L'Oréal", 5, 62000, "", "Cuidado Facial", "Original", "30ml", 35000, 3, "Tratamiento facial intensivo para hidratación profunda", "NO"}
        };

        for (int i = 0; i < sampleData.length; i++) {
            Row row = sheet.createRow(1 + i);
            row.setHeightInPoints(20);
            for (int j = 0; j < sampleData[i].length; j++) {
                Cell cell = row.createCell(j);
                Object val = sampleData[i][j];
                if (val instanceof Number n) {
                    cell.setCellValue(n.doubleValue());
                } else if (val != null) {
                    cell.setCellValue(val.toString());
                }
            }
        }

        sheet.createFreezePane(0, 1);
        for (int i = 0; i < HEADERS.length; i++) {
            sheet.setColumnWidth(i, 5500);
        }
    }

    private XSSFCellStyle createHeaderStyle(XSSFWorkbook workbook, byte[] rgb) {
        XSSFCellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(new XSSFColor(rgb, new DefaultIndexedColorMap()));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        style.setVerticalAlignment(VerticalAlignment.CENTER);

        XSSFFont font = workbook.createFont();
        font.setBold(true);
        font.setColor(new XSSFColor(new byte[]{(byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, new DefaultIndexedColorMap()));
        font.setFontHeightInPoints((short) 11);
        style.setFont(font);

        return style;
    }
}
