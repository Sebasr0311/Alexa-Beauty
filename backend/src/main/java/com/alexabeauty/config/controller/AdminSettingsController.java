package com.alexabeauty.config.controller;

import com.alexabeauty.common.dto.ApiResponse;
import com.alexabeauty.config.dto.AdminSettingsResponse;
import com.alexabeauty.config.dto.UpdateWhatsAppNumberRequest;
import com.alexabeauty.config.service.BusinessSettingsService;
import com.alexabeauty.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/settings")
@RequiredArgsConstructor
@Tag(name = "Admin Settings", description = "Gestión administrativa de la configuración operativa")
public class AdminSettingsController {

    private final BusinessSettingsService businessSettingsService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
    @Operation(summary = "Consultar configuración administrativa", description = "Retorna el número oficial de WhatsApp y fecha de última actualización")
    public ResponseEntity<ApiResponse<AdminSettingsResponse>> getAdminSettings() {
        return ResponseEntity.ok(ApiResponse.ok(
                "Configuración obtenida exitosamente",
                businessSettingsService.getAdminSettings()
        ));
    }

    @PutMapping("/whatsapp")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Actualizar número oficial de WhatsApp", description = "Normaliza, valida y persiste el número de WhatsApp para tienda y pedidos")
    public ResponseEntity<ApiResponse<AdminSettingsResponse>> updateWhatsAppNumber(
            @Valid @RequestBody UpdateWhatsAppNumberRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        AdminSettingsResponse response = businessSettingsService.updateWhatsAppNumber(request.getWhatsappNumber(), principal);
        return ResponseEntity.ok(ApiResponse.ok("Número de WhatsApp actualizado exitosamente", response));
    }
}
