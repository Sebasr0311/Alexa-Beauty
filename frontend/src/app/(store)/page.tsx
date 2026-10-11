'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { PRODUCTS } from '@/lib/data';
import { ProductCard } from '@/components/ProductCard';
import { ArrowUpRight, ArrowRight, MessageCircleHeart, Sparkles, Heart, ShieldCheck } from 'lucide-react';

export default function HomePage() {
  const featuredProducts = PRODUCTS.slice(0, 4);

  const handleWhatsAppDemo = () => {
    alert(
      'Demostración de WhatsApp:\n\nEn la versión conectada con backend, este enlace abrirá WhatsApp Business con una conversación preconfigurada y asesoría personalizada de Alexa Beauty.'
    );
  };

  return (
    <div className="space-y-12 sm:space-y-16">
      {/* 1. Hero Section */}
      <section className="relative rounded-3xl overflow-hidden bg-gradient-to-br from-[#f7dfe5] via-[#f1e0df] to-[#e7b9c8] border border-border shadow-soft">
        <div className="grid grid-cols-1 lg:grid-cols-[1.1fr_0.9fr] items-center min-h-[460px]">
          {/* Copy Column */}
          <div className="p-8 sm:p-12 md:p-16 flex flex-col justify-center items-start text-left">
            <span className="text-[11px] sm:text-xs font-extrabold uppercase tracking-[0.22em] text-wine mb-3 flex items-center gap-1.5">
              <Sparkles className="w-3.5 h-3.5" />
              Belleza que inspira
            </span>

            <h1 className="font-serif text-4xl sm:text-5xl md:text-6xl text-wineDark font-normal tracking-tight leading-[1.08] mb-4">
              Tu belleza,<br />
              <i className="italic text-[#af5c7c]">tu esencia.</i>
            </h1>

            <p className="text-base sm:text-lg text-[#6d5260] max-w-md leading-relaxed mb-8">
              Descubre esos pequeños detalles que te hacen brillar. Cosméticos y maquillaje seleccionados con amor para todos tus momentos.
            </p>

            <div className="flex flex-wrap items-center gap-3 w-full sm:w-auto">
              <Link
                href="/catalogo"
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-full bg-wine hover:bg-wineDark text-white font-bold px-7 py-3.5 shadow-glow transition-all active:scale-95"
              >
                Explorar colección
                <ArrowUpRight className="w-4 h-4 stroke-[2.5]" />
              </Link>

              <button
                type="button"
                onClick={handleWhatsAppDemo}
                className="w-full sm:w-auto inline-flex items-center justify-center gap-2 rounded-full border border-[#b9899c] bg-white/80 hover:bg-white text-wine font-bold px-6 py-3.5 transition-all shadow-sm"
              >
                <MessageCircleHeart className="w-4 h-4 text-wine" />
                Asesoría WhatsApp
              </button>
            </div>
          </div>

          {/* Art Illustration Column */}
          <div className="relative h-72 sm:h-80 lg:h-full flex items-center justify-center overflow-hidden p-6">
            {/* Concentric Decorative Rings */}
            <div className="absolute w-[340px] sm:w-[440px] h-[340px] sm:h-[440px] rounded-full border border-wine/15 shadow-[0_0_0_35px_rgba(255,255,255,0.18),0_0_0_90px_rgba(255,255,255,0.1)] pointer-events-none" />

            {/* Central Round Avatar / Logo */}
            <div className="relative w-52 sm:w-64 md:w-72 aspect-square rounded-full overflow-hidden shadow-[0_16px_36px_rgba(84,25,46,0.25)] border-4 border-white/90 bg-blush z-10">
              <Image
                src="/branding/logo.png"
                alt="Retrato de Alexa Beauty"
                fill
                sizes="(max-width: 768px) 240px, 320px"
                className="object-cover"
                priority
              />
            </div>

            {/* Floating Sparkle Icon */}
            <div
              className="absolute right-[12%] bottom-[16%] text-3xl sm:text-5xl text-wine/80 rotate-12 select-none pointer-events-none drop-shadow-sm z-20"
              aria-hidden="true"
            >
              ✦
            </div>
          </div>
        </div>
      </section>

      {/* 2. Perks Strip */}
      <section aria-label="Beneficios de compra">
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-3.5">
          <div className="bg-white border border-border rounded-2xl p-4 sm:p-5 text-center shadow-sm flex items-center justify-center gap-2.5">
            <Sparkles className="w-4 h-4 text-wine flex-shrink-0" />
            <span className="font-semibold text-wine text-xs sm:text-sm">
              Productos seleccionados
            </span>
          </div>

          <div className="bg-white border border-border rounded-2xl p-4 sm:p-5 text-center shadow-sm flex items-center justify-center gap-2.5">
            <Heart className="w-4 h-4 text-wine flex-shrink-0" />
            <span className="font-semibold text-wine text-xs sm:text-sm">
              Atención personalizada
            </span>
          </div>

          <div className="bg-white border border-border rounded-2xl p-4 sm:p-5 text-center shadow-sm flex items-center justify-center gap-2.5">
            <ShieldCheck className="w-4 h-4 text-wine flex-shrink-0" />
            <span className="font-semibold text-wine text-xs sm:text-sm">
              Pedidos seguros por WhatsApp
            </span>
          </div>
        </div>
      </section>

      {/* 3. Featured Products Grid */}
      <section aria-labelledby="featured-heading">
        <div className="flex flex-col sm:flex-row sm:items-end justify-between gap-4 mb-6">
          <div>
            <span className="text-[11px] font-extrabold uppercase tracking-widest text-muted">
              Favoritos de la tienda
            </span>
            <h2
              id="featured-heading"
              className="font-serif text-3xl sm:text-4xl text-wine font-normal tracking-tight mt-1"
            >
              Amor a primera vista
            </h2>
            <p className="text-muted text-sm mt-1">
              Un vistazo a nuestros productos más pedidos y recomendados.
            </p>
          </div>

          <Link
            href="/catalogo"
            className="inline-flex items-center gap-1.5 text-wine font-bold text-sm hover:underline self-start sm:self-auto"
          >
            Ver todo el catálogo
            <ArrowRight className="w-4 h-4" />
          </Link>
        </div>

        <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-4 gap-3.5 sm:gap-5">
          {featuredProducts.map((product) => (
            <ProductCard key={product.id} product={product} />
          ))}
        </div>
      </section>

      {/* 4. WhatsApp Advisory Callout */}
      <section aria-label="Asesoría personalizada">
        <div className="bg-wineDark text-white rounded-3xl p-8 sm:p-12 flex flex-col md:flex-row items-center justify-between gap-6 shadow-soft">
          <div className="text-center md:text-left max-w-xl">
            <h2 className="font-serif text-2xl sm:text-3xl font-normal leading-tight">
              ¿Tienes dudas sobre algún tono o producto?
            </h2>
            <p className="text-white/80 text-sm sm:text-base mt-2 leading-relaxed">
              Te ayudamos a elegir la base perfecta, el labial ideal para tu estilo y resolvemos cualquier pregunta antes de confirmar tu pedido.
            </p>
          </div>

          <button
            type="button"
            onClick={handleWhatsAppDemo}
            className="w-full sm:w-auto flex-shrink-0 inline-flex items-center justify-center gap-2 rounded-full bg-white text-wine font-extrabold px-8 py-4 hover:bg-blush transition shadow-lg"
          >
            <MessageCircleHeart className="w-5 h-5 text-wine" />
            <span>Consultar por WhatsApp</span>
          </button>
        </div>
      </section>
    </div>
  );
}
