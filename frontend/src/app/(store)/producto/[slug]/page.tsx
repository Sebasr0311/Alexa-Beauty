'use client';

import React, { useEffect, useState, use } from 'react';
import Link from 'next/link';
import { api, ProductDetailDto, ProductVariantDto } from '@/lib/api';
import { whatsappContact } from '@/lib/contact';
import { formatCOP } from '@/lib/utils';
import { ShadeSelector } from '@/components/catalog/ShadeSelector';
import { initialVariant, variantImages, variantLabel } from '@/lib/shades';
import { useCart } from '@/context/CartContext';
import { ArrowLeft, Check, ChevronLeft, ChevronRight, Heart, Minus, Plus, Share2, ShieldCheck, Sparkles, Truck } from 'lucide-react';

interface ProductPageProps {
  params: Promise<{ slug: string }>;
}

export default function ProductDetailPage({ params }: ProductPageProps) {
  const { slug } = use(params);

  const { addItem } = useCart();
  const [product, setProduct] = useState<ProductDetailDto | null>(null);
  const [selectedVariant, setSelectedVariant] = useState<ProductVariantDto | null>(null);
  const [quantity, setQuantity] = useState(1);
  const [selectedImageIndex, setSelectedImageIndex] = useState(0);
  const [isHovered, setIsHovered] = useState(false);
  const [imageError, setImageError] = useState(false);
  const [justAdded, setJustAdded] = useState(false);
  const [loading, setLoading] = useState(true);

  const images = product ? variantImages(product.images, selectedVariant?.id) : [];

  useEffect(() => {
    if (!product || images.length <= 1 || isHovered) return;
    const interval = setInterval(() => {
      setSelectedImageIndex((prev) => (prev + 1) % images.length);
    }, 3500);
    return () => clearInterval(interval);
  }, [images.length, isHovered, product]);

  useEffect(() => {
    setImageError(false);
  }, [selectedImageIndex, selectedVariant?.id]);

  useEffect(() => {
    async function loadProduct() {
      try {
        setLoading(true);
        const data = await api.getProductBySlug(slug);
        setProduct(data);
        setSelectedVariant(initialVariant(data.variants));
        setQuantity(1); setSelectedImageIndex(0);
      } catch {
        setProduct(null); setSelectedVariant(null);
      } finally {
        setLoading(false);
      }
    }

    loadProduct();
  }, [slug]);

  if (loading) {
    return (
      <div className="max-w-4xl mx-auto px-4 py-16 text-center">
        <div className="w-12 h-12 border-4 border-wine/20 border-t-wine rounded-full animate-spin mx-auto mb-4" />
        <p className="text-muted text-sm font-medium">Cargando detalles de belleza...</p>
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
    <div className="max-w-5xl mx-auto px-4 py-6 sm:py-10">
      {/* Breadcrumb / Back button */}
      <div className="mb-6">
        <Link href="/catalogo" className="inline-flex items-center gap-1.5 text-xs sm:text-sm font-bold text-muted hover:text-wine transition">
          <ArrowLeft className="w-4 h-4" /> Volver al catálogo
        </Link>
      </div>

      <div className="grid grid-cols-1 md:grid-cols-2 gap-8 lg:gap-12 items-start">
        {/* Visual Gallery 1:1 Aspect Ratio */}
        <div className="space-y-4">
          <div
            onMouseEnter={() => setIsHovered(true)}
            onMouseLeave={() => setIsHovered(false)}
            className="group/carousel relative aspect-square w-full rounded-3xl overflow-hidden bg-gradient-to-br from-[#f8e4ea] to-[#ebd0d9] border border-border shadow-[0_12px_36px_rgba(92,26,49,0.06)] grid place-items-center"
          >
            {images[selectedImageIndex]?.url && !imageError ? (
              <img
                key={images[selectedImageIndex].url}
                src={images[selectedImageIndex].url}
                alt={images[selectedImageIndex].altText || product.name}
                onError={() => setImageError(true)}
                className="w-full h-full object-contain p-4 transition-all duration-300"
              />
            ) : (
              <span className="text-8xl select-none filter drop-shadow-lg -rotate-6" aria-hidden="true">
                💄
              </span>
            )}

            {/* Carousel navigation & indicators if multiple images */}
            {images.length > 1 && (
              <>
                <button
                  type="button"
                  onClick={() => setSelectedImageIndex((prev) => (prev - 1 + images.length) % images.length)}
                  aria-label="Foto anterior"
                  className="absolute left-3 top-1/2 -translate-y-1/2 z-20 w-9 h-9 rounded-full bg-white/85 backdrop-blur-xs text-wine shadow-md flex items-center justify-center opacity-0 group-hover/carousel:opacity-100 transition-opacity hover:bg-white active:scale-95"
                >
                  <ChevronLeft className="w-5 h-5 stroke-[2.5]" />
                </button>
                <button
                  type="button"
                  onClick={() => setSelectedImageIndex((prev) => (prev + 1) % images.length)}
                  aria-label="Foto siguiente"
                  className="absolute right-3 top-1/2 -translate-y-1/2 z-20 w-9 h-9 rounded-full bg-white/85 backdrop-blur-xs text-wine shadow-md flex items-center justify-center opacity-0 group-hover/carousel:opacity-100 transition-opacity hover:bg-white active:scale-95"
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
            <div className="flex gap-3 overflow-x-auto pb-2">
              {images.map((img, idx) => (
                <button
                  key={img.id}
                  onClick={() => setSelectedImageIndex(idx)}
                  aria-label={`Ver imagen ${idx + 1}`} className={`w-16 h-16 shrink-0 rounded-xl border-2 overflow-hidden transition ${
                    selectedImageIndex === idx ? 'border-wine scale-105' : 'border-border/60 hover:border-wine/40'
                  }`}
                >
                  <img src={img.url} alt={img.altText || product.name} className="w-full h-full object-contain" />
                </button>
              ))}
            </div>
          )}
        </div>

        {/* Product Details & Actions */}
        <div className="space-y-6">
          <div>
            <span className="text-wine text-xs font-extrabold tracking-widest uppercase block mb-1">
              {product.category.name} · {product.brand.name}
            </span>
            <h1 className="font-serif text-2xl sm:text-3xl lg:text-4xl text-wineDark font-normal leading-tight">
              {product.name}
            </h1>
            <p className="text-muted text-xs mt-1">SKU: <span className="font-mono">{selectedVariant?.sku || product.sku}</span></p>
          </div>

          {/* Price & Stock status */}
          <div aria-live="polite" className="flex flex-wrap items-baseline gap-4 py-2 border-y border-border/60">
            <span className="font-serif text-3xl sm:text-4xl text-wine font-bold">
              {formatCOP(currentPrice)}
            </span>
            <div>
              {isOutOfStock ? (
                <span className="inline-flex items-center gap-1.5 text-xs font-bold text-red-600 bg-red-50 px-2.5 py-1 rounded-full border border-red-200">
                  Sin existencias
                </span>
              ) : currentStock <= 3 ? (
                <span className="inline-flex items-center gap-1.5 text-xs font-bold text-amber-800 bg-amber-50 px-2.5 py-1 rounded-full border border-amber-200">
                  ¡Solo quedan {currentStock} unidades!
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
          <div aria-live="polite" className="min-h-16 text-sm break-words">
            {selectedVariant && <><p>Tono seleccionado: {variantLabel(selectedVariant)}</p>
              <p className={currentStock > 0 ? 'text-emerald-700 font-medium' : 'text-rose-700 font-semibold'}>{currentStock > 0 ? `${currentStock} unidades disponibles` : 'Agotado (no disponible para compra)'}</p></>}
            {!selectedVariant && <p>No hay variantes disponibles.</p>}
          </div>

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

              <button
                onClick={handleAddToCart}
                disabled={isOutOfStock}
                className={`flex-1 py-3.5 px-6 rounded-full font-extrabold text-sm sm:text-base flex items-center justify-center gap-2 transition shadow-lg ${
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
            </div>

            {/* Direct WhatsApp Consultation CTA */}
            <a
              href={whatsappContact(`Hola Alexa Beauty, tengo dudas sobre ${product.name}.`)}
              aria-disabled={!whatsappContact('Consulta')}
              target="_blank"
              rel="noopener noreferrer"
              className="w-full py-3 px-4 rounded-full border border-wine/40 bg-white hover:bg-cream text-wine font-bold text-xs sm:text-sm text-center flex items-center justify-center gap-2 transition"
            >
              ♡ ¿Dudas con el tono? Consultar por WhatsApp
            </a>
          </div>

          {/* Description & Perks */}
          <div className="pt-4 border-t border-border/60 space-y-4">
            <h3 className="font-serif text-lg text-wineDark font-normal">Descripción del producto</h3>
            <p className="text-muted text-sm leading-relaxed">{product.description}</p>

            <div className="grid grid-cols-2 gap-3 pt-2">
              <div className="flex items-center gap-2.5 p-3 rounded-2xl bg-white border border-border/80 text-xs text-wine font-semibold">
                <Truck className="w-4 h-4 text-mauve shrink-0" />
                <span>Envíos a todo el país coordinados por WhatsApp</span>
              </div>
              <div className="flex items-center gap-2.5 p-3 rounded-2xl bg-white border border-border/80 text-xs text-wine font-semibold">
                <Sparkles className="w-4 h-4 text-mauve shrink-0" />
                <span>Cosméticos 100% originales garantizados</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
