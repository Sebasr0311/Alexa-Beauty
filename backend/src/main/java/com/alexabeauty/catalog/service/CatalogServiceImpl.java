package com.alexabeauty.catalog.service;

import com.alexabeauty.auth.domain.User;
import com.alexabeauty.auth.repository.UserRepository;
import com.alexabeauty.catalog.domain.Brand;
import com.alexabeauty.catalog.domain.Category;
import com.alexabeauty.catalog.domain.Product;
import com.alexabeauty.catalog.domain.ProductImage;
import com.alexabeauty.catalog.domain.ProductVariant;
import com.alexabeauty.catalog.dto.BrandResponse;
import com.alexabeauty.catalog.dto.CategoryResponse;
import com.alexabeauty.catalog.dto.CreateBrandRequest;
import com.alexabeauty.catalog.dto.CreateCategoryRequest;
import com.alexabeauty.catalog.dto.CreateProductRequest;
import com.alexabeauty.catalog.dto.ProductDetailResponse;
import com.alexabeauty.catalog.dto.ProductImageResponse;
import com.alexabeauty.catalog.dto.ProductSummaryResponse;
import com.alexabeauty.catalog.dto.ProductVariantRequest;
import com.alexabeauty.catalog.dto.ProductVariantResponse;
import com.alexabeauty.catalog.dto.UpdateProductRequest;
import com.alexabeauty.catalog.repository.BrandRepository;
import com.alexabeauty.catalog.repository.CategoryRepository;
import com.alexabeauty.catalog.repository.ProductRepository;
import com.alexabeauty.catalog.repository.ProductVariantRepository;
import com.alexabeauty.catalog.util.SlugUtils;
import com.alexabeauty.common.error.ConflictException;
import com.alexabeauty.common.error.ResourceNotFoundException;
import com.alexabeauty.common.error.ValidationException;
import com.alexabeauty.inventory.domain.InventoryMovement;
import com.alexabeauty.inventory.domain.MovementType;
import com.alexabeauty.inventory.repository.InventoryMovementRepository;
import com.alexabeauty.media.MediaService;
import com.alexabeauty.security.UserPrincipal;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@Transactional
public class CatalogServiceImpl implements CatalogService {

    private static final Logger log = LoggerFactory.getLogger(CatalogServiceImpl.class);

    private final ProductRepository productRepository;
    private final ProductVariantRepository productVariantRepository;
    private final BrandRepository brandRepository;
    private final CategoryRepository categoryRepository;
    private final InventoryMovementRepository inventoryMovementRepository;
    private final UserRepository userRepository;
    private final MediaService mediaService;

    public CatalogServiceImpl(
            ProductRepository productRepository,
            ProductVariantRepository productVariantRepository,
            BrandRepository brandRepository,
            CategoryRepository categoryRepository,
            InventoryMovementRepository inventoryMovementRepository,
            UserRepository userRepository,
            MediaService mediaService
    ) {
        this.productRepository = productRepository;
        this.productVariantRepository = productVariantRepository;
        this.brandRepository = brandRepository;
        this.categoryRepository = categoryRepository;
        this.inventoryMovementRepository = inventoryMovementRepository;
        this.userRepository = userRepository;
        this.mediaService = mediaService;
    }

