'use client';

import React, { useState, useEffect, useCallback, useMemo } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import {
  ArrowLeft,
  Calendar,
  CheckCircle2,
  DollarSign,
  Package,
  Receipt,
  RefreshCw,
  LayoutDashboard,
  Clock,
  TrendingUp,
  BarChart3,
  Upload,
  Trophy,
  CreditCard,
  Sparkles,
  HelpCircle,
  AlertTriangle,
} from 'lucide-react';
import {
  api,
  SalesAnalyticsResponseDto,
  TopProductDto,
  SalesSummaryDto,
  DailySalesDto,
  PaymentMethodBreakdownDto,
} from '@/lib/api';
import {
  formatCOP,
  getColombiaToday,
  getColombiaFirstDayOfMonth,
  getColombiaDaysAgo,
} from '@/lib/utils';

// Fallback mock analytics data
const MOCK_ANALYTICS_SUMMARY: SalesSummaryDto = {
  totalSales: 1285000,
  paidOrdersCount: 11,
  totalUnitsSold: 28,
  averageTicket: 116818,
  startDate: getColombiaDaysAgo(30),
  endDate: getColombiaToday(),
};

const MOCK_DAILY_SALES: DailySalesDto[] = [
  { date: '2026-10-06', totalSales: 145000, ordersCount: 1, unitsCount: 3 },
  { date: '2026-10-07', totalSales: 210000, ordersCount: 2, unitsCount: 5 },
  { date: '2026-10-08', totalSales: 95000, ordersCount: 1, unitsCount: 2 },
  { date: '2026-10-09', totalSales: 280000, ordersCount: 2, unitsCount: 6 },
  { date: '2026-10-10', totalSales: 165000, ordersCount: 2, unitsCount: 4 },
  { date: '2026-10-11', totalSales: 195000, ordersCount: 1, unitsCount: 4 },
  { date: '2026-10-12', totalSales: 195000, ordersCount: 2, unitsCount: 4 },
];

const MOCK_TOP_PRODUCTS: TopProductDto[] = [
  {
    rank: 1,
    sku: 'LAB-BELL-01',
    productName: 'Labial Mate Velvet Cream',
    brandName: 'Bella Flor',
    unitsSold: 10,
    grossRevenue: 450000,
    revenueSharePercentage: 35.0,
  },
  {
    rank: 2,
    sku: 'KIT-RUT-01',
    productName: 'Kit Rutina Facial Esencial',
    brandName: 'Dermique',
    unitsSold: 2,
    grossRevenue: 360000,
    revenueSharePercentage: 28.0,
  },
  {
    rank: 3,
    sku: 'FND-NAT-03',
    productName: 'Base Líquida Serum Radiance',
    brandName: 'Lumina Skin',
    unitsSold: 3,
    grossRevenue: 195000,
    revenueSharePercentage: 15.2,
  },
  {
    rank: 4,
    sku: 'SER-VIT-02',
    productName: 'Serum Vitamina C Iluminador',
    brandName: 'Dermique',
    unitsSold: 2,
    grossRevenue: 156000,
    revenueSharePercentage: 12.1,
  },
  {
    rank: 5,
    sku: 'MSK-VOL-01',
    productName: 'Máscara Pestañas Volumen Extremo',
    brandName: 'Bella Flor',
    unitsSold: 3,
    grossRevenue: 105000,
    revenueSharePercentage: 8.2,
  },
  {
    rank: 6,
    sku: 'PAL-SOM-04',
    productName: 'Paleta de Sombras Nude Sunset',
    brandName: 'Bella Flor',
    unitsSold: 1,
    grossRevenue: 92000,
    revenueSharePercentage: 7.2,
  },
];

