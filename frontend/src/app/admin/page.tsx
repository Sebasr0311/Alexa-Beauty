'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import {
  LayoutDashboard,
  Package,
  Boxes,
  FileSpreadsheet,
  Clock,
  TrendingUp,
  FileText,
  Plus,
  Download,
  Upload,
  ArrowLeft,
  AlertTriangle,
  CheckCircle2,
  BarChart3,
} from 'lucide-react';

export default function AdminDashboardPage() {
  const [activeTab, setActiveTab] = useState('Resumen');

  const navItems = [
    { label: 'Resumen', icon: LayoutDashboard, href: '/admin' },
    { label: 'Pedidos', icon: Clock, href: '/admin/pedidos' },
    { label: 'Ventas', icon: TrendingUp, href: '/admin/ventas' },
    { label: 'Analítica', icon: BarChart3, href: '/admin/analitica' },
    { label: 'Importar Excel', icon: Upload, href: '/admin/importar' },
    { label: 'Productos', icon: Package },
    { label: 'Inventario', icon: Boxes },
  ];

  const handleActionNotice = (actionName: string) => {
    alert(
      `Acción: "${actionName}"\n\nEn Sprint 2 (backend y panel completo), esta acción conectará con la API REST de Spring Boot y la base de datos PostgreSQL de Supabase.`
    );
  };

  return (
    <div className="min-h-screen bg-cream text-ink">
      {/* Admin Demo Disclaimer */}
      <aside
        role="alert"
        className="bg-[#FFF5DD] border-b border-[#EFD7A2] text-[#865B0D] text-xs font-semibold py-2.5 px-4 text-center flex items-center justify-center gap-2"
      >
        <AlertTriangle className="w-4 h-4 flex-shrink-0" />
        <span>
          PANEL ADMINISTRATIVO — VISTA DE DISEÑO. Los valores y registros son ficticios para validación de experiencia; no hay autenticación ni base de datos conectada en esta fase.
        </span>
      </aside>

      {/* Admin Header Bar */}
      <header className="bg-white border-b border-border sticky top-0 z-20">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 py-3 flex items-center justify-between gap-4">
          <div className="flex items-center gap-3">
            <div className="relative w-9 h-9 rounded-full overflow-hidden border border-border bg-blush">
              <Image
                src="/branding/logo.png"
                alt="Logo Alexa Beauty"
                fill
                sizes="36px"
                className="object-cover"
              />
            </div>
            <div>
              <span className="font-serif text-lg font-bold text-wine leading-none block">
                Alexa Beauty <span className="text-mauve font-normal text-xs uppercase tracking-wider ml-1">Admin</span>
              </span>
              <span className="text-[10px] text-muted">Panel de control y gestión comercial</span>
            </div>
          </div>

          <Link
            href="/"
            className="inline-flex items-center gap-1.5 text-xs font-bold text-wine hover:text-wineDark bg-blush/60 hover:bg-blush px-3.5 py-2 rounded-full transition"
          >
            <ArrowLeft className="w-3.5 h-3.5" />
            Volver a la tienda
          </Link>
        </div>
      </header>

      {/* Admin Body Container */}
      <div className="max-w-7xl mx-auto px-4 sm:px-6 py-8">
        <div className="grid grid-cols-1 lg:grid-cols-[220px_1fr] gap-8 items-start">
          {/* Sidebar Navigation */}
          <aside
            aria-label="Secciones del panel administrativo"
            className="bg-wineDark text-white rounded-3xl p-4 shadow-soft flex lg:flex-col overflow-x-auto lg:overflow-visible gap-1 no-scrollbar"
          >
            <div className="hidden lg:block px-3 py-2 text-[10px] uppercase font-bold tracking-widest text-white/50">
              Navegación
            </div>
            {navItems.map((item) => {
              const Icon = item.icon;
              const isActive = activeTab === item.label;
              const buttonClasses = `flex items-center gap-2.5 px-3.5 py-2.5 rounded-xl text-xs sm:text-sm font-semibold transition whitespace-nowrap w-full text-left ${
                isActive
                  ? 'bg-white/20 text-white shadow-xs'
                  : 'text-white/80 hover:bg-white/10 hover:text-white'
              }`;

              if (item.href && item.href !== '/admin') {
                return (
                  <Link
                    key={item.label}
                    href={item.href}
                    className={buttonClasses}
                  >
                    <Icon className="w-4 h-4 flex-shrink-0" />
                    <span>{item.label}</span>
                  </Link>
                );
              }

              return (
                <button
                  key={item.label}
                  type="button"
                  onClick={() => {
                    setActiveTab(item.label);
                    if (item.label !== 'Resumen') {
                      handleActionNotice(`Módulo ${item.label}`);
                    }
                  }}
                  className={buttonClasses}
                >
                  <Icon className="w-4 h-4 flex-shrink-0" />
                  <span>{item.label}</span>
                </button>
              );
            })}
          </aside>

          {/* Main Dashboard Content */}
          <main className="space-y-8 min-w-0">
            {/* Header Title */}
            <div>
              <span className="text-[11px] font-extrabold uppercase tracking-[0.2em] text-wine">
                Panel de control
              </span>
              <h1 className="font-serif text-3xl sm:text-4xl text-wineDark font-normal tracking-tight mt-1">
                Hola, administrador ♡
              </h1>
              <p className="text-muted text-sm mt-1">
                Resumen general de Alexa Beauty · Ventas, pedidos y alertas de inventario.
              </p>
            </div>

            {/* KPI Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
              <div className="bg-white border border-border rounded-2xl p-5 shadow-sm">
                <span className="text-xs text-muted font-medium block">
                  Ventas pagadas · hoy
                </span>
                <p className="font-serif text-3xl font-bold text-wine my-2 tracking-tight">
                  $ 189.000
                </p>
                <span className="text-xs text-success font-semibold flex items-center gap-1">
                  <CheckCircle2 className="w-3.5 h-3.5" />
                  ↑ +12% vs. promedio
                </span>
              </div>

              <div className="bg-white border border-border rounded-2xl p-5 shadow-sm">
                <span className="text-xs text-muted font-medium block">
                  Pedidos por confirmar
                </span>
                <p className="font-serif text-3xl font-bold text-wine my-2 tracking-tight">
                  04
                </p>
                <span className="text-xs text-amber-700 font-semibold flex items-center gap-1">
                  <Clock className="w-3.5 h-3.5" />
                  Requieren verificación manual
                </span>
              </div>

              <div className="bg-white border border-border rounded-2xl p-5 shadow-sm">
                <span className="text-xs text-muted font-medium block">
                  Productos con stock bajo
                </span>
                <p className="font-serif text-3xl font-bold text-wine my-2 tracking-tight">
                  03
                </p>
                <span className="text-xs text-wine font-semibold flex items-center gap-1">
                  <AlertTriangle className="w-3.5 h-3.5" />
                  Revisar existencias mínimas
                </span>
              </div>
            </div>

            {/* Recent Orders Table Panel */}
            <div className="bg-white border border-border rounded-3xl p-6 sm:p-8 shadow-soft">
              <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-4 mb-5">
                <div>
                  <h2 className="font-serif text-xl sm:text-2xl text-wine font-normal">
                    Últimos pedidos registrados
                  </h2>
                  <p className="text-muted text-xs sm:text-sm mt-0.5">
                    Seguimiento de pedidos generados desde la tienda y WhatsApp
                  </p>
                </div>

                <div className="flex items-center gap-4 self-start sm:self-auto">
                  <Link
                    href="/admin/pedidos"
                    className="inline-flex items-center gap-1.5 text-xs font-bold text-wine hover:underline"
                  >
                    Ver todos los pedidos →
                  </Link>

                  <button
                    type="button"
                    onClick={() => handleActionNotice('Exportar pedidos a Excel')}
                    className="inline-flex items-center gap-1.5 text-xs font-bold text-muted hover:text-wine hover:underline"
                  >
                    <Download className="w-3.5 h-3.5" />
                    Exportar a Excel
                  </button>
                </div>
              </div>

              <div className="overflow-x-auto">
                <table className="w-full text-left text-xs sm:text-sm border-collapse">
                  <thead>
                    <tr className="bg-blush text-wine font-bold border-b border-border">
                      <th className="py-3 px-3.5 rounded-l-xl">Referencia</th>
                      <th className="py-3 px-3.5">Cliente</th>
                      <th className="py-3 px-3.5 hidden sm:table-cell">Fecha</th>
                      <th className="py-3 px-3.5">Total</th>
                      <th className="py-3 px-3.5 rounded-r-xl">Estado</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/60">
                    <tr className="hover:bg-cream/50 transition">
                      <td className="py-3.5 px-3.5 font-mono font-semibold text-wine">
                        AB-000123
                      </td>
                      <td className="py-3.5 px-3.5 font-medium text-ink">
                        Camila Morales
                      </td>
                      <td className="py-3.5 px-3.5 text-muted hidden sm:table-cell">
                        Hoy 14:20
                      </td>
                      <td className="py-3.5 px-3.5 font-bold text-ink">
                        $ 60.000
                      </td>
                      <td className="py-3.5 px-3.5">
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-[#E2F5EA] text-[#24734E]">
                          Pagado
                        </span>
                      </td>
                    </tr>

                    <tr className="hover:bg-cream/50 transition">
                      <td className="py-3.5 px-3.5 font-mono font-semibold text-wine">
                        AB-000124
                      </td>
                      <td className="py-3.5 px-3.5 font-medium text-ink">
                        Valeria Restrepo
                      </td>
                      <td className="py-3.5 px-3.5 text-muted hidden sm:table-cell">
                        Hoy 11:05
                      </td>
                      <td className="py-3.5 px-3.5 font-bold text-ink">
                        $ 35.000
                      </td>
                      <td className="py-3.5 px-3.5">
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-[#FFF3D8] text-[#8A5C11]">
                          Pendiente
                        </span>
                      </td>
                    </tr>

                    <tr className="hover:bg-cream/50 transition">
                      <td className="py-3.5 px-3.5 font-mono font-semibold text-wine">
                        AB-000125
                      </td>
                      <td className="py-3.5 px-3.5 font-medium text-ink">
                        Daniela Gómez
                      </td>
                      <td className="py-3.5 px-3.5 text-muted hidden sm:table-cell">
                        Ayer 18:45
                      </td>
                      <td className="py-3.5 px-3.5 font-bold text-ink">
                        $ 94.000
                      </td>
                      <td className="py-3.5 px-3.5">
                        <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-[#E2F5EA] text-[#24734E]">
                          Pagado
                        </span>
                      </td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>

            {/* Quick Action Buttons */}
            <div className="flex flex-wrap items-center gap-3 pt-2">
              <Link
                href="/admin/pedidos"
                className="inline-flex items-center gap-2 rounded-full bg-wine hover:bg-wineDark text-white font-bold text-xs sm:text-sm px-6 py-3 shadow-glow transition active:scale-95"
              >
                <Clock className="w-4 h-4 stroke-[2.5]" />
                Gestionar pedidos
              </Link>

              <Link
                href="/admin/ventas"
                className="inline-flex items-center gap-2 rounded-full bg-white border border-wine hover:bg-cream text-wine font-bold text-xs sm:text-sm px-6 py-3 transition shadow-sm active:scale-95"
              >
                <TrendingUp className="w-4 h-4 stroke-[2.5]" />
                Ventas confirmadas
              </Link>

              <Link
                href="/admin/analitica"
                className="inline-flex items-center gap-2 rounded-full bg-white border border-wine hover:bg-cream text-wine font-bold text-xs sm:text-sm px-6 py-3 transition shadow-sm active:scale-95"
              >
                <BarChart3 className="w-4 h-4 stroke-[2.5]" />
                Analítica y métricas
              </Link>

              <Link
                href="/admin/importar"
                className="inline-flex items-center gap-2 rounded-full bg-white border border-[#b9899c] hover:bg-cream text-wine font-bold text-xs sm:text-sm px-6 py-3 transition shadow-sm active:scale-95"
              >
                <Upload className="w-4 h-4" />
                Importar productos
              </Link>

              <button
                type="button"
                onClick={() => handleActionNotice('Descargar plantilla Excel oficial')}
                className="inline-flex items-center gap-2 rounded-full bg-white border border-[#b9899c] hover:bg-cream text-wine font-bold text-xs sm:text-sm px-6 py-3 transition shadow-sm active:scale-95"
              >
                <FileSpreadsheet className="w-4 h-4" />
                Descargar plantilla Excel
              </button>
            </div>

          </main>
        </div>
      </div>
    </div>
  );
}
