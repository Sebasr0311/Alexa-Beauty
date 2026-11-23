const WHATSAPP_NUMBER_PATTERN = /^[1-9]\d{9,14}$/;
const COLOMBIA_MOBILE_PATTERN = /^3\d{9}$/;

export function normalizeWhatsAppNumber(raw: string | null | undefined): string {
  const digits = (raw ?? '').replace(/\D/g, '');
  if (!digits) return '';
  return COLOMBIA_MOBILE_PATTERN.test(digits) ? `57${digits}` : digits;
}

export function isValidWhatsAppNumber(raw: string | null | undefined): boolean {
  return WHATSAPP_NUMBER_PATTERN.test(normalizeWhatsAppNumber(raw));
}

export function formatWhatsAppNumberDisplay(raw: string | null | undefined): string {
  const normalized = normalizeWhatsAppNumber(raw);
  if (!normalized) return '';
  if (/^573\d{9}$/.test(normalized)) {
    return `+57 ${normalized.slice(2, 5)} ${normalized.slice(5, 8)} ${normalized.slice(8)}`;
  }
  return `+${normalized}`;
}

export function whatsappContact(
  message: string,
  businessNumber?: string | null,
): string | undefined {
  const normalized = normalizeWhatsAppNumber(businessNumber);
  return WHATSAPP_NUMBER_PATTERN.test(normalized)
    ? `https://wa.me/${normalized}?text=${encodeURIComponent(message)}`
    : undefined;
}
