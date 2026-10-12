package com.alexabeauty.catalog.controller;

import com.alexabeauty.catalog.dto.BrandResponse;
import com.alexabeauty.catalog.dto.CategoryResponse;
import com.alexabeauty.catalog.dto.ProductDetailResponse;
import com.alexabeauty.catalog.dto.ProductSummaryResponse;
import com.alexabeauty.catalog.service.CatalogService;
import com.alexabeauty.common.dto.ApiResponse;
import com.alexabeauty.common.dto.PagedResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/catalog")
@Tag(name = "Public Catalog", description = "Endpoints públicos de consulta del catálogo de productos, categorías y marcas")
public class PublicCatalogController {

    private final CatalogService catalogService;

    public PublicCatalogController(CatalogService catalogService) {
        this.catalogService = catalogService;
    }

    @GetMapping("/products")
    @Operation(summary = "Listar productos del catálogo", description = "Retorna productos activos paginados con filtros por búsqueda, categoría, marca, precio y stock")
    public ResponseEntity<ApiResponse<PagedResponse<ProductSummaryResponse>>> listProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String brand,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false, defaultValue = "false") Boolean inStockOnly,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ProductSummaryResponse> page = catalogService.listPublicProducts(
                query, category, brand, minPrice, maxPrice, inStockOnly, pageable
        );
        return ResponseEntity.ok(ApiResponse.ok("Productos obtenidos exitosamente", PagedResponse.from(page)));
    }

    @GetMapping("/products/{slug}")
    @Operation(summary = "Detalle del producto por slug", description = "Retorna la información completa de un producto por su slug")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductBySlug(@PathVariable String slug) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isAdmin = auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_OPERADOR"));

        ProductDetailResponse product = catalogService.getProductDetailBySlug(slug, isAdmin);
        return ResponseEntity.ok(ApiResponse.ok("Detalle del producto obtenido exitosamente", product));
    }

    @GetMapping("/categories")
    @Operation(summary = "Listar categorías activas", description = "Retorna las categorías activas del catálogo")
    public ResponseEntity<ApiResponse<List<CategoryResponse>>> getCategories() {
        List<CategoryResponse> categories = catalogService.listCategories(true);
        return ResponseEntity.ok(ApiResponse.ok("Categorías obtenidas exitosamente", categories));
    }

    @GetMapping("/brands")
    @Operation(summary = "Listar marcas activas", description = "Retorna las marcas activas del catálogo")
    public ResponseEntity<ApiResponse<List<BrandResponse>>> getBrands() {
        List<BrandResponse> brands = catalogService.listBrands(true);
        return ResponseEntity.ok(ApiResponse.ok("Marcas obtenidas exitosamente", brands));
    }
}
