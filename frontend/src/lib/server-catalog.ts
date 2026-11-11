import 'server-only';
import type { CategoryDto, PageDto, ProductDetailDto, ProductSummaryDto } from './api';

/**
 * Server-side catalog reads used to pre-render public pages (ISR).
 * Every call degrades to `null` so the client components can fall back to
 * fetching in the browser when the backend is cold or unreachable.
 */

const REVALIDATE_SECONDS = 60;
const TIMEOUT_MS = 6000;

function backendBaseUrl(): string {
  const fallback = process.env.NODE_ENV === 'production'
    ? 'https://alexa-beauty-96wl.onrender.com/api/v1'
    : 'http://localhost:8080/api/v1';
  const clean = (process.env.API_BACKEND_URL || fallback).trim().replace(/^["']+|["']+$/g, '').replace(/\/+$/, '');
  return clean.endsWith('/api/v1') ? clean : `${clean}/api/v1`;
}

async function getJson<T>(path: string): Promise<T | null> {
  try {
    const response = await fetch(`${backendBaseUrl()}${path}`, {
      headers: { Accept: 'application/json' },
      next: { revalidate: REVALIDATE_SECONDS, tags: ['catalog'] },
      signal: AbortSignal.timeout(TIMEOUT_MS),
    });
    if (!response.ok) return null;
    const json = await response.json();
    return (json?.data !== undefined ? json.data : json) as T;
  } catch {
    return null;
  }
}

function normalizePage(page: PageDto<ProductSummaryDto> | null): PageDto<ProductSummaryDto> | null {
  if (!page || !Array.isArray(page.content)) return null;
  const raw = page as PageDto<ProductSummaryDto> & { number?: number; size?: number };
  return {
    ...page,
    content: page.content.map((item) => ({
      ...item,
      brandName: item.brandName ?? item.brand?.name ?? '',
      categoryName: item.categoryName ?? item.category?.name ?? '',
    })),
    pageNumber: page.pageNumber ?? raw.number ?? 0,
    pageSize: page.pageSize ?? raw.size ?? page.content.length,
  };
}

export async function fetchCatalogPage(params: { size: number; inStockOnly?: boolean }) {
  const query = new URLSearchParams({ page: '0', size: String(params.size) });
  if (params.inStockOnly) query.set('inStockOnly', 'true');
  return normalizePage(await getJson<PageDto<ProductSummaryDto>>(`/catalog/products?${query}`));
}

export async function fetchCategories() {
  const categories = await getJson<CategoryDto[]>('/catalog/categories');
  return Array.isArray(categories) ? categories : null;
}

export async function fetchProduct(slug: string) {
  const product = await getJson<ProductDetailDto>(`/catalog/products/${encodeURIComponent(slug)}`);
  return product && product.id ? product : null;
}
