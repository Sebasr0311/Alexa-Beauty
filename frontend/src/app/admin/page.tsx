'use client';

import React, { useState, useEffect } from 'react';
import Link from 'next/link';
import {
  Clock,
  TrendingUp,
  AlertTriangle,
  CheckCircle2,
  BarChart3,
  Upload,
  FileSpreadsheet,
  Package,
  Boxes,
  DollarSign,
  Store,
  Sparkles,
  Receipt,
  ArrowRight,
} from 'lucide-react';
import AdminShell from '@/components/admin/AdminShell';
import { formatCOP } from '@/lib/utils';
import {
  api,
  AdminOrderSummaryDto,
  SalesSummaryDto,
} from '@/lib/api';

export default function AdminDashboardPage() {
  const [todaySalesSummary, setTodaySalesSummary] = useState<SalesSummaryDto | null>(null);
  const [salesSummary, setSalesSummary] = useState<SalesSummaryDto | null>(null);
  const [recentOrders, setRecentOrders] = useState<AdminOrderSummaryDto[]>([]);
  const [lowStockAlerts, setLowStockAlerts] = useState<number>(0);
  const [pendingOrdersCount, setPendingOrdersCount] = useState<number>(0);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState<boolean>(true);

  useEffect(() => {
    async function loadDashboardData() {
      setLoading(true);
      try {
        const [todaySalesRes, salesRes, ordersRes, pendingRes, invRes] = await Promise.allSettled([
          api.getAdminSalesToday({ size: 1 }),
          api.getAdminSales({ size: 5 }),
          api.getAdminOrders({ size: 5 }),
          api.getAdminOrders({ status: 'PENDIENTE_PAGO', size: 1 }),
          api.getInventorySummary({ lowStockOnly: true, size: 5 }),
        ]);

        if ([todaySalesRes, salesRes, ordersRes, pendingRes, invRes].some((r) => r.status === 'rejected')) {
          setError('No pudimos consultar todos los indicadores. Los datos faltantes no están disponibles.');
        }

        if (todaySalesRes.status === 'fulfilled' && todaySalesRes.value?.summary) {
          setTodaySalesSummary(todaySalesRes.value.summary);
        }
        if (salesRes.status === 'fulfilled' && salesRes.value?.summary) {
          setSalesSummary(salesRes.value.summary);
        }
        if (ordersRes.status === 'fulfilled' && ordersRes.value?.content) {
          setRecentOrders(ordersRes.value.content);
        }
        if (pendingRes.status === 'fulfilled' && pendingRes.value?.totalElements !== undefined) {
          setPendingOrdersCount(pendingRes.value.totalElements);
        }
        if (invRes.status === 'fulfilled' && invRes.value?.totalElements !== undefined) {
          setLowStockAlerts(invRes.value.totalElements);
        }
      } catch {
        // Fallback
      } finally {
        setLoading(false);
      }
    }

    loadDashboardData();
  }, []);

  return (
    <AdminShell
      badge="Panel de control"
      title="Hola, administrador ♡"
      subtitle="Resumen general de Alexa Beauty · Ventas en vivo, pedidos y alertas de inventario."
      actions={
        <div className="flex flex-wrap items-center gap-2.5">
          <Link
            href="/admin/ventas?pos=1"
            className="inline-flex items-center gap-2 rounded-full bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-xs sm:text-sm px-5 py-2.5 shadow-sm transition active:scale-95"
          >
            <Store className="w-4 h-4 stroke-[2.5]" />
            <span>Registrar Venta Física</span>
          </Link>
          <Link
            href="/admin/ventas"
            className="inline-flex items-center gap-2 rounded-full bg-wine hover:bg-wineDark text-white font-bold text-xs sm:text-sm px-5 py-2.5 shadow-glow transition active:scale-95"
          >
            <FileSpreadsheet className="w-4 h-4 stroke-[2.5]" />
            <span>Reporte de Ventas</span>
          </Link>
        </div>
      }
    >
      <div className="space-y-6">
        {error && <p role="alert" className="text-red-700 text-sm font-medium">{error}</p>}

        {/* Sección Rendimiento de Hoy (En Vivo) */}
        <section aria-label="Rendimiento del día" className="space-y-3">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
            <div className="flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
              <h2 className="font-serif text-lg sm:text-xl font-bold text-wine">
                Corte de Hoy (En Vivo)
              </h2>
              <span className="text-xs text-muted font-medium hidden sm:inline">
                · Ganancias y ventas acumuladas del día en curso
              </span>
            </div>
            <Link
              href="/admin/ventas"
              className="text-xs font-bold text-wine hover:underline inline-flex items-center gap-1 self-start sm:self-auto"
            >
              <span>Ver todas las ventas de hoy</span>
              <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
            {/* Ventas del Día */}
            <div className="bg-white border-2 border-wine/25 hover:border-wine rounded-2xl p-5 shadow-sm transition group">
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted font-semibold">Ventas del Día</span>
                <div className="w-8 h-8 rounded-xl bg-wine/10 text-wine flex items-center justify-center">
                  <DollarSign className="w-4 h-4" />
                </div>
              </div>
              <p className="font-serif text-2xl sm:text-3xl font-bold text-wine my-2 tracking-tight">
                {formatCOP(todaySalesSummary?.totalSales ?? 0)}
              </p>
              <span className="text-[11px] text-[#24734E] font-semibold flex items-center gap-1">
                <CheckCircle2 className="w-3.5 h-3.5" />
                <span>
                  {todaySalesSummary?.paidOrdersCount ?? 0}{' '}
                  {todaySalesSummary?.paidOrdersCount === 1 ? 'venta confirmada hoy' : 'ventas confirmadas hoy'}
                </span>
              </span>
            </div>

            {/* Ganancias de Hoy (Utilidad Bruta) */}
            <div className="bg-white border-2 border-emerald-600/30 hover:border-emerald-700 rounded-2xl p-5 shadow-sm transition group">
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted font-semibold">Ganancias de Hoy</span>
                <div className="w-8 h-8 rounded-xl bg-[#E2F5EA] text-[#24734E] flex items-center justify-center">
                  <TrendingUp className="w-4 h-4" />
                </div>
              </div>
              <p className="font-serif text-2xl sm:text-3xl font-bold text-[#196B3E] my-2 tracking-tight">
                {formatCOP(todaySalesSummary?.grossProfit ?? 0)}
              </p>
              <span className="text-[11px] text-[#24734E] font-semibold flex items-center gap-1">
                <Sparkles className="w-3 h-3 text-emerald-600" />
                <span>
                  {todaySalesSummary?.grossMarginPercentage != null
                    ? `Margen: ${todaySalesSummary.grossMarginPercentage}% (Ventas - Costos)`
                    : 'Utilidad neta estimada'}
                </span>
              </span>
            </div>

            {/* Unidades Vendidas Hoy */}
            <div className="bg-white border border-border hover:border-wine/40 rounded-2xl p-5 shadow-sm transition">
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted font-semibold">Unidades Vendidas Hoy</span>
                <div className="w-8 h-8 rounded-xl bg-mauve/15 text-mauve flex items-center justify-center">
                  <Package className="w-4 h-4" />
                </div>
              </div>
              <p className="font-serif text-2xl sm:text-3xl font-bold text-ink my-2 tracking-tight">
                {todaySalesSummary?.totalUnitsSold ?? 0}
              </p>
              <span className="text-[11px] text-muted flex items-center gap-1">
                Artículos entregados o por despachar
              </span>
            </div>

            {/* Ticket Promedio Hoy */}
            <div className="bg-white border border-border hover:border-wine/40 rounded-2xl p-5 shadow-sm transition">
              <div className="flex items-center justify-between">
                <span className="text-xs text-muted font-semibold">Ticket Promedio Hoy</span>
                <div className="w-8 h-8 rounded-xl bg-blush text-wine flex items-center justify-center">
                  <Receipt className="w-4 h-4" />
                </div>
              </div>
              <p className="font-serif text-2xl sm:text-3xl font-bold text-ink my-2 tracking-tight">
                {formatCOP(todaySalesSummary?.averageTicket ?? 0)}
              </p>
              <span className="text-[11px] text-muted flex items-center gap-1">
                Promedio por transacción de hoy
              </span>
            </div>
          </div>
        </section>

        {/* 3 Tarjetas Secundarias: Operación y Acumulado */}
        <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
          <div className="bg-white border border-border rounded-2xl p-5 shadow-sm">
            <span className="text-xs text-muted font-medium block">
              Ventas pagadas · acumuladas (30 días)
            </span>
            <p className="font-serif text-3xl font-bold text-wine my-2 tracking-tight">
              {formatCOP(salesSummary?.totalSales ?? 0)}
            </p>
            <span className="text-xs text-success font-semibold flex items-center gap-1">
              <CheckCircle2 className="w-3.5 h-3.5" />
              <span>{salesSummary?.paidOrdersCount ?? 0} pedidos verificados</span>
            </span>
          </div>

          <div className="bg-white border border-border rounded-2xl p-5 shadow-sm">
            <span className="text-xs text-muted font-medium block">
              Pedidos por confirmar
            </span>
            <p className="font-serif text-3xl font-bold text-amber-600 my-2 tracking-tight">
              {pendingOrdersCount.toString().padStart(2, '0')}
            </p>
            <span className="text-xs text-amber-700 font-semibold flex items-center gap-1">
              <Clock className="w-3.5 h-3.5" />
              <span>Requieren verificación manual</span>
            </span>
          </div>

          <div className="bg-white border border-border rounded-2xl p-5 shadow-sm">
            <span className="text-xs text-muted font-medium block">
              Productos con stock bajo
            </span>
            <p className="font-serif text-3xl font-bold text-rose-600 my-2 tracking-tight">
              {lowStockAlerts.toString().padStart(2, '0')}
            </p>
            <span className="text-xs text-rose-600 font-semibold flex items-center gap-1">
              <AlertTriangle className="w-3.5 h-3.5" />
              <span>Revisar existencias mínimas</span>
            </span>
          </div>
        </div>

        {/* Tabla de Últimos Pedidos */}
        <div className="bg-white border border-border rounded-3xl p-6 sm:p-8 shadow-soft">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 border-b border-border pb-4 mb-5">
            <div>
              <h2 className="font-serif text-xl sm:text-2xl text-wine font-normal">
                Últimos pedidos registrados
              </h2>
              <p className="text-muted text-xs sm:text-sm mt-0.5">
                Seguimiento de pedidos generados desde la tienda, punto físico y WhatsApp
              </p>
            </div>

            <div className="flex items-center gap-4 self-start sm:self-auto">
              <Link
                href="/admin/pedidos"
                className="inline-flex items-center gap-1.5 text-xs font-bold text-wine hover:underline"
              >
                Ver todos los pedidos →
              </Link>

              <Link
                href="/admin/ventas"
                className="inline-flex items-center gap-1.5 text-xs font-bold text-muted hover:text-wine hover:underline"
              >
                <FileSpreadsheet className="w-3.5 h-3.5" />
                Reporte de Ventas
              </Link>
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
                {loading ? (
                  <tr>
                    <td colSpan={5} className="py-12 text-center text-muted">
                      <div className="inline-block animate-spin rounded-full h-6 w-6 border-b-2 border-wine mb-2" />
                      <p className="text-sm text-muted">Cargando pedidos recientes...</p>
                    </td>
                  </tr>
                ) : recentOrders.length === 0 ? (
                  <tr>
                    <td colSpan={5} className="py-12 text-center text-muted">
                      <Package className="w-8 h-8 mx-auto mb-2 opacity-40 text-wine" />
                      <p className="font-medium text-ink">No hay pedidos registrados aún</p>
                      <p className="text-xs text-muted mt-1">Los pedidos generados desde la tienda, punto físico o WhatsApp aparecerán aquí.</p>
                    </td>
                  </tr>
                ) : (
                  recentOrders.map((o) => (
                    <tr key={o.id} className="hover:bg-cream/50 transition">
                      <td className="py-3.5 px-3.5 font-mono font-semibold text-wine">
                        {o.reference}
                      </td>
                      <td className="py-3.5 px-3.5 font-medium text-ink">
                        {o.customerName}
                      </td>
                      <td className="py-3.5 px-3.5 text-muted hidden sm:table-cell">
                        {new Date(o.createdAt).toLocaleDateString('es-CO')}
                      </td>
                      <td className="py-3.5 px-3.5 font-bold text-ink">
                        {formatCOP(o.total)}
                      </td>
                      <td className="py-3.5 px-3.5">
                        <span
                          className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                            o.status === 'PAGADO' || o.status === 'ENTREGADO'
                              ? 'bg-[#E2F5EA] text-[#24734E]'
                              : o.status === 'CANCELADO' || o.status === 'EXPIRADO'
                              ? 'bg-rose-100 text-rose-800'
                              : 'bg-[#FFF3D8] text-[#8A5C11]'
                          }`}
                        >
                          {o.status}
                        </span>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>

        {/* Botones de Acceso Rápido */}
        <div className="flex flex-wrap items-center gap-3 pt-2">
          <Link
            href="/admin/ventas?pos=1"
            className="inline-flex items-center gap-2 rounded-full bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-xs sm:text-sm px-6 py-3 transition shadow-sm active:scale-95"
          >
            <Store className="w-4 h-4 stroke-[2.5]" />
            <span>Registrar venta física</span>
          </Link>

          <Link
            href="/admin/pedidos"
            className="inline-flex items-center gap-2 rounded-full bg-wine hover:bg-wineDark text-white font-bold text-xs sm:text-sm px-6 py-3 shadow-glow transition active:scale-95"
          >
            <Clock className="w-4 h-4 stroke-[2.5]" />
            <span>Gestionar pedidos</span>
          </Link>

          <Link
            href="/admin/ventas"
            className="inline-flex items-center gap-2 rounded-full bg-white border border-wine hover:bg-cream text-wine font-bold text-xs sm:text-sm px-6 py-3 transition shadow-sm active:scale-95"
          >
            <TrendingUp className="w-4 h-4 stroke-[2.5]" />
            <span>Ventas confirmadas</span>
          </Link>

          <Link
            href="/admin/analitica"
            className="inline-flex items-center gap-2 rounded-full bg-white border border-wine hover:bg-cream text-wine font-bold text-xs sm:text-sm px-6 py-3 transition shadow-sm active:scale-95"
          >
            <BarChart3 className="w-4 h-4 stroke-[2.5]" />
            <span>Analítica y métricas</span>
          </Link>

          <Link
            href="/admin/productos"
            className="inline-flex items-center gap-2 rounded-full bg-white border border-[#b9899c] hover:bg-cream text-wine font-bold text-xs sm:text-sm px-6 py-3 transition shadow-sm active:scale-95"
          >
            <Package className="w-4 h-4" />
            <span>Catálogo de productos</span>
          </Link>

          <Link
            href="/admin/inventario"
            className="inline-flex items-center gap-2 rounded-full bg-white border border-[#b9899c] hover:bg-cream text-wine font-bold text-xs sm:text-sm px-6 py-3 transition shadow-sm active:scale-95"
          >
            <Boxes className="w-4 h-4" />
            <span>Kardex e inventario</span>
          </Link>

          <Link
            href="/admin/importar"
            className="inline-flex items-center gap-2 rounded-full bg-white border border-[#b9899c] hover:bg-cream text-wine font-bold text-xs sm:text-sm px-6 py-3 transition shadow-sm active:scale-95"
          >
            <Upload className="w-4 h-4" />
            <span>Importar por Excel</span>
          </Link>
        </div>
      </div>
    </AdminShell>
  );
}
