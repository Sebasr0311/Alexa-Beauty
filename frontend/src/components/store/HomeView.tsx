'use client';

import React, { useEffect, useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { Product, toStoreProduct } from '@/lib/data';
import { api, ProductSummaryDto } from '@/lib/api';
import { whatsappContact } from '@/lib/contact';
import { ProductCard } from '@/components/ProductCard';
import { ProductGridSkeleton } from '@/components/store/ProductGridSkeleton';
import { Reveal } from '@/components/ui/Reveal';
import BlurText from '@/components/reactbits/BlurText';
import ShinyText from '@/components/reactbits/ShinyText';
import CircularText from '@/components/reactbits/CircularText';
import Magnet from '@/components/reactbits/Magnet';
import { ArrowUpRight, ArrowRight, MessageCircleHeart, Sparkles, Heart, ShieldCheck } from 'lucide-react';

const FEATURED_COUNT = 4;

const PERKS = [
  { icon: Sparkles, label: 'Productos seleccionados' },
  { icon: Heart, label: 'Atención personalizada' },
  { icon: ShieldCheck, label: 'Pedidos seguros por WhatsApp' },
];

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

  const contact = whatsappContact('Hola Alexa Beauty, quisiera asesoría sobre sus productos.');
  const handleWhatsAppContact = () => { if (contact) window.open(contact, '_blank', 'noopener,noreferrer'); };

  return (
    <div className="space-y-10 sm:space-y-16">
      {/* 1. Hero Section */}
      <section className="relative rounded-3xl overflow-hidden bg-gradient-to-br from-[#f7dfe5] via-[#f1e0df] to-[#e7b9c8] border border-border shadow-soft">
        <div className="grid grid-cols-1 md:grid-cols-[1.1fr_0.9fr] items-center md:min-h-[420px] lg:min-h-[480px]">
          {/* Copy Column */}
          <div className="order-2 md:order-1 px-6 pb-8 pt-2 sm:p-12 md:py-12 md:pl-10 md:pr-4 lg:p-16 flex flex-col justify-center items-start text-left">
            <span className="text-[11px] sm:text-xs font-extrabold uppercase tracking-[0.22em] mb-3 flex items-center gap-1.5 text-wine">
              <Sparkles className="w-3.5 h-3.5" aria-hidden="true" />
              <ShinyText text="Belleza que inspira" color="#711A38" shineColor="#F5C6D4" speed={3} />
            </span>

            <h1 className="font-serif text-[2.6rem] sm:text-5xl lg:text-6xl text-wineDark font-normal tracking-tight leading-[1.05] mb-4">
              <span className="sr-only">Tu belleza, tu esencia.</span>
              <span aria-hidden="true">
                <BlurText text="Tu belleza," trigger="load" delay={90} className="m-0" />
                <BlurText text="tu esencia." trigger="load" delay={90} className="m-0 italic text-[#af5c7c]" />
              </span>
            </h1>

            <p className="text-[15px] sm:text-lg text-[#6d5260] max-w-md leading-relaxed mb-7 sm:mb-8">
              Descubre esos pequeños detalles que te hacen brillar. Cosméticos y maquillaje seleccionados con amor para todos tus momentos.
            </p>

            <div className="flex flex-col sm:flex-row sm:flex-wrap items-stretch sm:items-center gap-3 w-full sm:w-auto">
              <Magnet padding={60} magnetStrength={4} wrapperClassName="w-full sm:w-auto" innerClassName="w-full sm:w-auto">
                <Link
                  href="/catalogo"
                  className="w-full sm:w-auto min-h-12 inline-flex items-center justify-center gap-2 rounded-full bg-wine hover:bg-wineDark text-white font-bold px-7 py-3.5 shadow-glow transition-colors active:scale-95"
                >
                  Explorar colección
                  <ArrowUpRight className="w-4 h-4 stroke-[2.5]" />
                </Link>
              </Magnet>

              <button
                type="button"
                onClick={handleWhatsAppContact}
                disabled={!contact}
                className="w-full sm:w-auto min-h-12 inline-flex items-center justify-center gap-2 rounded-full border border-[#b9899c] bg-white/80 hover:bg-white text-wine font-bold px-6 py-3.5 transition-all shadow-sm disabled:opacity-60"
              >
                <MessageCircleHeart className="w-4 h-4 text-wine" />
                Asesoría WhatsApp
              </button>
            </div>
          </div>

          {/* Art Column: logo framed by a slowly rotating circular caption */}
          <div className="order-1 md:order-2 relative h-[224px] sm:h-[320px] md:h-full flex items-center justify-center overflow-hidden">
            <div className="absolute w-[300px] sm:w-[420px] h-[300px] sm:h-[420px] rounded-full border border-wine/15 shadow-[0_0_0_35px_rgba(255,255,255,0.18),0_0_0_90px_rgba(255,255,255,0.1)] pointer-events-none" />
            <div className="scale-[0.62] sm:scale-[0.88] md:scale-[0.8] lg:scale-100 origin-center">
              <CircularText
                text="ALEXA ✦ BEAUTY ✦ TU ESENCIA ✦ "
                size={340}
                fontSize={17}
                fontWeight={700}
                letterSpacing={2}
                spinDuration={28}
                color="#711A38"
                onHover="slowDown"
              >
                <div className="relative w-[230px] aspect-square rounded-full overflow-hidden shadow-[0_16px_36px_rgba(84,25,46,0.25)] border-4 border-white/90 bg-blush">
                  <Image
                    src="/branding/logo-640.webp"
                    alt="Logo de Alexa Beauty"
                    fill
                    sizes="(max-width: 640px) 160px, 240px"
                    className="object-cover"
                    priority
                  />
                </div>
              </CircularText>
            </div>
          </div>
        </div>
      </section>

      {/* 2. Perks Strip: horizontal rail on phones, grid on larger screens */}
      <Reveal as="section" className="-mx-4 sm:mx-0">
        <h2 className="sr-only">Beneficios de compra</h2>
        <ul className="flex sm:grid sm:grid-cols-3 gap-3 sm:gap-3.5 overflow-x-auto no-scrollbar snap-x snap-mandatory px-4 sm:px-0">
          {PERKS.map(({ icon: Icon, label }) => (
            <li
              key={label}
              className="snap-start shrink-0 bg-white border border-border rounded-2xl px-4 py-3.5 sm:p-5 shadow-sm flex items-center justify-center gap-2.5"
            >
              <Icon className="w-4 h-4 text-wine flex-shrink-0" aria-hidden="true" />
              <span className="font-semibold text-wine text-xs sm:text-sm whitespace-nowrap sm:whitespace-normal sm:text-center">{label}</span>
            </li>
          ))}
        </ul>
      </Reveal>

      {/* 3. Featured Products Grid */}
      <section aria-labelledby="featured-heading">
        <Reveal className="flex flex-col sm:flex-row sm:items-end justify-between gap-3 sm:gap-4 mb-5 sm:mb-6">
          <div>
            <span className="text-[11px] font-extrabold uppercase tracking-widest text-muted">
              Favoritos de la tienda
            </span>
            <h2 id="featured-heading" className="font-serif text-3xl sm:text-4xl text-wine font-normal tracking-tight mt-1">
              Amor a primera vista
            </h2>
            <p className="text-muted text-sm mt-1">
              Un vistazo a nuestros productos más pedidos y recomendados.
            </p>
          </div>

          <Link
            href="/catalogo"
            className="inline-flex items-center gap-1.5 min-h-11 text-wine font-bold text-sm hover:underline self-start sm:self-auto"
          >
            Ver todo el catálogo
            <ArrowRight className="w-4 h-4" />
          </Link>
        </Reveal>

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

      {/* 4. WhatsApp Advisory Callout */}
      <Reveal as="section">
        <h2 className="sr-only">Asesoría personalizada</h2>
        <div className="bg-wineDark text-white rounded-3xl p-6 sm:p-12 flex flex-col md:flex-row items-center justify-between gap-6 shadow-soft">
          <div className="text-center md:text-left max-w-xl">
            <p className="font-serif text-2xl sm:text-3xl font-normal leading-tight">
              ¿Tienes dudas sobre algún tono o producto?
            </p>
            <p className="text-white/80 text-sm sm:text-base mt-2 leading-relaxed">
              Te ayudamos a elegir la base perfecta, el labial ideal para tu estilo y resolvemos cualquier pregunta antes de confirmar tu pedido.
            </p>
          </div>

          <button
            type="button"
            onClick={handleWhatsAppContact}
            disabled={!contact}
            className="w-full sm:w-auto min-h-12 flex-shrink-0 inline-flex items-center justify-center gap-2 rounded-full bg-white text-wine font-extrabold px-8 py-4 hover:bg-blush transition shadow-lg disabled:opacity-60"
          >
            <MessageCircleHeart className="w-5 h-5 text-wine" />
            <span>Consultar por WhatsApp</span>
          </button>
        </div>
      </Reveal>
    </div>
  );
}
