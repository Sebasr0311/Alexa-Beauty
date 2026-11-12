'use client';

import React, { useEffect, useRef, useState } from 'react';
import Link from 'next/link';
import { api, ProductDetailDto, ProductVariantDto } from '@/lib/api';
import { whatsappContact } from '@/lib/contact';
import { formatCOP } from '@/lib/utils';
import { ShadeSelector } from '@/components/catalog/ShadeSelector';
import { initialVariant, variantImages, variantLabel } from '@/lib/shades';
import { useCart } from '@/context/CartContext';
import { ProductImage } from '@/components/ProductImage';
import ClickSpark from '@/components/reactbits/ClickSpark';
import { ArrowLeft, Check, ChevronLeft, ChevronRight, MessageCircleHeart, Minus, Plus, ShieldCheck, Sparkles, Truck } from 'lucide-react';

interface ProductDetailViewProps {
  slug: string;
  initialProduct: ProductDetailDto | null;
}

const SWIPE_THRESHOLD_PX = 40;

export default function ProductDetailView({ slug, initialProduct }: ProductDetailViewProps) {
  const { addItem } = useCart();
  const [product, setProduct] = useState<ProductDetailDto | null>(initialProduct);
  const [selectedVariant, setSelectedVariant] = useState<ProductVariantDto | null>(
    initialProduct ? initialVariant(initialProduct.variants) : null,
  );
  const [quantity, setQuantity] = useState(1);
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);
  const [isPaused, setIsPaused] = useState(false);
  const [justAdded, setJustAdded] = useState(false);
  const [loading, setLoading] = useState(!initialProduct);
  const touchStartX = useRef<number | null>(null);

  const images = product ? variantImages(product.images, selectedVariant?.id) : [];

  useEffect(() => {
    if (!product || images.length <= 1 || isPaused) return;
    const interval = setInterval(() => {
      setSelectedImageIndex((prev) => (prev + 1) % images.length);
    }, 4500);
    return () => clearInterval(interval);
  }, [images.length, isPaused, product]);

  // Always refresh in the browser: the server snapshot may be up to 60s old and
  // stock must be current before the customer adds to cart.
  useEffect(() => {
    let cancelled = false;
    api.getProductBySlug(slug)
      .then((data) => {
        if (cancelled) return;
        setProduct(data);
        setSelectedVariant((current) => data.variants.find((variant) => variant.id === current?.id) ?? initialVariant(data.variants));
      })
      .catch(() => {
        if (!cancelled && !initialProduct) { setProduct(null); setSelectedVariant(null); }
      })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [slug, initialProduct]);

  const showImage = (index: number) => {
    if (images.length === 0) return;
    setSelectedImageIndex((index + images.length) % images.length);
  };

  const onTouchStart = (e: React.TouchEvent) => {
    touchStartX.current = e.touches[0].clientX;
    setIsPaused(true);
  };

  const onTouchEnd = (e: React.TouchEvent) => {
    if (touchStartX.current === null) return;
    const delta = e.changedTouches[0].clientX - touchStartX.current;
    touchStartX.current = null;
    if (Math.abs(delta) < SWIPE_THRESHOLD_PX) return;
    showImage(selectedImageIndex + (delta < 0 ? 1 : -1));
  };

  if (loading) {
    return (
      <div className="max-w-5xl mx-auto py-2 sm:py-8" role="status" aria-label="Cargando producto">
        <div className="h-4 w-32 rounded-full shimmer mb-6" />
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6 md:gap-10">
          <div className="aspect-square w-full rounded-3xl shimmer" />
          <div className="space-y-4">
            <div className="h-3 w-40 rounded-full shimmer" />
            <div className="h-8 w-3/4 rounded-xl shimmer" />
            <div className="h-10 w-1/2 rounded-xl shimmer" />
            <div className="h-12 w-full rounded-full shimmer" />
          </div>
        </div>
      </div>
    );
  }

  if (!product) {
    return (
      <div className="max-w-md mx-auto px-4 py-20 text-center">
        <h2 className="font-serif text-2xl text-wine mb-2">Producto no encontrado</h2>
        <p className="text-muted text-sm mb-6">El producto no existe o no pudimos consultar sus datos. No mostraremos existencias sin verificar.</p>
        <Link href="/catalogo" className="inline-flex items-center gap-2 bg-wine text-white px-6 py-3 rounded-full font-bold text-sm hover:bg-wineDark transition">
          <ArrowLeft className="w-4 h-4" /> Volver al catálogo
        </Link>
      </div>
    );
  }

  const currentPrice = selectedVariant ? selectedVariant.salePrice : 0;
  const whatsappHref = whatsappContact(`Hola Alexa Beauty, tengo dudas sobre ${product.name}.`);
  const currentStock = selectedVariant ? selectedVariant.availableStock : 0;
  const isOutOfStock = currentStock <= 0;

  const handleAddToCart = () => {
    if (isOutOfStock || !selectedVariant?.active) return;
    const chosenImage = images[selectedImageIndex]?.url || images[0]?.url || (product.images && product.images[0]?.url);
    // Map to CartContext format
    const res = addItem(
      {
        id: product.id,
        slug: product.slug,
        brand: product.brand.name,
        name: product.name,
        category: (product.category.name as any) || 'Rostro',
        price: currentPrice,
        stock: currentStock,
        icon: '💄',
        visual: 'dark',
        badge: 'Destacado',
        variantId: selectedVariant?.id,
        imageUrl: chosenImage,
      },
      quantity,
      selectedVariant?.id,
      selectedVariant ? variantLabel(selectedVariant) : undefined
    );

    if (res.success) {
      setJustAdded(true);
      setTimeout(() => setJustAdded(false), 1500);
    } else if (res.message) {
      alert(res.message);
    }
  };

  return (
    <div className="max-w-5xl mx-auto py-1 sm:py-8 pb-28 md:pb-0">
      {/* Breadcrumb / Back button */}
      <div className="mb-4 sm:mb-6">
        <Link href="/catalogo" className="inline-flex items-center gap-1.5 min-h-11 text-sm font-bold text-muted hover:text-wine transition">
          <ArrowLeft className="w-4 h-4" /> Volver al catálogo
        </Link>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-6 lg:gap-12 items-start">
        {/* Visual Gallery 1:1 Aspect Ratio */}
        <div className="space-y-3 sm:space-y-4 md:sticky md:top-24">
          <div
            onMouseEnter={() => setIsPaused(true)}
            onMouseLeave={() => setIsPaused(false)}
            onTouchStart={onTouchStart}
            onTouchEnd={onTouchEnd}
            className="group/carousel relative aspect-square w-full rounded-3xl overflow-hidden bg-gradient-to-br from-[#f8e4ea] to-[#ebd0d9] border border-border shadow-[0_12px_36px_rgba(92,26,49,0.06)] grid place-items-center touch-pan-y"
          >
            {/* Preload every gallery image so switching slides is instant */}
            {images.length > 0 ? (
              images.map((img, idx) => (
                <ProductImage
                  key={img.id ?? img.url}
                  src={img.url}
                  alt={img.altText || product.name}
                  sizes="(max-width: 768px) 100vw, 520px"
                  priority={idx === 0}
                  quality={80}
                  className={`transition-opacity duration-500 ${idx === selectedImageIndex ? 'opacity-100' : 'opacity-0 pointer-events-none'}`}
                  imgClassName="object-contain p-4"
                />
              ))
            ) : (
              <Sparkles className="w-20 h-20 text-wine/25" strokeWidth={1.2} aria-hidden="true" />
            )}

            {/* Carousel navigation & indicators if multiple images */}
            {images.length > 1 && (
              <>
                <button
                  type="button"
                  onClick={() => setSelectedImageIndex((prev) => (prev - 1 + images.length) % images.length)}
                  aria-label="Foto anterior"
                  className="absolute left-2 sm:left-3 top-1/2 -translate-y-1/2 z-20 w-10 h-10 rounded-full bg-white/85 backdrop-blur-sm text-wine shadow-md flex items-center justify-center md:opacity-0 md:group-hover/carousel:opacity-100 focus-visible:opacity-100 transition-opacity hover:bg-white active:scale-95"
                >
                  <ChevronLeft className="w-5 h-5 stroke-[2.5]" />
                </button>
                <button
                  type="button"
                  onClick={() => setSelectedImageIndex((prev) => (prev + 1) % images.length)}
                  aria-label="Foto siguiente"
                  className="absolute right-2 sm:right-3 top-1/2 -translate-y-1/2 z-20 w-10 h-10 rounded-full bg-white/85 backdrop-blur-sm text-wine shadow-md flex items-center justify-center md:opacity-0 md:group-hover/carousel:opacity-100 focus-visible:opacity-100 transition-opacity hover:bg-white active:scale-95"
                >
                  <ChevronRight className="w-5 h-5 stroke-[2.5]" />
                </button>

                {/* Pagination indicator dots */}
                <div className="absolute bottom-3 inset-x-0 z-20 flex items-center justify-center gap-1.5">
                  {images.map((_, idx) => (
                    <button
                      key={idx}
                      type="button"
                      onClick={() => setSelectedImageIndex(idx)}
                      aria-label={`Ver foto ${idx + 1}`}
                      className={`h-2 rounded-full transition-all duration-300 ${
                        selectedImageIndex === idx ? 'w-5 bg-wine' : 'w-2 bg-wine/30 hover:bg-wine/60'
                      }`}
                    />
                  ))}
                </div>
              </>
            )}

            {isOutOfStock && (
              <span className="absolute top-4 left-4 z-10 bg-red-600/95 text-white text-xs font-bold px-3 py-1.5 rounded-full shadow-md">
                Agotado
              </span>
            )}
            <span className="absolute top-4 right-4 z-10 bg-white/80 backdrop-blur-sm text-wine text-[11px] font-extrabold tracking-wider uppercase px-3 py-1 rounded-full border border-border/50">
              {product.brand.name}
            </span>
          </div>

          {images.length > 1 && (
            <div className="flex gap-3 overflow-x-auto no-scrollbar p-1 -m-1 snap-x">
              {images.map((img, idx) => (
                <button
                  key={img.id}
                  type="button"
                  onClick={() => { setIsPaused(true); setSelectedImageIndex(idx); }}
                  aria-label={`Ver imagen ${idx + 1}`}
                  aria-current={selectedImageIndex === idx}
                  className={`relative w-16 h-16 shrink-0 snap-start rounded-xl border-2 overflow-hidden bg-white transition ${
                    selectedImageIndex === idx ? 'border-wine' : 'border-border/60 hover:border-wine/40'
                  }`}
                >
                  <ProductImage src={img.url} alt="" sizes="64px" imgClassName="object-contain" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Product Details & Actions */}
        <div className="space-y-6">
          <div>
            <h1 className="font-serif text-[1.85rem] sm:text-4xl lg:text-[2.75rem] text-wineDark font-normal tracking-[-0.015em] leading-[1.1]">
              {product.name}
            </h1>
            <p className="text-muted text-xs mt-2">
              {product.category.name} · {product.brand.name} · <span className="tabular-nums">SKU {selectedVariant?.sku || product.sku}</span>
            </p>
          </div>

          {/* Price & Stock status */}
          <div aria-live="polite" className="flex flex-wrap items-baseline gap-4 py-2 border-y border-border/60">
            <span className="font-serif text-[2rem] sm:text-4xl text-wine font-semibold tabular-nums">
              {formatCOP(currentPrice)}
            </span>
            <div>
              {isOutOfStock ? (
                <span className="inline-flex items-center gap-1.5 text-xs font-bold text-red-600 bg-red-50 px-2.5 py-1 rounded-full border border-red-200">
                  Sin existencias
                </span>
              ) : currentStock <= 3 ? (
                <span className="inline-flex items-center gap-1.5 text-xs font-bold text-amber-800 bg-amber-50 px-2.5 py-1 rounded-full border border-amber-200">
                  {currentStock === 1 ? '¡Solo queda 1 unidad!' : `¡Solo quedan ${currentStock} unidades!`}
                </span>
              ) : (
                <span className="inline-flex items-center gap-1.5 text-xs font-bold text-green-700 bg-green-50 px-2.5 py-1 rounded-full border border-green-200">
                  <ShieldCheck className="w-3.5 h-3.5" /> En stock ({currentStock} disponibles)
                </span>
              )}
            </div>
          </div>

          <ShadeSelector variants={product.variants} selectedId={selectedVariant?.id} onSelect={variant => {
            setSelectedVariant(variant); setQuantity(1); setSelectedImageIndex(0); setJustAdded(false);
          }} />
          {!selectedVariant && <p className="text-sm text-muted">No hay variantes disponibles.</p>}

          {/* Quantity Selector & Add to Cart */}
          <div className="space-y-4 pt-2">
            <div className="flex flex-col items-stretch sm:flex-row sm:items-center gap-4">
              <div className="flex w-fit items-center border border-border rounded-full bg-white px-2 py-1">
                <button
                  onClick={() => setQuantity((q) => Math.max(1, q - 1))}
                  disabled={quantity <= 1 || isOutOfStock}
                  className="w-11 h-11 rounded-full flex items-center justify-center text-ink hover:bg-cream disabled:opacity-30 transition"
                  aria-label="Disminuir cantidad"
                >
                  <Minus className="w-3.5 h-3.5" />
                </button>
                <span className="w-10 text-center font-bold text-sm text-ink">{quantity}</span>
                <button
                  onClick={() => setQuantity((q) => Math.min(currentStock, q + 1))}
                  disabled={quantity >= currentStock || isOutOfStock}
                  className="w-11 h-11 rounded-full flex items-center justify-center text-ink hover:bg-cream disabled:opacity-30 transition"
                  aria-label="Aumentar cantidad"
                >
                  <Plus className="w-3.5 h-3.5" />
                </button>
              </div>

              <ClickSpark className="flex-1" sparkColor={["#711A38", "#B7748C", "#E9B8C7"]} sparkCount={10} sparkRadius={28} disabled={isOutOfStock}>
              <button
                type="button"
                onClick={handleAddToCart}
                disabled={isOutOfStock}
                className={`w-full min-h-12 py-3.5 px-6 rounded-full font-extrabold text-sm sm:text-base flex items-center justify-center gap-2 transition shadow-lg ${
                  isOutOfStock
                    ? 'bg-gray-200 text-gray-400 cursor-not-allowed shadow-none'
                    : justAdded
                    ? 'bg-success text-white scale-102'
                    : 'bg-wine hover:bg-wineDark text-white shadow-[0_8px_25px_rgba(113,26,56,0.25)] active:scale-98'
                }`}
              >
                {justAdded ? (
                  <>
                    <Check className="w-5 h-5" /> ¡Agregado al carrito!
                  </>
                ) : isOutOfStock ? (
                  'Agotado temporalmente'
                ) : (
                  <>
                    <Plus className="w-5 h-5 stroke-[2.5]" /> Agregar al carrito ({formatCOP(currentPrice * quantity)})
                  </>
                )}
              </button>
              </ClickSpark>
            </div>

            {/* Direct WhatsApp Consultation CTA */}
            {whatsappHref && (
              <a
                href={whatsappHref}
                target="_blank"
                rel="noopener noreferrer"
                className="w-full min-h-12 px-4 rounded-full border border-wine/25 bg-white hover:bg-blush/60 text-wine font-bold text-sm text-center flex items-center justify-center gap-2 transition-colors"
              >
                <MessageCircleHeart className="w-4 h-4" aria-hidden="true" />
                ¿Dudas con el tono? Escríbenos
              </a>
            )}
          </div>

          {/* Description & Perks */}
          <div className="pt-4 border-t border-border/60 space-y-4">
            {product.description?.trim() && (
              <>
                <h2 className="font-serif text-xl text-wineDark font-normal">Descripción</h2>
                <p className="text-[#5f4a53] text-[15px] leading-relaxed max-w-prose whitespace-pre-line">{product.description}</p>
              </>
            )}

            <ul className="space-y-2.5 pt-1 text-sm text-wine font-medium">
              <li className="flex items-center gap-2.5">
                <Truck className="w-4 h-4 text-mauve shrink-0" aria-hidden="true" />
                Envíos a todo el país coordinados por WhatsApp
              </li>
              <li className="flex items-center gap-2.5">
                <Sparkles className="w-4 h-4 text-mauve shrink-0" aria-hidden="true" />
                Cosméticos 100% originales garantizados
              </li>
            </ul>
          </div>
        </div>
      </div>

      {/* Mobile sticky purchase bar: keeps the primary action within thumb reach */}
      {!isOutOfStock && selectedVariant && (
        <div className="md:hidden fixed inset-x-0 z-30 bottom-[calc(4rem+env(safe-area-inset-bottom,0px))] px-3 pb-2 pointer-events-none">
          <div className="pointer-events-auto flex items-center gap-3 rounded-2xl bg-white/95 backdrop-blur-md border border-border shadow-[0_10px_30px_rgba(76,17,38,0.16)] p-2 pl-4">
            <div className="min-w-0 flex-1">
              <p className="text-[11px] text-muted truncate">{variantLabel(selectedVariant)}</p>
              <p className="font-extrabold text-wine leading-tight">{formatCOP(currentPrice * quantity)}</p>
            </div>
            <button
              type="button"
              onClick={handleAddToCart}
              className={`min-h-11 shrink-0 rounded-xl px-5 font-bold text-sm text-white flex items-center gap-1.5 transition active:scale-95 ${justAdded ? 'bg-success' : 'bg-wine'}`}
            >
              {justAdded ? <><Check className="w-4 h-4" /> Agregado</> : <><Plus className="w-4 h-4 stroke-[2.5]" /> Agregar</>}
            </button>
          </div>
        </div>
      )}
    </div>
  );
}
