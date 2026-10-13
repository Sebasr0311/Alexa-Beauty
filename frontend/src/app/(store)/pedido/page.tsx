'use client';

import React, { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import Link from 'next/link';
import { useCart } from '@/context/CartContext';
import { api, CreateOrderRequestDto, OrderItemRequestDto } from '@/lib/api';
import { formatCOP } from '@/lib/utils';
import {
  ArrowLeft,
  Truck,
  Store,
  ShieldCheck,
  Clock,
  Sparkles,
  ShoppingBag,
  AlertCircle,
  Loader2,
  CheckCircle2,
} from 'lucide-react';

export default function CheckoutPage() {
  const router = useRouter();
  const { items, totalAmount, totalCount, clearCart } = useCart();

  // Form states
  const [customerName, setCustomerName] = useState('');
  const [customerPhone, setCustomerPhone] = useState('');
  const [deliveryType, setDeliveryType] = useState<'ENVIO_DOMICILIO' | 'RETIRO_TIENDA'>('ENVIO_DOMICILIO');
  const [city, setCity] = useState('');
  const [address, setAddress] = useState('');
  const [customerNotes, setCustomerNotes] = useState('');

  // UI state
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Auto-generate idempotency key for this session
  const [idempotencyKey] = useState(() => `IDEM-${Date.now()}-${Math.random().toString(36).substring(2, 9)}`);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (items.length === 0) {
      setErrorMessage('Tu carrito está vacío. Agrega productos antes de continuar.');
      return;
    }

    if (!customerName.trim()) {
      setErrorMessage('Por favor ingresa tu nombre completo.');
      return;
    }

    if (!customerPhone.trim()) {
      setErrorMessage('Por favor ingresa tu número de WhatsApp para contacto.');
      return;
    }

    if (deliveryType === 'ENVIO_DOMICILIO' && (!city.trim() || !address.trim())) {
      setErrorMessage('Para entrega a domicilio, por favor indica ciudad y dirección.');
      return;
    }

    setLoading(true);
    setErrorMessage(null);

    try {
      // 1. Resolve variant IDs for all items
      let catalogVariantsMap = new Map<string, string>();
      const needsVariantLookup = items.some((i) => !i.variantId);

      if (needsVariantLookup) {
        try {
          const catalogData = await api.getCatalogProducts({ size: 100 });
          for (const prod of catalogData.content) {
            // Fetch product detail to get variants
            try {
              const detail = await api.getProductBySlug(prod.slug);
              if (detail.variants && detail.variants.length > 0) {
                catalogVariantsMap.set(prod.name.toLowerCase().trim(), detail.variants[0].id);
                catalogVariantsMap.set(prod.sku.toLowerCase().trim(), detail.variants[0].id);
              }
            } catch {
              // ignore per-item detail failure
            }
          }
        } catch {
          // ignore catalog lookup errors
        }
      }

      const orderItems: OrderItemRequestDto[] = [];
      for (const item of items) {
        let variantId = item.variantId;
        if (!variantId) {
          // Attempt resolution by name or sku from catalog map
          variantId =
            catalogVariantsMap.get(item.product.name.toLowerCase().trim()) ||
            catalogVariantsMap.get(item.product.name.split(' - ')[0].toLowerCase().trim());
        }

        if (!variantId) {
          throw new Error(
            `El producto "${item.product.name}" no tiene una variante asignada en el catálogo actual. Por favor retíralo o selecciona uno desde el catálogo.`
          );
        }

        orderItems.push({
          variantId,
          quantity: item.quantity,
        });
      }

      // 2. Prepare payload
      const orderPayload: CreateOrderRequestDto = {
        customerName: customerName.trim(),
        customerPhone: customerPhone.trim(),
        deliveryType,
        city: deliveryType === 'ENVIO_DOMICILIO' ? city.trim() : undefined,
        address: deliveryType === 'ENVIO_DOMICILIO' ? address.trim() : undefined,
        customerNotes: customerNotes.trim() ? customerNotes.trim() : undefined,
        items: orderItems,
      };

      // 3. Submit order to backend
      const response = await api.createOrder(orderPayload, idempotencyKey);

      // 4. On success: clear cart and redirect to order confirmation
      clearCart();
      router.push(`/pedido/confirmado/${encodeURIComponent(response.reference)}?token=${encodeURIComponent(response.trackingToken)}`);
    } catch (err: any) {
      console.error('Checkout error:', err);
      setErrorMessage(
        err.message || 'Ocurrió un error al procesar tu pedido. Por favor verifica las existencias e intenta nuevamente.'
      );
    } finally {
      setLoading(false);
    }
  };

  if (items.length === 0) {
    return (
      <div className="max-w-2xl mx-auto py-16 px-4 text-center space-y-6">
        <div className="w-16 h-16 rounded-full bg-blush mx-auto flex items-center justify-center text-wine">
          <ShoppingBag className="w-8 h-8" />
        </div>
        <h1 className="font-serif text-3xl text-wine font-medium">Tu carrito está vacío</h1>
        <p className="text-muted text-sm max-w-md mx-auto">
          Para realizar un pedido, primero agrega tus productos cosméticos favoritos a tu bolsa de compra.
        </p>
        <div>
          <Link
            href="/catalogo"
            className="inline-flex items-center gap-2 bg-wine hover:bg-wineDark text-white px-6 py-3 rounded-full text-sm font-bold shadow transition"
          >
            <ArrowLeft className="w-4 h-4" /> Ir al catálogo
          </Link>
        </div>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto space-y-8">
      {/* Header */}
      <div className="border-b border-border pb-6 flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <div className="flex items-center gap-2 text-xs font-bold uppercase tracking-wider text-wine mb-1">
            <Sparkles className="w-3.5 h-3.5" />
            <span>Paso final</span>
          </div>
          <h1 className="font-serif text-3xl sm:text-4xl text-wine font-normal tracking-tight">
            Confirmar pedido y reserva
          </h1>
          <p className="text-muted text-sm mt-1">
            Tus productos quedarán reservados por <strong>45 minutos</strong> mientras coordinas la confirmación vía WhatsApp.
          </p>
        </div>

        <Link
          href="/carrito"
          className="inline-flex items-center gap-1.5 text-xs font-bold text-muted hover:text-wine self-start sm:self-auto transition"
        >
          <ArrowLeft className="w-4 h-4" /> Volver al carrito
        </Link>
      </div>

      {/* Main Grid: Form on Left, Order Summary on Right */}
      <div className="grid grid-cols-1 lg:grid-cols-[1.4fr_1fr] gap-8 items-start">
        {/* Customer & Delivery Form */}
        <div className="bg-white border border-border rounded-3xl p-6 sm:p-8 shadow-soft space-y-6">
          <h2 className="font-serif text-xl sm:text-2xl text-wine font-medium border-b border-border pb-4">
            Datos de contacto y entrega
          </h2>

          {errorMessage && (
            <div className="bg-red-50 border border-red-200 text-red-800 rounded-2xl p-4 text-xs sm:text-sm flex items-start gap-3">
              <AlertCircle className="w-5 h-5 flex-shrink-0 text-red-500 mt-0.5" />
              <div>
                <strong>No se pudo completar el pedido:</strong>
                <p className="mt-1 leading-relaxed">{errorMessage}</p>
              </div>
            </div>
          )}

          <form onSubmit={handleSubmit} className="space-y-5">
            {/* Customer Name */}
            <div>
              <label htmlFor="customerName" className="block text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
                Nombre y apellido <span className="text-wine">*</span>
              </label>
              <input
                id="customerName"
                type="text"
                required
                value={customerName}
                onChange={(e) => setCustomerName(e.target.value)}
                placeholder="Ej. Camila Morales"
                className="w-full bg-cream border border-border rounded-xl px-4 py-3 text-sm text-ink outline-none focus:border-wine transition"
              />
            </div>

            {/* Customer WhatsApp */}
            <div>
              <label htmlFor="customerPhone" className="block text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
                Número de WhatsApp <span className="text-wine">*</span>
              </label>
              <input
                id="customerPhone"
                type="tel"
                required
                value={customerPhone}
                onChange={(e) => setCustomerPhone(e.target.value)}
                placeholder="Ej. 300 123 4567"
                className="w-full bg-cream border border-border rounded-xl px-4 py-3 text-sm text-ink outline-none focus:border-wine transition"
              />
              <span className="text-[11px] text-muted block mt-1">
                Te contactaremos a este número para validar existencias y compartir datos de pago.
              </span>
            </div>

            {/* Delivery Method Selector */}
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-muted mb-2">
                Método de entrega <span className="text-wine">*</span>
              </label>
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                <button
                  type="button"
                  onClick={() => setDeliveryType('ENVIO_DOMICILIO')}
                  className={`p-4 rounded-2xl border text-left transition flex items-start gap-3 ${
                    deliveryType === 'ENVIO_DOMICILIO'
                      ? 'border-wine bg-wine/5 shadow-xs'
                      : 'border-border bg-cream/50 hover:bg-cream'
                  }`}
                >
                  <Truck className={`w-5 h-5 mt-0.5 ${deliveryType === 'ENVIO_DOMICILIO' ? 'text-wine' : 'text-muted'}`} />
                  <div>
                    <span className="block font-bold text-xs uppercase tracking-wider text-ink">
                      Envío a domicilio
                    </span>
                    <span className="text-[11px] text-muted mt-0.5 block">
                      Entrega a cualquier ciudad de Colombia
                    </span>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => setDeliveryType('RETIRO_TIENDA')}
                  className={`p-4 rounded-2xl border text-left transition flex items-start gap-3 ${
                    deliveryType === 'RETIRO_TIENDA'
                      ? 'border-wine bg-wine/5 shadow-xs'
                      : 'border-border bg-cream/50 hover:bg-cream'
                  }`}
                >
                  <Store className={`w-5 h-5 mt-0.5 ${deliveryType === 'RETIRO_TIENDA' ? 'text-wine' : 'text-muted'}`} />
                  <div>
                    <span className="block font-bold text-xs uppercase tracking-wider text-ink">
                      Retiro en tienda
                    </span>
                    <span className="text-[11px] text-muted mt-0.5 block">
                      Recoge tu pedido sin costo en punto físico
                    </span>
                  </div>
                </button>
              </div>
            </div>

            {/* Delivery address fields (shown only for ENVIO_DOMICILIO) */}
            {deliveryType === 'ENVIO_DOMICILIO' && (
              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
                <div>
                  <label htmlFor="city" className="block text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
                    Ciudad / Municipio <span className="text-wine">*</span>
                  </label>
                  <input
                    id="city"
                    type="text"
                    required={deliveryType === 'ENVIO_DOMICILIO'}
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    placeholder="Ej. Medellín, Bogotá, Cali..."
                    className="w-full bg-cream border border-border rounded-xl px-4 py-3 text-sm text-ink outline-none focus:border-wine transition"
                  />
                </div>

                <div>
                  <label htmlFor="address" className="block text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
                    Dirección completa <span className="text-wine">*</span>
                  </label>
                  <input
                    id="address"
                    type="text"
                    required={deliveryType === 'ENVIO_DOMICILIO'}
                    value={address}
                    onChange={(e) => setAddress(e.target.value)}
                    placeholder="Ej. Carrera 43A # 1-50 Apt 402"
                    className="w-full bg-cream border border-border rounded-xl px-4 py-3 text-sm text-ink outline-none focus:border-wine transition"
                  />
                </div>
              </div>
            )}

            {/* Customer Notes */}
            <div>
              <label htmlFor="customerNotes" className="block text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
                Notas adicionales para tu pedido (opcional)
              </label>
              <textarea
                id="customerNotes"
                rows={2}
                value={customerNotes}
                onChange={(e) => setCustomerNotes(e.target.value)}
                placeholder="Ej. Tono específico, horario preferido de entrega o requerimientos especiales"
                className="w-full bg-cream border border-border rounded-xl px-4 py-3 text-sm text-ink outline-none focus:border-wine transition resize-none"
              />
            </div>

            {/* Submit Button */}
            <div className="pt-2">
              <button
                type="submit"
                disabled={loading || items.length === 0}
                className="w-full py-4 rounded-full bg-wine hover:bg-wineDark text-white font-bold text-sm sm:text-base flex items-center justify-center gap-2.5 shadow-glow transition-all active:scale-98 disabled:opacity-50 disabled:cursor-not-allowed"
              >
                {loading ? (
                  <>
                    <Loader2 className="w-5 h-5 animate-spin" />
                    Reservando tus productos...
                  </>
                ) : (
                  <>
                    <Clock className="w-5 h-5" />
                    Confirmar pedido y reservar por 45 min ↗
                  </>
                )}
              </button>
            </div>
          </form>

          {/* Honest Stock Reservation Notice */}
          <div className="bg-[#FFF9EA] border border-[#F5E2B2] rounded-2xl p-4 text-xs text-[#8A5C11] flex items-start gap-3">
            <Clock className="w-5 h-5 text-[#B37400] flex-shrink-0 mt-0.5" />
            <p className="leading-relaxed">
              <strong>Compromiso honesto de stock:</strong> Al presionar confirmar, bloqueamos las unidades de tu pedido por 45 minutos. Ningún otro comprador podrá reservarlas mientras completas tu confirmación por WhatsApp.
            </p>
          </div>
        </div>

        {/* Right Column: Order Summary */}
        <div className="bg-white border border-border rounded-3xl p-6 sm:p-8 shadow-soft space-y-6">
          <h2 className="font-serif text-xl sm:text-2xl text-wine font-medium border-b border-border pb-4">
            Resumen de compra ({totalCount})
          </h2>

          {/* Items list */}
          <div className="divide-y divide-border max-h-80 overflow-y-auto pr-1">
            {items.map((item, idx) => (
              <div key={`${item.product.id}-${item.variantId || idx}`} className="py-3.5 flex items-center justify-between gap-3 text-sm">
                <div className="flex items-center gap-3 min-w-0">
                  <div className="w-11 h-11 rounded-xl bg-gradient-to-br from-[#f8eadc] to-[#edd0d9] flex items-center justify-center text-lg flex-shrink-0 shadow-inner">
                    {item.product.icon || '💄'}
                  </div>
                  <div className="min-w-0">
                    <p className="font-semibold text-xs sm:text-sm text-ink truncate">
                      {item.product.name}
                    </p>
                    <p className="text-[11px] text-muted">
                      {item.quantity} × {formatCOP(item.product.price)}
                    </p>
                  </div>
                </div>

                <div className="text-right flex-shrink-0 font-bold text-wine text-xs sm:text-sm">
                  {formatCOP(item.product.price * item.quantity)}
                </div>
              </div>
            ))}
          </div>

          {/* Calculations */}
          <div className="space-y-2.5 border-t border-border pt-4 text-sm">
            <div className="flex justify-between text-muted text-xs sm:text-sm">
              <span>Subtotal ({totalCount} unidades)</span>
              <span className="font-semibold text-ink">{formatCOP(totalAmount)}</span>
            </div>

            <div className="flex justify-between text-muted text-xs sm:text-sm">
              <span>Costo de envío</span>
              <span className="text-ink font-medium">
                {deliveryType === 'RETIRO_TIENDA' ? 'Gratis' : 'Por coordinar'}
              </span>
            </div>

            <div className="border-t border-border pt-3 flex justify-between items-baseline">
              <span className="font-bold text-ink text-sm sm:text-base">Total productos</span>
              <span className="font-serif text-2xl font-bold text-wine">
                {formatCOP(totalAmount)}
              </span>
            </div>
          </div>

          {/* Safety & Trust guarantees */}
          <div className="border-t border-border pt-4 space-y-2 text-xs text-muted">
            <div className="flex items-center gap-2">
              <ShieldCheck className="w-4 h-4 text-wine flex-shrink-0" />
              <span>Garantía de productos 100% auténticos</span>
            </div>
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 text-success flex-shrink-0" />
              <span>Confirmación asistida con asesor vía WhatsApp</span>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}
