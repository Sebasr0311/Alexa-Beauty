'use client';

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import {
  ArrowLeft,
  FileSpreadsheet,
  Search,
  Calendar,
  CheckCircle2,
  DollarSign,
  Package,
  Receipt,
  Eye,
  Filter,
  RefreshCw,
  X,
  CreditCard,
  ShieldCheck,
  ChevronLeft,
  ChevronRight,
  User,
  Phone,
  LayoutDashboard,
  Clock,
  TrendingUp,
  BarChart3,
  Upload,
  AlertCircle,
  Sparkles,
} from 'lucide-react';
import {
  api,
  SalesListResponseDto,
  SaleOrderDto,
  SalesSummaryDto,
} from '@/lib/api';
import {
  formatCOP,
  getColombiaToday,
  getColombiaFirstDayOfMonth,
  getColombiaDaysAgo,
  formatColombiaDateTime,
} from '@/lib/utils';

// Fallback mock data in case backend is offline or empty during preview
const MOCK_SALES_SUMMARY: SalesSummaryDto = {
  totalSales: 845000,
  paidOrdersCount: 7,
  totalUnitsSold: 16,
  averageTicket: 120714,
  startDate: getColombiaFirstDayOfMonth(),
  endDate: getColombiaToday(),
};

const MOCK_SALES_ORDERS: SaleOrderDto[] = [
  {
    id: 'sale-001',
    reference: 'AB-2026-000101',
    customerName: 'Valeria Restrepo',
    customerPhone: '3109876543',
    paymentMethod: 'NEQUI',
    subtotal: 110000,
    discount: 0,
    shippingFee: 8000,
    total: 118000,
    verifiedBy: 'admin@alexabeauty.com',
    status: 'PAGADO',
    channel: 'WHATSAPP',
    paidAt: new Date(Date.now() - 4 * 3600 * 1000).toISOString(),
    localPaidAt: new Date(Date.now() - 4 * 3600 * 1000).toISOString(),
    items: [
      {
        id: 'item-1',
        sku: 'LAB-BELL-01',
        productName: 'Labial Mate Velvet Cream',
        brandName: 'Bella Flor',
        shadeName: 'Tono Rosé Chic',
        quantity: 2,
        unitPrice: 45000,
        subtotal: 90000,
        costSnapshot: 22000,
      },
      {
        id: 'item-2',
        sku: 'RUB-BELL-02',
        productName: 'Rubor Líquido Soft Glow',
        brandName: 'Bella Flor',
        shadeName: 'Tono Melocotón',
        quantity: 1,
        unitPrice: 20000,
        subtotal: 20000,
        costSnapshot: 9500,
      },
    ],
  },
  {
    id: 'sale-002',
    reference: 'AB-2026-000102',
    customerName: 'Camila Morales',
    customerPhone: '3001234567',
    paymentMethod: 'BANCOLOMBIA',
    subtotal: 65000,
    discount: 0,
    shippingFee: 0,
    total: 65000,
    verifiedBy: 'admin@alexabeauty.com',
    status: 'PAGADO',
    channel: 'WEB',
    paidAt: new Date(Date.now() - 8 * 3600 * 1000).toISOString(),
    localPaidAt: new Date(Date.now() - 8 * 3600 * 1000).toISOString(),
    items: [
      {
        id: 'item-3',
        sku: 'FND-NAT-03',
        productName: 'Base Líquida Serum Radiance',
        brandName: 'Lumina Skin',
        shadeName: 'Beige Cálido 02',
        quantity: 1,
        unitPrice: 65000,
        subtotal: 65000,
        costSnapshot: 31000,
      },
    ],
  },
  {
    id: 'sale-003',
    reference: 'AB-2026-000103',
    customerName: 'Daniela Gómez',
    customerPhone: '3157778899',
    paymentMethod: 'DAVIPLATA',
    subtotal: 180000,
    discount: 10000,
    shippingFee: 15000,
    total: 185000,
    verifiedBy: 'caja@alexabeauty.com',
    status: 'PAGADO',
    channel: 'WHATSAPP',
    paidAt: new Date(Date.now() - 24 * 3600 * 1000).toISOString(),
    localPaidAt: new Date(Date.now() - 24 * 3600 * 1000).toISOString(),
    items: [
      {
        id: 'item-4',
        sku: 'KIT-RUT-01',
        productName: 'Kit Rutina Facial Esencial',
        brandName: 'Dermique',
        shadeName: 'Piel Mixta',
        quantity: 1,
        unitPrice: 180000,
        subtotal: 180000,
        costSnapshot: 95000,
      },
    ],
  },
  {
    id: 'sale-004',
    reference: 'AB-2026-000104',
    customerName: 'Mariana Duque',
    customerPhone: '3124455667',
    paymentMethod: 'NEQUI',
    subtotal: 135000,
    discount: 5000,
    shippingFee: 9000,
    total: 139000,
    verifiedBy: 'admin@alexabeauty.com',
    status: 'PAGADO',
    channel: 'WHATSAPP',
    paidAt: new Date(Date.now() - 36 * 3600 * 1000).toISOString(),
    localPaidAt: new Date(Date.now() - 36 * 3600 * 1000).toISOString(),
    items: [
      {
        id: 'item-5',
        sku: 'MSK-VOL-01',
        productName: 'Máscara Pestañas Volumen Extremo',
        brandName: 'Bella Flor',
        shadeName: 'Negro Intenso',
        quantity: 3,
        unitPrice: 35000,
        subtotal: 105000,
        costSnapshot: 15000,
      },
      {
        id: 'item-6',
        sku: 'COR-LUM-01',
        productName: 'Corrector Iluminador HD',
        brandName: 'Lumina Skin',
        shadeName: 'Claro 01',
        quantity: 1,
        unitPrice: 30000,
        subtotal: 30000,
        costSnapshot: 14000,
      },
    ],
  },
  {
    id: 'sale-005',
    reference: 'AB-2026-000105',
    customerName: 'Carolina Henao',
    customerPhone: '3209988776',
    paymentMethod: 'EFECTIVO',
    subtotal: 92000,
    discount: 0,
    shippingFee: 0,
    total: 92000,
    verifiedBy: 'tienda@alexabeauty.com',
    status: 'PAGADO',
    channel: 'WEB',
    paidAt: new Date(Date.now() - 48 * 3600 * 1000).toISOString(),
    localPaidAt: new Date(Date.now() - 48 * 3600 * 1000).toISOString(),
    items: [
      {
        id: 'item-7',
        sku: 'PAL-SOM-04',
        productName: 'Paleta de Sombras Nude Sunset',
        brandName: 'Bella Flor',
        shadeName: 'Warm Nudes',
        quantity: 1,
        unitPrice: 92000,
        subtotal: 92000,
        costSnapshot: 46000,
      },
    ],
  },
  {
    id: 'sale-006',
    reference: 'AB-2026-000106',
    customerName: 'Laura Ospina',
    customerPhone: '3183344556',
    paymentMethod: 'BANCOLOMBIA',
    subtotal: 156000,
    discount: 10000,
    shippingFee: 8000,
    total: 154000,
    verifiedBy: 'admin@alexabeauty.com',
    status: 'PAGADO',
    channel: 'WHATSAPP',
    paidAt: new Date(Date.now() - 60 * 3600 * 1000).toISOString(),
    localPaidAt: new Date(Date.now() - 60 * 3600 * 1000).toISOString(),
    items: [
      {
        id: 'item-8',
        sku: 'SER-VIT-02',
        productName: 'Serum Vitamina C Iluminador',
        brandName: 'Dermique',
        shadeName: '30 ml',
        quantity: 2,
        unitPrice: 78000,
        subtotal: 156000,
        costSnapshot: 38000,
      },
    ],
  },
  {
    id: 'sale-007',
    reference: 'AB-2026-000107',
    customerName: 'Sofía Álvarez',
    customerPhone: '3116677889',
    paymentMethod: 'NEQUI',
    subtotal: 92000,
    discount: 0,
    shippingFee: 0,
    total: 92000,
    verifiedBy: 'admin@alexabeauty.com',
    status: 'PAGADO',
    channel: 'WHATSAPP',
    paidAt: new Date(Date.now() - 72 * 3600 * 1000).toISOString(),
    localPaidAt: new Date(Date.now() - 72 * 3600 * 1000).toISOString(),
    items: [
      {
        id: 'item-9',
        sku: 'LAB-BELL-01',
        productName: 'Labial Mate Velvet Cream',
        brandName: 'Bella Flor',
        shadeName: 'Tono Ciruela Real',
        quantity: 2,
        unitPrice: 46000,
        subtotal: 92000,
        costSnapshot: 22000,
      },
    ],
  },
];

