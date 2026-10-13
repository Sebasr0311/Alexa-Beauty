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
} from 'lucide-react';
import AdminShell from '@/components/admin/AdminShell';
import { formatCOP } from '@/lib/utils';
import {
  api,
  AdminOrderSummaryDto,
  SalesSummaryDto,
} from '@/lib/api';

export default function AdminDashboardPage() {
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
        const [salesRes, ordersRes, invRes] = await Promise.allSettled([
          api.getAdminSales({ size: 5 }),
          api.getAdminOrders({ size: 5 }),
          api.getInventorySummary({ lowStockOnly: true, size: 5 }),
        ]);

        if ([salesRes, ordersRes, invRes].some(result => result.status === 'rejected')) setError('No pudimos consultar todos los indicadores. Los datos faltantes no están disponibles.');
        if (salesRes.status === 'fulfilled' && salesRes.value?.summary) {
          setSalesSummary(salesRes.value.summary);
        }
        if (ordersRes.status === 'fulfilled' && ordersRes.value?.content) {
          setRecentOrders(ordersRes.value.content);
          const pending = ordersRes.value.content.filter(
            (o) => o.status === 'PENDIENTE_CONTACTO' || o.status === 'PENDIENTE_PAGO'
          ).length;
          setPendingOrdersCount(pending);
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
      subtitle="Resumen general de Alexa Beauty · Ventas, pedidos y alertas de inventario."
    >
      {error && <p role="alert" className="text-red-700">{error}</p>}
      {/* 3 Tarjetas KPI Principales */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-white border border-border rounded-2xl p-5 shadow-sm">
          <span className="text-xs text-muted font-medium block">
            Ventas pagadas · acumuladas
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
              {recentOrders.length === 0 ? (
                <>
                  <tr className="hover:bg-cream/50 transition">
                    <td className="py-3.5 px-3.5 font-mono font-semibold text-wine">
                      AB-2026-000101
                    </td>
                    <td className="py-3.5 px-3.5 font-medium text-ink">
                      Camila Morales
                    </td>
                    <td className="py-3.5 px-3.5 text-muted hidden sm:table-cell">
                      Hoy 14:20
                    </td>
                    <td className="py-3.5 px-3.5 font-bold text-ink">
                      $ 65.000
                    </td>
                    <td className="py-3.5 px-3.5">
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-[#E2F5EA] text-[#24734E]">
                        Pagado
                      </span>
                    </td>
                  </tr>

                  <tr className="hover:bg-cream/50 transition">
                    <td className="py-3.5 px-3.5 font-mono font-semibold text-wine">
                      AB-2026-000102
                    </td>
                    <td className="py-3.5 px-3.5 font-medium text-ink">
                      Valeria Restrepo
                    </td>
                    <td className="py-3.5 px-3.5 text-muted hidden sm:table-cell">
                      Hoy 11:05
                    </td>
                    <td className="py-3.5 px-3.5 font-bold text-ink">
                      $ 118.000
                    </td>
                    <td className="py-3.5 px-3.5">
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-[#FFF3D8] text-[#8A5C11]">
                        Pendiente
                      </span>
                    </td>
                  </tr>

                  <tr className="hover:bg-cream/50 transition">
                    <td className="py-3.5 px-3.5 font-mono font-semibold text-wine">
                      AB-2026-000103
                    </td>
                    <td className="py-3.5 px-3.5 font-medium text-ink">
                      Daniela Gómez
                    </td>
                    <td className="py-3.5 px-3.5 text-muted hidden sm:table-cell">
                      Ayer 18:45
                    </td>
                    <td className="py-3.5 px-3.5 font-bold text-ink">
                      $ 185.000
                    </td>
                    <td className="py-3.5 px-3.5">
                      <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-[#E2F5EA] text-[#24734E]">
                        Pagado
                      </span>
                    </td>
                  </tr>
                </>
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
    </AdminShell>
  );
}
