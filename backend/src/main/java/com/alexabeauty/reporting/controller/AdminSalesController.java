package com.alexabeauty.reporting.controller;

import com.alexabeauty.common.dto.ApiResponse;
import com.alexabeauty.common.error.ValidationException;
import com.alexabeauty.payment.domain.PaymentMethod;
import com.alexabeauty.reporting.dto.CreatePhysicalSaleRequest;
import com.alexabeauty.reporting.dto.SaleOrderResponse;
import com.alexabeauty.reporting.dto.SalesAnalyticsResponse;
import com.alexabeauty.reporting.dto.SalesListResponse;
import com.alexabeauty.reporting.dto.TopProductDto;
import com.alexabeauty.reporting.service.SalesService;
import com.alexabeauty.reporting.service.SalesServiceImpl;
import com.alexabeauty.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
@Tag(name = "Admin Sales & Analytics", description = "Reportes de ventas confirmadas, registro de ventas físicas, métricas y exportación Excel")
@RequiredArgsConstructor
public class AdminSalesController {

    private final SalesService salesService;

    @GetMapping("/sales")
    @Operation(summary = "Listar ventas confirmadas con filtros de fecha y método de pago")
    public ResponseEntity<ApiResponse<SalesListResponse>> getSales(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) PaymentMethod method,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        LocalDate[] range = resolveDateRange(from, to);
        SalesListResponse response = salesService.getSales(range[0], range[1], method, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/sales/today")
    @Operation(summary = "Consultar ventas y ganancias del día en curso (hora Colombia)")
    public ResponseEntity<ApiResponse<SalesListResponse>> getTodaySales(
            @RequestParam(required = false) PaymentMethod method,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        LocalDate today = LocalDate.now(SalesServiceImpl.BOGOTA_ZONE);
        SalesListResponse response = salesService.getSales(today, today, method, pageable);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/sales")
    @Operation(summary = "Registrar una venta realizada en punto físico con descuento automático de stock y pago verificado")
    public ResponseEntity<ApiResponse<SaleOrderResponse>> createPhysicalSale(
            @Valid @RequestBody CreatePhysicalSaleRequest request,
            @AuthenticationPrincipal UserPrincipal actor
    ) {
        SaleOrderResponse response = salesService.createPhysicalSale(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Venta en punto físico registrada exitosamente", response));
    }

    @DeleteMapping("/sales/{id}")
    @Operation(summary = "Anular una venta física o confirmada y reponer stock", description = "Anula la venta, devuelve el inventario a bodega registrando contra-movimiento en Kardex")
    public ResponseEntity<ApiResponse<Void>> cancelSale(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal actor
    ) {
        salesService.cancelSale(id, actor);
        return ResponseEntity.ok(ApiResponse.ok("Venta anulada exitosamente y stock restituido a inventario", null));
    }

    @PostMapping("/sales/{id}/cancel")
    @Operation(summary = "Anular una venta física o confirmada (vía POST)", description = "Alternativa POST para anulación de venta y reposición de inventario")
    public ResponseEntity<ApiResponse<Void>> cancelSalePost(
            @PathVariable UUID id,
            @AuthenticationPrincipal UserPrincipal actor
    ) {
        salesService.cancelSale(id, actor);
        return ResponseEntity.ok(ApiResponse.ok("Venta anulada exitosamente y stock restituido a inventario", null));
    }

    @GetMapping("/sales/export.xlsx")
    @Operation(summary = "Exportar reporte oficial de ventas en formato Excel de 5 hojas")
    public ResponseEntity<byte[]> exportSalesReport(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(required = false) PaymentMethod method,
            @AuthenticationPrincipal UserPrincipal actor
    ) {
        LocalDate[] range = resolveDateRange(from, to);
        byte[] excelBytes = salesService.exportSalesReport(range[0], range[1], method, actor);

        String filename = String.format("AlexaBeauty_Reporte_Ventas_%s_%s.xlsx", range[0], range[1]);

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(excelBytes);
    }

    @GetMapping("/analytics/overview")
    @Operation(summary = "Consultar resumen general y analítica comercial de ventas")
    public ResponseEntity<ApiResponse<SalesAnalyticsResponse>> getAnalyticsOverview(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDate[] range = resolveDateRange(from, to);
        SalesAnalyticsResponse response = salesService.getAnalytics(range[0], range[1]);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/analytics/top-products")
    @Operation(summary = "Ranking de productos más vendidos por ingresos brutos")
    public ResponseEntity<ApiResponse<List<TopProductDto>>> getTopProducts(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to
    ) {
        LocalDate[] range = resolveDateRange(from, to);
        List<TopProductDto> topProducts = salesService.getTopProducts(range[0], range[1]);
        return ResponseEntity.ok(ApiResponse.ok(topProducts));
    }

    private LocalDate[] resolveDateRange(LocalDate from, LocalDate to) {
        LocalDate effectiveTo = (to != null) ? to : LocalDate.now(SalesServiceImpl.BOGOTA_ZONE);
        LocalDate effectiveFrom = (from != null) ? from : effectiveTo.minusDays(30);

        if (effectiveTo.isBefore(effectiveFrom)) {
            throw new ValidationException(String.format("La fecha 'to' (%s) no puede ser anterior a la fecha 'from' (%s).",
                    effectiveTo, effectiveFrom));
        }

        return new LocalDate[]{effectiveFrom, effectiveTo};
    }
}
