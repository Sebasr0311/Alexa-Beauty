package com.alexabeauty.catalog.controller;

import com.alexabeauty.catalog.dto.CategoryResponse;
import com.alexabeauty.catalog.dto.CreateCategoryRequest;
import com.alexabeauty.catalog.service.CatalogService;
import com.alexabeauty.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
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
@RequestMapping("/api/v1/admin/categories")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
@Tag(name = "Admin Categories", description = "Gestión administrativa de categorías de productos")
public class AdminCategoryController {

    private final CatalogService catalogService;

    public AdminCategoryController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @PostMapping
    @Operation(summary = "Crear categoría", description = "Crea una nueva categoría en el catálogo")
    public ResponseEntity<ApiResponse<CategoryResponse>> createCategory(@Valid @RequestBody CreateCategoryRequest request) {
        CategoryResponse response = catalogService.createCategory(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Categoría creada exitosamente", response));
    }

    @GetMapping
    @Operation(summary = "Listar todas las categorías", description = "Retorna todas las categorías para administración")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> listCategories() {
        List<CategoryResponse> categories = catalogService.listCategories(false);
        return ResponseEntity.ok(ApiResponse.ok("Categorías obtenidas exitosamente", categories));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener categoría por ID", description = "Retorna los datos de una categoría por su ID")
    public ResponseEntity<ApiResponse<CategoryResponse>> getCategoryById(@PathVariable UUID id) {
        CategoryResponse response = catalogService.getCategoryById(id);
        return ResponseEntity.ok(ApiResponse.ok("Categoría obtenida exitosamente", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar categoría", description = "Actualiza los datos de una categoría existente")
    public ResponseEntity<ApiResponse<CategoryResponse>> updateCategory(
            @PathVariable UUID id,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        CategoryResponse response = catalogService.updateCategory(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Categoría actualizada exitosamente", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar categoría", description = "Elimina una categoría si no tiene productos asociados")
    public ResponseEntity<ApiResponse<Void>> deleteCategory(@PathVariable UUID id) {
        catalogService.deleteCategory(id);
        return ResponseEntity.ok(ApiResponse.ok("Categoría eliminada exitosamente", null));
    }
}
