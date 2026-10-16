'use client';

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import { AdminShell } from '@/components/admin/AdminShell';
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
  Plus,
  Trash2,
  Store,
} from 'lucide-react';
import {
  api,
  SalesListResponseDto,
  SaleOrderDto,
  SalesSummaryDto,
  InventorySummaryDto,
  CreatePhysicalSaleRequestDto,
} from '@/lib/api';
import {
  formatCOP,
  getColombiaToday,
  getColombiaFirstDayOfMonth,
  getColombiaDaysAgo,
  formatColombiaDateTime,
} from '@/lib/utils';

// Fallback mock data in case backend is offline or empty during preview
const EMPTY_SALES_SUMMARY: SalesSummaryDto = { totalSales: 0, paidOrdersCount: 0, totalUnitsSold: 0, averageTicket: 0, startDate: '', endDate: '' };

const EMPTY_SALES_ORDERS: SaleOrderDto[] = [];

interface PosLineItem {
  variantId: string;
  quantity: number;
}

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
    summary: EMPTY_SALES_SUMMARY,
    orders: EMPTY_SALES_ORDERS,
    page: 0,
    size: pageSize,
    totalElements: EMPTY_SALES_ORDERS.length,
    totalPages: 1,
  });

  // Modal detail state
  const [selectedOrder, setSelectedOrder] = useState<SaleOrderDto | null>(null);

  // Physical Store Sale (POS) Modal states
  const [isPosModalOpen, setIsPosModalOpen] = useState<boolean>(false);
  const [loadingVariants, setLoadingVariants] = useState<boolean>(false);
  const [availableVariants, setAvailableVariants] = useState<InventorySummaryDto[]>([]);
  const [posItems, setPosItems] = useState<PosLineItem[]>([{ variantId: '', quantity: 1 }]);
  const [posPaymentMethod, setPosPaymentMethod] = useState<CreatePhysicalSaleRequestDto['paymentMethod']>('EFECTIVO');
  const [posTransferReference, setPosTransferReference] = useState<string>('');
  const [posDiscount, setPosDiscount] = useState<string>('');
  const [posCustomerName, setPosCustomerName] = useState<string>('');
  const [posCustomerPhone, setPosCustomerPhone] = useState<string>('');
  const [posNotes, setPosNotes] = useState<string>('');
  const [submittingPos, setSubmittingPos] = useState<boolean>(false);
  const [posError, setPosError] = useState<string | null>(null);

  // Fetch sales from API
  const fetchSales = useCallback(async () => {
    setLoading(true);
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
      setFeedbackMessage({ type: 'error', text: 'No pudimos consultar las ventas. No se mostrarán ventas de demostración.' });
      setSalesResponse({ summary: EMPTY_SALES_SUMMARY, orders: [], page: 0, size: pageSize, totalElements: 0, totalPages: 0 });
    } finally {
      setLoading(false);
    }
  }, [fromDate, toDate, paymentMethodFilter, page, pageSize]);

  useEffect(() => {
    fetchSales();
  }, [fetchSales]);

  const openPosModal = async () => {
    setPosError(null);
    setPosPaymentMethod('EFECTIVO');
    setPosTransferReference('');
    setPosDiscount('');
    setPosCustomerName('');
    setPosCustomerPhone('');
    setPosNotes('');
    setIsPosModalOpen(true);
    setLoadingVariants(true);
    try {
      const pageRes = await api.getInventorySummary({ size: 200 });
      const activeWithStock = (pageRes.content || []).filter((v) => v.active && v.availableStock > 0);
      setAvailableVariants(activeWithStock);
      if (activeWithStock.length > 0) {
        setPosItems([{ variantId: activeWithStock[0].variantId, quantity: 1 }]);
      } else {
        setPosItems([{ variantId: '', quantity: 1 }]);
      }
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al cargar inventario disponible.';
      setPosError(msg);
    } finally {
      setLoadingVariants(false);
    }
  };

  const posSubtotal = useMemo(() => {
    return posItems.reduce((acc, item) => {
      const variant = availableVariants.find((v) => v.variantId === item.variantId);
      const unitPrice = variant?.salePrice || 0;
      const qty = Math.max(0, Number(item.quantity) || 0);
      return acc + unitPrice * qty;
    }, 0);
  }, [posItems, availableVariants]);

  const posDiscountAmount = useMemo(() => {
    const parsed = Number(posDiscount);
    return Number.isFinite(parsed) && parsed > 0 ? parsed : 0;
  }, [posDiscount]);

  const posTotal = useMemo(() => {
    return Math.max(0, posSubtotal - posDiscountAmount);
  }, [posSubtotal, posDiscountAmount]);

  const handleAddPosLine = () => {
    const usedIds = new Set(posItems.map((i) => i.variantId));
    const nextAvailable = availableVariants.find((v) => !usedIds.has(v.variantId)) || availableVariants[0];
    setPosItems((prev) => [...prev, { variantId: nextAvailable ? nextAvailable.variantId : '', quantity: 1 }]);
  };

  const handleRemovePosLine = (index: number) => {
    setPosItems((prev) => (prev.length > 1 ? prev.filter((_, idx) => idx !== index) : prev));
  };

  const handlePosLineChange = (index: number, field: 'variantId' | 'quantity', value: string | number) => {
    setPosItems((prev) =>
      prev.map((line, idx) => {
        if (idx !== index) return line;
        if (field === 'variantId') {
          const newVar = availableVariants.find((v) => v.variantId === String(value));
          const maxAvail = newVar ? newVar.availableStock : 1;
          return {
            variantId: String(value),
            quantity: Math.min(Math.max(1, line.quantity), maxAvail),
          };
        }
        const currentVar = availableVariants.find((v) => v.variantId === line.variantId);
        const maxAvail = currentVar ? currentVar.availableStock : 999;
        const parsedQty = Math.max(1, Math.min(Number(value) || 1, maxAvail));
        return { ...line, quantity: parsedQty };
      })
    );
  };

  const handleSubmitPosSale = async (e: React.FormEvent) => {
    e.preventDefault();
    setPosError(null);

    const validItems = posItems.filter((item) => item.variantId && item.quantity >= 1);
    if (validItems.length === 0) {
      setPosError('Selecciona al menos un producto disponible con cantidad válida.');
      return;
    }

    // Aggregate quantities by variantId to validate against availableStock before submitting
    const requestedByVariant = new Map<string, number>();
    for (const item of validItems) {
      requestedByVariant.set(item.variantId, (requestedByVariant.get(item.variantId) || 0) + item.quantity);
    }

    for (const [variantId, totalQty] of requestedByVariant.entries()) {
      const variant = availableVariants.find((v) => v.variantId === variantId);
      if (!variant) {
        setPosError('Uno de los productos seleccionados ya no está disponible.');
        return;
      }
      if (totalQty > variant.availableStock) {
        setPosError(
          `La cantidad solicitada (${totalQty}) para "${variant.productName}" supera las unidades disponibles (${variant.availableStock}).`
        );
        return;
      }
    }

    if (posDiscountAmount > posSubtotal) {
      setPosError('El descuento no puede superar el subtotal de la venta.');
      return;
    }

    setSubmittingPos(true);
    try {
      const created = await api.createPhysicalSale({
        customerName: posCustomerName.trim() || undefined,
        customerPhone: posCustomerPhone.trim() || undefined,
        paymentMethod: posPaymentMethod,
        transferReference: posTransferReference.trim() || undefined,
        discount: posDiscountAmount > 0 ? posDiscountAmount : undefined,
        notes: posNotes.trim() || undefined,
        items: validItems.map((i) => ({
          variantId: i.variantId,
          quantity: Number(i.quantity),
        })),
      });

      setIsPosModalOpen(false);
      // Ensure 'toDate' includes today so the newly registered sale is immediately visible
      const today = getColombiaToday();
      if (toDate < today) {
        setToDate(today);
      }
      await fetchSales();
      setFeedbackMessage({
        type: 'success',
        text: `Venta en punto físico ${created.reference} registrada exitosamente (${created.paymentMethod}) por ${formatCOP(created.total)}. Stock descontado automáticamente.`,
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'No se pudo registrar la venta en punto físico.';
      setPosError(msg);
    } finally {
      setSubmittingPos(false);
    }
  };

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
    <AdminShell
      title="Ventas Confirmadas"
      badge="Reportes Comerciales"
      subtitle="Reporte oficial de pedidos pagados y verificados con impacto contable e inventario descontado."
      actions={
        <div className="flex flex-wrap items-center gap-2.5">
          <button
            type="button"
            onClick={openPosModal}
            className="inline-flex items-center gap-2 rounded-full bg-emerald-700 hover:bg-emerald-800 text-white font-bold text-xs sm:text-sm px-5 py-2.5 shadow-sm transition active:scale-95"
          >
            <Store className="w-4 h-4 stroke-[2.5]" />
            <span>Registrar Venta Física</span>
          </button>
          <button
            type="button"
            onClick={handleExportExcel}
            disabled={exporting}
            className="inline-flex items-center gap-2 rounded-full bg-wine hover:bg-wineDark disabled:opacity-50 text-white font-bold text-xs sm:text-sm px-5 py-2.5 shadow-glow transition active:scale-95"
          >
            {exporting ? (
              <RefreshCw className="w-4 h-4 animate-spin" />
            ) : (
              <FileSpreadsheet className="w-4 h-4 stroke-[2.5]" />
            )}
            <span>{exporting ? 'Generando Excel...' : 'Descargar Reporte Excel (.xlsx)'}</span>
          </button>
        </div>
      }
    >
      <div className="space-y-6">
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
                        <span
                          className={`inline-flex items-center px-2 py-0.5 rounded-md text-[10px] font-bold uppercase tracking-wider border ${
                            order.channel === 'PUNTO_FISICO'
                              ? 'bg-emerald-50 border-emerald-200 text-emerald-800'
                              : 'bg-cream border-border text-muted'
                          }`}
                        >
                          {order.channel === 'PUNTO_FISICO' ? 'Punto Físico' : order.channel}
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
      </div>

      {/* Registrar Venta en Punto Físico (POS) Modal */}
      {isPosModalOpen && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="pos-modal-title"
          className="fixed inset-0 z-50 bg-black/60 backdrop-blur-xs flex items-center justify-center p-4 sm:p-6"
        >
          <form
            onSubmit={handleSubmitPosSale}
            className="bg-white rounded-3xl max-w-3xl w-full max-h-[92vh] overflow-y-auto shadow-2xl border border-border p-6 sm:p-8 space-y-6 animate-in fade-in zoom-in-95 duration-200"
          >
            {/* Header */}
            <div className="flex items-start justify-between border-b border-border pb-4">
              <div>
                <span className="inline-flex items-center gap-1.5 text-[11px] font-bold bg-emerald-50 text-emerald-800 border border-emerald-200 px-2.5 py-0.5 rounded-full">
                  <Store className="w-3.5 h-3.5" /> Punto Físico · Mostrador
                </span>
                <h3 id="pos-modal-title" className="font-serif text-xl sm:text-2xl text-wine font-medium mt-1">
                  Registrar Venta en Punto Físico
                </h3>
                <p className="text-muted text-xs">
                  Selecciona los productos entregados al cliente y el método de pago recibido. El stock se descontará al instante.
                </p>
              </div>

              <button
                type="button"
                onClick={() => setIsPosModalOpen(false)}
                className="w-8 h-8 rounded-full bg-cream hover:bg-blush flex items-center justify-center text-muted hover:text-wine transition"
                aria-label="Cerrar modal de venta física"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            {posError && (
              <div role="alert" className="p-3.5 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-rose-600 flex-shrink-0" />
                <span>{posError}</span>
              </div>
            )}

            {/* Step 1: Products Selection */}
            <div className="space-y-3">
              <div className="flex items-center justify-between">
                <label className="text-xs font-bold uppercase tracking-wider text-wine">
                  1. Productos Vendidos ({posItems.length})
                </label>
                <button
                  type="button"
                  onClick={handleAddPosLine}
                  disabled={loadingVariants || availableVariants.length === 0}
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-full bg-blush/70 hover:bg-wine hover:text-white text-wine text-xs font-bold transition disabled:opacity-40"
                >
                  <Plus className="w-3.5 h-3.5" />
                  <span>Agregar otro producto</span>
                </button>
              </div>

              {loadingVariants ? (
                <div className="p-6 rounded-2xl bg-cream/50 border border-border text-center text-xs text-muted flex items-center justify-center gap-2">
                  <RefreshCw className="w-4 h-4 animate-spin text-wine" />
                  <span>Cargando productos con stock disponible...</span>
                </div>
              ) : availableVariants.length === 0 ? (
                <div className="p-6 rounded-2xl bg-amber-50 border border-amber-200 text-amber-900 text-xs text-center">
                  No hay productos activos con unidades disponibles en inventario para vender en este momento.
                </div>
              ) : (
                <div className="space-y-2.5">
                  {posItems.map((line, idx) => {
                    const selectedVar = availableVariants.find((v) => v.variantId === line.variantId);
                    const unitPrice = selectedVar?.salePrice || 0;
                    const maxStock = selectedVar?.availableStock || 1;
                    const lineSub = unitPrice * (Number(line.quantity) || 0);

                    return (
                      <div
                        key={idx}
                        className="grid grid-cols-1 sm:grid-cols-12 gap-3 items-center bg-cream/50 border border-border rounded-2xl p-3"
                      >
                        <div className="sm:col-span-7">
                          <label htmlFor={`pos-product-${idx}`} className="block text-[10px] font-bold uppercase text-muted mb-1">
                            Producto disponible
                          </label>
                          <select
                            id={`pos-product-${idx}`}
                            aria-label={`Producto ${idx + 1}`}
                            value={line.variantId}
                            onChange={(e) => handlePosLineChange(idx, 'variantId', e.target.value)}
                            className="w-full rounded-xl bg-white border border-border px-3 py-2 text-xs font-semibold text-ink focus:outline-hidden focus:border-wine"
                            required
                          >
                            {availableVariants.map((v) => (
                              <option key={v.variantId} value={v.variantId}>
                                {v.sku} — {v.productName}
                                {v.shadeName ? ` (${v.shadeName})` : ''} · {formatCOP(v.salePrice || 0)} ({v.availableStock} disp.)
                              </option>
                            ))}
                          </select>
                        </div>

                        <div className="sm:col-span-2">
                          <label htmlFor={`pos-qty-${idx}`} className="block text-[10px] font-bold uppercase text-muted mb-1">
                            Cantidad (máx {maxStock})
                          </label>
                          <input
                            id={`pos-qty-${idx}`}
                            aria-label={`Cantidad producto ${idx + 1}`}
                            type="number"
                            min={1}
                            max={maxStock}
                            value={line.quantity}
                            onChange={(e) => handlePosLineChange(idx, 'quantity', e.target.value)}
                            className="w-full rounded-xl bg-white border border-border px-3 py-2 text-xs font-bold text-wine text-center focus:outline-hidden focus:border-wine"
                            required
                          />
                        </div>

                        <div className="sm:col-span-2 text-right">
                          <span className="block text-[10px] font-bold uppercase text-muted mb-1">Subtotal</span>
                          <span className="text-xs font-bold text-ink">{formatCOP(lineSub)}</span>
                        </div>

                        <div className="sm:col-span-1 flex justify-end">
                          {posItems.length > 1 && (
                            <button
                              type="button"
                              onClick={() => handleRemovePosLine(idx)}
                              className="p-2 rounded-xl text-rose-600 hover:bg-rose-50 transition"
                              aria-label={`Quitar producto ${idx + 1}`}
                            >
                              <Trash2 className="w-4 h-4" />
                            </button>
                          )}
                        </div>
                      </div>
                    );
                  })}
                </div>
              )}
            </div>

            {/* Step 2: Payment Method & Discount */}
            <div className="space-y-3 border-t border-border pt-4">
              <label className="text-xs font-bold uppercase tracking-wider text-wine block">
                2. Método de Pago y Condiciones
              </label>

              <div className="grid grid-cols-2 sm:grid-cols-5 gap-2">
                {(['EFECTIVO', 'NEQUI', 'DAVIPLATA', 'BANCOLOMBIA', 'OTRO'] as const).map((method) => {
                  const isSelected = posPaymentMethod === method;
                  return (
                    <button
                      key={method}
                      type="button"
                      onClick={() => setPosPaymentMethod(method)}
                      aria-pressed={isSelected}
                      className={`py-2.5 px-3 rounded-xl border text-xs font-bold transition flex items-center justify-center gap-1.5 ${
                        isSelected
                          ? 'bg-wine text-white border-wine shadow-xs'
                          : 'bg-cream/60 text-ink border-border hover:border-wine/50'
                      }`}
                    >
                      <CreditCard className="w-3.5 h-3.5" />
                      <span>{method === 'EFECTIVO' ? 'Efectivo' : method === 'NEQUI' ? 'Nequi' : method === 'DAVIPLATA' ? 'Daviplata' : method === 'BANCOLOMBIA' ? 'Bancolombia' : 'Otro'}</span>
                    </button>
                  );
                })}
              </div>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4 pt-1">
                <div>
                  <label htmlFor="pos-transfer-ref" className="block text-xs font-semibold text-muted mb-1">
                    Referencia de pago / comprobante (opcional)
                  </label>
                  <input
                    id="pos-transfer-ref"
                    type="text"
                    placeholder="Ej. Comprobante Nequi o Recibo mostrador"
                    value={posTransferReference}
                    onChange={(e) => setPosTransferReference(e.target.value)}
                    className="w-full rounded-xl bg-cream/50 border border-border px-3.5 py-2 text-xs text-ink focus:outline-hidden focus:border-wine"
                  />
                </div>

                <div>
                  <label htmlFor="pos-discount" className="block text-xs font-semibold text-muted mb-1">
                    Descuento en COP (opcional)
                  </label>
                  <input
                    id="pos-discount"
                    type="number"
                    min={0}
                    step="100"
                    placeholder="0"
                    value={posDiscount}
                    onChange={(e) => setPosDiscount(e.target.value)}
                    className="w-full rounded-xl bg-cream/50 border border-border px-3.5 py-2 text-xs text-ink focus:outline-hidden focus:border-wine"
                  />
                </div>
              </div>
            </div>

            {/* Step 3: Optional Customer Details */}
            <div className="space-y-3 border-t border-border pt-4">
              <label className="text-xs font-bold uppercase tracking-wider text-wine block">
                3. Datos del Cliente (Opcional)
              </label>

              <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
                <div>
                  <label htmlFor="pos-customer-name" className="block text-xs font-semibold text-muted mb-1">
                    Nombre del cliente (opcional)
                  </label>
                  <input
                    id="pos-customer-name"
                    type="text"
                    placeholder="Cliente Punto Físico"
                    value={posCustomerName}
                    onChange={(e) => setPosCustomerName(e.target.value)}
                    className="w-full rounded-xl bg-cream/50 border border-border px-3.5 py-2 text-xs text-ink focus:outline-hidden focus:border-wine"
                  />
                </div>

                <div>
                  <label htmlFor="pos-customer-phone" className="block text-xs font-semibold text-muted mb-1">
                    Teléfono / WhatsApp (opcional)
                  </label>
                  <input
                    id="pos-customer-phone"
                    type="text"
                    placeholder="Mostrador"
                    value={posCustomerPhone}
                    onChange={(e) => setPosCustomerPhone(e.target.value)}
                    className="w-full rounded-xl bg-cream/50 border border-border px-3.5 py-2 text-xs text-ink focus:outline-hidden focus:border-wine"
                  />
                </div>
              </div>

              <div>
                <label htmlFor="pos-notes" className="block text-xs font-semibold text-muted mb-1">
                  Observaciones de la venta (opcional)
                </label>
                <input
                  id="pos-notes"
                  type="text"
                  placeholder="Ej. Venta entregada directamente en local..."
                  value={posNotes}
                  onChange={(e) => setPosNotes(e.target.value)}
                  className="w-full rounded-xl bg-cream/50 border border-border px-3.5 py-2 text-xs text-ink focus:outline-hidden focus:border-wine"
                />
              </div>
            </div>

            {/* Financial Summary */}
            <div className="bg-blush/40 border border-border rounded-2xl p-4 space-y-2">
              <div className="flex justify-between text-xs text-muted">
                <span>Subtotal artículos:</span>
                <span className="font-semibold text-ink">{formatCOP(posSubtotal)}</span>
              </div>
              {posDiscountAmount > 0 && (
                <div className="flex justify-between text-xs text-[#24734E]">
                  <span>Descuento aplicado:</span>
                  <span className="font-semibold">- {formatCOP(posDiscountAmount)}</span>
                </div>
              )}
              <div className="border-t border-border pt-2 flex justify-between items-center text-sm font-bold text-wine">
                <span>Total cobrado ({posPaymentMethod}):</span>
                <span className="font-serif text-lg">{formatCOP(posTotal)}</span>
              </div>
            </div>

            {/* Actions */}
            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                onClick={() => setIsPosModalOpen(false)}
                className="px-5 py-2.5 rounded-full border border-border hover:bg-cream text-xs font-bold text-ink transition"
              >
                Cancelar
              </button>
              <button
                type="submit"
                disabled={submittingPos || loadingVariants || availableVariants.length === 0}
                className="inline-flex items-center gap-2 px-6 py-2.5 rounded-full bg-emerald-700 hover:bg-emerald-800 disabled:opacity-50 text-white text-xs sm:text-sm font-bold shadow-sm transition"
              >
                {submittingPos ? (
                  <RefreshCw className="w-4 h-4 animate-spin" />
                ) : (
                  <CheckCircle2 className="w-4 h-4" />
                )}
                <span>{submittingPos ? 'Registrando venta...' : 'Confirmar Venta Física'}</span>
              </button>
            </div>
          </form>
        </div>
      )}

      {/* Sale Order Details Modal */}
      {selectedOrder && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="sale-detail-modal-title"
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
                <h3 id="sale-detail-modal-title" className="font-serif text-xl sm:text-2xl text-wine font-medium mt-1">
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
                  Canal de origen: <span className="font-semibold text-ink">{selectedOrder.channel === 'PUNTO_FISICO' ? 'Punto Físico' : selectedOrder.channel}</span>
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
    </AdminShell>
  );
}
