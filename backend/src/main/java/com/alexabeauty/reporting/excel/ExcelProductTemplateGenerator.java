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
 * Generates the official Excel bulk product import template workbook (ALEXA_PRODUCTS_V2).
 * Supports product grouping by producto_codigo, shade variations, and atomic validation.
 * Comprises:
 *  - Sheet 1: PRODUCTOS with exact headers and wine styling (#711A38).
 *  - Sheet 2: INSTRUCCIONES detailing requirements, data types, grouping rules and safety.
 *  - Sheet 3: EJEMPLOS_NO_IMPORTAR with 4 sample rows illustrating grouping and variants.
 */
@Component
public class ExcelProductTemplateGenerator {

    public static final String TEMPLATE_VERSION = "ALEXA_PRODUCTS_V2";
    public static final String TEMPLATE_FILENAME = "AlexaBeauty_Plantilla_Productos_v2.xlsx";

    public static final String[] HEADERS_V1 = {
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

    public static final String[] HEADERS = {
            "producto_codigo",
            "nombre",
            "marca",
            "categoria",
            "sku_variante",
            "tono_codigo",
            "tono_nombre",
            "tono_hex",
            "presentacion",
            "precio_venta_cop",
            "costo_unitario_cop",
            "stock_inicial",
            "stock_minimo",
            "descripcion",
            "publicado"
    };

    public static final String[] HEADERS_V2 = HEADERS;

    private static final byte[] WINE_RGB = new byte[]{(byte) 0x71, (byte) 0x1A, (byte) 0x38}; // #711A38
    private static final byte[] GOLD_RGB = new byte[]{(byte) 0xC9, (byte) 0xA2, (byte) 0x27}; // #C9A227

    /**
     * Generates the XLSX V2 template as a byte array.
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

        int[] widths = {5500, 7000, 5000, 5000, 5000, 4000, 5000, 3500, 4000, 5000, 5000, 4000, 4000, 8000, 3500};
        for (int i = 0; i < widths.length && i < HEADERS.length; i++) {
            sheet.setColumnWidth(i, widths[i]);
        }
    }

    private void createInstruccionesSheet(XSSFWorkbook workbook, XSSFCellStyle headerStyle) {
        XSSFSheet sheet = workbook.createSheet("INSTRUCCIONES");

        Row titleRow = sheet.createRow(0);
        titleRow.setHeightInPoints(24);
        Cell titleCell = titleRow.createCell(0);
        titleCell.setCellValue("ALEXA BEAUTY - GUÍA DE IMPORTACIÓN MASIVA DE PRODUCTOS (V2 AGRUPADA)");

        String[][] instructions = {
                {"producto_codigo", "OBLIGATORIO en V2. Código que agrupa variantes bajo el mismo producto (ej. BASE-FITME). Filas con el mismo código formarán un único producto con múltiples tonos."},
                {"nombre", "Obligatorio (2–150 caracteres). Nombre comercial del producto."},
                {"marca", "Obligatorio (2–100 caracteres). Si la marca no existe, se creará automáticamente."},
                {"categoria", "Opcional. Categoría principal (ej. Rostro, Labios, Ojos). Por defecto: 'General'."},
                {"sku_variante", "Opcional. Código SKU único de la variante o tono (máx. 50 caracteres). Si se deja vacío, el sistema generará uno automáticamente."},
                {"tono_codigo", "Opcional si tiene nombre o presentación. Código de tono comercial (ej. 110, 115, 01)."},
                {"tono_nombre", "Opcional si tiene código o presentación. Nombre descriptivo del tono (ej. Classic Ivory, Rojo Pasión)."},
                {"tono_hex", "Opcional. Color hexadecimal para el swatch visual (ej. #E5B899). Debe iniciar con # seguido de 6 dígitos hexadecimales."},
                {"presentacion", "Opcional si tiene código o nombre de tono. Contenido o presentación (ej. 30ml, 5g)."},
                {"precio_venta_cop", "Obligatorio. Número decimal mayor o igual a 0. Precio de venta en pesos colombianos."},
                {"costo_unitario_cop", "Opcional. Costo de referencia para control administrativo y márgenes."},
                {"stock_inicial", "Obligatorio. Número entero mayor o igual a 0. Cantidad física inicial en mano. Se registrará un movimiento ENTRADA_INICIAL en Kardex."},
                {"stock_minimo", "Opcional. Entero mayor a 0 para alerta de bajo stock. Por defecto: 3."},
                {"descripcion", "Opcional. Texto libre para la ficha del producto."},
                {"publicado", "Opcional (SI / NO). Si el producto estará activo en el catálogo público inmediatamente. Por defecto: SI."}
        };

        Row headerRow = sheet.createRow(2);
        Cell h1 = headerRow.createCell(0);
        h1.setCellValue("Columna");
        h1.setCellStyle(headerStyle);
        Cell h2 = headerRow.createCell(1);
        h2.setCellValue("Reglas y Descripción");
        h2.setCellStyle(headerStyle);

        for (int i = 0; i < instructions.length; i++) {
            Row row = sheet.createRow(3 + i);
            row.createCell(0).setCellValue(instructions[i][0]);
            row.createCell(1).setCellValue(instructions[i][1]);
        }

        sheet.setColumnWidth(0, 6000);
        sheet.setColumnWidth(1, 24000);
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
                {"BASE-FITME", "Base Líquida Fit Me Matte", "Maybelline", "Rostro", "MAY-FIT-110", "110", "Porcelain", "#E5B899", "30ml", 48000, 26000, 15, 3, "Base líquida mate con cobertura uniforme", "SI"},
                {"BASE-FITME", "Base Líquida Fit Me Matte", "Maybelline", "Rostro", "MAY-FIT-115", "115", "Ivory", "#E1AD8B", "30ml", 48000, 26000, 10, 3, "Base líquida mate con cobertura uniforme", "SI"},
                {"BASE-FITME", "Base Líquida Fit Me Matte", "Maybelline", "Rostro", "MAY-FIT-120", "120", "Classic Ivory", "#DC9F78", "30ml", 48000, 26000, 8, 3, "Base líquida mate con cobertura uniforme", "SI"},
                {"SERUM-HIAL", "Serum Hidratante Ácido Hialurónico", "L'Oréal", "Cuidado Facial", "LOR-SER-30ML", "", "Original", "", "30ml", 62000, 35000, 5, 3, "Tratamiento facial intensivo para hidratación profunda", "SI"}
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