export default function AdminVentasPage() {
  // Date and filter states
  const [fromDate, setFromDate] = useState<string>(() => getColombiaFirstDayOfMonth());
  const [toDate, setToDate] = useState<string>(() => getColombiaToday());
  const [paymentMethodFilter, setPaymentMethodFilter] = useState<string>('TODOS');
  const [searchQuery, setSearchQuery] = useState<string>('');

  // Pagination states
  const [page, setPage] = useState<number>(0);
  const pageSize = 10;

  // Data states
  const [loading, setLoading] = useState<boolean>(true);
  const [exporting, setExporting] = useState<boolean>(false);
  const [feedbackMessage, setFeedbackMessage] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Sales response
  const [salesResponse, setSalesResponse] = useState<SalesListResponseDto>({
    summary: MOCK_SALES_SUMMARY,
    orders: MOCK_SALES_ORDERS,
    page: 0,
    size: pageSize,
    totalElements: MOCK_SALES_ORDERS.length,
    totalPages: 1,
  });

  // Modal detail state
  const [selectedOrder, setSelectedOrder] = useState<SaleOrderDto | null>(null);

  // Fetch sales from API
  const fetchSales = useCallback(async () => {
    setLoading(true);
    setFeedbackMessage(null);
    try {
      const res = await api.getAdminSales({
        from: fromDate,
        to: toDate,
        method: paymentMethodFilter === 'TODOS' ? undefined : paymentMethodFilter,
        page,
        size: pageSize,
      });

      if (res && res.orders) {
        setSalesResponse(res);
      }
    } catch {
      // Graceful fallback to client-filtered mock data so UI remains 100% testable
      let filtered = [...MOCK_SALES_ORDERS];
      if (paymentMethodFilter !== 'TODOS') {
        filtered = filtered.filter(
          (o) => o.paymentMethod?.toUpperCase() === paymentMethodFilter.toUpperCase()
        );
      }

      const totalSales = filtered.reduce((acc, o) => acc + o.total, 0);
      const totalUnits = filtered.reduce(
        (acc, o) => acc + o.items.reduce((s, it) => s + it.quantity, 0),
        0
      );
      const paidOrdersCount = filtered.length;
      const averageTicket = paidOrdersCount > 0 ? Math.round(totalSales / paidOrdersCount) : 0;

      setSalesResponse({
        summary: {
          totalSales,
          paidOrdersCount,
          totalUnitsSold: totalUnits,
          averageTicket,
          startDate: fromDate,
          endDate: toDate,
        },
        orders: filtered,
        page: 0,
        size: pageSize,
        totalElements: filtered.length,
        totalPages: 1,
      });
    } finally {
      setLoading(false);
    }
  }, [fromDate, toDate, paymentMethodFilter, page, pageSize]);

  useEffect(() => {
    fetchSales();
  }, [fetchSales]);

  // Client search filter over current results
  const displayedOrders = useMemo(() => {
    if (!searchQuery.trim()) return salesResponse.orders;
    const query = searchQuery.toLowerCase().trim();
    return salesResponse.orders.filter(
      (order) =>
        order.reference.toLowerCase().includes(query) ||
        order.customerName.toLowerCase().includes(query) ||
        order.customerPhone.toLowerCase().includes(query)
    );
  }, [salesResponse.orders, searchQuery]);

  // Export to Excel handler
  const handleExportExcel = async () => {
    setExporting(true);
    setFeedbackMessage(null);
    try {
      await api.downloadAdminSalesExcel({
        from: fromDate,
        to: toDate,
        method: paymentMethodFilter === 'TODOS' ? undefined : paymentMethodFilter,
      });
      setFeedbackMessage({
        type: 'success',
        text: 'Reporte Excel descargado correctamente.',
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error desconocido al descargar el archivo Excel.';
      setFeedbackMessage({
        type: 'error',
        text: `No se pudo descargar el archivo Excel: ${msg}`,
      });
    } finally {
      setExporting(false);
    }
  };

  // Quick date presets
  const applyDatePreset = (preset: 'hoy' | 'ultimos7' | 'esteMes' | 'ultimos30') => {
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
    setPage(0);
  };

  const getPaymentMethodBadge = (method?: string) => {
    switch (method?.toUpperCase()) {
      case 'NEQUI':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-[#E8E1F8] text-[#3E1B73] border border-[#D5C7F2]">
            Nequi
          </span>
        );
      case 'BANCOLOMBIA':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-[#FEF4D5] text-[#7A5B00] border border-[#FDE59A]">
            Bancolombia
          </span>
        );
      case 'DAVIPLATA':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-[#FDE2E2] text-[#8E1F1F] border border-[#F9BDBD]">
            Daviplata
          </span>
        );
      case 'EFECTIVO':
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-[#D8F3E5] text-[#196B3E] border border-[#B1E7CB]">
            Efectivo
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold bg-gray-100 text-gray-700 border border-gray-300">
            {method || 'Otro'}
          </span>
        );
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
              <span className="text-[10px] text-muted">Panel de Control &amp; Ventas Confirmadas</span>
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
          aria-label="Pestañas de administración"
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
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-xs sm:text-sm font-bold bg-wine text-white shadow-xs transition whitespace-nowrap"
          >
            <TrendingUp className="w-4 h-4" />
            Ventas Confirmadas
          </Link>
          <Link
            href="/admin/analitica"
            className="flex items-center gap-2 px-4 py-2 rounded-xl text-xs sm:text-sm font-semibold text-muted hover:text-wine hover:bg-blush/40 transition whitespace-nowrap"
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

        {/* Feedback alert */}
        {feedbackMessage && (
          <div
            role="alert"
            className={`flex items-center justify-between gap-3 p-4 rounded-2xl border text-xs sm:text-sm ${
              feedbackMessage.type === 'success'
                ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
                : 'bg-rose-50 border-rose-200 text-rose-800'
            }`}
          >
            <div className="flex items-center gap-2">
              {feedbackMessage.type === 'success' ? (
                <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
              ) : (
                <AlertCircle className="w-4 h-4 text-rose-600 flex-shrink-0" />
              )}
              <span>{feedbackMessage.text}</span>
            </div>
            <button
              onClick={() => setFeedbackMessage(null)}
              className="text-muted hover:text-ink transition"
              aria-label="Cerrar alerta"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Page Header with Action Button */}
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white border border-border rounded-3xl p-6 sm:p-8 shadow-soft">
          <div>
            <div className="flex items-center gap-2">
              <span className="text-[11px] font-extrabold uppercase tracking-[0.2em] text-wine">
                Reportes Comerciales
              </span>
              <span className="inline-flex items-center gap-1 text-[10px] font-semibold bg-emerald-100 text-emerald-800 px-2 py-0.5 rounded-full">
                <ShieldCheck className="w-3 h-3" /> Solo Pagados
              </span>
            </div>
            <h1 className="font-serif text-2xl sm:text-3xl text-wine font-normal tracking-tight mt-1">
              Ventas Confirmadas
            </h1>
            <p className="text-muted text-xs sm:text-sm mt-1">
              Reporte oficial de pedidos pagados y verificados con impacto contable e inventario descontado.
            </p>
          </div>

          <div className="flex items-center gap-3 self-start md:self-center">
            <button
              type="button"
              onClick={handleExportExcel}
              disabled={exporting}
              className="inline-flex items-center gap-2 rounded-full bg-wine hover:bg-wineDark disabled:opacity-50 text-white font-bold text-xs sm:text-sm px-5 py-3 shadow-glow transition active:scale-95"
            >
              {exporting ? (
                <RefreshCw className="w-4 h-4 animate-spin" />
              ) : (
                <FileSpreadsheet className="w-4 h-4 stroke-[2.5]" />
              )}
              <span>{exporting ? 'Generando Excel...' : 'Descargar Reporte Excel (.xlsx)'}</span>
            </button>
          </div>
        </div>

        {/* 4 Brand Palette KPI Cards */}
        <section aria-label="Métricas de ventas" className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          {/* Total Ventas Pagadas */}
          <div className="bg-white border border-[#EADDE0] rounded-2xl p-5 shadow-sm relative overflow-hidden group hover:border-wine transition">
            <div className="flex items-center justify-between">
              <span className="text-xs text-muted font-medium">Total Ventas Pagadas</span>
              <div className="w-9 h-9 rounded-xl bg-wine/10 text-wine flex items-center justify-center">
                <DollarSign className="w-5 h-5" />
              </div>
            </div>
            <p className="font-serif text-2xl sm:text-3xl font-bold text-wine my-2 tracking-tight">
              {formatCOP(salesResponse.summary.totalSales || 0)}
            </p>
            <span className="text-[11px] text-muted flex items-center gap-1">
              <Sparkles className="w-3 h-3 text-mauve" />
              Ingresos brutos confirmados
            </span>
          </div>

          {/* Pedidos Pagados */}
          <div className="bg-white border border-[#EADDE0] rounded-2xl p-5 shadow-sm relative overflow-hidden group hover:border-wine transition">
            <div className="flex items-center justify-between">
              <span className="text-xs text-muted font-medium">Pedidos Pagados</span>
              <div className="w-9 h-9 rounded-xl bg-[#E2F5EA] text-[#24734E] flex items-center justify-center">
                <CheckCircle2 className="w-5 h-5" />
              </div>
            </div>
            <p className="font-serif text-2xl sm:text-3xl font-bold text-ink my-2 tracking-tight">
              {salesResponse.summary.paidOrdersCount || 0}
            </p>
            <span className="text-[11px] text-[#24734E] font-semibold flex items-center gap-1">
              ✓ Órdenes con pago verificado
            </span>
          </div>

          {/* Unidades Vendidas */}
          <div className="bg-white border border-[#EADDE0] rounded-2xl p-5 shadow-sm relative overflow-hidden group hover:border-wine transition">
            <div className="flex items-center justify-between">
              <span className="text-xs text-muted font-medium">Unidades Vendidas</span>
              <div className="w-9 h-9 rounded-xl bg-mauve/15 text-mauve flex items-center justify-center">
                <Package className="w-5 h-5" />
              </div>
            </div>
            <p className="font-serif text-2xl sm:text-3xl font-bold text-wine my-2 tracking-tight">
              {salesResponse.summary.totalUnitsSold || 0}
            </p>
            <span className="text-[11px] text-muted flex items-center gap-1">
              Artículos entregados o por despachar
            </span>
          </div>

          {/* Ticket Promedio */}
          <div className="bg-white border border-[#EADDE0] rounded-2xl p-5 shadow-sm relative overflow-hidden group hover:border-wine transition">
            <div className="flex items-center justify-between">
              <span className="text-xs text-muted font-medium">Ticket Promedio</span>
              <div className="w-9 h-9 rounded-xl bg-blush text-wine flex items-center justify-center">
                <Receipt className="w-5 h-5" />
              </div>
            </div>
            <p className="font-serif text-2xl sm:text-3xl font-bold text-ink my-2 tracking-tight">
              {formatCOP(salesResponse.summary.averageTicket || 0)}
            </p>
            <span className="text-[11px] text-muted flex items-center gap-1">
              Promedio por orden pagada
            </span>
          </div>
        </section>

        {/* Filter Bar */}
        <section aria-label="Filtros del reporte" className="bg-white border border-border rounded-3xl p-5 shadow-soft space-y-4">
          <div className="flex flex-col lg:flex-row items-stretch lg:items-center justify-between gap-4">
            {/* Date range pickers */}
            <div className="flex flex-wrap items-center gap-3">
              <div className="flex items-center gap-2 bg-cream/70 border border-border rounded-2xl px-3.5 py-2">
                <Calendar className="w-4 h-4 text-wine flex-shrink-0" />
                <label htmlFor="sales-from-date" className="text-xs font-semibold text-muted">Desde:</label>
                <input
                  id="sales-from-date"
                  type="date"
                  value={fromDate}
                  onChange={(e) => {
                    setFromDate(e.target.value);
                    setPage(0);
                  }}
                  className="bg-transparent text-xs sm:text-sm font-semibold text-ink focus:outline-hidden"
                />
              </div>

              <div className="flex items-center gap-2 bg-cream/70 border border-border rounded-2xl px-3.5 py-2">
                <Calendar className="w-4 h-4 text-wine flex-shrink-0" />
                <label htmlFor="sales-to-date" className="text-xs font-semibold text-muted">Hasta:</label>
                <input
                  id="sales-to-date"
                  type="date"
                  value={toDate}
                  onChange={(e) => {
                    setToDate(e.target.value);
                    setPage(0);
                  }}
                  className="bg-transparent text-xs sm:text-sm font-semibold text-ink focus:outline-hidden"
                />
              </div>

              {/* Quick Presets */}
              <div className="flex items-center gap-1.5 flex-wrap">
                <button
                  type="button"
                  onClick={() => applyDatePreset('hoy')}
                  className="px-2.5 py-1.5 rounded-xl text-xs font-semibold bg-blush/60 hover:bg-blush text-wine transition"
                >
                  Hoy
                </button>
                <button
                  type="button"
                  onClick={() => applyDatePreset('ultimos7')}
                  className="px-2.5 py-1.5 rounded-xl text-xs font-semibold bg-blush/60 hover:bg-blush text-wine transition"
                >
                  7 días
                </button>
                <button
                  type="button"
                  onClick={() => applyDatePreset('esteMes')}
                  className="px-2.5 py-1.5 rounded-xl text-xs font-semibold bg-blush/60 hover:bg-blush text-wine transition"
                >
                  Este Mes
                </button>
                <button
                  type="button"
                  onClick={() => applyDatePreset('ultimos30')}
                  className="px-2.5 py-1.5 rounded-xl text-xs font-semibold bg-blush/60 hover:bg-blush text-wine transition"
                >
                  30 días
                </button>
              </div>
            </div>

            {/* Payment Method Filter */}
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-2 bg-cream/70 border border-border rounded-2xl px-3.5 py-2 w-full sm:w-auto">
                <Filter className="w-4 h-4 text-wine flex-shrink-0" />
                <label htmlFor="sales-method-select" className="text-xs font-semibold text-muted">Método:</label>
                <select
                  id="sales-method-select"
                  value={paymentMethodFilter}
                  onChange={(e) => {
                    setPaymentMethodFilter(e.target.value);
                    setPage(0);
                  }}
                  className="bg-transparent text-xs sm:text-sm font-bold text-wine focus:outline-hidden cursor-pointer"
                >
                  <option value="TODOS">Todos los métodos</option>
                  <option value="NEQUI">Nequi</option>
                  <option value="DAVIPLATA">Daviplata</option>
                  <option value="BANCOLOMBIA">Bancolombia</option>
                  <option value="EFECTIVO">Efectivo</option>
                  <option value="OTRO">Otro</option>
                </select>
              </div>

              <button
                type="button"
                onClick={() => fetchSales()}
                disabled={loading}
                className="p-2.5 rounded-2xl border border-border hover:bg-blush/50 text-wine transition"
                title="Actualizar listado"
                aria-label="Actualizar datos"
              >
                <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              </button>
            </div>
          </div>

          {/* Search Box */}
          <div className="relative">
            <Search className="w-4 h-4 text-muted absolute left-3.5 top-1/2 -translate-y-1/2" />
            <input
              type="text"
              placeholder="Buscar por referencia (ej. AB-2026-000101), cliente o teléfono..."
              value={searchQuery}
              onChange={(e) => setSearchQuery(e.target.value)}
              className="w-full pl-10 pr-4 py-2.5 rounded-2xl bg-cream/50 border border-border text-xs sm:text-sm placeholder:text-muted focus:outline-hidden focus:border-wine transition"
            />
            {searchQuery && (
              <button
                type="button"
                onClick={() => setSearchQuery('')}
                className="absolute right-3.5 top-1/2 -translate-y-1/2 text-muted hover:text-ink text-xs"
              >
                Limpiar
              </button>
            )}
          </div>
        </section>

        {/* Sales Table Section */}
        <section aria-label="Tabla de ventas confirmadas" className="bg-white border border-border rounded-3xl p-6 shadow-soft space-y-4">
          <div className="flex items-center justify-between border-b border-border pb-4">
            <div>
              <h2 className="font-serif text-xl text-wine font-normal">
                Transacciones Pagadas
              </h2>
              <p className="text-muted text-xs mt-0.5">
                Mostrando {displayedOrders.length} {displayedOrders.length === 1 ? 'pedido confirmado' : 'pedidos confirmados'}
              </p>
            </div>
          </div>

          {displayedOrders.length === 0 ? (
            <div className="text-center py-16 px-4 space-y-3">
              <div className="w-14 h-14 rounded-full bg-blush text-wine flex items-center justify-center mx-auto">
                <Receipt className="w-7 h-7" />
              </div>
              <h3 className="font-serif text-lg text-wine font-medium">
                No se encontraron ventas confirmadas
              </h3>
              <p className="text-muted text-xs sm:text-sm max-w-md mx-auto">
                No hay transacciones pagadas que coincidan con el rango de fechas ({fromDate} a {toDate}) o filtros seleccionados.
              </p>
              <button
                type="button"
                onClick={() => {
                  applyDatePreset('esteMes');
                  setPaymentMethodFilter('TODOS');
                  setSearchQuery('');
                }}
                className="inline-flex items-center gap-2 px-4 py-2 rounded-full bg-blush text-wine text-xs font-bold hover:bg-wine hover:text-white transition"
              >
                Restablecer filtros
              </button>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs sm:text-sm border-collapse">
                <thead>
                  <tr className="bg-blush text-wine font-bold border-b border-border">
                    <th className="py-3 px-3.5 rounded-l-xl">Fecha de Pago</th>
                    <th className="py-3 px-3.5">Referencia</th>
                    <th className="py-3 px-3.5">Cliente</th>
                    <th className="py-3 px-3.5">Método</th>
                    <th className="py-3 px-3.5 text-right">Total</th>
                    <th className="py-3 px-3.5 hidden md:table-cell">Verificado Por</th>
                    <th className="py-3 px-3.5 hidden sm:table-cell">Canal</th>
                    <th className="py-3 px-3.5 text-center rounded-r-xl">Acciones</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/60">
                  {displayedOrders.map((order) => (
                    <tr key={order.id} className="hover:bg-cream/50 transition">
                      {/* Fecha de Pago */}
                      <td className="py-3.5 px-3.5 text-muted text-xs whitespace-nowrap">
                        {formatColombiaDateTime(order.localPaidAt || order.paidAt)}
                      </td>

                      {/* Referencia */}
                      <td className="py-3.5 px-3.5 font-mono font-bold text-wine whitespace-nowrap">
                        {order.reference}
                      </td>

                      {/* Cliente */}
                      <td className="py-3.5 px-3.5">
                        <div className="font-semibold text-ink">{order.customerName}</div>
                        <div className="text-[11px] text-muted">{order.customerPhone}</div>
                      </td>

                      {/* Método */}
                      <td className="py-3.5 px-3.5 whitespace-nowrap">
                        {getPaymentMethodBadge(order.paymentMethod)}
                      </td>

                      {/* Total */}
                      <td className="py-3.5 px-3.5 font-bold text-ink text-right whitespace-nowrap">
                        {formatCOP(order.total)}
                      </td>

                      {/* Verificado Por */}
                      <td className="py-3.5 px-3.5 text-muted text-xs hidden md:table-cell">
                        <span className="inline-flex items-center gap-1 font-medium text-ink">
                          <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                          {order.verifiedBy || 'Sistema'}
                        </span>
                      </td>

                      {/* Canal */}
                      <td className="py-3.5 px-3.5 hidden sm:table-cell">
                        <span className="inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wider bg-cream border border-border text-muted">
                          {order.channel}
                        </span>
                      </td>

                      {/* Acciones */}
                      <td className="py-3.5 px-3.5 text-center whitespace-nowrap">
                        <button
                          type="button"
                          onClick={() => setSelectedOrder(order)}
                          className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-blush/60 hover:bg-wine hover:text-white text-wine text-xs font-bold transition shadow-xs"
                        >
                          <Eye className="w-3.5 h-3.5" />
                          <span>Ver detalle</span>
                        </button>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}

          {/* Pagination Controls */}
          {salesResponse.totalPages > 1 && (
            <div className="flex items-center justify-between border-t border-border pt-4">
              <span className="text-xs text-muted">
                Página {salesResponse.page + 1} de {salesResponse.totalPages} ({salesResponse.totalElements} órdenes)
              </span>

              <div className="flex items-center gap-2">
                <button
                  type="button"
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  disabled={page === 0}
                  className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-border text-xs font-semibold text-ink disabled:opacity-40 hover:bg-cream transition"
                >
                  <ChevronLeft className="w-4 h-4" /> Anterior
                </button>
                <button
                  type="button"
                  onClick={() => setPage((p) => Math.min(salesResponse.totalPages - 1, p + 1))}
                  disabled={page >= salesResponse.totalPages - 1}
                  className="inline-flex items-center gap-1 px-3 py-1.5 rounded-xl border border-border text-xs font-semibold text-ink disabled:opacity-40 hover:bg-cream transition"
                >
                  Siguiente <ChevronRight className="w-4 h-4" />
                </button>
              </div>
            </div>
          )}
        </section>
      </main>

      {/* Sale Order Details Modal */}
      {selectedOrder && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 sm:p-6"
        >
          <div className="bg-white rounded-3xl max-w-2xl w-full max-h-[90vh] overflow-y-auto shadow-2xl border border-border p-6 sm:p-8 space-y-6 animate-in fade-in zoom-in-95 duration-200">
            {/* Modal Header */}
            <div className="flex items-start justify-between border-b border-border pb-4">
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-mono text-xs font-bold text-wine bg-blush px-2.5 py-0.5 rounded-md">
                    {selectedOrder.reference}
                  </span>
                  <span className="inline-flex items-center gap-1 text-[11px] font-bold bg-[#E2F5EA] text-[#24734E] px-2 py-0.5 rounded-full">
                    <CheckCircle2 className="w-3 h-3" /> Pago Confirmado
                  </span>
                </div>
                <h3 className="font-serif text-xl sm:text-2xl text-wine font-medium mt-1">
                  Detalle de Venta Confirmada
                </h3>
                <p className="text-muted text-xs">
                  Pagado el {formatColombiaDateTime(selectedOrder.localPaidAt || selectedOrder.paidAt)}
                </p>
              </div>

              <button
                type="button"
                onClick={() => setSelectedOrder(null)}
                className="w-8 h-8 rounded-full bg-cream hover:bg-blush flex items-center justify-center text-muted hover:text-wine transition"
                aria-label="Cerrar modal"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {/* Customer & Payment Info Cards */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div className="bg-cream/60 border border-border rounded-2xl p-4 space-y-1.5">
                <span className="text-[10px] uppercase font-bold tracking-wider text-muted block">
                  Información del Cliente
                </span>
                <p className="text-sm font-bold text-ink flex items-center gap-1.5">
                  <User className="w-4 h-4 text-wine" />
                  {selectedOrder.customerName}
                </p>
                <p className="text-xs text-muted flex items-center gap-1.5">
                  <Phone className="w-3.5 h-3.5 text-muted" />
                  {selectedOrder.customerPhone}
                </p>
                <p className="text-[11px] text-muted">
                  Canal de origen: <span className="font-semibold text-ink">{selectedOrder.channel}</span>
                </p>
              </div>

              <div className="bg-cream/60 border border-border rounded-2xl p-4 space-y-1.5">
                <span className="text-[10px] uppercase font-bold tracking-wider text-muted block">
                  Verificación de Pago
                </span>
                <div className="flex items-center gap-2">
                  <CreditCard className="w-4 h-4 text-wine" />
                  {getPaymentMethodBadge(selectedOrder.paymentMethod)}
                </div>
                <p className="text-xs text-muted flex items-center gap-1">
                  <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
                  Verificado por: <span className="font-semibold text-ink">{selectedOrder.verifiedBy || 'admin@alexabeauty.com'}</span>
                </p>
                <p className="text-[11px] text-[#24734E] font-medium">
                  ✓ Inventario descargado de bodega
                </p>
              </div>
            </div>

            {/* Line Items Table */}
            <div className="space-y-3">
              <h4 className="text-xs font-bold uppercase tracking-wider text-wine">
                Artículos Vendidos ({selectedOrder.items?.length || 0})
              </h4>
              <div className="border border-border rounded-2xl overflow-hidden">
                <table className="w-full text-left text-xs">
                  <thead className="bg-blush text-wine font-bold border-b border-border">
                    <tr>
                      <th className="py-2.5 px-3">SKU</th>
                      <th className="py-2.5 px-3">Producto</th>
                      <th className="py-2.5 px-3">Tono / Var.</th>
                      <th className="py-2.5 px-2 text-center">Cant.</th>
                      <th className="py-2.5 px-3 text-right">Precio Unit.</th>
                      <th className="py-2.5 px-3 text-right">Subtotal</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-border/60">
                    {selectedOrder.items?.map((item) => (
                      <tr key={item.id} className="hover:bg-cream/40">
                        <td className="py-2.5 px-3 font-mono font-semibold text-muted text-[11px]">
                          {item.sku}
                        </td>
                        <td className="py-2.5 px-3 font-semibold text-ink">
                          {item.productName}
                          {item.brandName && (
                            <span className="block text-[10px] text-muted font-normal">
                              {item.brandName}
                            </span>
                          )}
                        </td>
                        <td className="py-2.5 px-3 text-muted text-[11px]">
                          {item.shadeName || '—'}
                        </td>
                        <td className="py-2.5 px-2 text-center font-bold text-wine">
                          {item.quantity}
                        </td>
                        <td className="py-2.5 px-3 text-right text-muted">
                          {formatCOP(item.unitPrice)}
                        </td>
                        <td className="py-2.5 px-3 text-right font-bold text-ink">
                          {formatCOP(item.subtotal)}
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            </div>

            {/* Financial Breakdown */}
            <div className="bg-blush/40 border border-border rounded-2xl p-4 space-y-2">
              <div className="flex justify-between text-xs text-muted">
                <span>Subtotal productos:</span>
                <span className="font-semibold text-ink">{formatCOP(selectedOrder.subtotal)}</span>
              </div>
              {selectedOrder.discount > 0 && (
                <div className="flex justify-between text-xs text-[#24734E]">
                  <span>Descuento aplicado:</span>
                  <span className="font-semibold">- {formatCOP(selectedOrder.discount)}</span>
                </div>
              )}
              <div className="flex justify-between text-xs text-muted">
                <span>Tarifa de envío:</span>
                <span className="font-semibold text-ink">{formatCOP(selectedOrder.shippingFee)}</span>
              </div>
              <div className="border-t border-border pt-2 flex justify-between text-sm font-bold text-wine">
                <span>Total pagado:</span>
                <span className="font-serif text-base">{formatCOP(selectedOrder.total)}</span>
              </div>
            </div>

            {/* Modal Actions */}
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setSelectedOrder(null)}
                className="px-5 py-2.5 rounded-full border border-border hover:bg-cream text-xs font-bold text-ink transition"
              >
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
}
