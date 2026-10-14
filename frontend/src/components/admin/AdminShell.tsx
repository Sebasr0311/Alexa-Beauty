'use client';

import React, { useState, useEffect } from 'react';
import { api } from '@/lib/api';
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

// Module-level cache to eliminate flickering when switching between admin pages
let globalAdminAuth: boolean | null = null;

export function AdminShell({
  children,
  title,
  subtitle,
  badge,
  actions,
}: AdminShellProps) {
  const pathname = usePathname();
  const [authenticated, setAuthenticated] = useState<boolean | null>(() => {
    if (globalAdminAuth !== null) return globalAdminAuth;
    if (typeof window !== 'undefined') {
      try {
        if (sessionStorage.getItem('alexa_admin_auth') === 'true') {
          globalAdminAuth = true;
          return true;
        }
      } catch {
        // ignore
      }
    }
    return null;
  });
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loginError, setLoginError] = useState('');
  const [pending, setPending] = useState(false);
  const [showPassword, setShowPassword] = useState(false);

  useEffect(() => {
    let cancelled = false;
    api.getMe().then(user => {
      const isAuth = user.role === 'ADMIN' || user.role === 'OPERADOR';
      globalAdminAuth = isAuth;
      if (typeof window !== 'undefined') {
        try {
          if (isAuth) {
            sessionStorage.setItem('alexa_admin_auth', 'true');
          } else {
            sessionStorage.removeItem('alexa_admin_auth');
          }
        } catch {
          // ignore
        }
      }
      if (!cancelled) setAuthenticated(isAuth);
    }).catch(() => {
      globalAdminAuth = false;
      if (typeof window !== 'undefined') {
        try {
          sessionStorage.removeItem('alexa_admin_auth');
        } catch {
          // ignore
        }
      }
      if (!cancelled) setAuthenticated(false);
    });
    return () => { cancelled = true; };
  }, []);

  if (authenticated === null) {
    return (
      <main className="min-h-screen bg-gradient-to-br from-[#FFF5F7] via-[#FFF9F6] to-[#F7EFE8] flex items-center justify-center p-6">
        <div className="text-center space-y-4">
          <div className="w-12 h-12 border-4 border-wine/20 border-t-wine rounded-full animate-spin mx-auto" />
          <p className="text-muted text-sm font-medium">Verificando sesión segura...</p>
        </div>
      </main>
    );
  }

  if (!authenticated) {
    return (
      <main className="min-h-screen bg-gradient-to-br from-[#FFF5F7] via-[#FFF9F6] to-[#F7EFE8] flex items-center justify-center p-4 sm:p-6">
        <div className="max-w-md w-full">
          {/* Main Luxury Card */}
          <div className="bg-white/95 backdrop-blur-sm rounded-3xl border border-[#EAD5DC] shadow-[0_20px_50px_rgba(94,49,61,0.08)] p-8 sm:p-10 space-y-6 relative overflow-hidden">
            {/* Top decorative aura */}
            <div className="absolute -top-16 -right-16 w-40 h-40 bg-blush/60 rounded-full blur-2xl pointer-events-none" />

            {/* Header Brand */}
            <div className="text-center space-y-2 relative">
              <div className="w-16 h-16 rounded-2xl bg-gradient-to-br from-blush to-[#F6DFE7] p-2.5 mx-auto shadow-inner border border-[#E8D4DC] flex items-center justify-center">
                <Image
                  src="/branding/logo.png"
                  alt="Alexa Beauty"
                  width={44}
                  height={44}
                  className="object-contain"
                  priority
                />
              </div>
              <span className="inline-block bg-[#FFF5DD] border border-[#EFD7A2] text-[#865B0D] text-[10px] font-extrabold uppercase tracking-widest px-3 py-1 rounded-full">
                Acceso Exclusivo
              </span>
              <h1 className="font-serif text-2xl sm:text-3xl text-wine font-bold tracking-tight">
                Alexa Beauty
              </h1>
              <p className="text-muted text-xs sm:text-sm">
                Panel de control y gestión administrativa
              </p>
            </div>

            {/* Form */}
            <form
              className="space-y-4 pt-2"
              onSubmit={async (event) => {
                event.preventDefault();
                setPending(true);
                setLoginError('');
                try {
                  await api.login(email, password);
                  globalAdminAuth = true;
                  if (typeof window !== 'undefined') {
                    try { sessionStorage.setItem('alexa_admin_auth', 'true'); } catch {}
                  }
                  setAuthenticated(true);
                } catch {
                  setLoginError('Credenciales inválidas. Verifica tu correo y contraseña.');
                } finally {
                  setPending(false);
                }
              }}
            >
              {/* Email */}
              <div className="space-y-1.5">
                <label className="block text-xs font-bold uppercase tracking-wider text-muted">
                  Correo corporativo
                </label>
                <input
                  className="w-full bg-[#FCF9F7] border border-[#E0CBD1] rounded-xl px-4 py-3 text-sm text-ink outline-none focus:border-wine focus:ring-2 focus:ring-wine/10 transition"
                  type="email"
                  autoComplete="username"
                  required
                  placeholder="admin@alexabeauty.com"
                  value={email}
                  onChange={(event) => setEmail(event.target.value)}
                />
              </div>

              {/* Password */}
              <div className="space-y-1.5">
                <label className="block text-xs font-bold uppercase tracking-wider text-muted">
                  Contraseña
                </label>
                <div className="relative">
                  <input
                    className="w-full bg-[#FCF9F7] border border-[#E0CBD1] rounded-xl pl-4 pr-11 py-3 text-sm text-ink outline-none focus:border-wine focus:ring-2 focus:ring-wine/10 transition"
                    type={showPassword ? 'text' : 'password'}
                    autoComplete="current-password"
                    required
                    placeholder="••••••••••••••••"
                    value={password}
                    onChange={(event) => setPassword(event.target.value)}
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute right-3.5 top-1/2 -translate-y-1/2 text-muted hover:text-wine p-1 transition"
                    aria-label={showPassword ? 'Ocultar contraseña' : 'Ver contraseña'}
                  >
                    <span className="text-xs font-semibold select-none">
                      {showPassword ? 'Ocultar' : 'Ver'}
                    </span>
                  </button>
                </div>
              </div>

              {/* Error Alert */}
              {loginError && (
                <div
                  role="alert"
                  className="bg-red-50 border border-red-200 text-red-800 rounded-xl p-3 text-xs leading-relaxed"
                >
                  {loginError}
                </div>
              )}

              {/* Submit CTA */}
              <button
                type="submit"
                disabled={pending}
                className="w-full py-3.5 rounded-xl bg-gradient-to-r from-wine to-wineDark hover:from-wineDark hover:to-[#4A1624] text-white font-bold text-sm shadow-md hover:shadow-lg transition-all active:scale-98 disabled:opacity-60 disabled:cursor-not-allowed flex items-center justify-center gap-2"
              >
                {pending ? 'Ingresando al portal...' : 'Ingresar al panel'}
              </button>
            </form>

            {/* Back to store & security info */}
            <div className="border-t border-border/70 pt-4 text-center space-y-3">
              <Link
                href="/"
                className="inline-flex items-center gap-1.5 text-xs text-muted hover:text-wine font-semibold transition"
              >
                <ArrowLeft className="w-3.5 h-3.5" />
                <span>Volver a la tienda</span>
              </Link>
              <p className="text-[10px] text-muted/80 leading-normal">
                Conexión cifrada de extremo a extremo · Sesión protegida con cookie HttpOnly
              </p>
            </div>
          </div>
        </div>
      </main>
    );
  }

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
          PANEL ADMINISTRATIVO AUTORIZADO · ALEXA BEAUTY — Sesión verificada; las operaciones requieren confirmación del servidor.
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
            <button onClick={async () => { await api.logout(); window.location.reload(); }} className="text-xs text-wine">Cerrar sesión</button>
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
