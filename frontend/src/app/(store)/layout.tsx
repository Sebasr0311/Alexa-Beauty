'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { usePathname } from 'next/navigation';
import { useCart } from '@/context/CartContext';
import { Home, Sparkles, ShoppingBag, ShieldCheck, Search } from 'lucide-react';

export default function StoreLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const pathname = usePathname();
  const { totalCount } = useCart();

  const navLinks = [
    { href: '/', label: 'Inicio' },
    { href: '/catalogo', label: 'Catálogo' },
    { href: '/carrito', label: 'Carrito' },
    { href: '/admin', label: 'Vista admin' },
  ];

  return (
    <div className="min-h-screen flex flex-col bg-cream text-ink">
      {/* 1. Announcement Banner */}
      <aside
        role="region"
        aria-label="Información de compra"
        className="bg-wineDark text-white text-[11px] sm:text-xs font-semibold tracking-widest text-center py-2 px-3 selection:bg-mauve"
      >
        PEDIDOS CON RESERVA · PAGO MANUAL VERIFICADO POR NUESTRO EQUIPO
      </aside>

      {/* 2. Sticky Header */}
      <header className="sticky top-0 z-30 bg-cream/95 backdrop-blur-md border-b border-border shadow-sm">
        <div className="max-w-6xl mx-auto px-4 sm:px-6 py-2.5 flex items-center justify-between gap-4">
          {/* Brand Logo & Name */}
          <Link
            href="/"
            className="flex items-center gap-3 group select-none outline-none focus-visible:ring-2 focus-visible:ring-mauve rounded-xl p-1"
          >
            <div className="relative w-11 h-11 rounded-full overflow-hidden border border-border shadow-inner bg-blush flex-shrink-0">
              <Image
                src="/branding/logo.png"
                alt="Logo Alexa Beauty"
                fill
                sizes="44px"
                className="object-cover group-hover:scale-105 transition-transform duration-200"
                priority
              />
            </div>
            <div className="font-serif text-2xl tracking-tight text-wine font-bold leading-none">
              Alexa <span className="text-mauve font-normal">Beauty</span>
            </div>
          </Link>

          {/* Desktop Navigation */}
          <nav
            aria-label="Navegación principal de escritorio"
            className="hidden md:flex items-center gap-7"
          >
            {navLinks.map((link) => {
              const isActive =
                link.href === '/'
                  ? pathname === '/'
                  : pathname.startsWith(link.href);
              return (
                <Link
                  key={link.href}
                  href={link.href}
                  className={`text-sm font-semibold transition-colors py-1 relative ${
                    isActive
                      ? 'text-wine font-bold'
                      : 'text-ink/80 hover:text-wine'
                  }`}
                >
                  {link.label}
                  {isActive && (
                    <span className="absolute bottom-0 left-0 right-0 h-0.5 bg-wine rounded-full" />
                  )}
                </Link>
              );
            })}
          </nav>

          {/* Right Header Actions */}
          <div className="flex items-center gap-2.5">
            <Link
              href="/catalogo"
              aria-label="Buscar en el catálogo"
              className="w-10 h-10 rounded-full border border-border bg-white flex items-center justify-center text-ink hover:text-wine hover:border-mauve transition shadow-sm"
            >
              <Search className="w-4 h-4 stroke-[2.2]" />
            </Link>

            <Link
              href="/carrito"
              aria-label={`Ver carrito con ${totalCount} productos`}
              className="relative w-10 h-10 rounded-full border border-border bg-white flex items-center justify-center text-ink hover:text-wine hover:border-mauve transition shadow-sm"
            >
              <ShoppingBag className="w-4 h-4 stroke-[2.2]" />
              {totalCount > 0 && (
                <span className="absolute -top-1 -right-1.5 bg-wine text-white text-[10px] font-extrabold rounded-full min-w-5 h-5 flex items-center justify-center px-1 shadow-sm animate-in fade-in zoom-in duration-150">
                  {totalCount}
                </span>
              )}
            </Link>
          </div>
        </div>
      </header>

      {/* 3. Main Content Viewport */}
      <main className="flex-1 max-w-6xl w-full mx-auto px-4 sm:px-6 py-6 pb-24 md:pb-12">
        {children}
      </main>

      {/* 4. Desktop/Tablet Footer */}
      <footer className="border-t border-border bg-white/40 py-8 px-4 text-center text-xs text-muted">
        <div className="max-w-6xl mx-auto flex flex-col sm:flex-row items-center justify-between gap-4 text-center sm:text-left">
          <p>
            <span className="font-semibold text-wine">Alexa Beauty</span> · Tu
            belleza, tu esencia · 2026
          </p>
          <p className="text-[11px]">
            Coordinamos tu pedido por WhatsApp. No solicitamos pagos con tarjeta en esta tienda.
          </p>
        </div>
      </footer>

      {/* 5. Mobile Navigation Bar (360px - 430px friendly) */}
      <nav
        aria-label="Navegación móvil inferior"
        className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-md border-t border-border pb-safe shadow-[0_-4px_20px_rgba(76,17,38,0.06)]"
      >
        <div className="grid grid-cols-4 h-14">
          <Link
            href="/"
            className={`flex flex-col items-center justify-center gap-1 text-[11px] font-medium transition ${
              pathname === '/' ? 'text-wine font-bold' : 'text-muted hover:text-ink'
            }`}
          >
            <Home className="w-5 h-5" />
            <span>Inicio</span>
          </Link>

          <Link
            href="/catalogo"
            className={`flex flex-col items-center justify-center gap-1 text-[11px] font-medium transition ${
              pathname.startsWith('/catalogo')
                ? 'text-wine font-bold'
                : 'text-muted hover:text-ink'
            }`}
          >
            <Sparkles className="w-5 h-5" />
            <span>Catálogo</span>
          </Link>

          <Link
            href="/carrito"
            className={`relative flex flex-col items-center justify-center gap-1 text-[11px] font-medium transition ${
              pathname.startsWith('/carrito')
                ? 'text-wine font-bold'
                : 'text-muted hover:text-ink'
            }`}
          >
            <div className="relative">
              <ShoppingBag className="w-5 h-5" />
              {totalCount > 0 && (
                <span className="absolute -top-1.5 -right-2 bg-wine text-white text-[9px] font-extrabold rounded-full w-4 h-4 flex items-center justify-center">
                  {totalCount}
                </span>
              )}
            </div>
            <span>Carrito</span>
          </Link>

          <Link
            href="/admin"
            className={`flex flex-col items-center justify-center gap-1 text-[11px] font-medium transition ${
              pathname.startsWith('/admin')
                ? 'text-wine font-bold'
                : 'text-muted hover:text-ink'
            }`}
          >
            <ShieldCheck className="w-5 h-5" />
            <span>Admin</span>
          </Link>
        </div>
      </nav>
    </div>
  );
}
