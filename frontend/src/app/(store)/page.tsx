import HomeView from '@/components/store/HomeView';
import { fetchCatalogPage } from '@/lib/server-catalog';

export const revalidate = 60;

export default async function HomePage() {
  const featured = await fetchCatalogPage({ size: 4, inStockOnly: true });
  return <HomeView initialFeatured={featured?.content ?? null} />;
}
