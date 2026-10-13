import { type ClassValue, clsx } from 'clsx';
import { twMerge } from 'tailwind-merge';

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs));
}

/**
 * Format currency in Colombian Pesos (COP) without decimals.
 * e.g. 25000 -> "$ 25.000"
 */
export function formatCOP(amount: number): string {
  const formatted = new Intl.NumberFormat('es-CO', {
    maximumFractionDigits: 0,
  }).format(amount);
  return `$ ${formatted}`;
}

/**
 * Returns today's date in Colombia timezone (America/Bogota) in YYYY-MM-DD format.
 */
export function getColombiaToday(): string {
  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Bogota',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
  return formatter.format(new Date());
}

/**
 * Returns the first day of the current month in Colombia timezone in YYYY-MM-DD format.
 */
export function getColombiaFirstDayOfMonth(): string {
  const today = getColombiaToday();
  const [year, month] = today.split('-');
  return `${year}-${month}-01`;
}

/**
 * Returns date N days ago in Colombia timezone in YYYY-MM-DD format.
 */
export function getColombiaDaysAgo(days: number): string {
  const d = new Date();
  d.setDate(d.getDate() - days);
  const formatter = new Intl.DateTimeFormat('en-CA', {
    timeZone: 'America/Bogota',
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
  });
  return formatter.format(d);
}

/**
 * Formats a date/timestamp to Colombia local date time string.
 */
export function formatColombiaDateTime(dateStr?: string | null): string {
  if (!dateStr) return '—';
  try {
    const d = new Date(dateStr);
    if (isNaN(d.getTime())) return dateStr;
    return new Intl.DateTimeFormat('es-CO', {
      timeZone: 'America/Bogota',
      year: 'numeric',
      month: 'short',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
      hour12: true,
    }).format(d);
  } catch {
    return dateStr;
  }
}
