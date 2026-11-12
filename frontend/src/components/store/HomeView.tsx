'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { Product, toStoreProduct } from '@/lib/data';
import { api, ProductSummaryDto } from '@/lib/api';
import { whatsappContact } from '@/lib/contact';
import { ProductCard } from '@/components/ProductCard';
import { ProductGridSkeleton } from '@/components/store/ProductGridSkeleton';
import Magnet from '@/components/reactbits/Magnet';
import { ArrowUpRight, ArrowRight, MessageCircleHeart, Sparkles, Heart, ShieldCheck } from 'lucide-react';

const FEATURED_COUNT = 4;
const ORBIT_TEXT = 'ALEXA ✦ BEAUTY ✦ TU ESENCIA ✦ ALEXA ✦ BEAUTY ✦ TU ESENCIA ✦ ';

const PERKS = [
  { icon: Sparkles, label: 'Productos seleccionados' },
  { icon: Heart, label: 'Atención personalizada' },
  { icon: ShieldCheck, label: 'Pedidos seguros por WhatsApp' },
];

const WHATSAPP_HREF = whatsappContact('Hola Alexa Beauty, quisiera asesoría sobre sus productos.');

function HeadlineLine({ words, startIndex, className = '' }: { words: string[]; startIndex: number; className?: string }) {
  return (
    <span className={`block ${className}`}>
      {words.map((word, i) => (
        <span key={word} className="headline-word" style={{ animationDelay: `${(startIndex + i) * 110}ms` }}>
          {word}
          {i < words.length - 1 ? ' ' : ''}
        </span>
      ))}
    </span>
  );
}

interface HomeViewProps {
  initialFeatured: ProductSummaryDto[] | null;
}

