'use client';

import React, { useState, useEffect } from 'react';
import { Product, toStoreProduct } from '@/lib/data';
import { api, CategoryDto } from '@/lib/api';
import { ProductCard } from '@/components/ProductCard';
import { Search, X, Sparkles } from 'lucide-react';

export default function CatalogPage() {
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('');

  const [filteredProducts, setProducts] = useState<Product[]>([]);
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [page, setPage] = useState(0);
  const [totalPages, setTotalPages] = useState(0);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');
  useEffect(() => { api.getCategories().then(setCategories).catch(() => setError('No pudimos cargar las categorías.')); }, []);
  useEffect(() => {
    let cancelled = false;
    setLoading(true); setError('');
    const timer = setTimeout(() => {
      api.getCatalogProducts({ q: searchQuery, category: selectedCategory || undefined, page, size: 20 })
        .then(data => { if (!cancelled) { setProducts(data.content.map(toStoreProduct)); setTotalPages(data.totalPages); } })
        .catch(() => { if (!cancelled) { setProducts([]); setError('No pudimos cargar el catálogo. Intenta nuevamente.'); } })
        .finally(() => { if (!cancelled) setLoading(false); });
    }, 250);
    return () => { cancelled = true; clearTimeout(timer); };
  }, [searchQuery, selectedCategory, page]);

  return (
    <div className="space-y-8">
      {/* Header section */}
      <div className="border-b border-border pb-6">
        <span className="text-[11px] sm:text-xs font-extrabold uppercase tracking-[0.2em] text-wine flex items-center gap-1.5">
          <Sparkles className="w-3.5 h-3.5" />
          Toda la colección
        </span>
        <h1 className="font-serif text-3xl sm:text-4xl md:text-5xl text-wine font-normal tracking-tight mt-1">
          Encuentra tu favorito
        </h1>
        <p className="text-muted text-sm sm:text-base mt-2 max-w-xl">
          Explora nuestra selección de cosméticos de alta calidad. Precios transparentes en COP y asesoría personalizada.
        </p>

        {/* Search Input Bar */}
        <div className="mt-6 max-w-lg relative">
          <div className="flex items-center bg-white border border-border rounded-full px-4 py-2.5 shadow-sm focus-within:border-mauve focus-within:ring-2 focus-within:ring-mauve/20 transition">
            <Search className="w-4 h-4 text-muted flex-shrink-0 mr-2.5" />
            <input
              type="text"
              value={searchQuery}
              onChange={(e) => { setSearchQuery(e.target.value); setPage(0); }}
              placeholder="Buscar por nombre, marca, tono..."
              className="w-full bg-transparent border-0 outline-none text-ink placeholder:text-muted/70 text-sm"
              aria-label="Buscar productos en el catálogo"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                aria-label="Limpiar búsqueda"
                className="text-muted hover:text-ink p-1 rounded-full"
              >
                <X className="w-4 h-4" />
              </button>
            )}
          </div>
        </div>

        {/* Category Filter Chips */}
        <div
          role="tablist"
          aria-label="Filtrar por categoría"
          className="flex items-center gap-2 overflow-x-auto no-scrollbar pt-4 pb-1"
        >
          {[{ slug: '', name: 'Todos' }, ...categories].map((cat) => {
            const isSelected = selectedCategory === cat.slug;
            return (
              <button
                key={cat.slug}
                role="tab"
                aria-selected={isSelected}
                onClick={() => { setSelectedCategory(cat.slug); setPage(0); }}
                className={`flex-shrink-0 px-4 py-2 rounded-full text-xs sm:text-sm font-semibold transition-all ${
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
      <div className="flex items-center justify-between text-xs sm:text-sm text-muted">
        <p>
          Mostrando{' '}
          <strong className="text-wine font-bold">
            {filteredProducts.length}
          </strong>{' '}
          {filteredProducts.length === 1 ? 'producto' : 'productos'}
          {selectedCategory !== '' && ` en ${selectedCategory}`}
          {searchQuery && ` para "${searchQuery}"`}
        </p>

        {(searchQuery || selectedCategory !== '') && (
          <button
            type="button"
            onClick={() => {
              setSearchQuery('');
              setSelectedCategory(''); setPage(0);
            }}
            className="text-wine font-semibold hover:underline"
          >
            Restablecer filtros
          </button>
        )}
      </div>

      {loading && <p role="status">Cargando catálogo...</p>}
      {error && <p role="alert" className="text-red-700">{error}</p>}
      {/* Products Grid */}
      {loading || error ? null : filteredProducts.length > 0 ? (
        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3.5 sm:gap-5">
          {filteredProducts.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      ) : (
        <div className="text-center py-16 bg-white border border-border rounded-3xl p-8 max-w-md mx-auto shadow-sm">
          <span className="text-4xl mb-3 block">🔍</span>
          <h2 className="font-serif text-xl font-bold text-ink">
            No encontramos productos
          </h2>
          <p className="text-muted text-sm mt-1.5 leading-relaxed">
            Intenta con otro término de búsqueda o selecciona otra categoría.
          </p>
          <button
            type="button"
            onClick={() => {
              setSearchQuery('');
              setSelectedCategory(''); setPage(0);
            }}
            className="mt-5 inline-flex items-center justify-center rounded-full bg-wine text-white text-xs font-bold px-6 py-2.5 hover:bg-wineDark transition shadow"
          >
            Ver todos los productos
          </button>
        </div>
      )}
      {totalPages > 1 && !loading && !error && <nav aria-label="Paginación" className="flex gap-4 justify-center">
        <button disabled={page === 0} onClick={() => setPage(p => p - 1)}>Anterior</button>
        <span>{page + 1} / {totalPages}</span>
        <button disabled={page + 1 >= totalPages} onClick={() => setPage(p => p + 1)}>Siguiente</button>
      </nav>}
    </div>
  );
}
