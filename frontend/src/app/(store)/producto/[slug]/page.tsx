import type { Metadata } from 'next';
import ProductDetailView from '@/components/store/ProductDetailView';
import { fetchProduct } from '@/lib/server-catalog';

interface ProductPageProps {
  params: Promise<{ slug: string }>;
}

export const revalidate = 60;

export async function generateMetadata({ params }: ProductPageProps): Promise<Metadata> {
  const { slug } = await params;
  const product = await fetchProduct(slug);
  if (!product) return { title: 'Producto — Alexa Beauty' };
  const cover = product.images?.find((image) => image.isCover)?.url ?? product.images?.[0]?.url;
  const description = product.description?.slice(0, 160) || `${product.name} de ${product.brand.name} en Alexa Beauty.`;
  return {
    title: `${product.name} — Alexa Beauty`,
    description,
    openGraph: { title: product.name, description, images: cover ? [cover] : undefined },
  };
}

export default async function ProductPage({ params }: ProductPageProps) {
  const { slug } = await params;
  const product = await fetchProduct(slug);
  return <ProductDetailView key={slug} slug={slug} initialProduct={product} />;
}
