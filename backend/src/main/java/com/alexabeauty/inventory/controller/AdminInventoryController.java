package com.alexabeauty.inventory.controller;

import com.alexabeauty.common.dto.ApiResponse;
import com.alexabeauty.common.dto.PagedResponse;
import com.alexabeauty.inventory.domain.MovementType;
import com.alexabeauty.inventory.dto.CreateStockReceiptRequest;
import com.alexabeauty.inventory.dto.InventoryMovementResponse;
import com.alexabeauty.inventory.dto.InventorySummaryResponse;
import com.alexabeauty.inventory.dto.StockAdjustmentRequest;
import com.alexabeauty.inventory.dto.StockAdjustmentResponse;
import com.alexabeauty.inventory.dto.StockReceiptResponse;
import com.alexabeauty.inventory.service.InventoryService;
import com.alexabeauty.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/inventory")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
@Tag(name = "Admin Inventory", description = "Gestión de inventario, recepciones, ajustes y Kardex")
public class AdminInventoryController {

    private final InventoryService inventoryService;

    public AdminInventoryController(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    @GetMapping("/summary")
    @Operation(summary = "Resumen de inventario", description = "Listado paginado de existencias con stock físico, reservado, disponible y filtro de bajo stock")
    public ResponseEntity<ApiResponse<PagedResponse<InventorySummaryResponse>>> getInventorySummary(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Boolean lowStockOnly,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<InventorySummaryResponse> page = inventoryService.getInventorySummary(query, lowStockOnly, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Resumen de inventario obtenido exitosamente", PagedResponse.from(page)));
    }

    @GetMapping("/export.xlsx")
    @Operation(summary = "Exportar inventario completo a Excel", description = "Genera un archivo Excel oficial con todos los productos, variantes, stock físico, reservado, disponible, costos de inversión y precios de venta")
    public ResponseEntity<byte[]> exportInventoryReport(@AuthenticationPrincipal UserPrincipal actor) {
        byte[] bytes = inventoryService.exportInventoryReport(actor);
        String filename = String.format("AlexaBeauty_Inventario_Productos_%s.xlsx", LocalDate.now(ZoneId.of("America/Bogota")));
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(bytes);
    }

    @GetMapping("/movements")
    @Operation(summary = "Kardex de movimientos", description = "Historial paginado de movimientos de inventario con filtros por variante, tipo y rango de fechas")
    public ResponseEntity<ApiResponse<PagedResponse<InventoryMovementResponse>>> getMovements(
            @RequestParam(required = false) UUID variantId,
            @RequestParam(required = false) MovementType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<InventoryMovementResponse> page = inventoryService.getKardexMovements(variantId, type, from, to, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Movimientos de inventario obtenidos exitosamente", PagedResponse.from(page)));
    }

    @PostMapping("/receipts")
    @Operation(summary = "Registrar recepción de mercancía", description = "Crea una recepción de mercancía, incrementa stock_on_hand de las variantes y genera movimientos ENTRADA con soporte de idempotencia")
    public ResponseEntity<ApiResponse<StockReceiptResponse>> createReceipt(
            @Valid @RequestBody CreateStockReceiptRequest request,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        StockReceiptResponse response = inventoryService.createReceipt(request, idempotencyKey, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Recepción de mercancía registrada exitosamente", response));
    }

    @GetMapping("/receipts")
    @Operation(summary = "Listar recepciones de mercancía", description = "Listado paginado de recepciones ordenado cronológicamente descendente")
    public ResponseEntity<ApiResponse<PagedResponse<StockReceiptResponse>>> listReceipts(
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<StockReceiptResponse> page = inventoryService.listReceipts(pageable);
        return ResponseEntity.ok(ApiResponse.ok("Listado de recepciones obtenido exitosamente", PagedResponse.from(page)));
    }

    @GetMapping("/receipts/{id}")
    @Operation(summary = "Obtener recepción por ID", description = "Detalle completo de una recepción con sus ítems por UUID")
    public ResponseEntity<ApiResponse<StockReceiptResponse>> getReceiptById(@PathVariable UUID id) {
        StockReceiptResponse response = inventoryService.getReceiptById(id);
        return ResponseEntity.ok(ApiResponse.ok("Detalle de recepción obtenido exitosamente", response));
    }

    @PostMapping("/adjustments")
    @Operation(summary = "Ajuste manual de stock auditado", description = "Ajusta el stock físico de una variante garantizando que newStockOnHand >= stockReserved, registrando en Kardex y en el log de auditoría")
    public ResponseEntity<ApiResponse<StockAdjustmentResponse>> adjustStock(
            @Valid @RequestBody StockAdjustmentRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        StockAdjustmentResponse response = inventoryService.adjustStock(request, principal);
        return ResponseEntity.ok(ApiResponse.ok("Ajuste de inventario realizado exitosamente", response));
    }
}
