'use client';

import React from 'react';
import Link from 'next/link';
import { useRouter } from 'next/navigation';
import { useCart } from '@/context/CartContext';
import { formatCOP } from '@/lib/utils';
import { Trash2, Plus, Minus, ArrowRight, ShoppingBag, Sparkles } from 'lucide-react';

export default function CartPage() {
  const router = useRouter();
  const { items, updateQuantity, removeItem, clearCart, totalAmount, totalCount } =
    useCart();

  return (
    <div className="space-y-8">
      {/* Header */}
      <div className="border-b border-border pb-6">
        <span className="text-[11px] sm:text-xs font-extrabold uppercase tracking-[0.2em] text-wine flex items-center gap-1.5">
          <Sparkles className="w-3.5 h-3.5" />
          Tus elegidos
        </span>
        <h1 className="font-serif text-3xl sm:text-4xl text-wine font-normal tracking-tight mt-1">
          Mi carrito
        </h1>
        <p className="text-muted text-sm sm:text-base mt-2">
          El total es estimado. El servidor verifica precios y existencias al crear tu pedido; el pago se confirma manualmente.
        </p>
      </div>

        <div className="grid grid-cols-1 lg:grid-cols-[1.4fr_1fr] gap-8 items-start">
          {/* Left: Cart Items List */}
          <div className="bg-white border border-border rounded-3xl p-6 sm:p-8 shadow-soft">
            <div className="flex items-center justify-between border-b border-border pb-4 mb-4">
              <h2 className="font-serif text-xl sm:text-2xl text-wine font-normal">
                Productos seleccionados ({totalCount})
              </h2>
              {items.length > 0 && (
                <button
                  type="button"
                  onClick={clearCart}
                  className="text-xs text-muted hover:text-wine hover:underline transition"
                >
                  Vaciar carrito
                </button>
              )}
            </div>

            {items.length > 0 ? (
              <div className="divide-y divide-border">
                {items.map(({ product, quantity, variantId, variantName }) => (
                  <div
                    key={variantId}
                    className="py-4 flex flex-col sm:flex-row sm:items-center justify-between gap-4"
                  >
                    {/* Item info */}
                    <div className="flex items-center gap-3.5 min-w-0">
                      <div className="w-14 h-14 rounded-2xl bg-gradient-to-br from-[#f8eadc] to-[#edd0d9] flex items-center justify-center text-2xl flex-shrink-0 shadow-inner">
                        {product.icon}
                      </div>
                      <div className="min-w-0">
                        <span className="text-[10px] uppercase font-bold text-[#af7b89] tracking-wider block">
                          {product.brand}
                        </span>
                        <h3 className="font-semibold text-sm sm:text-base text-ink truncate">
                          {product.name}
                        </h3>
                        {variantName && <p className="text-xs text-wine font-semibold break-words">{variantName}</p>}
                        <p className="text-xs text-muted mt-0.5">
                          {formatCOP(product.price)} c/u
                        </p>
                      </div>
                    </div>

                    {/* Quantity & Actions */}
                    <div className="flex items-center justify-between sm:justify-end gap-5">
                      <div className="flex items-center border border-border rounded-full bg-cream p-1">
                        <button
                          type="button"
                          onClick={() => updateQuantity(variantId, -1)}
                          aria-label={`Reducir cantidad de ${product.name}`}
                          className="w-11 h-11 rounded-full bg-white flex items-center justify-center text-ink hover:text-wine shadow-xs transition"
                        >
                          <Minus className="w-3.5 h-3.5" />
                        </button>
                        <span className="w-9 text-center text-xs font-bold text-ink">
                          {quantity}
                        </span>
                        <button
                          type="button"
                          onClick={() => updateQuantity(variantId, 1)}
                          disabled={quantity >= product.stock}
                          aria-label={`Aumentar cantidad de ${product.name}`}
                          className="w-11 h-11 rounded-full bg-white flex items-center justify-center text-ink hover:text-wine disabled:opacity-40 disabled:cursor-not-allowed shadow-xs transition"
                        >
                          <Plus className="w-3.5 h-3.5" />
                        </button>
                      </div>

                      <div className="text-right min-w-[80px]">
                        <p className="font-bold text-wine text-sm sm:text-base">
                          {formatCOP(product.price * quantity)}
                        </p>
                      </div>

                      <button
                        type="button"
                        onClick={() => removeItem(variantId)}
                        aria-label={`Eliminar ${product.name} del carrito`}
                        className="text-muted hover:text-wine p-1.5 transition"
                      >
                        <Trash2 className="w-4 h-4" />
                      </button>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-12 space-y-4">
                <div className="w-16 h-16 rounded-full bg-blush mx-auto flex items-center justify-center text-wine">
                  <ShoppingBag className="w-7 h-7" />
                </div>
                <h3 className="font-serif text-xl font-bold text-ink">
                  Aún no tienes productos en tu carrito ♡
                </h3>
                <p className="text-muted text-sm max-w-sm mx-auto">
                  Explora nuestro catálogo para descubrir cosméticos seleccionados para tu rutina de belleza.
                </p>
                <Link
                  href="/catalogo"
                  className="inline-flex items-center gap-2 rounded-full bg-wine text-white text-xs font-bold px-6 py-3 hover:bg-wineDark transition shadow"
                >
                  Explorar catálogo
                  <ArrowRight className="w-4 h-4" />
                </Link>
              </div>
            )}
          </div>

          {/* Right: Summary Panel */}
          <div className="bg-white border border-border rounded-3xl p-6 sm:p-8 shadow-soft space-y-6">
            <h2 className="font-serif text-xl sm:text-2xl text-wine font-normal border-b border-border pb-4">
              Resumen de compra
            </h2>

            {/* Price Calculations */}
            <div className="space-y-3 text-sm">
              <div className="flex justify-between text-muted">
                <span>Subtotal productos ({totalCount})</span>
                <span className="font-semibold text-ink">
                  {formatCOP(totalAmount)}
                </span>
              </div>

              <div className="flex justify-between text-muted">
                <span>Envío (Valledupar)</span>
                <span className="text-ink font-medium">Contraentrega</span>
              </div>

              <div className="border-t border-border pt-3 flex justify-between items-baseline">
                <span className="font-bold text-base text-ink">
                  Total productos
                </span>
                <span className="font-serif text-2xl font-bold text-wine">
                  {formatCOP(totalAmount)}
                </span>
              </div>
            </div>

            <div className="bg-[#FFF8E7] border border-[#F0D597] rounded-2xl p-4 text-xs text-[#8A5C11] leading-relaxed">
              <strong>Nota sobre envío:</strong> El valor del domicilio en Valledupar se cancela directamente al repartidor al recibir tu pedido. No se cobra en este resumen.
            </div>

            {/* Checkout CTA */}
            <button
              type="button"
              onClick={() => router.push('/pedido')}
              disabled={items.length === 0}
              className="w-full py-4 rounded-full bg-wine hover:bg-wineDark text-white font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-glow transition-all active:scale-98 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              <span>Continuar con el pedido</span>
              <ArrowRight className="w-5 h-5" />
            </button>
          </div>
        </div>

    </div>
  );
}
