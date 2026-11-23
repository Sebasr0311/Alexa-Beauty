package com.alexabeauty.config.controller;

import com.alexabeauty.common.dto.ApiResponse;
import com.alexabeauty.config.dto.PublicStoreSettingsResponse;
import com.alexabeauty.config.service.BusinessSettingsService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/settings")
@RequiredArgsConstructor
@Tag(name = "Public Settings", description = "Configuración pública de contacto de la tienda")
public class PublicSettingsController {

    private final BusinessSettingsService businessSettingsService;

    @GetMapping("/public")
    @Operation(summary = "Obtener configuración pública de la tienda", description = "Retorna el número oficial de WhatsApp vigente")
    public ResponseEntity<ApiResponse<PublicStoreSettingsResponse>> getPublicSettings() {
        PublicStoreSettingsResponse response = businessSettingsService.getPublicSettings();
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(ApiResponse.ok("Configuración pública obtenida exitosamente", response));
    }
}
