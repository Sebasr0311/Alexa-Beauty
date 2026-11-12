'use client';

import React, { useEffect, useRef, useState } from 'react';
import { Product, toStoreProduct } from '@/lib/data';
import { api, CategoryDto, PageDto, ProductSummaryDto } from '@/lib/api';
import { ProductCard } from '@/components/ProductCard';
import { ProductGridSkeleton } from '@/components/store/ProductGridSkeleton';
import { ChevronLeft, ChevronRight, Search, X, SearchX } from 'lucide-react';

const CATALOG_PAGE_SIZE = 20;
const SEARCH_DEBOUNCE_MS = 300;

interface CatalogViewProps {
  initialPage: PageDto<ProductSummaryDto> | null;
  initialCategories: CategoryDto[] | null;
}

export default function CatalogView({ initialPage, initialCategories }: CatalogViewProps) {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('');
  const [products, setProducts] = useState<Product[]>(initialPage ? initialPage.content.map(toStoreProduct) : []);
  const [categories, setCategories] = useState<CategoryDto[]>(initialCategories ?? []);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(initialPage?.totalPages ?? 0);
  const [totalElements, setTotalElements] = useState(initialPage?.totalElements ?? 0);
  const [loading, setLoading] = useState(!initialPage);
  const [error, setError] = useState('');
  const gridTopRef = useRef<HTMLDivElement>(null);
  // Skip the first client fetch when the server already rendered the default view.
  const skipInitialFetch = useRef(Boolean(initialPage));

  useEffect(() => {
    if (initialCategories) return;
    api.getCategories().then(setCategories).catch(() => setError('No pudimos cargar las categorías.'));
  }, [initialCategories]);

  useEffect(() => {
    if (skipInitialFetch.current) {
      skipInitialFetch.current = false;
      return;
    }
    let cancelled = false;
    setLoading(true);
    setError('');
    const timer = setTimeout(() => {
      api.getCatalogProducts({ q: searchQuery, category: selectedCategory || undefined, page, size: CATALOG_PAGE_SIZE })
        .then((data) => {
          if (cancelled) return;
          setProducts(data.content.map(toStoreProduct));
          setTotalPages(data.totalPages);
          setTotalElements(data.totalElements ?? data.content.length);
        })
        .catch(() => { if (!cancelled) { setProducts([]); setError('No pudimos cargar el catálogo. Intenta nuevamente.'); } })
        .finally(() => { if (!cancelled) setLoading(false); });
    }, searchQuery ? SEARCH_DEBOUNCE_MS : 0);
    return () => { cancelled = true; clearTimeout(timer); };
  }, [searchQuery, selectedCategory, page]);

  const selectedCategoryName = categories.find((cat) => cat.slug === selectedCategory)?.name;
  const hasFilters = searchQuery !== '' || selectedCategory !== '';

  const resetFilters = () => {
    setSearchQuery('');
    setSelectedCategory('');
    setPage(0);
  };

  const goToPage = (next: number) => {
    setPage(next);
    gridTopRef.current?.scrollIntoView({ behavior: 'smooth', block: 'start' });
  };

  return (
    <div className="space-y-6 sm:space-y-8">
      {/* Header section */}
      <div className="border-b border-border pb-5 sm:pb-6">
        <h1 className="font-serif text-[2.1rem] sm:text-5xl text-wine font-normal tracking-[-0.02em] leading-tight">
          Encuentra tu favorito
        </h1>
        <p className="text-muted text-sm sm:text-base mt-2 max-w-xl">
          Explora nuestra selección de cosméticos de alta calidad. Precios transparentes en COP y asesoría personalizada.
        </p>

        {/* Search Input Bar — 16px font avoids iOS auto-zoom on focus */}
        <div className="mt-5 sm:mt-6 max-w-lg relative">
          <label className="flex items-center min-h-12 bg-white border border-border rounded-full px-4 shadow-sm focus-within:border-mauve focus-within:ring-2 focus-within:ring-mauve/20 transition">
            <Search className="w-4 h-4 text-muted flex-shrink-0 mr-2.5" aria-hidden="true" />
            <input
              type="search"
              inputMode="search"
              enterKeyHint="search"
              autoComplete="off"
              value={searchQuery}
              onChange={(e) => { setSearchQuery(e.target.value); setPage(0); }}
              placeholder="Buscar por nombre, marca, tono..."
              className="w-full bg-transparent border-0 outline-none text-ink placeholder:text-muted/70 text-base sm:text-sm [&::-webkit-search-cancel-button]:hidden"
              aria-label="Buscar productos en el catálogo"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => { setSearchQuery(''); setPage(0); }}
                aria-label="Limpiar búsqueda"
                className="text-muted hover:text-ink w-9 h-9 -mr-2 grid place-items-center rounded-full"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </label>
        </div>

        {/* Category Filter Chips — edge-to-edge scroll rail on phones */}
        <div
          role="tablist"
          aria-label="Filtrar por categoría"
          className="flex items-center gap-2 overflow-x-auto no-scrollbar snap-x scroll-px-4 sm:scroll-px-0 pt-4 pb-1 -mx-4 px-4 sm:mx-0 sm:px-0 sm:flex-wrap"
        >
          {[{ slug: '', name: 'Todos' }, ...categories].map((cat) => {
            const isSelected = selectedCategory === cat.slug;
            return (
              <button
                key={cat.slug || 'all'}
                type="button"
                role="tab"
                aria-selected={isSelected}
                onClick={() => { setSelectedCategory(cat.slug); setPage(0); }}
                className={`snap-start flex-shrink-0 min-h-10 px-4 rounded-full text-sm font-semibold transition-all ${
                  isSelected
                    ? 'bg-wine text-white shadow-sm'
                    : 'bg-white border border-[#e3cbd3] text-[#734e5e] hover:bg-blush'
                }`}
              >
                {cat.name}
              </button>
            );
          })}
        </div>
      </div>

      {/* Results Meta Info */}
      <div ref={gridTopRef} className="scroll-mt-24 flex items-center justify-between gap-3 text-xs sm:text-sm text-muted">
        <p aria-live="polite">
          {loading ? 'Buscando productos…' : (
            <>
              <strong className="text-wine font-bold">{totalElements || products.length}</strong>{' '}
              {(totalElements || products.length) === 1 ? 'producto' : 'productos'}
              {selectedCategoryName && ` en ${selectedCategoryName}`}
              {searchQuery && ` para "${searchQuery}"`}
            </>
          )}
        </p>

        {hasFilters && (
          <button type="button" onClick={resetFilters} className="text-wine font-semibold hover:underline min-h-10 shrink-0">
            Restablecer filtros
          </button>
        )}
      </div>

      {error && <p role="alert" className="text-red-700 text-sm">{error}</p>}

      {/* Products Grid */}
      {loading ? (
        <ProductGridSkeleton count={8} />
      ) : error ? null : products.length > 0 ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-5">
          {products.map((product, index) => (
            <ProductCard key={product.id} product={product} priority={page === 0 && index < 4} />
          ))}
        </div>
      ) : (
        <div className="text-center py-12 sm:py-16 bg-white border border-border rounded-3xl p-6 sm:p-8 max-w-md mx-auto shadow-sm">
          <SearchX className="w-9 h-9 mx-auto mb-3 text-mauve" strokeWidth={1.6} aria-hidden="true" />
          <h2 className="font-serif text-xl font-bold text-ink">No encontramos productos</h2>
          <p className="text-muted text-sm mt-1.5 leading-relaxed">
            Intenta con otro término de búsqueda o selecciona otra categoría.
          </p>
          <button
            type="button"
            onClick={resetFilters}
            className="mt-5 inline-flex items-center justify-center min-h-11 rounded-full bg-wine text-white text-sm font-bold px-6 hover:bg-wineDark transition shadow"
          >
            Ver todos los productos
          </button>
        </div>
      )}

      {totalPages > 1 && !loading && !error && (
        <nav aria-label="Paginación" className="flex items-center justify-center gap-3 pt-2">
          <button
            type="button"
            disabled={page === 0}
            onClick={() => goToPage(page - 1)}
            className="inline-flex items-center gap-1 min-h-11 px-4 rounded-full border border-border bg-white text-sm font-semibold text-wine disabled:opacity-40 hover:bg-blush transition"
          >
            <ChevronLeft className="w-4 h-4" /> Anterior
          </button>
          <span className="text-sm text-muted tabular-nums min-w-14 text-center" aria-current="page">
            {page + 1} / {totalPages}
          </span>
          <button
            type="button"
            disabled={page + 1 >= totalPages}
            onClick={() => goToPage(page + 1)}
            className="inline-flex items-center gap-1 min-h-11 px-4 rounded-full border border-border bg-white text-sm font-semibold text-wine disabled:opacity-40 hover:bg-blush transition"
          >
            Siguiente <ChevronRight className="w-4 h-4" />
          </button>
        </nav>
      )}
    </div>
  );
}
