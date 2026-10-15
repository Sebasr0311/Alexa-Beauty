package com.alexabeauty.catalog.service;

import com.alexabeauty.auth.repository.UserRepository;
import com.alexabeauty.catalog.domain.*;
import com.alexabeauty.catalog.dto.*;
import com.alexabeauty.catalog.repository.*;
import com.alexabeauty.common.error.*;
import com.alexabeauty.inventory.domain.*;
import com.alexabeauty.inventory.repository.InventoryMovementRepository;
import com.alexabeauty.security.UserPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

@Service @Transactional
public class VariantManagementService {
    private final ProductRepository products;
    private final ProductVariantRepository variants;
    private final InventoryMovementRepository movements;
    private final UserRepository users;
    public VariantManagementService(ProductRepository products, ProductVariantRepository variants,
            InventoryMovementRepository movements, UserRepository users) {
        this.products=products; this.variants=variants; this.movements=movements; this.users=users;
    }
    @Transactional(readOnly=true)
    public List<ProductVariantResponse> list(UUID productId) {
        if (!products.existsById(productId)) throw new ResourceNotFoundException("Product", productId);
        return variants.findByProductId(productId).stream().map(ProductVariantResponse::from).toList();
    }
    private Product lock(UUID productId) {
        return products.findByIdForUpdate(productId).orElseThrow(() -> new ResourceNotFoundException("Product", productId));
    }
    private ProductVariant owned(UUID productId, UUID variantId) {
        var variant=variants.findByIdForUpdate(variantId).orElseThrow(() -> new ResourceNotFoundException("ProductVariant",variantId));
        if (!variant.getProduct().getId().equals(productId)) throw new ResourceNotFoundException("ProductVariant",variantId);
        return variant;
    }
    private void duplicate(UUID productId, UUID except, String key) {
        if (variants.findByProductId(productId).stream().anyMatch(v -> !v.getId().equals(except) && VariantRules.key(v).equals(key)))
            throw new ConflictException("Ya existe este tono y presentación en el producto.");
    }
    public ProductVariantResponse add(UUID productId, ProductVariantRequest request, UserPrincipal principal) {
        var product=lock(productId);
        VariantRules.validateCommercialIdentity(request);
        duplicate(productId,null,VariantRules.key(request));
        String sku=VariantRules.sku(request.sku());
        if (variants.existsBySku(sku) || products.existsBySku(sku)) throw new ConflictException("El SKU ya existe: "+sku);
        if (request.salePrice()==null) throw new ValidationException("El precio es obligatorio.");
        var variant=ProductVariant.builder().product(product).sku(sku).salePrice(request.salePrice())
                .referenceCost(request.referenceCost()).minStock(request.minStock()==null?3:request.minStock())
                .active(request.active()==null||request.active()).stockOnHand(request.stockOnHand()==null?0:request.stockOnHand()).build();
        VariantRules.metadata(variant,request);
        variant = variants.saveAndFlush(variant);
        if (variant.getStockOnHand()>0) movements.save(InventoryMovement.builder().variant(variant)
                .movementType(MovementType.ENTRADA_INICIAL).qtyBefore(0).qtyAfter(variant.getStockOnHand())
                .quantityDelta(variant.getStockOnHand()).reference("ALTA_VARIANTE").reason("Stock inicial al registrar variante")
                .user(principal==null?null:users.findById(principal.getId()).orElse(null)).build());
        return ProductVariantResponse.from(variant);
    }
    public ProductVariantResponse update(UUID productId, UUID variantId, ProductVariantRequest request) {
        lock(productId);
        var variant=owned(productId,variantId);
        if (request.stockOnHand()!=null) throw new ValidationException("stockOnHand: utilice el módulo de inventario para modificar existencias.");
        if (request.sku()!=null && !request.sku().equals(variant.getSku())) throw new ValidationException("El SKU existente no se modifica.");
        if (request.shadeCode() != null || request.shadeName() != null || request.presentation() != null) {
            String nextCode = request.shadeCode() != null ? request.shadeCode() : variant.getShadeCode();
            String nextName = request.shadeName() != null ? request.shadeName() : variant.getShadeName();
            String nextPres = request.presentation() != null ? request.presentation() : variant.getPresentation();
            VariantRules.validateCommercialIdentity(nextCode, nextName, nextPres);
        }
        if (!VariantRules.key(variant).equals(VariantRules.key(request))) duplicate(productId,variantId,VariantRules.key(request));
        VariantRules.metadata(variant,request);
        if (request.salePrice()==null) throw new ValidationException("El precio es obligatorio.");
        variant.setSalePrice(request.salePrice());
        if(request.minStock()!=null)variant.setMinStock(request.minStock());
        if(request.referenceCost()!=null)variant.setReferenceCost(request.referenceCost());
        if(request.active()!=null)variant.setActive(request.active());
        return ProductVariantResponse.from(variants.saveAndFlush(variant));
    }
    public ProductVariantResponse status(UUID productId, UUID variantId, boolean active) {
        lock(productId); var variant=owned(productId,variantId); variant.setActive(active);
        return ProductVariantResponse.from(variants.saveAndFlush(variant));
    }
}
