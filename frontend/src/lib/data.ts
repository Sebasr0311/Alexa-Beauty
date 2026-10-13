import type { ProductSummaryDto } from './api';
export interface Product {
  id: string; slug: string; brand: string; name: string; category: string;
  price: number; stock: number; icon: string; visual: 'dark' | 'blush' | 'gold';
  badge: string; imageUrl?: string; description?: string; variantId?: string;
}
export function toStoreProduct(product: ProductSummaryDto): Product {
  return { id: product.id, slug: product.slug, brand: product.brandName || product.brand?.name || '',
    name: product.name, category: product.categoryName || product.category?.name || '', price: product.minPrice,
    stock: product.totalAvailableStock, icon: '💄', visual: 'blush',
    badge: product.totalAvailableStock > 0 ? 'Disponible' : 'Agotado', imageUrl: product.coverImageUrl };
}
