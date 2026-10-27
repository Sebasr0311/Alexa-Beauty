'use client';

import React, { useState, useEffect, useCallback } from 'react';
import {
  Boxes,
  Plus,
  Search,
  RefreshCw,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  FileSpreadsheet,
  ArrowDownRight,
  ArrowUpRight,
  SlidersHorizontal,
  X,
  Clock,
  ShieldAlert,
  Trash2,
  Loader2,
} from 'lucide-react';
import AdminShell from '@/components/admin/AdminShell';
import {
  api,
  InventorySummaryDto,
  InventoryMovementDto,
} from '@/lib/api';
import { formatCOP } from '@/lib/utils';

const EMPTY_SUMMARY: InventorySummaryDto[] = [];

const EMPTY_MOVEMENTS: InventoryMovementDto[] = [];

export default function AdminInventarioPage() {
  const [activeTab, setActiveTab] = useState<'EXISTENCIAS' | 'KARDEX'>('EXISTENCIAS');
  const [summaryList, setSummaryList] = useState<InventorySummaryDto[]>(EMPTY_SUMMARY);
  const [movementsList, setMovementsList] = useState<InventoryMovementDto[]>(EMPTY_MOVEMENTS);
  const [searchQuery, setSearchQuery] = useState('');
  const [lowStockFilter, setLowStockFilter] = useState(false);
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  // Modal Ajuste de Stock
  const [adjustModalOpen, setAdjustModalOpen] = useState(false);
  const [selectedVariantForAdjust, setSelectedVariantForAdjust] = useState<InventorySummaryDto | null>(null);
  const [newStockOnHand, setNewStockOnHand] = useState<number>(0);
  const [adjustReason, setAdjustReason] = useState('');
  const [adjusting, setAdjusting] = useState(false);

  // Modal Recepción de Mercancía
  const [receiptModalOpen, setReceiptModalOpen] = useState(false);
  const [receiptIdempotencyKey, setReceiptIdempotencyKey] = useState<string>('');
  const [receiptProvider, setReceiptProvider] = useState('');
  const [receiptInvoice, setReceiptInvoice] = useState('');
  const [receiptVariantId, setReceiptVariantId] = useState('');
  const [receiptQuantity, setReceiptQuantity] = useState<number>(10);
  const [receiptUnitCost, setReceiptUnitCost] = useState<number>(15000);
  const [receiving, setReceiving] = useState(false);

  // Modal Eliminar Producto con Doble Confirmación
  const [productToDelete, setProductToDelete] = useState<InventorySummaryDto | null>(null);
  const [deleteStep, setDeleteStep] = useState<1 | 2>(1);
  const [deletingProduct, setDeletingProduct] = useState(false);

  const [summaryPage, setSummaryPage] = useState<number>(0);
  const [summaryTotalPages, setSummaryTotalPages] = useState<number>(1);
  const [summaryTotalElements, setSummaryTotalElements] = useState<number>(0);

  useEffect(() => {
    setSummaryPage(0);
  }, [searchQuery, lowStockFilter]);

  const loadData = useCallback(async () => {
    setLoading(true);
    try {
      const [sumRes, movRes] = await Promise.allSettled([
        api.getInventorySummary({
          query: searchQuery || undefined,
          lowStockOnly: lowStockFilter || undefined,
          page: summaryPage,
          size: 20,
        }),
        api.getInventoryMovements({ size: 50 }),
      ]);

      if (sumRes.status === 'fulfilled') {
        setSummaryList(sumRes.value?.content || []);
        setSummaryTotalPages(sumRes.value?.totalPages || 1);
        setSummaryTotalElements(sumRes.value?.totalElements || 0);
      }
      if (movRes.status === 'fulfilled') {
        setMovementsList(movRes.value?.content || []);
      }
    } catch { setFeedback({ type: "error", text: "No se pudo confirmar la operación de inventario." }); } finally {
      setLoading(false);
    }
  }, [searchQuery, lowStockFilter, summaryPage]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Totales consolidados
  const totalOnHand = summaryList.reduce((acc, curr) => acc + curr.stockOnHand, 0);
  const totalReserved = summaryList.reduce((acc, curr) => acc + curr.stockReserved, 0);
  const totalAvailable = summaryList.reduce((acc, curr) => acc + curr.availableStock, 0);
  const lowStockCount = summaryList.filter((s) => s.isLowStock || s.availableStock <= s.minStock).length;
  const totalInvestmentCost = summaryList.reduce(
    (acc, curr) => acc + curr.stockOnHand * (curr.referenceCost || 0),
    0
  );
  const totalRetailValue = summaryList.reduce(
    (acc, curr) => acc + curr.stockOnHand * (curr.salePrice || 0),
    0
  );

  const handleExportExcel = async () => {
    setExporting(true);
    setFeedback(null);
    try {
      await api.downloadAdminInventoryExcel();
      setFeedback({
        type: 'success',
        text: 'Reporte oficial de inventario y productos descargado correctamente en Excel (.xlsx).',
      });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al exportar inventario';
      setFeedback({
        type: 'error',
        text: `No se pudo descargar el archivo Excel: ${msg}`,
      });
    } finally {
      setExporting(false);
    }
  };

  const handleOpenAdjust = (variant: InventorySummaryDto) => {
    setSelectedVariantForAdjust(variant);
    setNewStockOnHand(variant.stockOnHand);
    setAdjustReason('');
    setAdjustModalOpen(true);
  };

  const handleSaveAdjustment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedVariantForAdjust) return;

    if (newStockOnHand < selectedVariantForAdjust.stockReserved) {
      setFeedback({
        type: 'error',
        text: `El nuevo stock físico (${newStockOnHand}) no puede ser menor a las unidades reservadas (${selectedVariantForAdjust.stockReserved}).`,
      });
      return;
    }

    setAdjusting(true);
    setFeedback(null);

    try {
      await api.adjustStock({
        variantId: selectedVariantForAdjust.variantId,
        newStockOnHand,
        reason: adjustReason.trim() || undefined,
      });
      setFeedback({
        type: 'success',
        text: `Ajuste registrado exitosamente para ${selectedVariantForAdjust.sku}.`,
      });
      setAdjustModalOpen(false);
      await loadData();
    } catch (err: any) { setFeedback({ type: "error", text: "No se pudo confirmar la operación de inventario." }); } finally {
      setAdjusting(false);
    }
  };

  const handleSaveReceipt = async (e: React.FormEvent) => {
    e.preventDefault();
    const targetVariantId = receiptVariantId || summaryList[0]?.variantId;
    if (!targetVariantId || receiptQuantity <= 0) {
      setFeedback({ type: 'error', text: 'Selecciona una variante y cantidad válida mayor a 0.' });
      return;
    }

    setReceiving(true);
    setFeedback(null);

    const idempotencyKey = receiptIdempotencyKey || crypto.randomUUID();
    if (!receiptIdempotencyKey) {
      setReceiptIdempotencyKey(idempotencyKey);
    }

    try {
      await api.createStockReceipt({
        providerName: receiptProvider.trim() || 'Proveedor General',
        invoiceNumber: receiptInvoice.trim() || undefined,
        items: [
          {
            variantId: targetVariantId,
            quantity: receiptQuantity,
            unitCost: receiptUnitCost > 0 ? receiptUnitCost : undefined,
          },
        ],
      }, idempotencyKey);
      setFeedback({ type: 'success', text: `Recepción de mercancía registrada exitosamente (+${receiptQuantity} unidades).` });
      setReceiptModalOpen(false);
      setReceiptIdempotencyKey('');
      await loadData();
    } catch { setFeedback({ type: "error", text: "No se pudo confirmar la operación de inventario." }); } finally {
      setReceiving(false);
    }
  };

  const handleOpenDelete = (item: InventorySummaryDto) => {
    setProductToDelete(item);
    setDeleteStep(1);
  };

  const handleConfirmDelete = async () => {
    if (!productToDelete) return;
    setDeletingProduct(true);
    try {
      await api.deleteProduct(productToDelete.productId);
      setSummaryList((prev) => prev.filter((s) => s.productId !== productToDelete.productId));
      setFeedback({
        type: 'success',
        text: `El producto "${productToDelete.productName}" fue eliminado del catálogo y del inventario.`,
      });
      setProductToDelete(null);
      setDeleteStep(1);
      await loadData();
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'No se pudo eliminar el producto.';
      setFeedback({
        type: 'error',
        text: msg,
      });
    } finally {
      setDeletingProduct(false);
    }
  };

  const filteredSummary = summaryList.filter((s) => {
    const matchesSearch =
      s.productName.toLowerCase().includes(searchQuery.toLowerCase()) ||
      s.sku.toLowerCase().includes(searchQuery.toLowerCase()) ||
      (s.shadeName && s.shadeName.toLowerCase().includes(searchQuery.toLowerCase()));
    const matchesLowStock = !lowStockFilter || s.isLowStock || s.availableStock <= s.minStock;
    return matchesSearch && matchesLowStock;
  });

  return (
    <AdminShell
      badge="Epic 3 · Inventario Transaccional"
      title="Control de Inventario y Kardex"
      subtitle="Ecuación garantizada: disponible = físico - reservado. Trazabilidad inmutable por movimiento."
      actions={
        <>
          <button
            type="button"
            onClick={handleExportExcel}
            disabled={exporting}
            className="inline-flex items-center gap-2 rounded-full border border-wine/30 bg-wine/5 hover:bg-wine hover:text-white text-wine font-bold text-xs sm:text-sm px-5 py-2.5 transition active:scale-95 disabled:opacity-50"
          >
            {exporting ? (
              <Loader2 className="w-4 h-4 animate-spin" />
            ) : (
              <FileSpreadsheet className="w-4 h-4" />
            )}
            <span>{exporting ? 'Generando Excel...' : 'Exportar a Excel (.xlsx)'}</span>
          </button>
          <button
            type="button"
            onClick={() => {
              setReceiptIdempotencyKey(crypto.randomUUID());
              setReceiptModalOpen(true);
            }}
            className="inline-flex items-center gap-2 rounded-full bg-wine hover:bg-wineDark text-white font-bold text-xs sm:text-sm px-5 py-2.5 shadow-glow transition active:scale-95"
          >
            <Plus className="w-4 h-4" />
            <span>Recepción de Mercancía</span>
          </button>
        </>
      }
    >
      {/* Feedback Alert */}
      {feedback && (
        <div
          role="status"
          className={`p-4 rounded-2xl flex items-center justify-between gap-3 text-sm font-medium ${
            feedback.type === 'success'
              ? 'bg-[#EBF7F0] border border-[#BCE8CD] text-[#1E7444]'
              : 'bg-[#FDF2F2] border border-[#F9C5C5] text-[#9E2A2B]'
          }`}
        >
          <div className="flex items-center gap-2.5">
            {feedback.type === 'success' ? (
              <CheckCircle2 className="w-5 h-5 flex-shrink-0" />
            ) : (
              <AlertTriangle className="w-5 h-5 flex-shrink-0" />
            )}
            <span>{feedback.text}</span>
          </div>
          <button
            type="button"
            onClick={() => setFeedback(null)}
            className="text-muted hover:text-ink p-1"
          >
            <X className="w-4 h-4" />
          </button>
        </div>
      )}

      {/* 6 Tarjetas KPI de Inventario */}
      <div className="grid grid-cols-2 md:grid-cols-3 lg:grid-cols-6 gap-3 sm:gap-4">
        <div className="bg-white border border-border rounded-2xl p-4 shadow-xs">
          <span className="text-xs text-muted font-medium block">Stock Físico (On Hand)</span>
          <p className="font-serif text-xl sm:text-2xl font-bold text-wine my-1.5 tracking-tight">
            {totalOnHand} <span className="text-xs font-sans font-normal text-muted">un.</span>
          </p>
          <span className="text-[11px] text-muted font-medium">En bodega / anaquel</span>
        </div>

        <div className="bg-white border border-border rounded-2xl p-4 shadow-xs">
          <span className="text-xs text-muted font-medium block">Stock Reservado</span>
          <p className="font-serif text-xl sm:text-2xl font-bold text-amber-600 my-1.5 tracking-tight">
            {totalReserved} <span className="text-xs font-sans font-normal text-muted">un.</span>
          </p>
          <span className="text-[11px] text-amber-700 font-medium">En espera de pago</span>
        </div>

        <div className="bg-white border border-border rounded-2xl p-4 shadow-xs">
          <span className="text-xs text-muted font-medium block">Stock Disponible</span>
          <p className="font-serif text-xl sm:text-2xl font-bold text-emerald-700 my-1.5 tracking-tight">
            {totalAvailable} <span className="text-xs font-sans font-normal text-muted">un.</span>
          </p>
          <span className="text-[11px] text-emerald-700 font-medium">Listo para venta</span>
        </div>

        <div className="bg-white border border-border rounded-2xl p-4 shadow-xs">
          <span className="text-xs text-muted font-medium block">Alertas Stock Bajo</span>
          <p className="font-serif text-xl sm:text-2xl font-bold text-rose-600 my-1.5 tracking-tight">
            {lowStockCount} <span className="text-xs font-sans font-normal text-muted">var.</span>
          </p>
          <span className="text-[11px] text-rose-600 font-medium">Bajo mínimo</span>
        </div>

        <div className="bg-white border border-border rounded-2xl p-4 shadow-xs">
          <span className="text-xs text-muted font-medium block">Inversión en Stock</span>
          <p className="font-serif text-xl sm:text-2xl font-bold text-ink my-1.5 tracking-tight">
            {formatCOP(totalInvestmentCost)}
          </p>
          <span className="text-[11px] text-muted font-medium">Costo de adquisición</span>
        </div>

        <div className="bg-white border border-border rounded-2xl p-4 shadow-xs">
          <span className="text-xs text-muted font-medium block">Valor Venta Stock</span>
          <p className="font-serif text-xl sm:text-2xl font-bold text-wine my-1.5 tracking-tight">
            {formatCOP(totalRetailValue)}
          </p>
          <span className="text-[11px] text-muted font-medium">Estimado comercial</span>
        </div>
      </div>

      {/* Selector de Pestañas: Existencias vs Kardex */}
      <div className="flex items-center gap-2 border-b border-border pb-1">
        <button
          type="button"
          onClick={() => setActiveTab('EXISTENCIAS')}
          className={`px-5 py-2.5 font-bold text-xs sm:text-sm rounded-t-xl transition border-b-2 ${
            activeTab === 'EXISTENCIAS'
              ? 'border-wine text-wine bg-white'
              : 'border-transparent text-muted hover:text-ink'
          }`}
        >
          Existencias por Producto ({filteredSummary.length})
        </button>
        <button
          type="button"
          onClick={() => setActiveTab('KARDEX')}
          className={`px-5 py-2.5 font-bold text-xs sm:text-sm rounded-t-xl transition border-b-2 ${
            activeTab === 'KARDEX'
              ? 'border-wine text-wine bg-white'
              : 'border-transparent text-muted hover:text-ink'
          }`}
        >
          Libro Kardex de Movimientos ({movementsList.length})
        </button>
      </div>

      {activeTab === 'EXISTENCIAS' ? (
        <div className="space-y-4">
          {/* Barra de Filtros de Existencias */}
          <div className="bg-white border border-border rounded-2xl p-4 shadow-sm flex flex-col md:flex-row items-center justify-between gap-3">
            <div className="relative w-full md:w-80">
              <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-muted" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Buscar por producto, tono o SKU..."
                className="w-full pl-9 pr-4 py-2 bg-cream/50 border border-border rounded-xl text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-wine/20 text-ink"
              />
            </div>

            <div className="flex items-center gap-2.5 self-start md:self-auto">
              <button
                type="button"
                onClick={() => setLowStockFilter(!lowStockFilter)}
                className={`inline-flex items-center gap-1.5 px-3 py-1.5 rounded-xl text-xs font-bold transition ${
                  lowStockFilter
                    ? 'bg-rose-100 text-rose-800 border border-rose-300'
                    : 'bg-cream text-muted hover:text-ink border border-border'
                }`}
              >
                <AlertTriangle className="w-3.5 h-3.5" />
                <span>Solo Stock Bajo</span>
              </button>

              <button
                type="button"
                onClick={loadData}
                title="Actualizar inventario"
                className="p-2 text-muted hover:text-wine rounded-xl bg-cream hover:bg-blush transition"
              >
                <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
              </button>
            </div>
          </div>

          {/* Tabla de Existencias */}
          <div className="bg-white border border-border rounded-3xl p-6 shadow-soft overflow-hidden">
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs sm:text-sm border-collapse">
                <thead>
                  <tr className="bg-blush text-wine font-bold border-b border-border">
                    <th className="py-3 px-3.5 rounded-l-xl">SKU &amp; Producto</th>
                    <th className="py-3 px-3.5">Tono / Presentación</th>
                    <th className="py-3 px-3.5 text-center">Físico</th>
                    <th className="py-3 px-3.5 text-center">Reservado</th>
                    <th className="py-3 px-3.5 text-center">Disponible</th>
                    <th className="py-3 px-3.5 text-right">Inversión (Costo)</th>
                    <th className="py-3 px-3.5 text-right">Precio Venta</th>
                    <th className="py-3 px-3.5 text-right">Inversión Total</th>
                    <th className="py-3 px-3.5 text-center">Mínimo</th>
                    <th className="py-3 px-3.5 text-right rounded-r-xl">Acciones</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-border/60">
                  {filteredSummary.length === 0 ? (
                    <tr>
                      <td colSpan={9} className="py-12 text-center text-muted">
                        <Boxes className="w-8 h-8 mx-auto mb-2 text-mauve/60" />
                        <p className="font-medium">No se encontraron existencias con este filtro.</p>
                      </td>
                    </tr>
                  ) : (
                    filteredSummary.map((s) => (
                      <tr key={s.variantId} className="hover:bg-cream/40 transition">
                        <td className="py-3.5 px-3.5">
                          <div>
                            <span className="font-mono text-xs font-bold text-wine block">
                              {s.sku}
                            </span>
                            <span className="font-semibold text-ink leading-tight">
                              {s.productName}
                            </span>
                            <span className="text-[11px] text-muted block">
                              {s.brandName || 'Alexa Beauty'} · {s.categoryName || 'General'}
                            </span>
                          </div>
                        </td>
                        <td className="py-3.5 px-3.5 text-muted font-medium">
                          {s.shadeName || 'Default'}
                        </td>
                        <td className="py-3.5 px-3.5 text-center font-bold text-ink">
                          {s.stockOnHand}
                        </td>
                        <td className="py-3.5 px-3.5 text-center font-bold text-amber-600">
                          {s.stockReserved}
                        </td>
                        <td className="py-3.5 px-3.5 text-center">
                          <span
                            className={`inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-bold ${
                              s.availableStock <= s.minStock
                                ? 'bg-rose-100 text-rose-800 border border-rose-200'
                                : 'bg-emerald-100 text-emerald-800 border border-emerald-200'
                            }`}
                          >
                            {s.availableStock} un.
                          </span>
                        </td>
                        <td className="py-3.5 px-3.5 text-right font-medium text-ink">
                          {s.referenceCost != null && s.referenceCost > 0 ? (
                            formatCOP(s.referenceCost)
                          ) : (
                            <span className="text-muted text-xs italic">Sin costo</span>
                          )}
                        </td>
                        <td className="py-3.5 px-3.5 text-right font-medium text-ink">
                          {s.salePrice != null && s.salePrice > 0 ? (
                            formatCOP(s.salePrice)
                          ) : (
                            <span className="text-muted text-xs italic">N/A</span>
                          )}
                        </td>
                        <td className="py-3.5 px-3.5 text-right font-bold text-wine">
                          {s.referenceCost != null && s.referenceCost > 0 ? (
                            formatCOP(s.stockOnHand * s.referenceCost)
                          ) : (
                            <span className="text-muted text-xs italic">-</span>
                          )}
                        </td>
                        <td className="py-3.5 px-3.5 text-center text-muted">
                          {s.minStock} un.
                        </td>
                        <td className="py-3.5 px-3.5 text-right whitespace-nowrap">
                          <div className="flex items-center justify-end gap-1.5">
                            <button
                              type="button"
                              onClick={() => handleOpenAdjust(s)}
                              className="inline-flex items-center gap-1 text-xs font-bold text-wine hover:text-wineDark bg-blush/60 hover:bg-blush px-3 py-1.5 rounded-full transition"
                              title="Ajustar stock físico"
                            >
                              <SlidersHorizontal className="w-3.5 h-3.5" />
                              <span>Ajustar</span>
                            </button>
                            <button
                              type="button"
                              onClick={() => handleOpenDelete(s)}
                              className="inline-flex items-center p-1.5 rounded-full text-xs text-rose-600 hover:bg-rose-50 transition"
                              title="Eliminar producto del inventario"
                            >
                              <Trash2 className="w-3.5 h-3.5" />
                            </button>
                          </div>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>

            {/* Pagination Controls */}
            <div className="flex flex-col sm:flex-row items-center justify-between gap-3 border-t border-border pt-4 mt-4">
              <span className="text-xs text-muted">
                Página {summaryPage + 1} de {Math.max(1, summaryTotalPages)} ({summaryTotalElements} variantes en total)
              </span>
              <div className="flex items-center gap-2">
                <button
                  type="button"
                  disabled={summaryPage === 0 || loading}
                  onClick={() => setSummaryPage((p) => Math.max(0, p - 1))}
                  className="px-3 py-1.5 rounded-lg border border-border text-xs font-semibold text-ink hover:bg-cream disabled:opacity-40 disabled:cursor-not-allowed transition"
                >
                  Anterior
                </button>
                <button
                  type="button"
                  disabled={summaryPage >= summaryTotalPages - 1 || loading}
                  onClick={() => setSummaryPage((p) => p + 1)}
                  className="px-3 py-1.5 rounded-lg border border-border text-xs font-semibold text-ink hover:bg-cream disabled:opacity-40 disabled:cursor-not-allowed transition"
                >
                  Siguiente
                </button>
              </div>
            </div>
          </div>
        </div>
      ) : (
        /* Pestaña Kardex de Movimientos */
        <div className="bg-white border border-border rounded-3xl p-6 shadow-soft overflow-hidden space-y-4">
          <div className="flex items-center justify-between border-b border-border pb-3">
            <div>
              <h3 className="font-serif text-lg font-bold text-wine">
                Libro Mayor de Movimientos (Kardex)
              </h3>
              <p className="text-xs text-muted">Registro auditable inmutable de entradas, salidas, ventas y ajustes.</p>
            </div>
            <button
              type="button"
              onClick={loadData}
              className="p-2 text-muted hover:text-wine rounded-xl bg-cream hover:bg-blush transition"
            >
              <RefreshCw className={`w-4 h-4 ${loading ? 'animate-spin' : ''}`} />
            </button>
          </div>

          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs sm:text-sm border-collapse">
              <thead>
                <tr className="bg-blush text-wine font-bold border-b border-border">
                  <th className="py-3 px-3.5 rounded-l-xl">Fecha</th>
                  <th className="py-3 px-3.5">Tipo</th>
                  <th className="py-3 px-3.5">SKU &amp; Producto</th>
                  <th className="py-3 px-3.5 text-center">Delta</th>
                  <th className="py-3 px-3.5 text-center">Antes ➔ Después</th>
                  <th className="py-3 px-3.5">Referencia / Motivo</th>
                  <th className="py-3 px-3.5 rounded-r-xl">Operador</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/60">
                {movementsList.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="py-12 text-center text-muted">
                      No hay movimientos registrados.
                    </td>
                  </tr>
                ) : (
                  movementsList.map((m) => (
                    <tr key={m.id} className="hover:bg-cream/40 transition">
                      <td className="py-3.5 px-3.5 text-muted whitespace-nowrap">
                        {new Date(m.createdAt).toLocaleString('es-CO', {
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </td>
                      <td className="py-3.5 px-3.5">
                        <span
                          className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                            m.movementType === 'ENTRADA' || m.movementType === 'ENTRADA_INICIAL'
                              ? 'bg-emerald-100 text-emerald-800'
                              : m.movementType === 'VENTA'
                              ? 'bg-purple-100 text-purple-800'
                              : 'bg-blue-100 text-blue-800'
                          }`}
                        >
                          {m.quantityDelta > 0 ? (
                            <ArrowUpRight className="w-3 h-3" />
                          ) : (
                            <ArrowDownRight className="w-3 h-3" />
                          )}
                          {m.movementType}
                        </span>
                      </td>
                      <td className="py-3.5 px-3.5">
                        <span className="font-mono text-xs font-bold text-wine block">
                          {m.sku}
                        </span>
                        <span className="text-ink font-medium leading-tight">
                          {m.productName}
                        </span>
                      </td>
                      <td className="py-3.5 px-3.5 text-center font-bold font-mono">
                        <span
                          className={m.quantityDelta > 0 ? 'text-emerald-700' : 'text-rose-600'}
                        >
                          {m.quantityDelta > 0 ? `+${m.quantityDelta}` : m.quantityDelta}
                        </span>
                      </td>
                      <td className="py-3.5 px-3.5 text-center font-mono text-muted text-xs">
                        {m.qtyBefore} ➔ <strong className="text-ink">{m.qtyAfter}</strong>
                      </td>
                      <td className="py-3.5 px-3.5">
                        <span className="font-semibold text-ink block text-xs">
                          {m.reference || 'N/A'}
                        </span>
                        <span className="text-[11px] text-muted">{m.reason || 'Sin motivo'}</span>
                      </td>
                      <td className="py-3.5 px-3.5 text-muted text-xs">
                        {m.actorEmail || 'Sistema'}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal Ajuste Manual de Stock */}
      {adjustModalOpen && selectedVariantForAdjust && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="adjust-modal-title"
          className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4"
        >
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl space-y-5 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <div className="flex items-center gap-2 text-wine">
                <SlidersHorizontal className="w-5 h-5" />
                <h3 id="adjust-modal-title" className="font-serif text-xl font-bold">Ajuste de Stock Auditado</h3>
              </div>
              <button
                type="button"
                onClick={() => setAdjustModalOpen(false)}
                className="text-muted hover:text-ink p-1 rounded-full hover:bg-cream"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <div className="bg-cream/60 border border-border rounded-2xl p-4 text-xs space-y-1.5">
              <p>
                <strong>Producto:</strong> {selectedVariantForAdjust.productName}
              </p>
              <p>
                <strong>SKU:</strong> <span className="font-mono">{selectedVariantForAdjust.sku}</span>
              </p>
              <div className="flex items-center justify-between pt-1 border-t border-border/60">
                <span>Stock Físico actual: <strong>{selectedVariantForAdjust.stockOnHand}</strong></span>
                <span className="text-amber-700">Reservado: <strong>{selectedVariantForAdjust.stockReserved}</strong></span>
              </div>
            </div>

            <form onSubmit={handleSaveAdjustment} className="space-y-4 text-xs sm:text-sm">
              <div>
                <label className="font-bold text-ink block mb-1">
                  Nuevo Stock Físico (On Hand) *
                </label>
                <input
                  type="number"
                  required
                  min={selectedVariantForAdjust.stockReserved}
                  value={newStockOnHand}
                  onChange={(e) => setNewStockOnHand(Number(e.target.value))}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-border bg-cream/40 focus:ring-2 focus:ring-wine/20 focus:outline-none font-bold text-lg"
                />
                <span className="text-[11px] text-muted mt-1 block">
                  Invariante: No puede ser inferior a las unidades reservadas ({selectedVariantForAdjust.stockReserved}).
                </span>
              </div>

              <div>
                <label className="font-bold text-ink block mb-1">
                  Motivo del ajuste (opcional)
                </label>
                <textarea
                  rows={2}
                  value={adjustReason}
                  onChange={(e) => setAdjustReason(e.target.value)}
                  placeholder="Ej. Conteo físico de inventario, merma por daño, muestra..."
                  className="w-full px-3.5 py-2.5 rounded-xl border border-border bg-cream/40 focus:ring-2 focus:ring-wine/20 focus:outline-none"
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-border">
                <button
                  type="button"
                  onClick={() => setAdjustModalOpen(false)}
                  className="px-4 py-2 rounded-full border border-border text-muted font-bold hover:bg-cream transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={adjusting}
                  className="px-6 py-2 rounded-full bg-wine hover:bg-wineDark text-white font-bold transition shadow-glow disabled:opacity-50"
                >
                  {adjusting ? 'Aplicando...' : 'Confirmar Ajuste'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Recepción de Mercancía */}
      {receiptModalOpen && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="receipt-modal-title"
          className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4"
        >
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl space-y-5 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <div className="flex items-center gap-2 text-wine">
                <Boxes className="w-5 h-5" />
                <h3 id="receipt-modal-title" className="font-serif text-xl font-bold">Recepción de Mercancía</h3>
              </div>
              <button
                type="button"
                onClick={() => setReceiptModalOpen(false)}
                className="text-muted hover:text-ink p-1 rounded-full hover:bg-cream"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleSaveReceipt} className="space-y-4 text-xs sm:text-sm">
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-bold text-ink block mb-1">Proveedor *</label>
                  <input
                    type="text"
                    required
                    value={receiptProvider}
                    onChange={(e) => setReceiptProvider(e.target.value)}
                    placeholder="Ej. Bella Cosmetics S.A."
                    className="w-full px-3.5 py-2.5 rounded-xl border border-border bg-cream/40 focus:ring-2 focus:ring-wine/20 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="font-bold text-ink block mb-1">Nº Factura / Remisión</label>
                  <input
                    type="text"
                    value={receiptInvoice}
                    onChange={(e) => setReceiptInvoice(e.target.value)}
                    placeholder="Ej. F-2026-991"
                    className="w-full px-3.5 py-2.5 rounded-xl border border-border bg-cream/40 focus:ring-2 focus:ring-wine/20 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="font-bold text-ink block mb-1">Producto / Variante a ingresar *</label>
                <select
                  value={receiptVariantId || summaryList[0]?.variantId}
                  onChange={(e) => setReceiptVariantId(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-border bg-cream/40 focus:ring-2 focus:ring-wine/20 focus:outline-none font-medium"
                >
                  {summaryList.map((s) => (
                    <option key={s.variantId} value={s.variantId}>
                      {s.sku} — {s.productName} ({s.shadeName || 'Default'}) [Stock actual: {s.stockOnHand}]
                    </option>
                  ))}
                </select>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-bold text-ink block mb-1">Cantidad a ingresar *</label>
                  <input
                    type="number"
                    required
                    min={1}
                    value={receiptQuantity}
                    onChange={(e) => setReceiptQuantity(Number(e.target.value))}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-border bg-cream/40 font-bold focus:ring-2 focus:ring-wine/20 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="font-bold text-ink block mb-1">Costo Unitario (COP)</label>
                  <input
                    type="number"
                    min={0}
                    step={500}
                    value={receiptUnitCost}
                    onChange={(e) => setReceiptUnitCost(Number(e.target.value))}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-border bg-cream/40 focus:ring-2 focus:ring-wine/20 focus:outline-none"
                  />
                </div>
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-border">
                <button
                  type="button"
                  onClick={() => setReceiptModalOpen(false)}
                  className="px-4 py-2 rounded-full border border-border text-muted font-bold hover:bg-cream transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={receiving}
                  className="px-6 py-2 rounded-full bg-wine hover:bg-wineDark text-white font-bold transition shadow-glow disabled:opacity-50"
                >
                  {receiving ? 'Guardando...' : 'Registrar Entrada'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: ELIMINAR PRODUCTO CON DOBLE CONFIRMACIÓN */}
      {productToDelete && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-border space-y-5 animate-in fade-in zoom-in-95 duration-150">
            {/* Step 1: Primera advertencia */}
            {deleteStep === 1 ? (
              <>
                <div className="flex items-center justify-between border-b border-border pb-3">
                  <div className="flex items-center gap-2.5">
                    <div className="w-9 h-9 rounded-full bg-amber-100 text-amber-700 flex items-center justify-center shrink-0">
                      <AlertTriangle className="w-5 h-5" />
                    </div>
                    <div>
                      <span className="text-[10px] font-extrabold uppercase tracking-widest text-amber-700">
                        Paso 1 de 2 · Advertencia
                      </span>
                      <h2 className="font-serif text-xl text-wineDark font-normal">
                        ¿Dar de baja producto?
                      </h2>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setProductToDelete(null);
                      setDeleteStep(1);
                    }}
                    className="text-muted hover:text-ink p-1 rounded-full hover:bg-cream"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                <div className="space-y-3 text-xs text-muted">
                  <div className="bg-amber-50/70 border border-amber-200 rounded-2xl p-4 space-y-1.5 text-amber-900">
                    <p className="font-bold text-sm text-ink">{productToDelete.productName}</p>
                    <p className="font-mono text-[11px] text-muted">SKU: {productToDelete.sku}</p>
                    <div className="flex items-center gap-3 pt-1 text-xs">
                      <span>Stock físico: <strong>{productToDelete.stockOnHand} un.</strong></span>
                      <span>Reservado: <strong>{productToDelete.stockReserved} un.</strong></span>
                    </div>
                  </div>
                  <p>
                    Estás a punto de solicitar la eliminación de este producto en el sistema de inventario y catálogo.
                  </p>
                  <p className="text-[11px] text-muted/90 italic">
                    * Si el producto cuenta con historial de pedidos o recepciones, se desactivará de forma segura para no romper la auditoría de ventas.
                  </p>
                </div>

                <div className="pt-2 flex items-center justify-end gap-3 border-t border-border">
                  <button
                    type="button"
                    onClick={() => {
                      setProductToDelete(null);
                      setDeleteStep(1);
                    }}
                    className="px-4 py-2 rounded-full text-xs font-semibold text-muted hover:text-ink"
                  >
                    Cancelar
                  </button>
                  <button
                    type="button"
                    onClick={() => setDeleteStep(2)}
                    className="px-5 py-2 rounded-full text-xs font-bold bg-amber-600 hover:bg-amber-700 text-white shadow-xs inline-flex items-center gap-1.5"
                  >
                    <span>Continuar al Paso 2</span>
                    <ArrowDownRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </>
            ) : (
              /* Step 2: Confirmación definitiva */
              <>
                <div className="flex items-center justify-between border-b border-border pb-3">
                  <div className="flex items-center gap-2.5">
                    <div className="w-9 h-9 rounded-full bg-rose-100 text-rose-700 flex items-center justify-center shrink-0">
                      <ShieldAlert className="w-5 h-5" />
                    </div>
                    <div>
                      <span className="text-[10px] font-extrabold uppercase tracking-widest text-rose-700">
                        Paso 2 de 2 · Confirmación Final
                      </span>
                      <h2 className="font-serif text-xl text-wineDark font-normal">
                        ¿Confirmar eliminación?
                      </h2>
                    </div>
                  </div>
                  <button
                    type="button"
                    onClick={() => {
                      setProductToDelete(null);
                      setDeleteStep(1);
                    }}
                    className="text-muted hover:text-ink p-1 rounded-full hover:bg-cream"
                  >
                    <X className="w-5 h-5" />
                  </button>
                </div>

                <div className="space-y-3 text-xs text-muted">
                  <div className="bg-rose-50 border border-rose-200 rounded-2xl p-4 text-rose-900 space-y-1">
                    <p className="font-bold text-sm">Esta acción dará de baja el producto:</p>
                    <p className="font-serif text-base text-wine font-semibold">
                      {productToDelete.productName}
                    </p>
                    <p className="text-[11px] text-rose-700 pt-1">
                      Se removerá de la lista de existencias activas y no estará disponible para venta pública.
                    </p>
                  </div>
                  <p className="font-medium text-ink">
                    ¿Estás totalmente seguro de proceder con la eliminación definitiva?
                  </p>
                </div>

                <div className="pt-2 flex items-center justify-between gap-3 border-t border-border">
                  <button
                    type="button"
                    onClick={() => setDeleteStep(1)}
                    className="px-4 py-2 rounded-full text-xs font-semibold text-muted hover:text-ink"
                  >
                    ← Volver al Paso 1
                  </button>
                  <button
                    type="button"
                    disabled={deletingProduct}
                    onClick={handleConfirmDelete}
                    className="px-5 py-2.5 rounded-full text-xs font-bold bg-rose-600 hover:bg-rose-700 text-white shadow-xs inline-flex items-center gap-1.5 disabled:opacity-50"
                  >
                    {deletingProduct ? (
                      <>
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                        <span>Eliminando...</span>
                      </>
                    ) : (
                      <>
                        <Trash2 className="w-3.5 h-3.5" />
                        <span>Sí, Eliminar Producto</span>
                      </>
                    )}
                  </button>
                </div>
              </>
            )}
          </div>
        </div>
      )}
    </AdminShell>
  );
}
