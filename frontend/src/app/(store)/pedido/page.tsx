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
  const [address, setAddress] = useState('');
  const [barrio, setBarrio] = useState('');
  const [paymentMethod, setPaymentMethod] = useState<'NEQUI' | 'BANCOLOMBIA' | 'DAVIPLATA' | 'EFECTIVO'>('NEQUI');
  const [customerNotes, setCustomerNotes] = useState('');

  // UI state
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Auto-generate idempotency key for this session
  const [idempotencyKey] = useState(() => crypto.randomUUID());

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

    if (deliveryType === 'ENVIO_DOMICILIO') {
      if (!address.trim()) {
        setErrorMessage('Por favor ingresa la dirección de entrega.');
        return;
      }
      if (!barrio.trim()) {
        setErrorMessage('Por favor indica el barrio de entrega en Valledupar.');
        return;
      }
    }

    setLoading(true);
    setErrorMessage(null);

    try {
      const orderItems: OrderItemRequestDto[] = items.map(item => ({ variantId: item.variantId, quantity: item.quantity }));

      const fullAddress = deliveryType === 'ENVIO_DOMICILIO'
        ? `${address.trim()} (Barrio: ${barrio.trim()})`
        : undefined;

      const notesWithPayment = `Método de pago preferido: ${paymentMethod}${customerNotes.trim() ? ` | Notas: ${customerNotes.trim()}` : ''}`;

      // Prepare payload (city fixed to Valledupar)
      const orderPayload: CreateOrderRequestDto = {
        customerName: customerName.trim(),
        customerPhone: customerPhone.trim(),
        deliveryType,
        city: 'Valledupar',
        address: fullAddress,
        customerNotes: notesWithPayment,
        items: orderItems,
      };

      // Submit order to backend
      const response = await api.createOrder(orderPayload, idempotencyKey);

      clearCart();

      // Seamless direct redirect to WhatsApp
      if (response.whatsappUrl) {
        window.open(response.whatsappUrl, '_blank');
      }

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
            <span>Valledupar · Pedido asistido</span>
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
            Datos de entrega y pago
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
                A este número enviaremos los datos de seguimiento del pedido.
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
                      ? 'border-wine bg-wine/5 shadow-xs ring-1 ring-wine/30'
                      : 'border-border bg-cream/50 hover:bg-cream'
                  }`}
                >
                  <Truck className={`w-5 h-5 mt-0.5 ${deliveryType === 'ENVIO_DOMICILIO' ? 'text-wine' : 'text-muted'}`} />
                  <div>
                    <span className="block font-bold text-xs uppercase tracking-wider text-ink">
                      Envío a domicilio
                    </span>
                    <span className="text-[11px] text-muted mt-0.5 block">
                      Entrega en Valledupar
                    </span>
                  </div>
                </button>

                <button
                  type="button"
                  onClick={() => setDeliveryType('RETIRO_TIENDA')}
                  className={`p-4 rounded-2xl border text-left transition flex items-start gap-3 ${
                    deliveryType === 'RETIRO_TIENDA'
                      ? 'border-wine bg-wine/5 shadow-xs ring-1 ring-wine/30'
                      : 'border-border bg-cream/50 hover:bg-cream'
                  }`}
                >
                  <Store className={`w-5 h-5 mt-0.5 ${deliveryType === 'RETIRO_TIENDA' ? 'text-wine' : 'text-muted'}`} />
                  <div>
                    <span className="block font-bold text-xs uppercase tracking-wider text-ink">
                      Retiro en tienda
                    </span>
                    <span className="text-[11px] text-muted mt-0.5 block">
                      Punto físico · Sin costo adicional
                    </span>
                  </div>
                </button>
              </div>
            </div>

            {/* Delivery notice and address fields (shown only for ENVIO_DOMICILIO) */}
            {deliveryType === 'ENVIO_DOMICILIO' && (
              <div className="space-y-4 pt-1">
                {/* Notice that customer pays shipping on delivery */}
                <div className="bg-[#FFF8E7] border border-[#F0D597] rounded-2xl p-3.5 text-xs text-[#8A5C11] leading-relaxed flex items-start gap-2.5">
                  <Truck className="w-4 h-4 text-[#B37400] flex-shrink-0 mt-0.5" />
                  <p>
                    <strong>Costo de envío en Valledupar:</strong> El valor del domicilio corre por cuenta del cliente al recibir el pedido (pago contraentrega al repartidor). <strong>No se suma al total del pedido.</strong>
                  </p>
                </div>

                <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                  <div>
                    <label htmlFor="address" className="block text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
                      Dirección exacta <span className="text-wine">*</span>
                    </label>
                    <input
                      id="address"
                      type="text"
                      required={deliveryType === 'ENVIO_DOMICILIO'}
                      value={address}
                      onChange={(e) => setAddress(e.target.value)}
                      placeholder="Ej. Calle 12 # 15-40 Casa 3"
                      className="w-full bg-cream border border-border rounded-xl px-4 py-3 text-sm text-ink outline-none focus:border-wine transition"
                    />
                  </div>

                  <div>
                    <label htmlFor="barrio" className="block text-xs font-bold uppercase tracking-wider text-muted mb-1.5">
                      Barrio en Valledupar <span className="text-wine">*</span>
                    </label>
                    <input
                      id="barrio"
                      type="text"
                      required={deliveryType === 'ENVIO_DOMICILIO'}
                      value={barrio}
                      onChange={(e) => setBarrio(e.target.value)}
                      placeholder="Ej. Los Cortijos, La Nevada, Novalito..."
                      className="w-full bg-cream border border-border rounded-xl px-4 py-3 text-sm text-ink outline-none focus:border-wine transition"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* Payment Method Selector */}
            <div>
              <label className="block text-xs font-bold uppercase tracking-wider text-muted mb-2">
                Método de pago preferido <span className="text-wine">*</span>
              </label>
              <div className="grid grid-cols-2 sm:grid-cols-4 gap-2.5">
                {[
                  { id: 'NEQUI', label: 'Nequi', icon: '🟣' },
                  { id: 'BANCOLOMBIA', label: 'Bancolombia', icon: '🟡' },
                  { id: 'DAVIPLATA', label: 'Daviplata', icon: '🔴' },
                  { id: 'EFECTIVO', label: 'Efectivo', icon: '💵' },
                ].map((method) => {
                  const isSelected = paymentMethod === method.id;
                  return (
                    <button
                      key={method.id}
                      type="button"
                      onClick={() => setPaymentMethod(method.id as any)}
                      className={`p-3 rounded-2xl border text-center transition flex flex-col items-center justify-center gap-1 ${
                        isSelected
                          ? 'border-wine bg-wine/10 font-bold text-wine ring-1 ring-wine/30'
                          : 'border-border bg-cream/40 text-ink hover:bg-cream'
                      }`}
                    >
                      <span className="text-lg">{method.icon}</span>
                      <span className="text-xs">{method.label}</span>
                    </button>
                  );
                })}
              </div>
              <span className="text-[11px] text-muted block mt-1.5">
                Los datos de transferencia o pago se confirmarán directamente en el chat de WhatsApp.
              </span>
            </div>

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
                placeholder="Ej. Tono preferido, horario de entrega o instrucciones para el repartidor"
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
                    Confirmar pedido y abrir WhatsApp ↗
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
                    {item.variantName && (
                      <p className="text-xs text-wine font-semibold break-words">
                        Tono: {item.variantName}
                      </p>
                    )}
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
              <span>Costo de envío (Valledupar)</span>
              <span className="text-ink font-medium">
                {deliveryType === 'RETIRO_TIENDA' ? 'Gratis' : 'Contraentrega (no incluido)'}
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
