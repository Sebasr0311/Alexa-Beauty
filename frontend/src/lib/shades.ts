import type { ProductImageDto, ProductVariantDto } from './api';

export function activeVariants(variants: ProductVariantDto[]): ProductVariantDto[] {
  return variants.filter(variant => variant.active);
}
export function initialVariant(variants: ProductVariantDto[]): ProductVariantDto | null {
  const active = activeVariants(variants);
  return active.find(variant => variant.availableStock > 0) || active[0] || null;
}
export function variantLabel(variant: Pick<ProductVariantDto, 'shadeCode' | 'shadeName' | 'presentation'>): string {
  const name = variant.shadeName?.trim();
  return [variant.shadeCode, name?.toLowerCase() === 'default' ? undefined : name, variant.presentation]
    .filter(value => value?.trim()).map(value => value!.trim()).join(' · ') || 'Única presentación';
}
export function variantImages(images: ProductImageDto[], variantId?: string): ProductImageDto[] {
  const own = images.filter(image => !!variantId && image.variantId === variantId);
  const general = images.filter(image => !image.variantId).sort((a,b) => Number(b.isCover) - Number(a.isCover) || a.sortOrder - b.sortOrder);
  return [...own, ...general];
}
