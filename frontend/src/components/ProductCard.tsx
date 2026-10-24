'use client';

import React, { useState, useRef, useEffect, useMemo } from 'react';
import Link from 'next/link';
import { Product } from '@/lib/data';
import { api, ProductDetailDto, ProductVariantDto } from '@/lib/api';
import { activeVariants, variantLabel, variantImages } from '@/lib/shades';
import { formatCOP } from '@/lib/utils';
import { useCart } from '@/context/CartContext';
import { ShadeSelectorModal } from '@/components/catalog/ShadeSelectorModal';
import { Check, Plus, Loader2 } from 'lucide-react';

interface ProductCardProps {
  product: Product;
}

export function ProductCard({ product }: ProductCardProps) {
  const { addItem } = useCart();
  const addButtonRef = useRef<HTMLButtonElement>(null);
  const [justAdded, setJustAdded] = useState(false);
  const [loadingVariants, setLoadingVariants] = useState(false);
  const [productDetail, setProductDetail] = useState<ProductDetailDto | null>(null);
  const [isShadeModalOpen, setIsShadeModalOpen] = useState(false);
  const [imageError, setImageError] = useState(false);
  const isOutOfStock = product.stock <= 0;



  const handleAdd = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (isOutOfStock || loadingVariants) return;

    try {
      setLoadingVariants(true);
      const detail = await api.getProductBySlug(product.slug);
      detail.variants = activeVariants(detail.variants);
      setProductDetail(detail);

      if (detail.variants && detail.variants.length > 1) {
        setIsShadeModalOpen(true);
      } else if (detail.variants && detail.variants.length === 1) {
        const single = detail.variants[0];
        const vImages = variantImages(detail.images, single.id);
        const chosenImage = vImages[0]?.url || product.imageUrl;
        const result = addItem(
          {
            ...product,
            price: single.salePrice,
            stock: single.availableStock,
            variantId: single.id,
            imageUrl: chosenImage,
          },
          1,
          single.id,
          variantLabel(single)
        );
        if (!result.success) { window.alert(result.message); return; }
        setJustAdded(true);
        setTimeout(() => setJustAdded(false), 1500);
      } else {
        window.alert('No hay variantes disponibles para este producto.');
      }
    } catch (err) {
      console.error('Error fetching variants on quick-add:', err);
      window.location.assign(`/producto/${encodeURIComponent(product.slug)}`);
    } finally {
      setLoadingVariants(false);
    }
  };

  const handleSelectVariant = (variant: ProductVariantDto) => {
    if (!productDetail) return;
    const vImages = variantImages(productDetail.images, variant.id);
    const chosenImage = vImages[0]?.url || product.imageUrl;
    const result = addItem(
      {
        ...product,
        name: product.name,
        price: variant.salePrice,
        stock: variant.availableStock,
        variantId: variant.id,
        imageUrl: chosenImage,
      },
      1,
      variant.id,
      variantLabel(variant)
    );
    if (!result.success) { window.alert(result.message); return; }
    setIsShadeModalOpen(false);
    setJustAdded(true);
    setTimeout(() => setJustAdded(false), 1500);
  };

  const gradientClass =
    product.visual === 'dark'
      ? 'from-[#f0d8de] to-[#d8a1b1]'
      : product.visual === 'gold'
      ? 'from-[#f8eadc] to-[#ead0be]'
      : 'from-[#f6e7e7] to-[#e6c8c8]';

  const slug = encodeURIComponent(product.slug);

  return (
    <article className="group bg-white border border-[#efdee3] rounded-2xl overflow-hidden shadow-[0_9px_26px_rgba(58,26,39,0.035)] hover:-translate-y-1 transition duration-200 flex flex-col justify-between">
      <div>
        {/* Visual Header */}
        <Link href={`/producto/${slug}`} className="block">
          <div
            className={`h-48 sm:h-56 relative grid place-items-center bg-gradient-to-br ${gradientClass} overflow-hidden`}
          >
            {/* Decorative circular aura */}
            <div className="absolute -top-4 -right-6 w-36 h-36 bg-white/40 rounded-full pointer-events-none blur-[1px]" />

            {/* Badge chip */}
            <span className="absolute top-3 left-3 z-10 bg-white/95 backdrop-blur-sm text-wine text-[10px] font-extrabold uppercase tracking-wider px-2.5 py-1 rounded-full shadow-sm">
              {product.badge}
            </span>

            {/* Product Image or Fallback Emoji */}
            {product.imageUrl && !imageError ? (
              <img
                src={product.imageUrl}
                alt={product.name}
                onError={() => setImageError(true)}
                className="w-full h-full object-contain p-3 transition-transform duration-300 group-hover:scale-105"
                loading="lazy"
              />
            ) : (
              <span
                className="text-6xl sm:text-7xl select-none filter drop-shadow-[0_15px_12px_rgba(94,49,61,0.18)] -rotate-6 group-hover:scale-105 transition-transform duration-300"
                aria-hidden="true"
              >
                {product.icon}
              </span>
            )}
          </div>
        </Link>

        {/* Content Body */}
        <div className="p-4">
          <span className="text-[#af7b89] text-[10px] font-extrabold tracking-widest uppercase">
            {product.brand}
          </span>
          <Link href={`/producto/${slug}`} className="block">
            <h3 className="text-ink font-semibold text-sm sm:text-base leading-snug mt-1 min-h-[42px] line-clamp-2 hover:text-wine transition-colors">
              {product.name}
            </h3>
          </Link>
          {(product.variants?.filter(variant => variant.active).length || 0) > 1 && <div className="mt-2 text-xs text-wine">
            <p>Disponible en {product.variants!.filter(variant => variant.active).length} tonos</p>
            <div className="flex gap-1 mt-1" aria-hidden="true">{product.variants!.filter(variant => variant.active && variant.shadeHex).slice(0,6).map(variant =>
              <span key={variant.id} className="w-4 h-4 rounded-full border border-black/20" style={{backgroundColor:variant.shadeHex}} />)}</div>
          </div>}
          <p className="text-muted text-xs mt-1">
            {isOutOfStock ? (
              <span className="text-red-600 font-medium">Agotado temporalmente</span>
            ) : product.stock <= 4 ? (
              <span className="text-amber-700 font-medium">¡Últimas {product.stock} unidades!</span>
            ) : (
              <span>Disponible ({product.stock} unids.)</span>
            )}
          </p>
        </div>
      </div>

      {/* Footer Price & Add */}
      <div className="px-4 pb-4 pt-1 flex items-center justify-between border-t border-border/40 mt-auto">
        <span className="text-wine font-extrabold text-lg sm:text-xl tracking-tight">
          {formatCOP(product.price)}
        </span>

        <button
          ref={addButtonRef}
          onClick={handleAdd}
          disabled={isOutOfStock || loadingVariants}
          aria-label={`Agregar o elegir tono de ${product.name}`}
          className={`w-11 h-11 rounded-full flex items-center justify-center transition-all ${
            isOutOfStock
              ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
              : justAdded
              ? 'bg-success text-white scale-105'
              : 'bg-wine hover:bg-wineDark text-white shadow-md active:scale-95'
          }`}
        >
          {loadingVariants ? (
            <Loader2 className="w-4 h-4 animate-spin text-white" />
          ) : justAdded ? (
            <Check className="w-5 h-5 stroke-[2.5]" />
          ) : isOutOfStock ? (
            <span className="text-sm font-bold">×</span>
          ) : (
            <Plus className="w-5 h-5 stroke-[2.5]" />
          )}
        </button>
      </div>

      {/* Shade Selection Modal for multi-tone products */}
      <ShadeSelectorModal
        returnFocusRef={addButtonRef}
        product={productDetail}
        isOpen={isShadeModalOpen}
        onClose={() => setIsShadeModalOpen(false)}
        onSelectVariant={handleSelectVariant}
      />
    </article>
  );
}
