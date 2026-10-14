'use client';

import React from 'react';
import { ProductDetailDto, ProductVariantDto } from '@/lib/api';
import { formatCOP } from '@/lib/utils';
import { X, Sparkles, Plus, Check } from 'lucide-react';

interface ShadeSelectorModalProps {
  product: ProductDetailDto | null;
  isOpen: boolean;
  onClose: () => void;
  onSelectVariant: (variant: ProductVariantDto) => void;
}

export function ShadeSelectorModal({
  product,
  isOpen,
  onClose,
  onSelectVariant,
}: ShadeSelectorModalProps) {
  if (!isOpen || !product) return null;

  const coverImage = product.images.find((img) => img.isCover)?.url || product.images[0]?.url;

  return (
    <div
      role="dialog"
      aria-modal="true"
      aria-labelledby="shade-selector-title"
      className="fixed inset-0 z-50 flex items-end sm:items-center justify-center p-0 sm:p-4 bg-black/50 backdrop-blur-xs animate-in fade-in duration-200"
      onClick={onClose}
    >
      <div
        className="bg-white w-full sm:max-w-md rounded-t-3xl sm:rounded-3xl border border-[#EAD5DC] shadow-[0_25px_60px_rgba(58,26,39,0.18)] p-6 space-y-5 max-h-[85vh] overflow-y-auto"
        onClick={(e) => e.stopPropagation()}
      >
        {/* Header */}
        <div className="flex items-start justify-between gap-3 border-b border-border/60 pb-4">
          <div className="flex items-center gap-3 min-w-0">
            <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-[#f8eadc] to-[#edd0d9] p-1 flex-shrink-0 flex items-center justify-center overflow-hidden border border-[#E8D4DC]">
              {coverImage ? (
                <img src={coverImage} alt={product.name} className="w-full h-full object-cover rounded-xl" />
              ) : (
                <span className="text-2xl">💄</span>
              )}
            </div>
            <div className="min-w-0">
              <span className="text-[10px] font-extrabold uppercase tracking-wider text-[#af7b89] block">
                {product.brand.name}
              </span>
              <h2 id="shade-selector-title" className="font-serif text-base sm:text-lg font-bold text-ink truncate">
                {product.name}
              </h2>
              <span className="text-xs text-wine font-extrabold">
                Desde {formatCOP(product.variants[0]?.salePrice || 0)}
              </span>
            </div>
          </div>

          <button
            type="button"
            onClick={onClose}
            className="w-8 h-8 rounded-full bg-cream hover:bg-blush text-ink hover:text-wine flex items-center justify-center transition"
            aria-label="Cerrar selector de tonos"
          >
            <X className="w-4 h-4" />
          </button>
        </div>

        {/* Instructions */}
        <div className="flex items-center gap-1.5 text-xs text-muted font-medium">
          <Sparkles className="w-3.5 h-3.5 text-wine" />
          <span>Selecciona tu tono o presentación para agregar:</span>
        </div>

        {/* Variants List */}
        <div className="space-y-2.5">
          {product.variants.map((variant) => {
            const isOutOfStock = variant.availableStock <= 0;
            const displayName =
              variant.shadeName && variant.shadeName.trim()
                ? variant.shadeName.trim()
                : variant.presentation && variant.presentation.trim()
                ? variant.presentation.trim()
                : `Variante ${variant.sku}`;

            return (
              <button
                key={variant.id}
                type="button"
                disabled={isOutOfStock}
                onClick={() => onSelectVariant(variant)}
                className={`w-full p-3.5 rounded-2xl border text-left transition flex items-center justify-between gap-3 ${
                  isOutOfStock
                    ? 'border-gray-200 bg-gray-50 opacity-60 cursor-not-allowed'
                    : 'border-[#EAD5DC] bg-[#FFFDFC] hover:border-wine hover:bg-blush/20 active:scale-[0.99] shadow-xs'
                }`}
              >
                <div className="min-w-0">
                  <span className="block font-bold text-sm text-ink truncate">
                    {displayName}
                  </span>
                  <div className="flex items-center gap-2 mt-0.5 text-xs">
                    <span className="text-wine font-extrabold">
                      {formatCOP(variant.salePrice)}
                    </span>
                    <span className="text-muted text-[11px]">
                      {isOutOfStock ? (
                        <span className="text-red-500 font-semibold">Agotado</span>
                      ) : variant.availableStock <= 3 ? (
                        <span className="text-amber-700 font-medium">¡Últimas {variant.availableStock}!</span>
                      ) : (
                        <span>{variant.availableStock} disp.</span>
                      )}
                    </span>
                  </div>
                </div>

                <div
                  className={`w-8 h-8 rounded-full flex items-center justify-center flex-shrink-0 transition ${
                    isOutOfStock
                      ? 'bg-gray-200 text-gray-400'
                      : 'bg-wine hover:bg-wineDark text-white shadow-xs'
                  }`}
                >
                  <Plus className="w-4 h-4 stroke-[2.5]" />
                </div>
              </button>
            );
          })}
        </div>
      </div>
    </div>
  );
}
