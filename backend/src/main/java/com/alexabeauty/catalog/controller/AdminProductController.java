package com.alexabeauty.catalog.controller;

import com.alexabeauty.catalog.domain.ProductImage;
import com.alexabeauty.catalog.dto.CreateProductRequest;
import com.alexabeauty.catalog.dto.ProductDetailResponse;
import com.alexabeauty.catalog.dto.ProductImageResponse;
import com.alexabeauty.catalog.dto.ProductSummaryResponse;
import com.alexabeauty.catalog.dto.UpdateProductRequest;
import com.alexabeauty.catalog.service.CatalogService;
import com.alexabeauty.common.dto.ApiResponse;
import com.alexabeauty.common.dto.PagedResponse;
import com.alexabeauty.common.error.ValidationException;
import com.alexabeauty.media.MediaService;
import com.alexabeauty.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/products")
@PreAuthorize("hasAnyRole('ADMIN', 'OPERADOR')")
@Tag(name = "Admin Products", description = "Gestión administrativa de productos, variantes e imágenes")
public class AdminProductController {

    private final CatalogService catalogService;
    private final MediaService mediaService;

    public AdminProductController(CatalogService catalogService, MediaService mediaService) {
        this.catalogService = catalogService;
        this.mediaService = mediaService;
    }

    @PostMapping
    @Operation(summary = "Crear nuevo producto", description = "Crea un producto con sus variantes y genera el movimiento de inventario inicial si corresponde")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> createProduct(
            @Valid @RequestBody CreateProductRequest request,
            @AuthenticationPrincipal UserPrincipal principal
    ) {
        ProductDetailResponse response = catalogService.createProduct(request, principal);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Producto creado exitosamente", response));
    }

    @GetMapping
    @Operation(summary = "Listar productos para administración", description = "Retorna productos paginados para el panel de administración")
    public ResponseEntity<ApiResponse<PagedResponse<ProductSummaryResponse>>> listProducts(
            @RequestParam(required = false) String query,
            @PageableDefault(size = 20) Pageable pageable
    ) {
        Page<ProductSummaryResponse> page = catalogService.listAdminProducts(query, pageable);
        return ResponseEntity.ok(ApiResponse.ok("Listado de productos obtenido exitosamente", PagedResponse.from(page)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Obtener producto por ID", description = "Retorna el detalle completo de un producto por su UUID")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> getProductById(@PathVariable UUID id) {
        ProductDetailResponse response = catalogService.getProductById(id);
        return ResponseEntity.ok(ApiResponse.ok("Detalle del producto obtenido exitosamente", response));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Actualizar producto", description = "Actualiza la información general del producto")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> updateProduct(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProductRequest request
    ) {
        ProductDetailResponse response = catalogService.updateProduct(id, request);
        return ResponseEntity.ok(ApiResponse.ok("Producto actualizado exitosamente", response));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Cambiar estado de activación del producto", description = "Activa o desactiva un producto")
    public ResponseEntity<ApiResponse<ProductDetailResponse>> toggleStatus(
            @PathVariable UUID id,
            @RequestParam(required = false) Boolean active,
            @RequestBody(required = false) Map<String, Boolean> body
    ) {
        boolean targetStatus;
        if (active != null) {
            targetStatus = active;
        } else if (body != null && body.containsKey("active")) {
            targetStatus = body.get("active");
        } else {
            throw new ValidationException("El parámetro o campo 'active' es requerido");
        }

        ProductDetailResponse response = catalogService.toggleProductStatus(id, targetStatus);
        return ResponseEntity.ok(ApiResponse.ok("Estado del producto actualizado exitosamente", response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Eliminar o suspender producto", description = "Elimina un producto del catálogo o lo desactiva si tiene historial de pedidos")
    public ResponseEntity<ApiResponse<Void>> deleteProduct(@PathVariable UUID id) {
        catalogService.deleteProduct(id);
        return ResponseEntity.ok(ApiResponse.empty("Producto procesado exitosamente"));
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Subir imagen del producto", description = "Sube una imagen para el producto y la almacena en el gateway configurado")
    public ResponseEntity<ApiResponse<ProductImageResponse>> uploadImage(
            @PathVariable UUID id,
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "altText", required = false) String altText,
            @RequestParam(value = "isCover", required = false, defaultValue = "false") Boolean isCover
    ) {
        ProductImage image = mediaService.uploadProductImage(id, file, altText, isCover);
        String publicUrl = mediaService.getPublicUrl(image.getStoragePath());
        ProductImageResponse response = ProductImageResponse.from(image, publicUrl);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("Imagen subida exitosamente", response));
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @Operation(summary = "Eliminar imagen del producto", description = "Elimina la imagen del almacenamiento y del catálogo")
    public ResponseEntity<ApiResponse<Void>> deleteImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId
    ) {
        mediaService.deleteProductImage(id, imageId);
        return ResponseEntity.ok(ApiResponse.empty("Imagen eliminada exitosamente"));
    }

    @PatchMapping("/{id}/images/{imageId}/cover")
    @Operation(summary = "Establecer imagen de portada", description = "Marca la imagen especificada como portada principal del producto")
    public ResponseEntity<ApiResponse<ProductImageResponse>> setCoverImage(
            @PathVariable UUID id,
            @PathVariable UUID imageId
    ) {
        ProductImage image = mediaService.setCoverImage(id, imageId);
        String publicUrl = mediaService.getPublicUrl(image.getStoragePath());
        ProductImageResponse response = ProductImageResponse.from(image, publicUrl);
        return ResponseEntity.ok(ApiResponse.ok("Imagen establecida como portada", response));
    }
}
