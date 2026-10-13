'use client';

import React from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { usePathname } from 'next/navigation';
import {
  LayoutDashboard,
  Clock,
  TrendingUp,
  BarChart3,
  Upload,
  Package,
  Boxes,
  ArrowLeft,
  ShieldCheck,
} from 'lucide-react';

export interface AdminShellProps {
  children: React.ReactNode;
  title?: string;
  subtitle?: string;
  badge?: string;
  actions?: React.ReactNode;
}

export const ADMIN_NAV_ITEMS = [
  { label: 'Resumen', href: '/admin', icon: LayoutDashboard },
  { label: 'Pedidos', href: '/admin/pedidos', icon: Clock },
  { label: 'Ventas', href: '/admin/ventas', icon: TrendingUp },
  { label: 'Analítica', href: '/admin/analitica', icon: BarChart3 },
  { label: 'Importar Excel', href: '/admin/importar', icon: Upload },
  { label: 'Productos', href: '/admin/productos', icon: Package },
  { label: 'Inventario', href: '/admin/inventario', icon: Boxes },
];

export function AdminShell({
  children,
  title,
  subtitle,
  badge,
  actions,
}: AdminShellProps) {
  const pathname = usePathname();

  const isItemActive = (href: string) => {
    if (href === '/admin') {
      return pathname === '/admin';
    }
    return pathname.startsWith(href);
  };

  return (
    <div className="min-h-screen bg-cream text-ink">
      {/* Top Banner */}
      <aside
        role="alert"
        className="bg-[#FFF5DD] border-b border-[#EFD7A2] text-[#865B0D] text-xs font-semibold py-2 px-4 text-center flex items-center justify-center gap-2"
      >
        <ShieldCheck className="w-4 h-4 text-[#865B0D] flex-shrink-0" />
        <span>
          PANEL ADMINISTRATIVO AUTORIZADO · ALEXA BEAUTY — Conexión autoritativa Spring Boot &amp; PostgreSQL activa.
        </span>
      </aside>

      {/* Admin Sticky Header */}
      <header className="bg-white border-b border-border sticky top-0 z-20 shadow-xs">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-3 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="relative w-9 h-9 rounded-full overflow-hidden border border-border bg-blush shadow-xs">
              <Image
                src="/branding/logo.png"
                alt="Logo Alexa Beauty"
                fill
                sizes="36px"
                className="object-cover"
                priority
              />
            </div>
            <div>
              <span className="font-serif text-lg font-bold text-wine leading-none block">
                Alexa Beauty <span className="text-mauve font-normal text-xs uppercase tracking-wider ml-1">Admin</span>
              </span>
              <span className="text-[10px] text-muted">Panel de control y gestión comercial</span>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <Link
              href="/"
              className="inline-flex items-center gap-1.5 text-xs font-bold text-wine hover:text-wineDark bg-blush/60 hover:bg-blush px-3.5 py-2 rounded-full transition"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span>Volver a la tienda</span>
            </Link>
          </div>
        </div>
      </header>

      {/* Main Container with 2-Column Grid */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-[220px_1fr] gap-8 items-start">
          {/* Universal Persistent Sidebar Navigation */}
          <aside
            aria-label="Secciones del panel administrativo"
            className="bg-wineDark text-white rounded-3xl p-4 shadow-soft flex lg:flex-col overflow-x-auto lg:overflow-visible gap-1 no-scrollbar sticky lg:top-24 z-10"
          >
            <div className="hidden lg:block px-3 py-2 text-[10px] uppercase font-bold tracking-widest text-white/50">
              Navegación
            </div>

            {ADMIN_NAV_ITEMS.map((item) => {
              const Icon = item.icon;
              const active = isItemActive(item.href);

              return (
                <Link
                  key={item.href}
                  href={item.href}
                  className={`flex items-center gap-2.5 px-3.5 py-2.5 rounded-xl text-xs sm:text-sm font-semibold transition whitespace-nowrap w-full text-left ${
                    active
                      ? 'bg-white/20 text-white shadow-xs font-bold ring-1 ring-white/30'
                      : 'text-white/80 hover:bg-white/10 hover:text-white'
                  }`}
                >
                  <Icon className="w-4 h-4 flex-shrink-0" />
                  <span>{item.label}</span>
                </Link>
              );
            })}
          </aside>

          {/* Main Content Area */}
          <main className="space-y-6 min-w-0">
            {(title || actions) && (
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border/60 pb-5">
                <div>
                  {badge && (
                    <span className="text-[11px] font-extrabold uppercase tracking-[0.2em] text-wine block mb-1">
                      {badge}
                    </span>
                  )}
                  {title && (
                    <h1 className="font-serif text-3xl sm:text-4xl text-wineDark font-normal tracking-tight">
                      {title}
                    </h1>
                  )}
                  {subtitle && (
                    <p className="text-muted text-sm mt-1">{subtitle}</p>
                  )}
                </div>

                {actions && (
                  <div className="flex items-center gap-2.5 flex-wrap">
                    {actions}
                  </div>
                )}
              </div>
            )}

            {children}
          </main>
        </div>
      </div>
    </div>
  );
}

export default AdminShell;