    @Override
    public ProductDetailResponse createProduct(CreateProductRequest request, UserPrincipal principal) {
        // 1. Validate Brand and Category existence
        Brand brand = brandRepository.findById(request.brandId())
                .orElseThrow(() -> new ResourceNotFoundException("Brand", request.brandId()));
        Category category = categoryRepository.findById(request.categoryId())
                .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));

        // 2. Validate price
        if (request.salePrice() == null || request.salePrice().compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidationException("El precio de venta no puede ser negativo");
        }

        // 3. Validate SKU uniqueness
        String baseSku = request.sku().trim();
        if (productRepository.existsBySku(baseSku) || productVariantRepository.existsBySku(baseSku)) {
            throw new ConflictException("El SKU ya existe: " + baseSku);
        }

        // Validate variant SKUs and prices if present
        Set<String> assignedSkus = new HashSet<>();
        assignedSkus.add(baseSku);

        if (request.variants() != null && !request.variants().isEmpty()) {
            for (ProductVariantRequest vr : request.variants()) {
                if (vr.salePrice() != null && vr.salePrice().compareTo(BigDecimal.ZERO) < 0) {
                    throw new ValidationException("El precio de venta de la variante no puede ser negativo");
                }
                if (vr.sku() != null && !vr.sku().isBlank()) {
                    String vSku = vr.sku().trim();
                    if (!assignedSkus.add(vSku)) {
                        throw new ConflictException("SKU duplicado en la solicitud: " + vSku);
                    }
                    if (productRepository.existsBySku(vSku) || productVariantRepository.existsBySku(vSku)) {
                        throw new ConflictException("El SKU de la variante ya existe: " + vSku);
                    }
                }
            }
        }

        // 4. Generate unique slug
        String slug = SlugUtils.generateUniqueSlug(request.name(), productRepository::existsBySlug);

        // 5. Build Product entity
        Product product = Product.builder()
                .sku(baseSku)
                .name(request.name().trim())
                .slug(slug)
                .brand(brand)
                .category(category)
                .description(request.description())
                .active(request.isActiveProduct())
                .featured(request.isFeaturedProduct())
                .variants(new ArrayList<>())
                .images(new ArrayList<>())
                .build();

        // 6. Build variants
        if (request.variants() == null || request.variants().isEmpty()) {
            int stockOnHand = request.stockOnHand() != null ? request.stockOnHand() : 0;
            int minStock = request.minStock() != null ? request.minStock() : 3;

            ProductVariant defaultVariant = ProductVariant.builder()
                    .product(product)
                    .sku(baseSku)
                    .shadeName("Default")
                    .salePrice(request.salePrice())
                    .stockOnHand(stockOnHand)
                    .stockReserved(0)
                    .minStock(minStock)
                    .active(true)
                    .build();
            product.getVariants().add(defaultVariant);
        } else {
            int index = 1;
            for (ProductVariantRequest vr : request.variants()) {
                String variantSku = vr.sku() != null && !vr.sku().isBlank()
                        ? vr.sku().trim()
                        : String.format("%s-%d", baseSku, index);
                BigDecimal price = vr.salePrice() != null ? vr.salePrice() : request.salePrice();
                int stock = vr.stockOnHand() != null ? vr.stockOnHand() : (index == 1 && request.stockOnHand() != null ? request.stockOnHand() : 0);
                int minStock = vr.minStock() != null ? vr.minStock() : (request.minStock() != null ? request.minStock() : 3);
                boolean variantActive = vr.active() == null || vr.active();

                ProductVariant variant = ProductVariant.builder()
                        .product(product)
                        .sku(variantSku)
                        .shadeName(vr.shadeName())
                        .presentation(vr.presentation())
                        .salePrice(price)
                        .referenceCost(vr.referenceCost())
                        .stockOnHand(stock)
                        .stockReserved(0)
                        .minStock(minStock)
                        .active(variantActive)
                        .build();
                product.getVariants().add(variant);
                index++;
            }
        }

        // 7. Save Product (variants saved by cascade)
        Product savedProduct = productRepository.save(product);

        // 8. CRITICAL INVENTORY RULE: create initial inventory movements
        User user = null;
        if (principal != null && principal.getId() != null) {
            user = userRepository.findById(principal.getId()).orElse(null);
        }

        for (ProductVariant variant : savedProduct.getVariants()) {
            if (variant.getStockOnHand() > 0) {
                InventoryMovement movement = InventoryMovement.builder()
                        .variant(variant)
                        .movementType(MovementType.ENTRADA_INICIAL)
                        .qtyBefore(0)
                        .qtyAfter(variant.getStockOnHand())
                        .quantityDelta(variant.getStockOnHand())
                        .reference("ALTA_PRODUCTO")
                        .reason("Stock inicial al registrar producto")
                        .user(user)
                        .build();
                inventoryMovementRepository.save(movement);
                log.info("Registered ENTRADA_INICIAL inventory movement for variant {} with qty {}", variant.getSku(), variant.getStockOnHand());
            }
        }

        return toProductDetailResponse(savedProduct);
    }

    @Override
    public ProductDetailResponse updateProduct(UUID id, UpdateProductRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        if (request.brandId() != null) {
            Brand brand = brandRepository.findById(request.brandId())
                    .orElseThrow(() -> new ResourceNotFoundException("Brand", request.brandId()));
            product.setBrand(brand);
        }

        if (request.categoryId() != null) {
            Category category = categoryRepository.findById(request.categoryId())
                    .orElseThrow(() -> new ResourceNotFoundException("Category", request.categoryId()));
            product.setCategory(category);
        }

        if (request.name() != null && !request.name().isBlank()) {
            String newName = request.name().trim();
            if (!newName.equalsIgnoreCase(product.getName())) {
                product.setName(newName);
                String newSlug = SlugUtils.generateUniqueSlug(newName, candidate ->
                        productRepository.existsBySlug(candidate) && !candidate.equals(product.getSlug()));
                product.setSlug(newSlug);
            }
        }

        if (request.description() != null) {
            product.setDescription(request.description());
        }

        if (request.featured() != null) {
            product.setFeatured(request.featured());
        }

        if (request.active() != null) {
            product.setActive(request.active());
        }

        Product updated = productRepository.save(product);
        return toProductDetailResponse(updated);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductDetailBySlug(String slug, boolean isAdmin) {
        Product product = productRepository.findBySlug(slug)
                .orElseThrow(() -> new ResourceNotFoundException("Product", slug));

        if (!product.isActive() && !isAdmin) {
            throw new ResourceNotFoundException("Product", slug);
        }

        return toProductDetailResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public ProductDetailResponse getProductById(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        return toProductDetailResponse(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> listPublicProducts(
            String query,
            String categorySlug,
            String brandSlug,
            BigDecimal minPrice,
            BigDecimal maxPrice,
            Boolean inStockOnly,
            Pageable pageable
    ) {
        Specification<Product> spec = (root, cq, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // 1. Only active products
            predicates.add(cb.isTrue(root.get("active")));

            // 2. Category filter
            if (categorySlug != null && !categorySlug.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.join("category").get("slug")), categorySlug.trim().toLowerCase(Locale.ROOT)));
            }

            // 3. Brand filter
            if (brandSlug != null && !brandSlug.isBlank()) {
                predicates.add(cb.equal(cb.lower(root.join("brand").get("slug")), brandSlug.trim().toLowerCase(Locale.ROOT)));
            }

            // 4. Text query filter
            if (query != null && !query.isBlank()) {
                String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
                Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
                Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
                Predicate skuLike = cb.like(cb.lower(root.get("sku")), pattern);
                predicates.add(cb.or(nameLike, descLike, skuLike));
            }

            // 5. Price & Stock filters via Subquery on variants
            boolean filterByVariant = (minPrice != null) || (maxPrice != null) || Boolean.TRUE.equals(inStockOnly);
            if (filterByVariant) {
                Subquery<UUID> subquery = cq.subquery(UUID.class);
                Root<ProductVariant> variantRoot = subquery.from(ProductVariant.class);
                subquery.select(variantRoot.get("product").get("id"));

                List<Predicate> variantPreds = new ArrayList<>();
                variantPreds.add(cb.isTrue(variantRoot.get("active")));

                if (minPrice != null) {
                    variantPreds.add(cb.greaterThanOrEqualTo(variantRoot.get("salePrice"), minPrice));
                }
                if (maxPrice != null) {
                    variantPreds.add(cb.lessThanOrEqualTo(variantRoot.get("salePrice"), maxPrice));
                }
                if (Boolean.TRUE.equals(inStockOnly)) {
                    variantPreds.add(cb.greaterThan(
                            cb.diff(variantRoot.get("stockOnHand"), variantRoot.get("stockReserved")),
                            0
                    ));
                }

                subquery.where(variantPreds.toArray(new Predicate[0]));
                predicates.add(root.get("id").in(subquery));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        return productRepository.findAll(spec, pageable).map(this::toProductSummaryResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<ProductSummaryResponse> listAdminProducts(String query, Pageable pageable) {
        Specification<Product> spec = (root, cq, cb) -> {
            if (query == null || query.isBlank()) {
                return cb.conjunction();
            }
            String pattern = "%" + query.trim().toLowerCase(Locale.ROOT) + "%";
            Predicate nameLike = cb.like(cb.lower(root.get("name")), pattern);
            Predicate descLike = cb.like(cb.lower(root.get("description")), pattern);
            Predicate skuLike = cb.like(cb.lower(root.get("sku")), pattern);
            Predicate brandLike = cb.like(cb.lower(root.join("brand", JoinType.LEFT).get("name")), pattern);
            return cb.or(nameLike, descLike, skuLike, brandLike);
        };

        return productRepository.findAll(spec, pageable).map(this::toProductSummaryResponse);
    }

    @Override
    public ProductDetailResponse toggleProductStatus(UUID id, boolean active) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));
        product.setActive(active);
        Product saved = productRepository.save(product);
        return toProductDetailResponse(saved);
    }

    @Override
    public void deleteProduct(UUID id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product", id));

        boolean hasOrders = productRepository.hasOrderItems(id);
        boolean hasReceipts = productRepository.hasStockReceipts(id);

        if (hasOrders || hasReceipts) {
            log.info("Product id={} has orders or receipts. Deactivating (soft-delete) instead of hard-delete.", id);
            product.setActive(false);
            productRepository.save(product);
            return;
        }

        productRepository.deleteReservationsByProductId(id);
        productRepository.deleteMovementsByProductId(id);

        List<ProductImage> images = new ArrayList<>(product.getImages());
        for (ProductImage img : images) {
            try {
                mediaService.deleteProductImage(id, img.getId());
            } catch (Exception e) {
                log.warn("Could not delete image id={} from storage: {}", img.getId(), e.getMessage());
            }
        }

        productRepository.delete(product);
        log.info("Product id={} deleted permanently.", id);
    }

    // Brand operations
    @Override
    public BrandResponse createBrand(CreateBrandRequest request) {
        String slug = (request.slug() != null && !request.slug().isBlank())
                ? SlugUtils.toSlug(request.slug())
                : SlugUtils.generateUniqueSlug(request.name(), brandRepository::existsBySlug);

        if (brandRepository.existsBySlug(slug)) {
            throw new ConflictException("La marca con el slug '" + slug + "' ya existe");
        }

        Brand brand = Brand.builder()
                .name(request.name().trim())
                .slug(slug)
                .active(request.active() == null || request.active())
                .build();

        return BrandResponse.from(brandRepository.save(brand));
    }

    @Override
    public BrandResponse updateBrand(UUID id, CreateBrandRequest request) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", id));

        if (request.name() != null && !request.name().isBlank()) {
            brand.setName(request.name().trim());
        }
        if (request.slug() != null && !request.slug().isBlank()) {
            String newSlug = SlugUtils.toSlug(request.slug());
            if (!newSlug.equals(brand.getSlug()) && brandRepository.existsBySlug(newSlug)) {
                throw new ConflictException("El slug '" + newSlug + "' ya está en uso");
            }
            brand.setSlug(newSlug);
        }
        if (request.active() != null) {
            brand.setActive(request.active());
        }

        return BrandResponse.from(brandRepository.save(brand));
    }

    @Override
    @Transactional(readOnly = true)
    public List<BrandResponse> listBrands(boolean onlyActive) {
        List<Brand> brands = onlyActive
                ? brandRepository.findByActiveTrue()
                : brandRepository.findAllByOrderByNameAsc();
        return brands.stream().map(BrandResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BrandResponse getBrandById(UUID id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Brand", id));
        return BrandResponse.from(brand);
    }

    // Category operations
    @Override
    public CategoryResponse createCategory(CreateCategoryRequest request) {
        String slug = (request.slug() != null && !request.slug().isBlank())
                ? SlugUtils.toSlug(request.slug())
                : SlugUtils.generateUniqueSlug(request.name(), categoryRepository::existsBySlug);

        if (categoryRepository.existsBySlug(slug)) {
            throw new ConflictException("La categoría con el slug '" + slug + "' ya existe");
        }

        Category category = Category.builder()
                .name(request.name().trim())
                .slug(slug)
                .sortOrder(request.sortOrder() != null ? request.sortOrder() : 0)
                .active(request.active() == null || request.active())
                .build();

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Override
    public CategoryResponse updateCategory(UUID id, CreateCategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));

        if (request.name() != null && !request.name().isBlank()) {
            category.setName(request.name().trim());
        }
        if (request.slug() != null && !request.slug().isBlank()) {
            String newSlug = SlugUtils.toSlug(request.slug());
            if (!newSlug.equals(category.getSlug()) && categoryRepository.existsBySlug(newSlug)) {
                throw new ConflictException("El slug '" + newSlug + "' ya está en uso");
            }
            category.setSlug(newSlug);
        }
        if (request.sortOrder() != null) {
            category.setSortOrder(request.sortOrder());
        }
        if (request.active() != null) {
            category.setActive(request.active());
        }

        return CategoryResponse.from(categoryRepository.save(category));
    }

    @Override
    @Transactional(readOnly = true)
    public List<CategoryResponse> listCategories(boolean onlyActive) {
        List<Category> categories = onlyActive
                ? categoryRepository.findByActiveTrueOrderBySortOrderAsc()
                : categoryRepository.findAllByOrderBySortOrderAsc();
        return categories.stream().map(CategoryResponse::from).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(UUID id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Category", id));
        return CategoryResponse.from(category);
    }

    // Helpers
    private ProductDetailResponse toProductDetailResponse(Product product) {
        List<ProductVariantResponse> variantResponses = product.getVariants().stream()
                .map(ProductVariantResponse::from)
                .toList();

        List<ProductImageResponse> imageResponses = product.getImages().stream()
                .map(img -> ProductImageResponse.from(img, mediaService.getPublicUrl(img.getStoragePath())))
                .toList();

        BigDecimal minPrice = product.getVariants().stream()
                .map(ProductVariant::getSalePrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = product.getVariants().stream()
                .map(ProductVariant::getSalePrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        int totalStockOnHand = product.getVariants().stream()
                .mapToInt(ProductVariant::getStockOnHand)
                .sum();

        int totalAvailableStock = product.getVariants().stream()
                .mapToInt(v -> Math.max(0, v.getStockOnHand() - v.getStockReserved()))
                .sum();

        return new ProductDetailResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getSlug(),
                BrandResponse.from(product.getBrand()),
                CategoryResponse.from(product.getCategory()),
                product.getDescription(),
                product.isActive(),
                product.isFeatured(),
                minPrice,
                maxPrice,
                totalStockOnHand,
                totalAvailableStock,
                variantResponses,
                imageResponses,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }

    private ProductSummaryResponse toProductSummaryResponse(Product product) {
        BigDecimal minPrice = product.getVariants().stream()
                .map(ProductVariant::getSalePrice)
                .min(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        BigDecimal maxPrice = product.getVariants().stream()
                .map(ProductVariant::getSalePrice)
                .max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);

        int totalStockOnHand = product.getVariants().stream()
                .mapToInt(ProductVariant::getStockOnHand)
                .sum();

        int totalAvailableStock = product.getVariants().stream()
                .mapToInt(v -> Math.max(0, v.getStockOnHand() - v.getStockReserved()))
                .sum();

        ProductImage coverImg = product.getImages().stream()
                .filter(ProductImage::isCover)
                .findFirst()
                .orElse(product.getImages().isEmpty() ? null : product.getImages().get(0));

        String coverUrl = coverImg != null ? mediaService.getPublicUrl(coverImg.getStoragePath()) : null;

        return new ProductSummaryResponse(
                product.getId(),
                product.getSku(),
                product.getName(),
                product.getSlug(),
                BrandResponse.from(product.getBrand()),
                CategoryResponse.from(product.getCategory()),
                product.getDescription(),
                product.isActive(),
                product.isFeatured(),
                minPrice,
                maxPrice,
                totalStockOnHand,
                totalAvailableStock,
                coverUrl,
                product.getCreatedAt(),
                product.getUpdatedAt()
        );
    }
}
