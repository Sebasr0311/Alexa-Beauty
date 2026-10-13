package com.alexabeauty.reporting.controller;

import com.alexabeauty.common.dto.ApiResponse;
import com.alexabeauty.reporting.dto.CommitImportRequest;
import com.alexabeauty.reporting.dto.ConflictPolicy;
import com.alexabeauty.reporting.dto.ExcelCommitResponse;
import com.alexabeauty.reporting.dto.ExcelImportPreviewResponse;
import com.alexabeauty.reporting.excel.ExcelProductTemplateGenerator;
import com.alexabeauty.reporting.service.ExcelImportService;
import com.alexabeauty.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

/**
 * Controller for bulk product import via Excel, template downloading, preview diagnostics, and atomic commit.
 */
@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin Product Import", description = "Importación masiva de productos vía Excel y gestión de diagnósticos")
public class AdminProductImportController {

    private final ExcelImportService excelImportService;

    public AdminProductImportController(ExcelImportService excelImportService) {
        this.excelImportService = excelImportService;
    }

    @GetMapping({"/import/template", "/imports/template"})
    @Operation(summary = "Descargar plantilla Excel oficial para importación de productos")
    public ResponseEntity<byte[]> downloadTemplate() {
        byte[] templateBytes = excelImportService.generateTemplate();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + ExcelProductTemplateGenerator.TEMPLATE_FILENAME + "\"")
                .body(templateBytes);
    }

    @PostMapping(value = "/imports/preview", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Diagnóstico y validación previa de archivo Excel")
    public ResponseEntity<ApiResponse<ExcelImportPreviewResponse>> previewImport(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ExcelImportPreviewResponse response = excelImportService.processPreview(file, principal);
        return ResponseEntity.ok(ApiResponse.ok("Diagnóstico de importación generado exitosamente", response));
    }

    @PostMapping("/imports/{jobId}/commit")
    @Operation(summary = "Compromiso atómico de importación masiva de productos")
    public ResponseEntity<ApiResponse<ExcelCommitResponse>> commitImport(
            @PathVariable UUID jobId,
            @RequestParam(required = false) ConflictPolicy policy,
            @RequestBody(required = false) CommitImportRequest requestBody,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ConflictPolicy effectivePolicy = ConflictPolicy.CANCEL_ON_ERROR;
        if (requestBody != null && requestBody.policy() != null) {
            effectivePolicy = requestBody.policy();
        } else if (policy != null) {
            effectivePolicy = policy;
        }

        ExcelCommitResponse response = excelImportService.commitImport(jobId, effectivePolicy, principal);
        return ResponseEntity.ok(ApiResponse.ok("Importación masiva completada exitosamente", response));
    }

    @GetMapping("/imports/{jobId}/rejected.xlsx")
    @Operation(summary = "Descargar archivo Excel con filas rechazadas y motivo")
    public ResponseEntity<byte[]> downloadRejectedRows(@PathVariable UUID jobId) {
        byte[] excelBytes = excelImportService.generateRejectedRowsExcel(jobId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"rechazados_import_" + jobId + ".xlsx\"")
                .body(excelBytes);
    }
}
