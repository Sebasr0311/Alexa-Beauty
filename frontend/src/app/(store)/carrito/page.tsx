'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import { useCart } from '@/context/CartContext';
import { formatCOP } from '@/lib/utils';
import { Trash2, Plus, Minus, ArrowRight, MessageCircleHeart, ShoppingBag, Sparkles } from 'lucide-react';

export default function CartPage() {
  const { items, updateQuantity, removeItem, clearCart, totalAmount, totalCount } =
    useCart();

  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [customerCity, setCustomerCity] = useState('');
  const [notes, setNotes] = useState('');
  const [orderPlaced, setOrderPlaced] = useState(false);

  const handleCheckoutWhatsApp = (e: React.FormEvent) => {
    e.preventDefault();
    if (items.length === 0) return;

    // Simulate WhatsApp order creation
    setOrderPlaced(true);
  };

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
          Precios y existencias de demostración. Los pedidos finales se coordinan y confirman vía WhatsApp.
        </p>
      </div>

      {orderPlaced ? (
        /* Order Confirmed Mock State */
        <div className="bg-white border border-[#22795B]/30 rounded-3xl p-8 sm:p-12 text-center max-w-xl mx-auto shadow-soft space-y-5">
          <div className="w-16 h-16 rounded-full bg-[#E2F5EA] text-success mx-auto flex items-center justify-center text-3xl">
            ✓
          </div>
          <span className="text-xs uppercase font-extrabold tracking-widest text-success">
            Pedido preparado para WhatsApp
          </span>
          <h2 className="font-serif text-2xl sm:text-3xl text-wine font-bold">
            ¡Referencia AB-2026-DEMO generada!
          </h2>
          <p className="text-muted text-sm sm:text-base leading-relaxed">
            En la versión productiva conectada con la API de Spring Boot, el sistema reservará tu stock por 45 minutos y abrirá WhatsApp con el siguiente resumen listo para enviar a Alexa Beauty.
          </p>

          <div className="bg-cream border border-border rounded-2xl p-4 text-left text-xs sm:text-sm font-mono space-y-1 text-ink/80">
            <p><strong>Cliente:</strong> {customerName || 'Cliente demo'}</p>
            <p><strong>Teléfono:</strong> {customerPhone || '300 000 0000'}</p>
            <p><strong>Ciudad:</strong> {customerCity || 'Colombia'}</p>
            <p><strong>Total pedido:</strong> {formatCOP(totalAmount)}</p>
            <p><strong>Artículos:</strong> {totalCount} unidades</p>
          </div>

          <div className="flex flex-col sm:flex-row gap-3 justify-center pt-2">
            <button
              onClick={() => setOrderPlaced(false)}
              className="px-6 py-3 rounded-full border border-border text-sm font-bold text-ink hover:bg-cream"
            >
              Volver al carrito
            </button>
            <Link
              href="/catalogo"
              className="px-6 py-3 rounded-full bg-wine text-white text-sm font-bold hover:bg-wineDark"
            >
              Seguir explorando
            </Link>
          </div>
        </div>
      ) : (
        /* Main Cart Layout */
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
                {items.map(({ product, quantity }) => (
                  <div
                    key={product.id}
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
                          onClick={() => updateQuantity(product.id, -1)}
                          aria-label={`Reducir cantidad de ${product.name}`}
                          className="w-7 h-7 rounded-full bg-white flex items-center justify-center text-ink hover:text-wine shadow-xs transition"
                        >
                          <Minus className="w-3.5 h-3.5" />
                        </button>
                        <span className="w-9 text-center text-xs font-bold text-ink">
                          {quantity}
                        </span>
                        <button
                          type="button"
                          onClick={() => updateQuantity(product.id, 1)}
                          disabled={quantity >= product.stock}
                          aria-label={`Aumentar cantidad de ${product.name}`}
                          className="w-7 h-7 rounded-full bg-white flex items-center justify-center text-ink hover:text-wine disabled:opacity-40 disabled:cursor-not-allowed shadow-xs transition"
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
                        onClick={() => removeItem(product.id)}
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

          {/* Right: Checkout Drawer / Summary Panel */}
          <div className="bg-white border border-border rounded-3xl p-6 sm:p-8 shadow-soft space-y-6">
            <h2 className="font-serif text-xl sm:text-2xl text-wine font-normal border-b border-border pb-4">
              Resumen del pedido
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
                <span>Envío</span>
                <span className="text-ink font-medium">Por coordinar</span>
              </div>

              <div className="border-t border-border pt-3 flex justify-between items-baseline">
                <span className="font-bold text-base text-ink">
                  Total estimado
                </span>
                <span className="font-serif text-2xl font-bold text-wine">
                  {formatCOP(totalAmount)}
                </span>
              </div>
            </div>

            {/* Customer Information Preview */}
            <form onSubmit={handleCheckoutWhatsApp} className="space-y-3.5 pt-2">
              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted mb-1">
                  Tu nombre
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ej. Camila Morales"
                  value={customerName}
                  onChange={(e) => setCustomerName(e.target.value)}
                  className="w-full bg-cream border border-border rounded-xl px-3.5 py-2.5 text-sm text-ink outline-none focus:border-mauve transition"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted mb-1">
                  Número de WhatsApp
                </label>
                <input
                  type="tel"
                  required
                  placeholder="Ej. 310 123 4567"
                  value={customerPhone}
                  onChange={(e) => setCustomerPhone(e.target.value)}
                  className="w-full bg-cream border border-border rounded-xl px-3.5 py-2.5 text-sm text-ink outline-none focus:border-mauve transition"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted mb-1">
                  Ciudad o Municipio (opcional)
                </label>
                <input
                  type="text"
                  placeholder="Ej. Bogotá, Medellín, Cali..."
                  value={customerCity}
                  onChange={(e) => setCustomerCity(e.target.value)}
                  className="w-full bg-cream border border-border rounded-xl px-3.5 py-2.5 text-sm text-ink outline-none focus:border-mauve transition"
                />
              </div>

              <div>
                <label className="block text-xs font-bold uppercase tracking-wider text-muted mb-1">
                  Nota o tono preferido
                </label>
                <input
                  type="text"
                  placeholder="Ej. Prefiero tono cálido, confirmar antes de envío"
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  className="w-full bg-cream border border-border rounded-xl px-3.5 py-2.5 text-sm text-ink outline-none focus:border-mauve transition"
                />
              </div>

              {/* WhatsApp Checkout CTA */}
              <button
                type="submit"
                disabled={items.length === 0}
                className="w-full mt-4 py-4 rounded-full bg-wine hover:bg-wineDark text-white font-bold text-sm sm:text-base flex items-center justify-center gap-2 shadow-glow transition-all active:scale-98 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                <MessageCircleHeart className="w-5 h-5" />
                Continuar por WhatsApp ↗
              </button>
            </form>

            {/* Architectural Note */}
            <div className="bg-[#FFF8E7] border border-[#F0D597] rounded-2xl p-4 text-xs text-[#8A5C11] leading-relaxed">
              <strong>Nota del flujo de compra:</strong> En la versión final, este botón registra el pedido en el servidor, bloquea el stock por 45 min y genera el mensaje de WhatsApp preformateado. La venta se marca como pagada solo cuando el administrador confirma la transferencia.
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
