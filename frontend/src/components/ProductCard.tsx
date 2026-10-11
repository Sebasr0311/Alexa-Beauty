'use client';

import React, { useState } from 'react';
import { Product } from '@/lib/data';
import { formatCOP } from '@/lib/utils';
import { useCart } from '@/context/CartContext';
import { Check, Plus } from 'lucide-react';

interface ProductCardProps {
  product: Product;
}

export function ProductCard({ product }: ProductCardProps) {
  const { addItem } = useCart();
  const [justAdded, setJustAdded] = useState(false);
  const isOutOfStock = product.stock <= 0;

  const handleAdd = () => {
    if (isOutOfStock) return;
    const res = addItem(product);
    if (res.success) {
      setJustAdded(true);
      setTimeout(() => setJustAdded(false), 1200);
    } else if (res.message) {
      alert(res.message);
    }
  };

  const gradientClass =
    product.visual === 'dark'
      ? 'from-[#f0d8de] to-[#d8a1b1]'
      : product.visual === 'gold'
      ? 'from-[#f8eadc] to-[#ead0be]'
      : 'from-[#f6e7e7] to-[#e6c8c8]';

  return (
    <article className="group bg-white border border-[#efdee3] rounded-2xl overflow-hidden shadow-[0_9px_26px_rgba(58,26,39,0.035)] hover:-translate-y-1 transition duration-200 flex flex-col justify-between">
      <div>
        {/* Visual Header */}
        <div
          className={`h-48 sm:h-56 relative grid place-items-center bg-gradient-to-br ${gradientClass} overflow-hidden`}
        >
          {/* Decorative circular aura */}
          <div className="absolute -top-4 -right-6 w-36 h-36 bg-white/40 rounded-full pointer-events-none blur-[1px]" />

          {/* Badge chip */}
          <span className="absolute top-3 left-3 bg-white/95 backdrop-blur-sm text-wine text-[10px] font-extrabold uppercase tracking-wider px-2.5 py-1 rounded-full shadow-sm">
            {product.badge}
          </span>

          {/* Product Emoji / Icon */}
          <span
            className="text-6xl sm:text-7xl select-none filter drop-shadow-[0_15px_12px_rgba(94,49,61,0.18)] -rotate-6 group-hover:scale-105 transition-transform duration-300"
            aria-hidden="true"
          >
            {product.icon}
          </span>
        </div>

        {/* Content Body */}
        <div className="p-4">
          <span className="text-[#af7b89] text-[10px] font-extrabold tracking-widest uppercase">
            {product.brand}
          </span>
          <h3 className="text-ink font-semibold text-sm sm:text-base leading-snug mt-1 min-h-[42px] line-clamp-2">
            {product.name}
          </h3>
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
          onClick={handleAdd}
          disabled={isOutOfStock}
          aria-label={`Agregar ${product.name} al carrito`}
          className={`w-9 h-9 rounded-full flex items-center justify-center transition-all ${
            isOutOfStock
              ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
              : justAdded
              ? 'bg-success text-white scale-105'
              : 'bg-wine hover:bg-wineDark text-white shadow-md active:scale-95'
          }`}
        >
          {justAdded ? (
            <Check className="w-5 h-5 stroke-[2.5]" />
          ) : isOutOfStock ? (
            <span className="text-sm font-bold">×</span>
          ) : (
            <Plus className="w-5 h-5 stroke-[2.5]" />
          )}
        </button>
      </div>
    </article>
  );
}