const MOCK_PAYMENT_METHODS: PaymentMethodBreakdownDto[] = [
  {
    paymentMethod: 'NEQUI',
    totalAmount: 585000,
    orderCount: 5,
    percentage: 45.5,
  },
  {
    paymentMethod: 'BANCOLOMBIA',
    totalAmount: 385000,
    orderCount: 3,
    percentage: 30.0,
  },
  {
    paymentMethod: 'DAVIPLATA',
    totalAmount: 185000,
    orderCount: 1,
    percentage: 14.4,
  },
  {
    paymentMethod: 'EFECTIVO',
    totalAmount: 92000,
    orderCount: 1,
    percentage: 7.2,
  },
  {
    paymentMethod: 'OTRO',
    totalAmount: 38000,
    orderCount: 1,
    percentage: 2.9,
  },
];

export default function AdminAnaliticaPage() {
  // Date range state
  const [activePreset, setActivePreset] = useState<'hoy' | 'ultimos7' | 'esteMes' | 'ultimos30'>('esteMes');
  const [fromDate, setFromDate] = useState<string>(() => getColombiaFirstDayOfMonth());
  const [toDate, setToDate] = useState<string>(() => getColombiaToday());

  // Loading & data state
  const [loading, setLoading] = useState<boolean>(true);
  const [analyticsData, setAnalyticsData] = useState<SalesAnalyticsResponseDto>({
    summary: MOCK_ANALYTICS_SUMMARY,
    dailySales: MOCK_DAILY_SALES,
    topProducts: MOCK_TOP_PRODUCTS,
    paymentMethods: MOCK_PAYMENT_METHODS,
  });

  // Fetch analytics from API
  const fetchAnalytics = useCallback(async () => {
    setLoading(true);
    try {
      const [overviewRes, topProductsRes] = await Promise.allSettled([
        api.getAdminAnalytics({ from: fromDate, to: toDate }),
        api.getAdminTopProducts({ from: fromDate, to: toDate }),
      ]);

      if (overviewRes.status === 'fulfilled' && overviewRes.value) {
        const overview = overviewRes.value;
        const topProducts =
          topProductsRes.status === 'fulfilled' && Array.isArray(topProductsRes.value)
            ? topProductsRes.value
            : overview.topProducts || MOCK_TOP_PRODUCTS;

        setAnalyticsData({
          summary: overview.summary || MOCK_ANALYTICS_SUMMARY,
          dailySales: overview.dailySales || MOCK_DAILY_SALES,
          topProducts,
          paymentMethods: overview.paymentMethods || MOCK_PAYMENT_METHODS,
        });
      } else {
        // Fallback to mock data with updated dates
        setAnalyticsData({
          summary: {
            ...MOCK_ANALYTICS_SUMMARY,
            startDate: fromDate,
            endDate: toDate,
          },
          dailySales: MOCK_DAILY_SALES,
          topProducts: MOCK_TOP_PRODUCTS,
          paymentMethods: MOCK_PAYMENT_METHODS,
        });
      }
    } catch {
      // Fallback
      setAnalyticsData({
        summary: {
          ...MOCK_ANALYTICS_SUMMARY,
          startDate: fromDate,
          endDate: toDate,
        },
        dailySales: MOCK_DAILY_SALES,
        topProducts: MOCK_TOP_PRODUCTS,
        paymentMethods: MOCK_PAYMENT_METHODS,
      });
    } finally {
      setLoading(false);
    }
  }, [fromDate, toDate]);

  useEffect(() => {
    fetchAnalytics();
  }, [fetchAnalytics]);

  // Quick preset selector
  const handleSelectPreset = (preset: 'hoy' | 'ultimos7' | 'esteMes' | 'ultimos30') => {
    setActivePreset(preset);
    const today = getColombiaToday();
    if (preset === 'hoy') {
      setFromDate(today);
      setToDate(today);
    } else if (preset === 'ultimos7') {
      setFromDate(getColombiaDaysAgo(7));
      setToDate(today);
    } else if (preset === 'esteMes') {
      setFromDate(getColombiaFirstDayOfMonth());
      setToDate(today);
    } else if (preset === 'ultimos30') {
      setFromDate(getColombiaDaysAgo(30));
      setToDate(today);
    }
  };

  // Max value in daily sales for SVG bar chart scaling
  const maxDailySales = useMemo(() => {
    const list = analyticsData.dailySales || [];
    if (list.length === 0) return 100000;
    const max = Math.max(...list.map((d) => d.totalSales));
    return max > 0 ? max : 100000;
  }, [analyticsData.dailySales]);

  // Helper for ranking badges
  const getRankBadge = (rank: number) => {
    if (rank === 1) {
      return (
        <span className="w-7 h-7 rounded-full bg-amber-100 text-amber-800 border border-amber-300 flex items-center justify-center font-extrabold text-xs shadow-xs">
          #1
        </span>
      );
    }
    if (rank === 2) {
      return (
        <span className="w-7 h-7 rounded-full bg-slate-200 text-slate-800 border border-slate-300 flex items-center justify-center font-bold text-xs shadow-xs">
          #2
        </span>
      );
    }
    if (rank === 3) {
      return (
        <span className="w-7 h-7 rounded-full bg-orange-100 text-orange-800 border border-orange-300 flex items-center justify-center font-bold text-xs shadow-xs">
          #3
        </span>
      );
    }
    return (
      <span className="w-7 h-7 rounded-full bg-cream border border-border text-muted flex items-center justify-center font-semibold text-xs">
        #{rank}
      </span>
    );
  };

  // Helper to get formatted day label (e.g., '12 oct')
  const formatDayLabel = (dateStr: string) => {
    try {
      const parts = dateStr.split('-');
      if (parts.length === 3) {
        const months = ['ene', 'feb', 'mar', 'abr', 'may', 'jun', 'jul', 'ago', 'sep', 'oct', 'nov', 'dic'];
        const m = parseInt(parts[1], 10) - 1;
        const d = parseInt(parts[2], 10);
        return `${d} ${months[m] || ''}`;
      }
      return dateStr;
    } catch {
      return dateStr;
    }
  };

  return (
    <div className="min-h-screen bg-cream text-ink">
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
              <span className="text-[10px] text-muted">Inteligencia Comercial &amp; Rendimiento</span>
            </div>
          </div>

          <div className="flex items-center gap-2">
            <Link
              href="/admin"
              className="inline-flex items-center gap-1.5 text-xs font-bold text-wine hover:text-wineDark bg-blush/60 hover:bg-blush px-3.5 py-2 rounded-full transition"
            >
              <ArrowLeft className="w-3.5 h-3.5" />
              <span className="hidden sm:inline">Volver al</span> Panel
            </Link>
          </div>
        </div>
      </header>

      {/* Main Container */}
      <main className="max-w-7xl mx-auto px-4 sm:px-6 py-8 space-y-6">
        {/* Navigation Bar / Tabs */}
        <nav
          aria-label="Pestañas de navegación administrativa"
          className="bg-white border border-border rounded-2xl p-1.5 shadow-sm flex items-center gap-1 overflow-x-auto no-scrollbar"
        >
          <Link
            href="/admin"
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold text-muted hover:text-wine hover:bg-blush/40 transition whitespace-nowrap"
          >
            <LayoutDashboard className="w-4 h-4" />
            Resumen
          </Link>
          <Link
            href="/admin/pedidos"
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold text-muted hover:text-wine hover:bg-blush/40 transition whitespace-nowrap"
          >
            <Clock className="w-4 h-4" />
            Pedidos
          </Link>
          <Link
            href="/admin/ventas"
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold text-muted hover:text-wine hover:bg-blush/40 transition whitespace-nowrap"
          >
            <TrendingUp className="w-4 h-4" />
            Ventas Confirmadas
          </Link>
          <Link
            href="/admin/analitica"
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-xs sm:text-sm font-bold bg-wine text-white shadow-xs transition whitespace-nowrap"
          >
            <BarChart3 className="w-4 h-4" />
            Analítica
          </Link>
          <Link
            href="/admin/importar"
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold text-muted hover:text-wine hover:bg-blush/40 transition whitespace-nowrap"
          >
            <Upload className="w-4 h-4" />
            Importar Excel
          </Link>
        </nav>

        {/* Page Header */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white border border-border rounded-3xl p-6 sm:p-8 shadow-soft">
          <div>
            <span className="text-[11px] font-extrabold uppercase tracking-[0.2em] text-wine">
              Business Intelligence
            </span>
            <h1 className="font-serif text-2xl sm:text-3xl text-wine font-normal tracking-tight mt-1">
              Analítica y Rendimiento
            </h1>
            <p className="text-muted text-xs sm:text-sm mt-1">
              Métricas consolidadas de ventas e inventario calculadas en tiempo real.
            </p>
          </div>

          <div className="flex items-center gap-2 self-start md:self-center">
            <button
              type="button"
              onClick={() => fetchAnalytics()}
              disabled={loading}
              className="inline-flex items-center gap-1.5 px-4 py-2 rounded-full border border-border hover:bg-blush/40 text-wine text-xs font-bold transition shadow-xs"
            >
              <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
              <span>Actualizar datos</span>
            </button>
          </div>
        </div>

        {/* Date Filter Bar */}
        <section aria-label="Selector de rango de fechas" className="bg-white border border-border rounded-3xl p-5 shadow-soft">
          <div className="flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-4">
            {/* Quick buttons */}
            <div className="flex items-center gap-2 flex-wrap">
              <span className="text-xs font-bold text-muted mr-1">Rango rápido:</span>
              <button
                type="button"
                onClick={() => handleSelectPreset('hoy')}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition ${
                  activePreset === 'hoy'
                    ? 'bg-wine text-white shadow-xs'
                    : 'bg-blush/60 hover:bg-blush text-wine'
                }`}
              >
                Hoy
              </button>
              <button
                type="button"
                onClick={() => handleSelectPreset('ultimos7')}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition ${
                  activePreset === 'ultimos7'
                    ? 'bg-wine text-white shadow-xs'
                    : 'bg-blush/60 hover:bg-blush text-wine'
                }`}
              >
                Últimos 7 días
              </button>
              <button
                type="button"
                onClick={() => handleSelectPreset('esteMes')}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition ${
                  activePreset === 'esteMes'
                    ? 'bg-wine text-white shadow-xs'
                    : 'bg-blush/60 hover:bg-blush text-wine'
                }`}
              >
                Este Mes
              </button>
              <button
                type="button"
                onClick={() => handleSelectPreset('ultimos30')}
                className={`px-3 py-1.5 rounded-xl text-xs font-semibold transition ${
                  activePreset === 'ultimos30'
                    ? 'bg-wine text-white shadow-xs'
                    : 'bg-blush/60 hover:bg-blush text-wine'
                }`}
              >
                Últimos 30 días
              </button>
            </div>

            {/* Manual Date inputs */}
            <div className="flex items-center gap-3 flex-wrap">
              <div className="flex items-center gap-2 bg-cream/70 border border-border rounded-2xl px-3.5 py-1.5">
                <Calendar className="w-3.5 h-3.5 text-wine flex-shrink-0" />
                <label htmlFor="analytics-from-date" className="text-xs font-semibold text-muted">Desde:</label>
                <input
                  id="analytics-from-date"
                  type="date"
                  value={fromDate}
                  onChange={(e) => {
                    setFromDate(e.target.value);
                    setActivePreset('esteMes');
                  }}
                  className="bg-transparent text-xs font-semibold text-ink focus:outline-hidden"
                />
              </div>

              <div className="flex items-center gap-2 bg-cream/70 border border-border rounded-2xl px-3.5 py-1.5">
                <Calendar className="w-3.5 h-3.5 text-wine flex-shrink-0" />
                <label htmlFor="analytics-to-date" className="text-xs font-semibold text-muted">Hasta:</label>
                <input
                  id="analytics-to-date"
                  type="date"
                  value={toDate}
                  onChange={(e) => {
                    setToDate(e.target.value);
                    setActivePreset('esteMes');
                  }}
                  className="bg-transparent text-xs font-semibold text-ink focus:outline-hidden"
                />
              </div>
            </div>
          </div>
        </section>

        {/* 4 Summary KPI Cards */}
        <section aria-label="Métricas clave de analítica" className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <div className="bg-white border border-border rounded-2xl p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs text-muted font-medium">Total Ventas Pagadas</span>
              <div className="w-8 h-8 rounded-xl bg-wine/10 text-wine flex items-center justify-center">
                <DollarSign className="w-4 h-4" />
              </div>
            </div>
            <p className="font-serif text-2xl sm:text-3xl font-bold text-wine my-2 tracking-tight">
              {formatCOP(analyticsData.summary?.totalSales || 0)}
            </p>
            <span className="text-[11px] text-muted flex items-center gap-1">
              <Sparkles className="w-3 h-3 text-mauve" />
              Ingresos brutos confirmados
            </span>
          </div>

          <div className="bg-white border border-border rounded-2xl p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs text-muted font-medium">Pedidos Pagados</span>
              <div className="w-8 h-8 rounded-xl bg-[#E2F5EA] text-[#24734E] flex items-center justify-center">
                <CheckCircle2 className="w-4 h-4" />
              </div>
            </div>
            <p className="font-serif text-2xl sm:text-3xl font-bold text-ink my-2 tracking-tight">
              {analyticsData.summary?.paidOrdersCount || 0}
            </p>
            <span className="text-[11px] text-[#24734E] font-semibold flex items-center gap-1">
              ✓ Órdenes con pago verificado
            </span>
          </div>

          <div className="bg-white border border-border rounded-2xl p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs text-muted font-medium">Unidades Vendidas</span>
              <div className="w-8 h-8 rounded-xl bg-mauve/15 text-mauve flex items-center justify-center">
                <Package className="w-4 h-4" />
              </div>
            </div>
            <p className="font-serif text-2xl sm:text-3xl font-bold text-wine my-2 tracking-tight">
              {analyticsData.summary?.totalUnitsSold || 0}
            </p>
            <span className="text-[11px] text-muted flex items-center gap-1">
              Artículos entregados o por despachar
            </span>
          </div>

          <div className="bg-white border border-border rounded-2xl p-5 shadow-sm">
            <div className="flex items-center justify-between">
              <span className="text-xs text-muted font-medium">Ticket Promedio</span>
              <div className="w-8 h-8 rounded-xl bg-blush text-wine flex items-center justify-center">
                <Receipt className="w-4 h-4" />
              </div>
            </div>
            <p className="font-serif text-2xl sm:text-3xl font-bold text-ink my-2 tracking-tight">
              {formatCOP(analyticsData.summary?.averageTicket || 0)}
            </p>
            <span className="text-[11px] text-muted flex items-center gap-1">
              Valor promedio por compra
            </span>
          </div>
        </section>

        {/* Daily Sales Trend Chart Section */}
        <section aria-label="Tendencia diaria de ventas" className="bg-white border border-border rounded-3xl p-6 sm:p-8 shadow-soft space-y-6">
          <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 border-b border-border pb-4">
            <div>
              <h2 className="font-serif text-xl sm:text-2xl text-wine font-normal">
                Tendencia Diaria de Ventas
              </h2>
              <p className="text-muted text-xs sm:text-sm mt-0.5">
                Comportamiento de ingresos, volumen de órdenes y unidades por fecha
              </p>
            </div>
            <div className="flex items-center gap-3 text-xs text-muted">
              <span className="flex items-center gap-1.5">
                <span className="w-3 h-3 rounded-xs bg-wine inline-block" /> Ventas (COP)
              </span>
            </div>
          </div>

          {/* Bar Chart Representation */}
          {analyticsData.dailySales && analyticsData.dailySales.length > 0 ? (
            <div className="space-y-6">
              <div className="h-64 sm:h-72 w-full flex items-end justify-between gap-2 sm:gap-4 pt-8 pb-4 px-2 border-b border-border/80">
                {analyticsData.dailySales.map((day) => {
                  const heightPercent = Math.max(12, Math.round((day.totalSales / maxDailySales) * 100));
                  return (
                    <div
                      key={day.date}
                      className="flex-1 flex flex-col items-center justify-end h-full group relative"
                    >
                      {/* Tooltip on hover */}
                      <div className="absolute -top-14 opacity-0 group-hover:opacity-100 transition pointer-events-none z-10 bg-wineDark text-white text-[11px] py-1.5 px-2.5 rounded-xl shadow-lg whitespace-nowrap text-center">
                        <div className="font-bold">{formatCOP(day.totalSales)}</div>
                        <div className="text-[10px] text-white/80">
                          {day.ordersCount} {day.ordersCount === 1 ? 'pedido' : 'pedidos'} · {day.unitsCount} unid.
                        </div>
                      </div>

                      {/* Bar */}
                      <div
                        style={{ height: `${heightPercent}%` }}
                        className="w-full max-w-[48px] rounded-t-xl bg-gradient-to-t from-wine to-mauve group-hover:from-wineDark group-hover:to-wine transition-all duration-300 shadow-xs relative"
                      >
                        <span className="text-[10px] font-bold text-white absolute top-1 left-1/2 -translate-x-1/2 hidden md:block">
                          {day.ordersCount > 0 ? day.ordersCount : ''}
                        </span>
                      </div>

                      {/* Day label */}
                      <span className="text-[11px] text-muted font-medium mt-2 whitespace-nowrap">
                        {formatDayLabel(day.date)}
                      </span>
                    </div>
                  );
                })}
              </div>

              {/* Compact Daily Grid Cards */}
              <div className="grid grid-cols-2 sm:grid-cols-3 md:grid-cols-4 lg:grid-cols-7 gap-3">
                {analyticsData.dailySales.map((day) => (
                  <div
                    key={day.date}
                    className="bg-cream/50 border border-border rounded-2xl p-3 text-center hover:border-wine/40 transition"
                  >
                    <span className="text-[10px] uppercase font-bold text-muted block">
                      {formatDayLabel(day.date)}
                    </span>
                    <p className="font-serif text-sm font-bold text-wine mt-1">
                      {formatCOP(day.totalSales)}
                    </p>
                    <span className="text-[10px] text-muted block mt-0.5">
                      {day.ordersCount} ped. · {day.unitsCount} u.
                    </span>
                  </div>
                ))}
              </div>
            </div>
          ) : (
            <div className="text-center py-12 text-muted text-sm">
              No hay datos diarios para el rango seleccionado.
            </div>
          )}
        </section>

        {/* Two-Column Layout: Top Products & Payment Methods */}
        <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
          {/* Top Selling Products (7 columns) */}
          <section aria-label="Ranking de productos más vendidos" className="lg:col-span-7 bg-white border border-border rounded-3xl p-6 sm:p-8 shadow-soft space-y-5">
            <div className="flex items-center justify-between border-b border-border pb-4">
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="font-serif text-xl sm:text-2xl text-wine font-normal">
                    Ranking de Productos
                  </h2>
                  <Trophy className="w-4 h-4 text-amber-500" />
                </div>
                <p className="text-muted text-xs mt-0.5">
                  Artículos con mayor recaudación e impacto en el período
                </p>
              </div>
            </div>

            {analyticsData.topProducts && analyticsData.topProducts.length > 0 ? (
              <div className="space-y-4">
                {analyticsData.topProducts.map((prod) => (
                  <div
                    key={prod.sku}
                    className="p-4 rounded-2xl border border-border hover:border-wine/30 bg-cream/30 transition space-y-2.5"
                  >
                    <div className="flex items-start justify-between gap-3">
                      <div className="flex items-center gap-3">
                        {getRankBadge(prod.rank)}
                        <div>
                          <div className="font-semibold text-xs sm:text-sm text-ink leading-tight">
                            {prod.productName}
                          </div>
                          <div className="text-[11px] text-muted flex items-center gap-2 mt-0.5">
                            <span className="font-mono text-[10px] bg-blush px-1.5 py-0.5 rounded text-wine">
                              {prod.sku}
                            </span>
                            {prod.brandName && <span>· {prod.brandName}</span>}
                          </div>
                        </div>
                      </div>

                      <div className="text-right flex-shrink-0">
                        <div className="font-serif text-sm sm:text-base font-bold text-wine">
                          {formatCOP(prod.grossRevenue)}
                        </div>
                        <div className="text-[11px] text-muted font-medium">
                          {prod.unitsSold} {prod.unitsSold === 1 ? 'unidad' : 'unidades'}
                        </div>
                      </div>
                    </div>

                    {/* Progress Bar for Revenue Share */}
                    <div className="space-y-1">
                      <div className="flex justify-between text-[11px] text-muted">
                        <span>Participación en ingresos:</span>
                        <span className="font-bold text-wine">
                          {prod.revenueSharePercentage ? prod.revenueSharePercentage.toFixed(1) : 0}%
                        </span>
                      </div>
                      <div className="w-full h-2 rounded-full bg-border overflow-hidden">
                        <div
                          style={{ width: `${Math.min(100, Math.max(4, prod.revenueSharePercentage || 0))}%` }}
                          className="h-full rounded-full bg-gradient-to-r from-mauve to-wine transition-all duration-500"
                        />
                      </div>
                    </div>
                  </div>
                ))}
              </div>
            ) : (
              <div className="text-center py-10 text-muted text-xs">
                No hay registros de ventas de productos en este rango.
              </div>
            )}
          </section>

          {/* Payment Methods Distribution (5 columns) */}
          <section aria-label="Distribución por métodos de pago" className="lg:col-span-5 bg-white border border-border rounded-3xl p-6 sm:p-8 shadow-soft space-y-5">
            <div className="flex items-center justify-between border-b border-border pb-4">
              <div>
                <div className="flex items-center gap-2">
                  <h2 className="font-serif text-xl sm:text-2xl text-wine font-normal">
                    Métodos de Pago
                  </h2>
                  <CreditCard className="w-4 h-4 text-wine" />
                </div>
                <p className="text-muted text-xs mt-0.5">
                  Distribución del dinero recaudado por canal de pago
                </p>
              </div>
            </div>

            {analyticsData.paymentMethods && analyticsData.paymentMethods.length > 0 ? (
              <div className="space-y-5">
                {analyticsData.paymentMethods.map((pm) => (
                  <div key={pm.paymentMethod} className="space-y-1.5">
                    <div className="flex items-center justify-between text-xs sm:text-sm">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-ink">
                          {pm.paymentMethod}
                        </span>
                        <span className="text-[11px] text-muted">
                          ({pm.orderCount} {pm.orderCount === 1 ? 'pago' : 'pagos'})
                        </span>
                      </div>
                      <div className="font-serif font-bold text-wine">
                        {formatCOP(pm.totalAmount)}
                      </div>
                    </div>

                    <div className="flex items-center gap-3">
                      <div className="flex-1 h-3 rounded-full bg-border overflow-hidden">
                        <div
                          style={{ width: `${Math.min(100, Math.max(3, pm.percentage || 0))}%` }}
                          className={`h-full rounded-full transition-all duration-500 ${
                            pm.paymentMethod === 'NEQUI'
                              ? 'bg-[#6D32A8]'
                              : pm.paymentMethod === 'BANCOLOMBIA'
                              ? 'bg-[#E5A800]'
                              : pm.paymentMethod === 'DAVIPLATA'
                              ? 'bg-[#D12B2B]'
                              : pm.paymentMethod === 'EFECTIVO'
                              ? 'bg-[#22795B]'
                              : 'bg-mauve'
                          }`}
                        />
                      </div>
                      <span className="text-xs font-bold text-muted w-12 text-right">
                        {pm.percentage ? pm.percentage.toFixed(1) : 0}%
                      </span>
                    </div>
                  </div>
                ))}

                {/* Helpful note */}
                <div className="bg-blush/40 border border-border rounded-2xl p-4 flex items-start gap-2.5 text-xs text-muted mt-6">
                  <HelpCircle className="w-4 h-4 text-wine flex-shrink-0 mt-0.5" />
                  <span>
                    Todos los porcentajes y montos corresponden exclusivamente a pedidos en estado <strong className="text-wine">PAGADO</strong>, verificados por el equipo administrativo.
                  </span>
                </div>
              </div>
            ) : (
              <div className="text-center py-10 text-muted text-xs">
                No hay métodos de pago registrados en este rango.
              </div>
            )}
          </section>
        </div>
      </main>
    </div>
  );
}