export default function HomeView({ initialFeatured }: HomeViewProps) {
  const [featuredProducts, setFeaturedProducts] = useState<Product[] | null>(
    initialFeatured ? initialFeatured.map(toStoreProduct) : null,
  );
  const [catalogError, setCatalogError] = useState('');

  // Fallback for when the server snapshot could not reach the backend.
  useEffect(() => {
    if (initialFeatured) return;
    let cancelled = false;
    api.getCatalogProducts({ size: FEATURED_COUNT, inStockOnly: true })
      .then((data) => { if (!cancelled) setFeaturedProducts(data.content.map(toStoreProduct)); })
      .catch(() => { if (!cancelled) { setFeaturedProducts([]); setCatalogError('El catálogo no está disponible en este momento.'); } });
    return () => { cancelled = true; };
  }, [initialFeatured]);

  return (
    <div className="space-y-12 sm:space-y-20">
      {/* 1. Hero */}
      <section className="relative rounded-[1.75rem] sm:rounded-[2rem] overflow-hidden bg-[radial-gradient(120%_90%_at_85%_10%,#f3c9d6_0%,#f7e3e8_45%,#fbefe9_100%)] border border-border shadow-soft">
        <div className="grid grid-cols-1 md:grid-cols-[1.1fr_0.9fr] items-center md:min-h-[440px] lg:min-h-[500px]">
          {/* Copy */}
          <div className="order-2 md:order-1 px-6 pb-9 pt-1 sm:p-12 md:py-14 md:pl-12 md:pr-4 lg:p-16 flex flex-col items-start text-left">
            <h1 className="font-serif text-[2.75rem] sm:text-6xl lg:text-7xl text-wineDark font-normal tracking-[-0.02em] leading-[1.02] mb-5">
              <HeadlineLine words={['Tu', 'belleza,']} startIndex={0} />
              <HeadlineLine words={['tu', 'esencia.']} startIndex={2} className="italic text-[#a3486c]" />
            </h1>

            <p className="text-[15px] sm:text-lg text-[#6a4c5a] max-w-md leading-relaxed mb-8">
              Descubre esos pequeños detalles que te hacen brillar. Cosméticos y maquillaje seleccionados con amor para todos tus momentos.
            </p>

            <div className="flex flex-col sm:flex-row sm:flex-wrap items-stretch sm:items-center gap-3 w-full sm:w-auto">
              <Magnet padding={60} magnetStrength={4} wrapperClassName="w-full sm:w-auto" innerClassName="w-full sm:w-auto">
                <Link
                  href="/catalogo"
                  className="w-full sm:w-auto min-h-[52px] inline-flex items-center justify-center gap-2 rounded-full bg-wine hover:bg-wineDark text-white font-bold px-8 shadow-glow transition-[background-color,transform] duration-200 active:scale-[0.97]"
                >
                  Explorar colección
                  <ArrowUpRight className="w-4 h-4 stroke-[2.5]" aria-hidden="true" />
                </Link>
              </Magnet>

              {WHATSAPP_HREF && (
                <a
                  href={WHATSAPP_HREF}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="w-full sm:w-auto min-h-[52px] inline-flex items-center justify-center gap-2 rounded-full border border-wine/25 bg-white/70 hover:bg-white text-wine font-bold px-7 transition-colors"
                >
                  <MessageCircleHeart className="w-4 h-4" aria-hidden="true" />
                  Asesoría por WhatsApp
                </a>
              )}
            </div>
          </div>

          {/* Art: logo framed by a slowly orbiting caption */}
          <div className="order-1 md:order-2 relative h-[248px] sm:h-[340px] md:h-full flex items-center justify-center">
            <div
              aria-hidden="true"
              className="absolute w-[300px] h-[300px] sm:w-[420px] sm:h-[420px] rounded-full bg-white/25 shadow-[0_0_0_36px_rgba(255,255,255,0.14),0_0_0_90px_rgba(255,255,255,0.08)]"
            />
            <div className="relative w-[214px] h-[214px] sm:w-[300px] sm:h-[300px] lg:w-[340px] lg:h-[340px]">
              <svg viewBox="0 0 340 340" className="orbit absolute inset-0 w-full h-full text-wine" aria-hidden="true">
                <defs>
                  <path id="hero-orbit" d="M170,170 m-150,0 a150,150 0 1,1 300,0 a150,150 0 1,1 -300,0" />
                </defs>
                <text className="fill-current" style={{ fontSize: 15, fontWeight: 700, letterSpacing: '0.32em' }}>
                  <textPath href="#hero-orbit" textLength="930" lengthAdjust="spacing">{ORBIT_TEXT}</textPath>
                </text>
              </svg>
              <div className="absolute inset-[16%] rounded-full overflow-hidden border-4 border-white shadow-[0_18px_40px_rgba(84,25,46,0.24)] bg-blush">
                <Image
                  src="/branding/logo-640.webp"
                  alt="Logo de Alexa Beauty"
                  fill
                  sizes="(max-width: 640px) 150px, 240px"
                  className="object-cover"
                  priority
                />
              </div>
            </div>
          </div>
        </div>
      </section>

      {/* 2. Promises: a quiet trust row, not a set of cards */}
      <section aria-label="Beneficios de compra">
        <ul className="grid grid-cols-3 divide-x divide-border border-y border-border py-4 sm:py-5">
          {PERKS.map(({ icon: Icon, label }) => (
            <li key={label} className="flex flex-col sm:flex-row items-center justify-center gap-1.5 sm:gap-2.5 px-2 text-center">
              <Icon className="w-[18px] h-[18px] text-mauve flex-shrink-0" aria-hidden="true" />
              <span className="text-[11.5px] sm:text-sm font-semibold text-wine leading-tight">{label}</span>
            </li>
          ))}
        </ul>
      </section>

      {/* 3. Featured products */}
      <section aria-labelledby="featured-heading">
        <div className="flex items-end justify-between gap-4 mb-5 sm:mb-7">
          <div>
            <h2 id="featured-heading" className="font-serif text-[1.85rem] sm:text-4xl text-wine font-normal tracking-[-0.015em] leading-tight">
              Amor a primera vista
            </h2>
            <p className="text-muted text-sm mt-1.5">Lo más pedido y recomendado de la tienda.</p>
          </div>

          <Link
            href="/catalogo"
            className="group shrink-0 inline-flex items-center gap-1 min-h-11 text-wine font-bold text-sm"
          >
            Ver todo
            <ArrowRight className="w-4 h-4 transition-transform group-hover:translate-x-0.5" aria-hidden="true" />
          </Link>
        </div>

        {catalogError && <p role="alert" className="text-sm text-red-700">{catalogError}</p>}
        {featuredProducts === null ? (
          <ProductGridSkeleton count={FEATURED_COUNT} />
        ) : featuredProducts.length === 0 && !catalogError ? (
          <p className="text-sm text-muted">Consulta el catálogo para ver la disponibilidad actual.</p>
        ) : (
          <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3 sm:gap-5">
            {featuredProducts.map((product, index) => (
              <ProductCard key={product.id} product={product} priority={index < 2} />
            ))}
          </div>
        )}
      </section>

      {/* 4. WhatsApp advisory */}
      {WHATSAPP_HREF && (
        <section aria-labelledby="advice-heading" className="relative overflow-hidden bg-wineDark text-white rounded-[1.75rem] p-7 sm:p-12 flex flex-col md:flex-row md:items-center justify-between gap-6 shadow-soft">
          <div aria-hidden="true" className="absolute -right-16 -top-20 w-64 h-64 rounded-full bg-[radial-gradient(circle,rgba(243,201,214,0.28),transparent_70%)]" />
          <div className="relative max-w-xl">
            <h2 id="advice-heading" className="font-serif text-[1.65rem] sm:text-3xl font-normal leading-tight">
              ¿Dudas sobre algún tono o producto?
            </h2>
            <p className="text-[#f1d9e1] text-sm sm:text-base mt-2.5 leading-relaxed">
              Te ayudamos a elegir la base perfecta, el labial ideal para tu estilo y resolvemos cualquier pregunta antes de confirmar tu pedido.
            </p>
          </div>

          <a
            href={WHATSAPP_HREF}
            target="_blank"
            rel="noopener noreferrer"
            className="relative w-full md:w-auto min-h-[52px] flex-shrink-0 inline-flex items-center justify-center gap-2 rounded-full bg-white text-wine font-extrabold px-8 hover:bg-blush transition-colors shadow-lg"
          >
            <MessageCircleHeart className="w-5 h-5" aria-hidden="true" />
            Consultar por WhatsApp
          </a>
        </section>
      )}
    </div>
  );
}
