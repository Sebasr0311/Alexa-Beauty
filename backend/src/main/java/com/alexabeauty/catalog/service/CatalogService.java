package com.alexabeauty.catalog.service;

import com.alexabeauty.catalog.dto.BrandResponse;
import com.alexabeauty.catalog.dto.CategoryResponse;
import com.alexabeauty.catalog.dto.CreateBrandRequest;
import com.alexabeauty.catalog.dto.CreateCategoryRequest;
import com.alexabeauty.catalog.dto.CreateProductRequest;
import com.alexabeauty.catalog.dto.ProductDetailResponse;
import com.alexabeauty.catalog.dto.ProductSummaryResponse;
import com.alexabeauty.catalog.dto.UpdateProductRequest;
import com.alexabeauty.security.UserPrincipal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public interface CatalogService {

    ProductDetailResponse createProduct(CreateProductRequest request, UserPrincipal principal);

    ProductDetailResponse updateProduct(UUID id, UpdateProductRequest request);

    ProductDetailResponse getProductDetailBySlug(String slug, boolean isAdmin);

    ProductDetailResponse getProductById(UUID id);

    Page<ProductSummaryResponse> listPublicProducts(
            String query,
            String categorySlug,
            String brandSlug,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStockOnly,
            Pageable pageable
    );

    Page<ProductSummaryResponse> listAdminProducts(String query, Pageable pageable);

    ProductDetailResponse toggleProductStatus(UUID id, boolean active);

    void deleteProduct(UUID id);

    // Brand operations
    BrandResponse createBrand(CreateBrandRequest request);

    BrandResponse updateBrand(UUID id, CreateBrandRequest request);

    List<BrandResponse> listBrands(boolean onlyActive);

    BrandResponse getBrandById(UUID id);

    // Category operations
    CategoryResponse createCategory(CreateCategoryRequest request);

    CategoryResponse updateCategory(UUID id, CreateCategoryRequest request);

    List<CategoryResponse> listCategories(boolean onlyActive);

    CategoryResponse getCategoryById(UUID id);

    void deleteCategory(UUID id);
}
