package com.alexabeauty.catalog.controller;

import com.alexabeauty.catalog.dto.BrandResponse;
import com.alexabeauty.catalog.dto.CreateBrandRequest;
import com.alexabeauty.catalog.service.CatalogService;
import com.alexabeauty.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/brands")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
@Tag(name = "Admin Brands", description = "Gestión administrativa de marcas de productos")
public class AdminBrandController {

    private final CatalogService catalogService;

    public AdminBrandController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping
    @Operation(summary = "Crear marca", description = "Crea una nueva marca en el catálogo")
    public ResponseEntity<ApiResponse<BrandResponse>> createBrand(@Valid @RequestBody CreateBrandRequest request) {
        BrandResponse response = catalogService.createBrand(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Marca creada exitosamente", response));
    }

    @GetMapping
    @Operation(summary = "Listar todas las marcas", description = "Retorna todas las marcas para administración")
    public ResponseEntity<ApiResponse<List<BrandResponse>>> listBrands() {
        List<BrandResponse> brands = catalogService.listBrands(false);
        return ResponseEntity.ok(ApiResponse.ok("Marcas obtenidas exitosamente", brands));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener marca por ID", description = "Retorna los datos de una marca por su ID")
    public ResponseEntity<ApiResponse<BrandResponse>> getBrandById(@PathVariable UUID id) {
        BrandResponse response = catalogService.getBrandById(id);
        return ResponseEntity.ok(ApiResponse.ok("Marca obtenida exitosamente", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar marca", description = "Actualiza los datos de una marca existente")
    public ResponseEntity<ApiResponse<BrandResponse>> updateBrand(
            @PathVariable UUID id,
            @Valid @RequestBody CreateBrandRequest request
    ) {
        BrandResponse response = catalogService.updateBrand(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Marca actualizada exitosamente", response));
    }
}
