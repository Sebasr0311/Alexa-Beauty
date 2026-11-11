'use client';

import React, { useState, useRef } from 'react';
import Link from 'next/link';
import { Product } from '@/lib/data';
import { api, ProductDetailDto, ProductVariantDto } from '@/lib/api';
import { activeVariants, variantLabel, variantImages } from '@/lib/shades';
import { formatCOP } from '@/lib/utils';
import { useCart } from '@/context/CartContext';
import { ShadeSelectorModal } from '@/components/catalog/ShadeSelectorModal';
import { ProductImage } from '@/components/ProductImage';
import SpotlightCard from '@/components/reactbits/SpotlightCard';
import ClickSpark from '@/components/reactbits/ClickSpark';
import { Check, Plus, Loader2 } from 'lucide-react';

interface ProductCardProps {
  product: Product;
  /** Mark above-the-fold cards so their image is fetched eagerly (LCP). */
  priority?: boolean;
}

const SPARK_COLORS = ['#711A38', '#B7748C', '#E9B8C7'];

export function ProductCard({ product, priority = false }: ProductCardProps) {
  const { addItem } = useCart();
  const addButtonRef = useRef<HTMLButtonElement>(null);
  const [justAdded, setJustAdded] = useState(false);
  const [loadingVariants, setLoadingVariants] = useState(false);
  const [productDetail, setProductDetail] = useState<ProductDetailDto | null>(null);
  const [isShadeModalOpen, setIsShadeModalOpen] = useState(false);
  const isOutOfStock = product.stock <= 0;
  const activeShades = product.variants?.filter((variant) => variant.active) ?? [];

  const flashAdded = () => {
    setJustAdded(true);
    setTimeout(() => setJustAdded(false), 1500);
  };

  const handleAdd = async (e: React.MouseEvent) => {
    e.preventDefault();
    e.stopPropagation();
    if (isOutOfStock || loadingVariants || isShadeModalOpen) return;
    if (productDetail && productDetail.variants && productDetail.variants.length > 1) {
      setIsShadeModalOpen(true);
      return;
    }

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
        flashAdded();
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
    flashAdded();
  };

  const gradientClass =
    product.visual === 'dark'
      ? 'from-[#f0d8de] to-[#d8a1b1]'
      : product.visual === 'gold'
      ? 'from-[#f8eadc] to-[#ead0be]'
      : 'from-[#f6e7e7] to-[#e6c8c8]';

  const slug = encodeURIComponent(product.slug);

  return (
    <SpotlightCard
      theme="light"
      spotlightColor="#B7748C"
      intensity={0.22}
      spotlightSize={220}
      borderGlow={0.8}
      className={`group !p-0 !rounded-2xl h-full ${isShadeModalOpen ? '' : 'md:hover:-translate-y-1'} transition-transform duration-300`}
    >
      <article className="flex h-full flex-col justify-between">
        <div>
          {/* Visual Header */}
          <Link href={`/producto/${slug}`} className="block" aria-label={`Ver ${product.name}`}>
            <div className={`aspect-square relative grid place-items-center bg-gradient-to-br ${gradientClass} overflow-hidden`}>
              {/* Decorative circular aura */}
              <div className="absolute -top-4 -right-6 w-36 h-36 bg-white/40 rounded-full pointer-events-none blur-[1px]" />

              {/* Badge chip */}
              <span className="absolute top-2.5 left-2.5 sm:top-3 sm:left-3 z-10 bg-white/95 backdrop-blur-sm text-wine text-[10px] font-extrabold uppercase tracking-wider px-2.5 py-1 rounded-full shadow-sm">
                {product.badge}
              </span>

              <ProductImage
                src={product.imageUrl}
                alt={product.name}
                priority={priority}
                sizes="(max-width: 640px) 50vw, (max-width: 1024px) 33vw, 280px"
                imgClassName="object-contain p-3 group-hover:scale-105"
                fallback={
                  <span
                    className="text-6xl sm:text-7xl select-none filter drop-shadow-[0_15px_12px_rgba(94,49,61,0.18)] -rotate-6"
                    aria-hidden="true"
                  >
                    {product.icon}
                  </span>
                }
              />
            </div>
          </Link>

          {/* Content Body */}
          <div className="p-3 sm:p-4">
            <span className="text-[#af7b89] text-[10px] font-extrabold tracking-widest uppercase line-clamp-1">
              {product.brand}
            </span>
            <Link href={`/producto/${slug}`} className="block">
              <h3 className="text-ink font-semibold text-sm sm:text-base leading-snug mt-1 min-h-[2.6em] line-clamp-2 hover:text-wine transition-colors">
                {product.name}
              </h3>
            </Link>
            {activeShades.length > 1 && (
              <div className="mt-2 text-xs text-wine">
                <p>Disponible en {activeShades.length} tonos</p>
                <div className="flex gap-1 mt-1" aria-hidden="true">
                  {activeShades.filter((variant) => variant.shadeHex).slice(0, 6).map((variant) => (
                    <span key={variant.id} className="w-4 h-4 rounded-full border border-black/20" style={{ backgroundColor: variant.shadeHex }} />
                  ))}
                </div>
              </div>
            )}
            <p className="text-muted text-xs mt-1">
              {isOutOfStock ? (
                <span className="text-red-600 font-medium">Agotado temporalmente</span>
              ) : product.stock <= 4 ? (
                <span className="text-amber-700 font-medium">
                  {product.stock === 1 ? '¡Última unidad!' : `¡Últimas ${product.stock} unidades!`}
                </span>
              ) : (
                <span>Disponible ({product.stock} unids.)</span>
              )}
            </p>
          </div>
        </div>

        {/* Footer Price & Add */}
        <div className="px-3 sm:px-4 pb-3 sm:pb-4 pt-1 flex items-center justify-between gap-2 border-t border-border/40 mt-auto">
          <span className="text-wine font-extrabold text-base sm:text-xl tracking-tight">
            {formatCOP(product.price)}
          </span>

          <ClickSpark className="!w-auto !h-auto shrink-0" sparkColor={SPARK_COLORS} sparkCount={8} sparkRadius={22} disabled={isOutOfStock}>
            <button
              type="button"
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
          </ClickSpark>
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
    </SpotlightCard>
  );
}
