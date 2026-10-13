const number = process.env.NEXT_PUBLIC_WHATSAPP_BUSINESS_NUMBER || '';
export function whatsappContact(message: string): string | undefined {
  return /^[1-9]\d{9,14}$/.test(number) ? `https://wa.me/${number}?text=${encodeURIComponent(message)}` : undefined;
}
