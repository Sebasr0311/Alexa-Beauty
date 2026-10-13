'use client';

import React, { useEffect, useState, use } from 'react';
import Link from 'next/link';
import { useSearchParams } from 'next/navigation';
import { api, OrderTrackingResponseDto } from '@/lib/api';
import { formatCOP } from '@/lib/utils';
import {
  Clock,
  MessageCircle,
  Copy,
  Check,
  CheckCircle2,
  AlertTriangle,
  ArrowRight,
  ShoppingBag,
  ExternalLink,
  ShieldAlert,
} from 'lucide-react';

interface OrderConfirmedPageProps {
  params: Promise<{ reference: string }>;
}

export default function OrderConfirmedPage({ params }: OrderConfirmedPageProps) {
  const { reference } = use(params);
  const searchParams = useSearchParams();
  const token = searchParams.get('token') || '';

  const [order, setOrder] = useState<OrderTrackingResponseDto | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  // Countdown timer states
  const [secondsRemaining, setSecondsRemaining] = useState<number | null>(null);
  const [copied, setCopied] = useState(false);

  // Fetch live tracking details
  useEffect(() => {
    async function fetchOrder() {
      if (!reference) return;
      try {
        setLoading(true);
        if (token) {
          const data = await api.trackOrder(reference, token);
          setOrder(data);
          if (data.expiresAt) {
            const diff = Math.max(0, Math.floor((new Date(data.expiresAt).getTime() - Date.now()) / 1000));
            setSecondsRemaining(diff);
          }
        }
      } catch (err: any) {
        console.error('Error tracking order:', err);
        setError('No se pudo cargar el detalle del pedido en tiempo real.');
      } finally {
        setLoading(false);
      }
    }

    fetchOrder();
  }, [reference, token]);

  // Countdown timer interval
  useEffect(() => {
    if (secondsRemaining === null || secondsRemaining <= 0) return;

    const interval = setInterval(() => {
      setSecondsRemaining((prev) => {
        if (prev === null || prev <= 1) {
          clearInterval(interval);
          return 0;
        }
        return prev - 1;
      });
    }, 1000);

    return () => clearInterval(interval);
  }, [secondsRemaining]);

  // Format seconds into MM:SS
  const formatTime = (totalSecs: number) => {
    const mins = Math.floor(totalSecs / 60);
    const secs = totalSecs % 60;
    return `${String(mins).padStart(2, '0')}:${String(secs).padStart(2, '0')}`;
  };

  const isExpired = secondsRemaining !== null && secondsRemaining <= 0;

  // Handle clipboard copy fallback (P24 matrix)
  const handleCopyMessage = async () => {
    const textToCopy =
      order?.whatsappMessageText ||
      `Hola, Alexa Beauty 🌷\nQuiero consultar y confirmar mi pedido ${reference}.\nTotal productos: ${order ? formatCOP(order.total) : ''}\n¿Me ayudan a confirmar disponibilidad y cómo realizar el pago?`;

    try {
      await navigator.clipboard.writeText(textToCopy);
      setCopied(true);
      setTimeout(() => setCopied(false), 2500);
    } catch {
      // Fallback if clipboard API fails
      prompt('Copia el siguiente mensaje de pedido para WhatsApp:', textToCopy);
    }
  };

  // WhatsApp click-to-chat URL fallback
  const whatsappUrl =
    order?.whatsappUrl ||
    `https://wa.me/573001234567?text=${encodeURIComponent(
      `Hola, Alexa Beauty 🌷\nQuiero consultar y confirmar mi pedido ${reference}.\nTotal: ${order ? formatCOP(order.total) : ''}\n¿Me ayudan a confirmar disponibilidad?`
    )}`;

  return (
    <div className="max-w-3xl mx-auto space-y-8 py-4 sm:py-8">
      {/* Main Confirmation Box */}
      <div className="bg-white border border-[#22795B]/30 rounded-3xl p-6 sm:p-10 text-center shadow-soft space-y-6">
        {/* Success Icon */}
        <div className="w-16 h-16 rounded-full bg-[#E2F5EA] text-[#22795B] mx-auto flex items-center justify-center text-3xl shadow-inner">
          <CheckCircle2 className="w-9 h-9" />
        </div>

        {/* Reference & Title */}
        <div className="space-y-2">
          <span className="text-[11px] sm:text-xs uppercase font-extrabold tracking-widest text-[#22795B] block">
            Pedido reservado exitosamente
          </span>
          <h1 className="font-serif text-3xl sm:text-4xl text-wine font-normal tracking-tight">
            Referencia: <span className="font-mono font-bold">{reference}</span>
          </h1>
        </div>

        {/* Countdown Timer Banner */}
        <div
          className={`rounded-2xl p-5 border transition-all ${
            isExpired
              ? 'bg-red-50 border-red-200 text-red-900'
              : 'bg-cream border-border text-ink'
          }`}
        >
          {isExpired ? (
            <div className="space-y-1">
              <div className="flex items-center justify-center gap-2 text-red-600 font-bold text-sm">
                <ShieldAlert className="w-4 h-4" />
                <span>Tiempo de reserva agotado (45 min)</span>
              </div>
              <p className="text-xs text-red-700 leading-relaxed max-w-md mx-auto">
                Tu reserva temporal ha finalizado y el stock fue liberado. Si aún deseas los productos, puedes volver a iniciar el pedido o consultarnos directamente en WhatsApp.
              </p>
            </div>
          ) : (
            <div className="space-y-1">
              <div className="flex items-center justify-center gap-2 text-wine font-extrabold text-xs uppercase tracking-wider">
                <Clock className="w-4 h-4" />
                <span>Tiempo restante de reserva exclusiva</span>
              </div>
              <div className="font-mono text-3xl sm:text-4xl font-extrabold text-wine tracking-widest my-1">
                {secondsRemaining !== null ? formatTime(secondsRemaining) : '45:00'}
              </div>
              <p className="text-[11px] text-muted">
                Tus artículos están separados y protegidos contra otros compradores.
              </p>
            </div>
          )}
        </div>

        {/* Honest Notice */}
        <div className="bg-[#FFF9EA] border border-[#F5E2B2] rounded-2xl p-4 sm:p-5 text-left text-xs sm:text-sm text-[#8A5C11] leading-relaxed flex items-start gap-3">
          <AlertTriangle className="w-5 h-5 text-[#B37400] flex-shrink-0 mt-0.5" />
          <p>
            <strong>Pedido guardado.</strong> Tus productos están reservados por <strong>45 minutos</strong>. Abre WhatsApp y presiona Enviar para confirmar disponibilidad y coordinar tu pago.
          </p>
        </div>

        {/* Primary and Secondary CTA Buttons */}
        <div className="flex flex-col sm:flex-row gap-3 pt-2">
          {/* Big Primary WhatsApp Button */}
          <a
            href={whatsappUrl}
            target="_blank"
            rel="noopener noreferrer"
            className="flex-1 py-4 px-6 rounded-full bg-[#25D366] hover:bg-[#1EBE5D] text-white font-bold text-sm sm:text-base flex items-center justify-center gap-2.5 shadow-md hover:shadow-lg transition-all active:scale-98"
          >
            <MessageCircle className="w-5 h-5 fill-current" />
            Continuar pedido en WhatsApp ↗
          </a>

          {/* Secondary Clipboard Fallback Button (P24 Matrix) */}
          <button
            type="button"
            onClick={handleCopyMessage}
            className="py-4 px-6 rounded-full border border-border bg-white hover:bg-cream text-ink font-bold text-sm sm:text-base flex items-center justify-center gap-2 transition-all active:scale-98"
          >
            {copied ? (
              <>
                <Check className="w-5 h-5 text-success" />
                <span className="text-success">¡Mensaje copiado!</span>
              </>
            ) : (
              <>
                <Copy className="w-5 h-5 text-muted" />
                <span>Copiar mensaje de pedido</span>
              </>
            )}
          </button>
        </div>

        {/* Order Details Accordion / Summary (if loaded) */}
        {order && (
          <div className="border-t border-border pt-6 text-left space-y-4">
            <h2 className="font-serif text-lg text-wine font-medium">Detalles del pedido reservado</h2>
            <div className="bg-cream/60 border border-border rounded-2xl p-4 text-xs space-y-2">
              <div className="flex justify-between">
                <span className="text-muted">Cliente:</span>
                <span className="font-semibold text-ink">{order.customerName || 'Cliente'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted">Método de entrega:</span>
                <span className="font-semibold text-ink">{order.deliveryType || 'ENVIO_DOMICILIO'}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-muted">Estado actual:</span>
                <span className="inline-block px-2 py-0.5 rounded-full bg-wine/10 text-wine font-bold text-[10px]">
                  {order.status}
                </span>
              </div>
              <div className="flex justify-between border-t border-border/60 pt-2 font-bold text-sm">
                <span>Total a confirmar:</span>
                <span className="text-wine font-serif text-base">{formatCOP(order.total)}</span>
              </div>
            </div>

            {order.items && order.items.length > 0 && (
              <div className="divide-y divide-border/60">
                {order.items.map((item) => (
                  <div key={item.id} className="py-2.5 flex justify-between items-center text-xs">
                    <div>
                      <span className="font-semibold text-ink">{item.productName}</span>
                      {item.variantName && <span className="text-muted block text-[11px]">{item.variantName}</span>}
                    </div>
                    <div className="text-right">
                      <span className="text-muted block">{item.quantity} × {formatCOP(item.unitPrice)}</span>
                      <span className="font-bold text-wine">{formatCOP(item.subtotal)}</span>
                    </div>
                  </div>
                ))}
              </div>
            )}
          </div>
        )}

        {/* Bottom Navigation */}
        <div className="border-t border-border pt-6 flex flex-col sm:flex-row justify-between items-center gap-3 text-xs text-muted">
          <span>¿Preguntas o cambios en tu pedido? Escríbenos en WhatsApp.</span>
          <Link
            href="/catalogo"
            className="font-bold text-wine hover:underline inline-flex items-center gap-1"
          >
            Seguir explorando el catálogo <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>
      </div>
    </div>
  );
}
