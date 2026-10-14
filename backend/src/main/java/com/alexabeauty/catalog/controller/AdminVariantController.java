package com.alexabeauty.catalog.controller;

import com.alexabeauty.catalog.dto.*;
import com.alexabeauty.catalog.service.VariantManagementService;
import com.alexabeauty.common.dto.ApiResponse;
import com.alexabeauty.security.UserPrincipal;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/admin/products/{productId}/variants")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
public class AdminVariantController {
    private final VariantManagementService service;
    public AdminVariantController(VariantManagementService service) { this.service=service; }
    public record StatusRequest(@NotNull Boolean active) {}
    @GetMapping public ApiResponse<List<ProductVariantResponse>> list(@PathVariable UUID productId) {
        return ApiResponse.ok("Variantes",service.list(productId));
    }
    @PostMapping public ResponseEntity<ApiResponse<ProductVariantResponse>> add(@PathVariable UUID productId,
            @Valid @RequestBody ProductVariantRequest request, @AuthenticationPrincipal UserPrincipal principal) {
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Variante creada",service.add(productId,request,principal)));
    }
    @PutMapping("/{variantId}") public ApiResponse<ProductVariantResponse> update(@PathVariable UUID productId,
            @PathVariable UUID variantId,@Valid @RequestBody ProductVariantRequest request) {
        return ApiResponse.ok("Variante actualizada",service.update(productId,variantId,request));
    }
    @PatchMapping("/{variantId}/status") public ApiResponse<ProductVariantResponse> status(@PathVariable UUID productId,
            @PathVariable UUID variantId,@Valid @RequestBody StatusRequest request) {
        return ApiResponse.ok("Estado actualizado",service.status(productId,variantId,request.active()));
    }
}
