'use client';

import React, { useState } from 'react';
import {
  CheckCircle2,
  AlertCircle,
  ExternalLink,
  Loader2,
  MessageCircle,
  Phone,
  Save,
  ShieldAlert,
} from 'lucide-react';
import { api } from '@/lib/api';
import {
  formatWhatsAppNumberDisplay,
  isValidWhatsAppNumber,
  normalizeWhatsAppNumber,
  whatsappContact,
} from '@/lib/contact';

export interface WhatsAppSettingsFormProps {
  initialNumber: string | null;
  initialUpdatedAt?: string | null;
  canEdit: boolean;
}

function formatUpdatedAt(value?: string | null): string | null {
  if (!value) return null;
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return null;
  return date.toLocaleString('es-CO', {
    dateStyle: 'medium',
    timeStyle: 'short',
  });
}

export function WhatsAppSettingsForm({
  initialNumber,
  initialUpdatedAt,
  canEdit,
}: WhatsAppSettingsFormProps) {
  const [savedNumber, setSavedNumber] = useState<string | null>(initialNumber);
  const [updatedAt, setUpdatedAt] = useState<string | null | undefined>(initialUpdatedAt);
  const [input, setInput] = useState<string>(initialNumber ?? '');
  const [saving, setSaving] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; message: string } | null>(null);

  const normalizedInput = normalizeWhatsAppNumber(input);
  const normalizedSaved = normalizeWhatsAppNumber(savedNumber);
  const hasInput = input.trim().length > 0;
  const isValid = isValidWhatsAppNumber(input);
  const isUnchanged = isValid && normalizedInput === normalizedSaved;
  const previewTestHref = isValid
    ? whatsappContact('Hola Alexa Beauty, mensaje de prueba de configuración.', input)
    : undefined;
  const formattedUpdatedAt = formatUpdatedAt(updatedAt);

  const handleSubmit = async (event: React.FormEvent) => {
    event.preventDefault();
    if (!canEdit || !isValid || isUnchanged || saving) return;

    setSaving(true);
    setFeedback(null);

    try {
      const updated = await api.updateWhatsAppNumber(input);
      const nextValue = updated.whatsappNumber ?? normalizedInput;
      setSavedNumber(nextValue);
      setUpdatedAt(updated.updatedAt ?? new Date().toISOString());
      setInput(nextValue ?? '');
      setFeedback({
        type: 'success',
        message: `Número oficial actualizado a ${formatWhatsAppNumberDisplay(nextValue)}. Los cambios ya están activos en la tienda.`,
      });
    } catch (err: unknown) {
      const message =
        err instanceof Error && err.message
          ? err.message
          : 'No pudimos actualizar el número de WhatsApp. Intenta nuevamente.';
      setFeedback({ type: 'error', message });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-[1.2fr_0.8fr] gap-6 items-start">
      <section className="bg-white border border-border rounded-3xl p-6 sm:p-8 shadow-soft space-y-6">
        <div className="flex items-start justify-between gap-4 border-b border-border pb-4">
          <div>
            <h2 className="font-serif text-xl sm:text-2xl text-wine font-medium">
              Número de WhatsApp de la tienda
            </h2>
            <p className="text-muted text-xs sm:text-sm mt-1">
              Los mensajes de pedidos y asesoría llegarán directamente a este número.
            </p>
          </div>
          <div className="w-10 h-10 rounded-2xl bg-[#E2F5EA] text-[#1F7A4C] flex items-center justify-center flex-shrink-0">
            <MessageCircle className="w-5 h-5" aria-hidden="true" />
          </div>
        </div>

        {/* Active Number Card */}
        <div className="rounded-2xl border border-border bg-cream/70 p-4 sm:p-5 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
          <div className="space-y-1">
            <span className="text-[11px] font-bold uppercase tracking-wider text-muted block">
              Número activo actualmente
            </span>
            <p className="font-serif text-2xl font-bold text-wine tracking-tight">
              {savedNumber ? formatWhatsAppNumberDisplay(savedNumber) : 'Sin configurar'}
            </p>
            {savedNumber && (
              <p className="text-xs font-mono text-muted">
                Formato E.164: {normalizeWhatsAppNumber(savedNumber)}
              </p>
            )}
          </div>

          {formattedUpdatedAt && (
            <div className="text-xs text-muted sm:text-right">
              <span className="block font-semibold text-ink">Última actualización</span>
              <span>{formattedUpdatedAt}</span>
            </div>
          )}
        </div>

        {!canEdit && (
          <div
            role="note"
            className="bg-[#FFF9EA] border border-[#F5E2B2] rounded-2xl p-4 text-xs sm:text-sm text-[#8A5C11] flex items-start gap-3"
          >
            <ShieldAlert className="w-5 h-5 text-[#B37400] flex-shrink-0 mt-0.5" aria-hidden="true" />
            <p>
              Solo un usuario con rol Administrador puede modificar este número.
            </p>
          </div>
        )}

        {feedback && (
          <div
            role={feedback.type === 'success' ? 'status' : 'alert'}
            className={`rounded-2xl p-4 text-xs sm:text-sm flex items-start gap-3 border ${
              feedback.type === 'success'
                ? 'bg-[#E2F5EA]/70 border-[#22795B]/30 text-[#196B3E]'
                : 'bg-red-50 border-red-200 text-red-800'
            }`}
          >
            {feedback.type === 'success' ? (
              <CheckCircle2 className="w-5 h-5 flex-shrink-0 mt-0.5" aria-hidden="true" />
            ) : (
              <AlertCircle className="w-5 h-5 flex-shrink-0 mt-0.5" aria-hidden="true" />
            )}
            <p className="leading-relaxed font-medium">{feedback.message}</p>
          </div>
        )}

        <form onSubmit={handleSubmit} className="space-y-5">
          <div>
            <label
              htmlFor="whatsappNumber"
              className="block text-xs font-bold uppercase tracking-wider text-muted mb-1.5"
            >
              Nuevo número de WhatsApp
            </label>
            <div className="relative">
              <Phone
                className="w-4 h-4 text-muted absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none"
                aria-hidden="true"
              />
              <input
                id="whatsappNumber"
                type="tel"
                inputMode="tel"
                autoComplete="tel"
                disabled={!canEdit || saving}
                value={input}
                onChange={(event) => {
                  setInput(event.target.value);
                  if (feedback) setFeedback(null);
                }}
                placeholder="Ej. 323 5814497 o 573235814497"
                aria-describedby="whatsapp-number-help"
                className="w-full bg-cream border border-border rounded-xl pl-11 pr-4 py-3 text-base sm:text-sm text-ink outline-none focus:border-wine focus:ring-2 focus:ring-wine/10 transition disabled:opacity-60 disabled:cursor-not-allowed"
              />
            </div>
            <p id="whatsapp-number-help" className="text-[11px] text-muted mt-1.5 leading-relaxed">
              Si ingresas un celular colombiano de 10 dígitos (ej. 323 5814497), agregaremos automáticamente el indicativo +57. Para otros países, incluye el código de país sin símbolos.
            </p>
          </div>

          {hasInput && !isValid && (
            <div
              role="alert"
              className="bg-red-50 border border-red-200 text-red-800 rounded-xl px-4 py-3 text-xs leading-relaxed"
            >
              Ingresa un número de WhatsApp válido (10 dígitos en Colombia o entre 10 y 15 dígitos con código de país).
            </div>
          )}

          {isValid && (
            <div className="rounded-2xl border border-wine/20 bg-blush/35 p-4 flex flex-col sm:flex-row sm:items-center justify-between gap-3">
              <div className="space-y-0.5 text-xs">
                <span className="font-bold uppercase tracking-wider text-wine block">
                  Vista previa del enlace
                </span>
                <p className="text-ink font-semibold text-sm">
                  {formatWhatsAppNumberDisplay(input)}
                </p>
                <p className="font-mono text-muted break-all">
                  https://wa.me/{normalizedInput}
                </p>
              </div>

              {previewTestHref && (
                <a
                  href={previewTestHref}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="inline-flex items-center justify-center gap-1.5 rounded-full border border-wine/30 bg-white hover:bg-cream text-wine font-bold text-xs px-4 py-2 transition flex-shrink-0"
                >
                  <span>Probar en WhatsApp</span>
                  <ExternalLink className="w-3.5 h-3.5" aria-hidden="true" />
                </a>
              )}
            </div>
          )}

          <div className="pt-1">
            <button
              type="submit"
              disabled={!canEdit || saving || !isValid || isUnchanged}
              className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-full bg-wine hover:bg-wineDark text-white font-bold text-sm px-7 py-3.5 shadow-glow transition active:scale-98 disabled:opacity-50 disabled:cursor-not-allowed"
            >
              {saving ? (
                <>
                  <Loader2 className="w-4 h-4 animate-spin" aria-hidden="true" />
                  <span>Guardando cambios...</span>
                </>
              ) : (
                <>
                  <Save className="w-4 h-4" aria-hidden="true" />
                  <span>Guardar número de WhatsApp</span>
                </>
              )}
            </button>
          </div>
        </form>
      </section>

      <aside className="bg-white border border-border rounded-3xl p-6 shadow-soft space-y-4">
        <h3 className="font-serif text-lg text-wine font-medium">
          ¿Cómo se aplica este cambio?
        </h3>
        <ul className="space-y-3 text-xs sm:text-sm text-muted leading-relaxed">
          <li className="flex items-start gap-2.5">
            <CheckCircle2 className="w-4 h-4 text-[#22795B] flex-shrink-0 mt-0.5" aria-hidden="true" />
            <span>
              <strong className="text-ink">Actualización inmediata:</strong> se refleja al instante en los botones de contacto de la tienda, nuevos pedidos y enlaces de seguimiento sin necesidad de redesplegar.
            </span>
          </li>
          <li className="flex items-start gap-2.5">
            <CheckCircle2 className="w-4 h-4 text-[#22795B] flex-shrink-0 mt-0.5" aria-hidden="true" />
            <span>
              <strong className="text-ink">Registro de auditoría:</strong> cada cambio queda guardado con fecha y usuario responsable.
            </span>
          </li>
        </ul>
      </aside>
    </div>
  );
}
