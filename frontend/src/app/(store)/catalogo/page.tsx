import type { Metadata } from 'next';
import CatalogView from '@/components/store/CatalogView';
import { fetchCatalogPage, fetchCategories } from '@/lib/server-catalog';

const CATALOG_PAGE_SIZE = 20;

export const revalidate = 60;

export const metadata: Metadata = {
  title: 'Catálogo — Alexa Beauty',
  description: 'Explora cosméticos y maquillaje seleccionados. Precios en COP y asesoría personalizada por WhatsApp.',
};

export default async function CatalogPage() {
  const [initialPage, initialCategories] = await Promise.all([
    fetchCatalogPage({ size: CATALOG_PAGE_SIZE }),
    fetchCategories(),
  ]);
  return <CatalogView initialPage={initialPage} initialCategories={initialCategories} />;
}
