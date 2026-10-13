'use client';

import React, { useState, useEffect, useMemo, useCallback } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import {
  ArrowLeft,
  Search,
  CheckCircle2,
  Clock,
  AlertTriangle,
  XCircle,
  Package,
  Truck,
  RotateCcw,
  RefreshCw,
  Eye,
  CreditCard,
  Calendar,
  User,
  Phone,
  MapPin,
  X,
  Send,
  ShieldCheck,
  ChevronDown,
  Info,
  LayoutDashboard,
  TrendingUp,
  BarChart3,
  Upload,
} from 'lucide-react';
import AdminShell from '@/components/admin/AdminShell';
import { formatCOP } from '@/lib/utils';
import {
  api,
  AdminOrderSummaryDto,
  AdminOrderDetailDto,
  ConfirmPaymentDto,
} from '@/lib/api';

type FilterTab = 'TODOS' | 'PENDIENTES' | 'PAGADOS' | 'PREPARANDO' | 'DESPACHADOS' | 'CANCELADOS';

const FILTER_TABS: { key: FilterTab; label: string }[] = [
  { key: 'TODOS', label: 'Todos los pedidos' },
  { key: 'PENDIENTES', label: 'Pendientes' },
  { key: 'PAGADOS', label: 'Pagados' },
  { key: 'PREPARANDO', label: 'En Preparación' },
  { key: 'DESPACHADOS', label: 'Despachados' },
  { key: 'CANCELADOS', label: 'Cancelados' },
];

const INITIAL_MOCK_ORDERS: AdminOrderDetailDto[] = [
  {
    id: 'ord-001',
    reference: 'AB-2026-000101',
    customerName: 'Valeria Restrepo',
    customerPhone: '3109876543',
    deliveryType: 'DOMICILIO_LOCAL',
    city: 'Medellín',
    address: 'Calle 10 # 43E-22, El Poblado',
    customerNotes: 'Timbre 301, favor entregar en portería si no contesto.',
    subtotal: 110000,
    discount: 0,
    shippingFee: 8000,
    total: 118000,
    status: 'PENDIENTE_CONTACTO',
    channel: 'WHATSAPP',
    expiresAt: new Date(Date.now() + 35 * 60 * 1000).toISOString(),
    createdAt: new Date(Date.now() - 10 * 60 * 1000).toISOString(),
    updatedAt: new Date(Date.now() - 10 * 60 * 1000).toISOString(),
    items: [
      {
        id: 'item-1',
        variantId: 'var-1',
        sku: 'LAB-BELL-01',
        productName: 'Labial Mate Velvet Cream',
        variantName: 'Tono Rosé Chic',
        unitPrice: 45000,
        quantity: 2,
        subtotal: 90000,
      },
      {
        id: 'item-2',
        variantId: 'var-2',
        sku: 'RUB-BELL-02',
        productName: 'Rubor Líquido Soft Glow',
        variantName: 'Tono Melocotón',
        unitPrice: 20000,
        quantity: 1,
        subtotal: 20000,
      },
    ],
    payments: [],
    reservations: [
      {
        id: 'res-1',
        variantId: 'var-1',
        sku: 'LAB-BELL-01',
        productName: 'Labial Mate Velvet Cream',
        quantity: 2,
        expiresAt: new Date(Date.now() + 35 * 60 * 1000).toISOString(),
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
      },
      {
        id: 'res-2',
        variantId: 'var-2',
        sku: 'RUB-BELL-02',
        productName: 'Rubor Líquido Soft Glow',
        quantity: 1,
        expiresAt: new Date(Date.now() + 35 * 60 * 1000).toISOString(),
        status: 'ACTIVE',
        createdAt: new Date().toISOString(),
      },
    ],
    statusHistory: [
      {
        id: 'hist-1',
        newStatus: 'PENDIENTE_CONTACTO',
        userEmail: 'sistema@alexabeauty.com',
        notes: 'Pedido generado por checkout de WhatsApp. Reserva activa 45 min.',
        createdAt: new Date(Date.now() - 10 * 60 * 1000).toISOString(),
      },
    ],
  },
  {
    id: 'ord-002',
    reference: 'AB-2026-000102',
    customerName: 'Camila Morales',
    customerPhone: '3001234567',
    deliveryType: 'LOCAL_PICKUP',
    city: 'Medellín',
    address: 'Recoge en Tienda Principal',
    customerNotes: 'Paso hoy a las 5pm.',
    subtotal: 65000,
    discount: 0,
    shippingFee: 0,
    total: 65000,
    status: 'PAGADO',
    channel: 'WHATSAPP',
    expiresAt: new Date(Date.now() + 20 * 60 * 1000).toISOString(),
    paidAt: new Date(Date.now() - 40 * 60 * 1000).toISOString(),
    createdAt: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
    updatedAt: new Date(Date.now() - 40 * 60 * 1000).toISOString(),
    items: [
      {
        id: 'item-3',
        variantId: 'var-3',
        sku: 'SER-GLOW-01',
        productName: 'Serum Vitamina C Radiance',
        variantName: '30ml',
        unitPrice: 65000,
        quantity: 1,
        subtotal: 65000,
      },
    ],
    payments: [
      {
        id: 'pay-1',
        orderId: 'ord-002',
        paymentMethod: 'NEQUI',
        transferReference: 'NEQ-8823194',
        amount: 65000,
        status: 'VERIFICADO',
        verifiedByEmail: 'admin@alexabeauty.com',
        verifiedAt: new Date(Date.now() - 40 * 60 * 1000).toISOString(),
        notes: 'Comprobante verificado en cuenta oficial Nequi.',
      },
    ],
    reservations: [],
    statusHistory: [
      {
        id: 'hist-2',
        newStatus: 'PENDIENTE_CONTACTO',
        notes: 'Pedido creado',
        createdAt: new Date(Date.now() - 60 * 60 * 1000).toISOString(),
      },
      {
        id: 'hist-3',
        previousStatus: 'PENDIENTE_CONTACTO',
        newStatus: 'PAGADO',
        userEmail: 'admin@alexabeauty.com',
        notes: 'Pago verificado por admin@alexabeauty.com',
        createdAt: new Date(Date.now() - 40 * 60 * 1000).toISOString(),
      },
    ],
  },
  {
    id: 'ord-003',
    reference: 'AB-2026-000103',
    customerName: 'Daniela Gómez',
    customerPhone: '3157778899',
    deliveryType: 'NACIONAL',
    city: 'Bogotá D.C.',
    address: 'Carrera 15 # 85-30 Apto 402',
    customerNotes: 'Enviar guía apenas despachen',
    subtotal: 180000,
    discount: 10000,
    shippingFee: 15000,
    total: 185000,
    status: 'PREPARANDO',
    channel: 'WHATSAPP',
    expiresAt: new Date(Date.now() + 60 * 60 * 1000).toISOString(),
    paidAt: new Date(Date.now() - 2 * 3600 * 1000).toISOString(),
    createdAt: new Date(Date.now() - 3 * 3600 * 1000).toISOString(),
    updatedAt: new Date(Date.now() - 1 * 3600 * 1000).toISOString(),
    items: [
      {
        id: 'item-4',
        variantId: 'var-4',
        sku: 'KIT-RUT-01',
        productName: 'Kit Rutina Facial Esencial',
        variantName: 'Piel Mixta',
        unitPrice: 180000,
        quantity: 1,
        subtotal: 180000,
      },
    ],
    payments: [
      {
        id: 'pay-2',
        orderId: 'ord-003',
        paymentMethod: 'BANCOLOMBIA',
        transferReference: 'BCOL-0091245',
        amount: 185000,
        status: 'VERIFICADO',
        verifiedByEmail: 'operador@alexabeauty.com',
        verifiedAt: new Date(Date.now() - 2 * 3600 * 1000).toISOString(),
      },
    ],
    reservations: [],
    statusHistory: [
      {
        id: 'hist-4',
        previousStatus: 'PAGADO',
        newStatus: 'PREPARANDO',
        notes: 'En empaque con detalles de regalo',
        createdAt: new Date(Date.now() - 1 * 3600 * 1000).toISOString(),
      },
    ],
  },
];

export default function AdminOrdersPage() {
  const [orders, setOrders] = useState<AdminOrderDetailDto[]>(INITIAL_MOCK_ORDERS);
  const [activeTab, setActiveTab] = useState<FilterTab>('TODOS');
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [successMessage, setSuccessMessage] = useState<string | null>(null);

  // Modals state
  const [selectedOrder, setSelectedOrder] = useState<AdminOrderDetailDto | null>(null);
  const [isDetailDrawerOpen, setIsDetailDrawerOpen] = useState(false);
  const [orderForPayment, setOrderForPayment] = useState<AdminOrderDetailDto | null>(null);
  const [orderForExtend, setOrderForExtend] = useState<AdminOrderDetailDto | null>(null);

  // Payment form state
  const [paymentMethod, setPaymentMethod] = useState<'NEQUI' | 'DAVIPLATA' | 'BANCOLOMBIA' | 'EFECTIVO' | 'OTRO'>('NEQUI');
  const [transferReference, setTransferReference] = useState('');
  const [paymentAmount, setPaymentAmount] = useState<number>(0);
  const [paymentNotes, setPaymentNotes] = useState('');
  const [paymentReceiptUrl, setPaymentReceiptUrl] = useState('');
  const [processingPayment, setProcessingPayment] = useState(false);

  // Extend reservation form state
  const [additionalMinutes, setAdditionalMinutes] = useState(30);
  const [extendNotes, setExtendNotes] = useState('');
  const [processingExtend, setProcessingExtend] = useState(false);

  // Transition form state
  const [processingTransition, setProcessingTransition] = useState(false);

  const fetchOrders = useCallback(async () => {
    setLoading(true);
    setErrorMessage(null);
    try {
      const pageResult = await api.getAdminOrders({
        search: searchQuery || undefined,
        size: 50,
      });

      if (pageResult && pageResult.content && pageResult.content.length > 0) {
        // Fetch full details for the returned summaries
        const details = await Promise.all(
          pageResult.content.map(async (sum) => {
            try {
              return await api.getAdminOrderDetail(sum.id);
            } catch {
              return {
                id: sum.id,
                reference: sum.reference,
                customerName: sum.customerName,
                customerPhone: sum.customerPhone,
                deliveryType: sum.deliveryType,
                subtotal: sum.total,
                discount: 0,
                shippingFee: 0,
                total: sum.total,
                status: sum.status,
                channel: sum.channel,
                expiresAt: sum.expiresAt,
                paidAt: sum.paidAt,
                createdAt: sum.createdAt,
                updatedAt: sum.createdAt,
                items: [],
                payments: [],
                reservations: [],
                statusHistory: [],
              } as AdminOrderDetailDto;
            }
          })
        );
        setOrders(details);
      }
    } catch {
      // Backend not yet populated or offline; keep fallback mock orders gracefully
      // console.info('Using local admin orders state:', e);
    } finally {
      setLoading(false);
    }
  }, [searchQuery]);

  useEffect(() => {
    fetchOrders();
  }, [fetchOrders]);

  // Filtered orders list
  const filteredOrders = useMemo(() => {
    return orders.filter((order) => {
      // Status filter
      if (activeTab === 'PENDIENTES') {
        const isPending =
          order.status === 'PENDIENTE' ||
          order.status === 'PENDIENTE_CONTACTO' ||
          order.status === 'PENDIENTE_PAGO' ||
          order.status === 'PAGO_EN_REVISION';
        if (!isPending) return false;
      } else if (activeTab === 'PAGADOS') {
        if (order.status !== 'PAGADO') return false;
      } else if (activeTab === 'PREPARANDO') {
        if (order.status !== 'PREPARANDO') return false;
      } else if (activeTab === 'DESPACHADOS') {
        if (order.status !== 'DESPACHADO' && order.status !== 'ENTREGADO') return false;
      } else if (activeTab === 'CANCELADOS') {
        if (order.status !== 'CANCELADO' && order.status !== 'EXPIRADO') return false;
      }

      // Search filter
      if (searchQuery.trim()) {
        const query = searchQuery.toLowerCase();
        const matchesRef = order.reference.toLowerCase().includes(query);
        const matchesName = order.customerName.toLowerCase().includes(query);
        const matchesPhone = order.customerPhone.toLowerCase().includes(query);
        if (!matchesRef && !matchesName && !matchesPhone) return false;
      }

      return true;
    });
  }, [orders, activeTab, searchQuery]);

  // Open payment verification modal
  const handleOpenPaymentModal = (order: AdminOrderDetailDto) => {
    setOrderForPayment(order);
    setPaymentAmount(order.total);
    setTransferReference('');
    setPaymentNotes('');
    setPaymentReceiptUrl('');
    setPaymentMethod('NEQUI');
    setErrorMessage(null);
  };

  // Submit payment confirmation
  const handleConfirmPayment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!orderForPayment) return;

    if (!transferReference.trim()) {
      setErrorMessage('La referencia de transferencia es obligatoria.');
      return;
    }

    setProcessingPayment(true);
    setErrorMessage(null);

    const payload: ConfirmPaymentDto = {
      paymentMethod,
      transferReference: transferReference.trim(),
      amount: paymentAmount,
      notes: paymentNotes.trim() || undefined,
      receiptImageUrl: paymentReceiptUrl.trim() || undefined,
    };

    try {
      await api.confirmAdminOrderPayment(orderForPayment.id, payload);
      setSuccessMessage(`¡Pago confirmado exitosamente para el pedido ${orderForPayment.reference}! Inventario descontado.`);
      setOrderForPayment(null);
      await fetchOrders();
    } catch {
      // Fallback state mutation if backend endpoint is unavailable during design preview
      setOrders((prev) =>
        prev.map((o) => {
          if (o.id !== orderForPayment.id) return o;
          return {
            ...o,
            status: 'PAGADO',
            paidAt: new Date().toISOString(),
            payments: [
              {
                id: 'pay-' + Date.now(),
                orderId: o.id,
                paymentMethod,
                transferReference: transferReference.trim(),
                amount: paymentAmount,
                status: 'VERIFICADO',
                verifiedByEmail: 'admin@alexabeauty.com',
                verifiedAt: new Date().toISOString(),
                notes: paymentNotes.trim() || undefined,
              },
            ],
            statusHistory: [
              ...o.statusHistory,
              {
                id: 'hist-' + Date.now(),
                previousStatus: o.status,
                newStatus: 'PAGADO',
                userEmail: 'admin@alexabeauty.com',
                notes: `Pago verificado ref: ${transferReference.trim()}`,
                createdAt: new Date().toISOString(),
              },
            ],
          };
        })
      );
      setSuccessMessage(`Pago verificado localmente para ${orderForPayment.reference}.`);
      setOrderForPayment(null);
    } finally {
      setProcessingPayment(false);
    }
  };

  // Transition order status (Preparar, Despachar, Entregar, Cancelar)
  const handleTransitionStatus = async (orderId: string, newStatus: string, notes?: string) => {
    setProcessingTransition(true);
    setErrorMessage(null);
    try {
      const updated = await api.transitionAdminOrderStatus(orderId, newStatus, notes);
      setSuccessMessage(`Estado actualizado a ${newStatus}.`);
      if (selectedOrder && selectedOrder.id === orderId) {
        setSelectedOrder(updated);
      }
      await fetchOrders();
    } catch {
      // Local fallback
      setOrders((prev) =>
        prev.map((o) => {
          if (o.id !== orderId) return o;
          const updatedOrder: AdminOrderDetailDto = {
            ...o,
            status: newStatus,
            statusHistory: [
              ...o.statusHistory,
              {
                id: 'hist-' + Date.now(),
                previousStatus: o.status,
                newStatus,
                userEmail: 'admin@alexabeauty.com',
                notes: notes || `Transición a ${newStatus}`,
                createdAt: new Date().toISOString(),
              },
            ],
          };
          if (selectedOrder && selectedOrder.id === orderId) {
            setSelectedOrder(updatedOrder);
          }
          return updatedOrder;
        })
      );
      setSuccessMessage(`Estado actualizado a ${newStatus}.`);
    } finally {
      setProcessingTransition(false);
    }
  };

  // Extend reservation
  const handleExtendReservation = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!orderForExtend) return;

    setProcessingExtend(true);
    setErrorMessage(null);
    try {
      const updated = await api.extendAdminOrderReservation(orderForExtend.id, additionalMinutes, extendNotes);
      setSuccessMessage(`Reserva extendida por ${additionalMinutes} minutos.`);
      setOrderForExtend(null);
      if (selectedOrder && selectedOrder.id === orderForExtend.id) {
        setSelectedOrder(updated);
      }
      await fetchOrders();
    } catch {
      setOrders((prev) =>
        prev.map((o) => {
          if (o.id !== orderForExtend.id) return o;
          const newExp = new Date(Date.now() + additionalMinutes * 60 * 1000).toISOString();
          return {
            ...o,
            expiresAt: newExp,
            statusHistory: [
              ...o.statusHistory,
              {
                id: 'hist-' + Date.now(),
                newStatus: o.status,
                userEmail: 'admin@alexabeauty.com',
                notes: `Reserva extendida por ${additionalMinutes} minutos. ${extendNotes}`,
                createdAt: new Date().toISOString(),
              },
            ],
          };
        })
      );
      setSuccessMessage(`Reserva extendida por ${additionalMinutes} minutos.`);
      setOrderForExtend(null);
    } finally {
      setProcessingExtend(false);
    }
  };

  const getStatusBadge = (status: string) => {
    switch (status) {
      case 'PENDIENTE':
      case 'PENDIENTE_CONTACTO':
      case 'PENDIENTE_PAGO':
      case 'PAGO_EN_REVISION':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-amber-100 text-amber-800 border border-amber-200">
            <Clock className="w-3.5 h-3.5" />
            Pendiente de Pago
          </span>
        );
      case 'PAGADO':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-emerald-100 text-emerald-800 border border-emerald-200">
            <CheckCircle2 className="w-3.5 h-3.5" />
            Pagado
          </span>
        );
      case 'PREPARANDO':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-blue-100 text-blue-800 border border-blue-200">
            <Package className="w-3.5 h-3.5" />
            En Preparación
          </span>
        );
      case 'DESPACHADO':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-purple-100 text-purple-800 border border-purple-200">
            <Truck className="w-3.5 h-3.5" />
            Despachado
          </span>
        );
      case 'ENTREGADO':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-teal-100 text-teal-800 border border-teal-200">
            <ShieldCheck className="w-3.5 h-3.5" />
            Entregado
          </span>
        );
      case 'CANCELADO':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-rose-100 text-rose-800 border border-rose-200">
            <XCircle className="w-3.5 h-3.5" />
            Cancelado
          </span>
        );
      case 'EXPIRADO':
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-gray-100 text-gray-700 border border-gray-200">
            <RotateCcw className="w-3.5 h-3.5" />
            Expirado
          </span>
        );
      default:
        return (
          <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-gray-100 text-gray-700 border border-gray-200">
            {status}
          </span>
        );
    }
  };

  return (
    <AdminShell
      badge="Epic 6 · Pedidos & Pagos"
      title="Control de Pedidos y Cobranza"
      subtitle="Verifica transferencias bancarias, descuenta stock atómicamente y gestiona el ciclo logístico."
      actions={
        <button
          type="button"
          onClick={fetchOrders}
          disabled={loading}
          className="inline-flex items-center gap-2 px-4 py-2 bg-white border border-[#b9899c] hover:bg-cream text-wine rounded-full text-xs font-bold transition shadow-xs disabled:opacity-50"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
          Actualizar Pedidos
        </button>
      }
    >

        {/* Alerts */}
        {successMessage && (
          <div className="bg-emerald-50 border border-emerald-200 text-emerald-800 text-sm px-4 py-3 rounded-2xl flex items-center justify-between gap-2 shadow-xs">
            <div className="flex items-center gap-2">
              <CheckCircle2 className="w-5 h-5 flex-shrink-0 text-emerald-600" />
              <span>{successMessage}</span>
            </div>
            <button
              type="button"
              onClick={() => setSuccessMessage(null)}
              className="text-emerald-700 hover:text-emerald-900 p-1"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {errorMessage && (
          <div className="bg-rose-50 border border-rose-200 text-rose-800 text-sm px-4 py-3 rounded-2xl flex items-center justify-between gap-2 shadow-xs">
            <div className="flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 flex-shrink-0 text-rose-600" />
              <span>{errorMessage}</span>
            </div>
            <button
              type="button"
              onClick={() => setErrorMessage(null)}
              className="text-rose-700 hover:text-rose-900 p-1"
            >
              <X className="w-4 h-4" />
            </button>
          </div>
        )}

        {/* Filter Chips & Search Bar */}
        <div className="bg-white border border-border rounded-3xl p-4 sm:p-6 shadow-soft space-y-4">
          <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
            {/* Status Tabs */}
            <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar pb-1">
              {FILTER_TABS.map((tab) => {
                const isActive = activeTab === tab.key;
                return (
                  <button
                    key={tab.key}
                    type="button"
                    onClick={() => setActiveTab(tab.key)}
                    className={`px-4 py-2 rounded-full text-xs sm:text-sm font-semibold transition whitespace-nowrap ${
                      isActive
                        ? 'bg-wine text-white shadow-xs'
                        : 'bg-blush/40 hover:bg-blush text-ink hover:text-wine'
                    }`}
                  >
                    {tab.label}
                  </button>
                );
              })}
            </div>

            {/* Search Input */}
            <div className="relative min-w-[260px] sm:w-80">
              <Search className="w-4 h-4 text-muted absolute left-3.5 top-1/2 -translate-y-1/2 pointer-events-none" />
              <input
                type="text"
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                placeholder="Buscar por referencia, cliente o tel..."
                className="w-full pl-9 pr-4 py-2 text-xs sm:text-sm bg-cream/50 border border-border rounded-full focus:outline-none focus:ring-2 focus:ring-wine/20 focus:border-wine transition placeholder:text-muted/70"
              />
            </div>
          </div>
        </div>

        {/* Orders Table Panel */}
        <div className="bg-white border border-border rounded-3xl shadow-soft overflow-hidden">
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs sm:text-sm border-collapse">
              <thead>
                <tr className="bg-blush/60 text-wine font-bold border-b border-border">
                  <th className="py-3.5 px-4">Referencia</th>
                  <th className="py-3.5 px-4">Cliente</th>
                  <th className="py-3.5 px-4 hidden md:table-cell">Entrega</th>
                  <th className="py-3.5 px-4">Total</th>
                  <th className="py-3.5 px-4">Estado</th>
                  <th className="py-3.5 px-4 hidden lg:table-cell">Fecha / Expiración</th>
                  <th className="py-3.5 px-4 text-right">Acciones</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-border/60">
                {filteredOrders.length === 0 ? (
                  <tr>
                    <td colSpan={7} className="py-12 text-center text-muted">
                      <div className="max-w-xs mx-auto space-y-2">
                        <Clock className="w-8 h-8 mx-auto text-muted/60 stroke-1" />
                        <p className="font-semibold text-ink">No hay pedidos en este estado</p>
                        <p className="text-xs">
                          Prueba seleccionando otra pestaña o limpiando el filtro de búsqueda.
                        </p>
                      </div>
                    </td>
                  </tr>
                ) : (
                  filteredOrders.map((order) => {
                    const isPending =
                      order.status === 'PENDIENTE' ||
                      order.status === 'PENDIENTE_CONTACTO' ||
                      order.status === 'PENDIENTE_PAGO' ||
                      order.status === 'PAGO_EN_REVISION';
                    const isPaid = order.status === 'PAGADO';
                    const isPreparing = order.status === 'PREPARANDO';
                    const isDispatched = order.status === 'DESPACHADO';

                    return (
                      <tr key={order.id} className="hover:bg-cream/40 transition">
                        <td className="py-4 px-4 font-mono font-bold text-wine">
                          {order.reference}
                        </td>
                        <td className="py-4 px-4">
                          <p className="font-semibold text-ink leading-tight">{order.customerName}</p>
                          <span className="text-[11px] text-muted flex items-center gap-1 mt-0.5">
                            <Phone className="w-3 h-3" />
                            {order.customerPhone}
                          </span>
                        </td>
                        <td className="py-4 px-4 hidden md:table-cell text-muted text-xs">
                          <span className="font-medium text-ink block">{order.deliveryType}</span>
                          {order.city && <span>{order.city}</span>}
                        </td>
                        <td className="py-4 px-4 font-bold text-wineDark">
                          {formatCOP(order.total)}
                        </td>
                        <td className="py-4 px-4">{getStatusBadge(order.status)}</td>
                        <td className="py-4 px-4 hidden lg:table-cell text-xs text-muted">
                          <div>
                            Creado: {new Date(order.createdAt).toLocaleDateString('es-CO', { hour: '2-digit', minute: '2-digit' })}
                          </div>
                          {isPending && (
                            <div className="text-amber-700 font-medium mt-0.5">
                              Expira: {new Date(order.expiresAt).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' })}
                            </div>
                          )}
                          {order.paidAt && (
                            <div className="text-emerald-700 font-medium mt-0.5">
                              Pagado: {new Date(order.paidAt).toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' })}
                            </div>
                          )}
                        </td>
                        <td className="py-4 px-4 text-right">
                          <div className="flex items-center justify-end gap-1.5 flex-wrap">
                            <button
                              type="button"
                              onClick={() => {
                                setSelectedOrder(order);
                                setIsDetailDrawerOpen(true);
                              }}
                              className="inline-flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-semibold bg-white border border-border hover:bg-cream text-ink transition shadow-xs"
                              title="Ver detalle del pedido"
                            >
                              <Eye className="w-3.5 h-3.5" />
                              <span className="hidden sm:inline">Detalle</span>
                            </button>

                            {isPending && (
                              <>
                                <button
                                  type="button"
                                  onClick={() => handleOpenPaymentModal(order)}
                                  className="inline-flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-bold bg-wine hover:bg-wineDark text-white transition shadow-xs"
                                  title="Verificar Pago"
                                >
                                  <CreditCard className="w-3.5 h-3.5" />
                                  <span>Verificar Pago</span>
                                </button>

                                <button
                                  type="button"
                                  onClick={() => setOrderForExtend(order)}
                                  className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-full text-xs font-semibold bg-amber-50 border border-amber-200 text-amber-800 hover:bg-amber-100 transition shadow-xs"
                                  title="Extender Reserva"
                                >
                                  <Clock className="w-3.5 h-3.5" />
                                  <span className="hidden xl:inline">+Tiempo</span>
                                </button>

                                <button
                                  type="button"
                                  onClick={() => {
                                    if (confirm(`¿Seguro que deseas cancelar el pedido ${order.reference}? El stock reservado se liberará.`)) {
                                      handleTransitionStatus(order.id, 'CANCELADO', 'Cancelado manualmente por administrador');
                                    }
                                  }}
                                  className="inline-flex items-center gap-1 px-2.5 py-1.5 rounded-full text-xs font-semibold bg-rose-50 border border-rose-200 text-rose-700 hover:bg-rose-100 transition shadow-xs"
                                  title="Cancelar Pedido"
                                >
                                  <XCircle className="w-3.5 h-3.5" />
                                  <span className="hidden xl:inline">Cancelar</span>
                                </button>
                              </>
                            )}

                            {isPaid && (
                              <button
                                type="button"
                                onClick={() => handleTransitionStatus(order.id, 'PREPARANDO', 'Pedido en proceso de empaque')}
                                disabled={processingTransition}
                                className="inline-flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-bold bg-blue-600 hover:bg-blue-700 text-white transition shadow-xs disabled:opacity-50"
                              >
                                <Package className="w-3.5 h-3.5" />
                                <span>Preparar</span>
                              </button>
                            )}

                            {isPreparing && (
                              <button
                                type="button"
                                onClick={() => handleTransitionStatus(order.id, 'DESPACHADO', 'Entregado a mensajería / transportadora')}
                                disabled={processingTransition}
                                className="inline-flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-bold bg-purple-600 hover:bg-purple-700 text-white transition shadow-xs disabled:opacity-50"
                              >
                                <Truck className="w-3.5 h-3.5" />
                                <span>Despachar</span>
                              </button>
                            )}

                            {isDispatched && (
                              <button
                                type="button"
                                onClick={() => handleTransitionStatus(order.id, 'ENTREGADO', 'Pedido entregado al cliente a satisfacción')}
                                disabled={processingTransition}
                                className="inline-flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-bold bg-teal-600 hover:bg-teal-700 text-white transition shadow-xs disabled:opacity-50"
                              >
                                <ShieldCheck className="w-3.5 h-3.5" />
                                <span>Entregar</span>
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>
        </div>

      {/* MODAL: VERIFICAR PAGO */}
      {orderForPayment && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl border border-border space-y-6 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between border-b border-border pb-4">
              <div>
                <span className="text-[10px] font-extrabold uppercase tracking-widest text-wine">
                  Verificación de Cobro
                </span>
                <h2 className="font-serif text-2xl text-wineDark font-normal">
                  Verificar Pago · {orderForPayment.reference}
                </h2>
              </div>
              <button
                type="button"
                onClick={() => setOrderForPayment(null)}
                className="text-muted hover:text-ink p-1 rounded-full hover:bg-cream transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Customer & Total Snapshot */}
            <div className="bg-blush/40 border border-[#b9899c]/40 rounded-2xl p-4 flex items-center justify-between">
              <div>
                <span className="text-xs text-muted block">Cliente</span>
                <p className="font-semibold text-ink text-sm">{orderForPayment.customerName}</p>
                <span className="text-xs text-muted">{orderForPayment.customerPhone}</span>
              </div>
              <div className="text-right">
                <span className="text-xs text-muted block">Total a Cobrar</span>
                <p className="font-serif text-2xl font-bold text-wine">
                  {formatCOP(orderForPayment.total)}
                </p>
              </div>
            </div>

            <form onSubmit={handleConfirmPayment} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-wine uppercase tracking-wider mb-1">
                  Método de Pago
                </label>
                <select
                  value={paymentMethod}
                  onChange={(e) => setPaymentMethod(e.target.value as any)}
                  className="w-full px-3.5 py-2.5 bg-cream/40 border border-border rounded-xl text-sm font-semibold focus:outline-none focus:ring-2 focus:ring-wine/20 focus:border-wine"
                >
                  <option value="NEQUI">Nequi (Transferencia)</option>
                  <option value="DAVIPLATA">Daviplata (Transferencia)</option>
                  <option value="BANCOLOMBIA">Bancolombia (Cuenta de Ahorros / QR)</option>
                  <option value="EFECTIVO">Efectivo (Contraentrega / Caja)</option>
                  <option value="OTRO">Otro Medio</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-bold text-wine uppercase tracking-wider mb-1">
                  Referencia de Transferencia / ID Transacción *
                </label>
                <input
                  type="text"
                  required
                  value={transferReference}
                  onChange={(e) => setTransferReference(e.target.value)}
                  placeholder="Ej: M12345678 o 987654321"
                  className="w-full px-3.5 py-2.5 bg-cream/40 border border-border rounded-xl text-sm font-mono focus:outline-none focus:ring-2 focus:ring-wine/20 focus:border-wine placeholder:text-muted/60"
                />
              </div>

              <div>
                <div className="flex items-center justify-between mb-1">
                  <label className="block text-xs font-bold text-wine uppercase tracking-wider">
                    Monto Recibido ($ COP) *
                  </label>
                  {paymentAmount === orderForPayment.total ? (
                    <span className="text-[11px] text-emerald-700 font-semibold flex items-center gap-1">
                      <CheckCircle2 className="w-3.5 h-3.5" /> Coincide con total
                    </span>
                  ) : (
                    <span className="text-[11px] text-amber-700 font-semibold flex items-center gap-1">
                      <AlertTriangle className="w-3.5 h-3.5" /> Difiere del total
                    </span>
                  )}
                </div>
                <input
                  type="number"
                  required
                  min={0}
                  step={100}
                  value={paymentAmount}
                  onChange={(e) => setPaymentAmount(Number(e.target.value))}
                  className="w-full px-3.5 py-2.5 bg-cream/40 border border-border rounded-xl text-sm font-bold focus:outline-none focus:ring-2 focus:ring-wine/20 focus:border-wine"
                />
              </div>

              <div>
                <label className="block text-xs font-bold text-wine uppercase tracking-wider mb-1">
                  Notas Internas de Auditoría (Opcional)
                </label>
                <textarea
                  rows={2}
                  value={paymentNotes}
                  onChange={(e) => setPaymentNotes(e.target.value)}
                  placeholder="Ej: Verificado por WhatsApp, titular cuenta coincide con cliente..."
                  className="w-full px-3.5 py-2 bg-cream/40 border border-border rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-wine/20 focus:border-wine placeholder:text-muted/60"
                />
              </div>

              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setOrderForPayment(null)}
                  className="px-5 py-2.5 rounded-full text-xs font-semibold text-muted hover:text-ink hover:bg-cream transition"
                >
                  Cancelar
                </button>

                <button
                  type="submit"
                  disabled={processingPayment}
                  className="inline-flex items-center gap-2 px-6 py-2.5 rounded-full text-xs font-bold bg-wine hover:bg-wineDark text-white transition shadow-glow active:scale-95 disabled:opacity-50"
                >
                  {processingPayment ? (
                    <>
                      <RefreshCw className="w-3.5 h-3.5 animate-spin" />
                      <span>Verificando y Descontando Stock...</span>
                    </>
                  ) : (
                    <>
                      <ShieldCheck className="w-4 h-4" />
                      <span>Confirmar Pago y Descontar Stock</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: EXTENDER RESERVA */}
      {orderForExtend && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/60 backdrop-blur-xs">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-border space-y-5 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <div>
                <span className="text-[10px] font-extrabold uppercase tracking-widest text-amber-700">
                  Prórroga de Reserva
                </span>
                <h2 className="font-serif text-xl text-wineDark font-normal">
                  Extender Tiempo · {orderForExtend.reference}
                </h2>
              </div>
              <button
                type="button"
                onClick={() => setOrderForExtend(null)}
                className="text-muted hover:text-ink p-1 rounded-full hover:bg-cream"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleExtendReservation} className="space-y-4">
              <div>
                <label className="block text-xs font-bold text-wine uppercase tracking-wider mb-2">
                  Minutos Adicionales
                </label>
                <div className="grid grid-cols-4 gap-2">
                  {[15, 30, 45, 60].map((mins) => (
                    <button
                      key={mins}
                      type="button"
                      onClick={() => setAdditionalMinutes(mins)}
                      className={`py-2 rounded-xl text-xs font-bold border transition ${
                        additionalMinutes === mins
                          ? 'bg-amber-100 border-amber-300 text-amber-900 shadow-xs'
                          : 'bg-cream/40 border-border text-ink hover:bg-cream'
                      }`}
                    >
                      +{mins} min
                    </button>
                  ))}
                </div>
              </div>

              <div>
                <label className="block text-xs font-bold text-wine uppercase tracking-wider mb-1">
                  Motivo o Nota
                </label>
                <textarea
                  rows={2}
                  value={extendNotes}
                  onChange={(e) => setExtendNotes(e.target.value)}
                  placeholder="Ej: Cliente informa que está en el banco..."
                  className="w-full px-3 py-2 bg-cream/40 border border-border rounded-xl text-xs focus:outline-none focus:ring-2 focus:ring-wine/20 focus:border-wine"
                />
              </div>

              <div className="pt-2 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setOrderForExtend(null)}
                  className="px-4 py-2 rounded-full text-xs font-semibold text-muted hover:text-ink"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={processingExtend}
                  className="px-5 py-2 rounded-full text-xs font-bold bg-amber-700 hover:bg-amber-800 text-white transition shadow-xs disabled:opacity-50"
                >
                  {processingExtend ? 'Guardando...' : 'Aplicar Extensión'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* DRAWER: DETALLE DEL PEDIDO */}
      {isDetailDrawerOpen && selectedOrder && (
        <div className="fixed inset-0 z-50 flex justify-end bg-black/50 backdrop-blur-xs">
          <div className="bg-white w-full max-w-xl h-full shadow-2xl flex flex-col border-l border-border animate-in slide-in-from-right duration-200">
            {/* Drawer Header */}
            <div className="p-6 border-b border-border flex items-center justify-between bg-cream/30">
              <div>
                <div className="flex items-center gap-2">
                  <span className="font-mono text-lg font-bold text-wine">
                    {selectedOrder.reference}
                  </span>
                  {getStatusBadge(selectedOrder.status)}
                </div>
                <span className="text-xs text-muted mt-0.5 block">
                  Canal: {selectedOrder.channel} · Creado el{' '}
                  {new Date(selectedOrder.createdAt).toLocaleString('es-CO')}
                </span>
              </div>
              <button
                type="button"
                onClick={() => setIsDetailDrawerOpen(false)}
                className="text-muted hover:text-ink p-2 rounded-full hover:bg-cream transition"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Drawer Scrollable Content */}
            <div className="flex-1 overflow-y-auto p-6 space-y-6">
              {/* Customer Info Box */}
              <div className="bg-cream/40 border border-border rounded-2xl p-4 space-y-2">
                <h3 className="text-xs font-bold uppercase tracking-wider text-wine flex items-center gap-1.5">
                  <User className="w-3.5 h-3.5" />
                  Datos del Cliente y Despacho
                </h3>
                <div className="grid grid-cols-2 gap-2 text-xs">
                  <div>
                    <span className="text-muted block">Nombre:</span>
                    <span className="font-semibold text-ink">{selectedOrder.customerName}</span>
                  </div>
                  <div>
                    <span className="text-muted block">Teléfono / WhatsApp:</span>
                    <a
                      href={`https://wa.me/57${selectedOrder.customerPhone}`}
                      target="_blank"
                      rel="noopener noreferrer"
                      className="font-semibold text-wine hover:underline"
                    >
                      {selectedOrder.customerPhone}
                    </a>
                  </div>
                  <div>
                    <span className="text-muted block">Tipo de Entrega:</span>
                    <span className="font-semibold text-ink">{selectedOrder.deliveryType}</span>
                  </div>
                  <div>
                    <span className="text-muted block">Ciudad / Dirección:</span>
                    <span className="font-semibold text-ink">
                      {selectedOrder.city || 'Medellín'} · {selectedOrder.address || 'N/A'}
                    </span>
                  </div>
                </div>
                {selectedOrder.customerNotes && (
                  <div className="pt-2 border-t border-border/60 text-xs">
                    <span className="text-muted block">Notas del cliente:</span>
                    <p className="italic text-ink">{selectedOrder.customerNotes}</p>
                  </div>
                )}
              </div>

              {/* Items List */}
              <div className="space-y-3">
                <h3 className="text-xs font-bold uppercase tracking-wider text-wine flex items-center gap-1.5">
                  <Package className="w-3.5 h-3.5" />
                  Productos en la Orden ({selectedOrder.items.length})
                </h3>
                <div className="border border-border rounded-2xl overflow-hidden divide-y divide-border">
                  {selectedOrder.items.map((item) => (
                    <div key={item.id} className="p-3.5 flex items-center justify-between text-xs">
                      <div>
                        <p className="font-semibold text-ink">{item.productName}</p>
                        <span className="text-[11px] text-muted font-mono">
                          SKU: {item.sku} {item.variantName ? `· ${item.variantName}` : ''}
                        </span>
                        <div className="text-[11px] text-muted">
                          {item.quantity} und. × {formatCOP(item.unitPrice)}
                        </div>
                      </div>
                      <div className="font-bold text-wine text-sm">
                        {formatCOP(item.subtotal)}
                      </div>
                    </div>
                  ))}
                </div>

                {/* Subtotal & Total */}
                <div className="bg-cream/20 p-3.5 rounded-xl border border-border space-y-1.5 text-xs">
                  <div className="flex justify-between text-muted">
                    <span>Subtotal:</span>
                    <span>{formatCOP(selectedOrder.subtotal)}</span>
                  </div>
                  {selectedOrder.discount > 0 && (
                    <div className="flex justify-between text-emerald-700">
                      <span>Descuento:</span>
                      <span>-{formatCOP(selectedOrder.discount)}</span>
                    </div>
                  )}
                  <div className="flex justify-between text-muted">
                    <span>Costo de Envío:</span>
                    <span>{formatCOP(selectedOrder.shippingFee)}</span>
                  </div>
                  <div className="flex justify-between text-base font-bold text-wine pt-1.5 border-t border-border">
                    <span>Total Pedido:</span>
                    <span>{formatCOP(selectedOrder.total)}</span>
                  </div>
                </div>
              </div>

              {/* Payments Section */}
              <div className="space-y-3">
                <h3 className="text-xs font-bold uppercase tracking-wider text-wine flex items-center gap-1.5">
                  <CreditCard className="w-3.5 h-3.5" />
                  Registro de Pagos y Comprobantes
                </h3>
                {selectedOrder.payments.length === 0 ? (
                  <div className="p-4 bg-cream/30 border border-border rounded-2xl text-xs text-muted text-center">
                    No se han registrado pagos verificados para este pedido.
                  </div>
                ) : (
                  selectedOrder.payments.map((p) => (
                    <div
                      key={p.id}
                      className="p-4 bg-emerald-50/50 border border-emerald-200 rounded-2xl space-y-2 text-xs"
                    >
                      <div className="flex items-center justify-between">
                        <span className="font-bold text-emerald-800">
                          {p.paymentMethod} · {p.transferReference}
                        </span>
                        <span className="font-mono font-bold text-emerald-900">
                          {formatCOP(p.amount)}
                        </span>
                      </div>
                      <div className="text-muted text-[11px] flex items-center justify-between">
                        <span>Verificado por: {p.verifiedByEmail || 'Sistema'}</span>
                        <span>{p.verifiedAt ? new Date(p.verifiedAt).toLocaleString('es-CO') : ''}</span>
                      </div>
                      {p.notes && <p className="text-[11px] text-ink italic">{p.notes}</p>}
                    </div>
                  ))
                )}
              </div>

              {/* Status History Timeline */}
              <div className="space-y-3">
                <h3 className="text-xs font-bold uppercase tracking-wider text-wine flex items-center gap-1.5">
                  <Clock className="w-3.5 h-3.5" />
                  Historial de Estados y Auditoría
                </h3>
                <div className="relative pl-5 border-l-2 border-border/80 space-y-4 text-xs">
                  {selectedOrder.statusHistory.map((h) => (
                    <div key={h.id} className="relative group">
                      <div className="absolute -left-[27px] top-1 w-3 h-3 rounded-full bg-wine ring-4 ring-white" />
                      <div className="font-semibold text-ink">
                        {h.previousStatus ? `${h.previousStatus} → ` : ''}
                        <span className="text-wine font-bold">{h.newStatus}</span>
                      </div>
                      <span className="text-[11px] text-muted block">
                        {new Date(h.createdAt).toLocaleString('es-CO')}
                        {h.userEmail ? ` · ${h.userEmail}` : ''}
                      </span>
                      {h.notes && <p className="text-[11px] text-muted/90 mt-0.5">{h.notes}</p>}
                    </div>
                  ))}
                </div>
              </div>
            </div>

            {/* Drawer Footer Actions */}
            <div className="p-4 border-t border-border bg-cream/30 flex items-center justify-between gap-3">
              <button
                type="button"
                onClick={() => setIsDetailDrawerOpen(false)}
                className="px-4 py-2 rounded-full text-xs font-semibold text-muted hover:text-ink hover:bg-cream"
              >
                Cerrar
              </button>

              <div className="flex items-center gap-2">
                {(selectedOrder.status === 'PENDIENTE_CONTACTO' ||
                  selectedOrder.status === 'PENDIENTE_PAGO') && (
                  <button
                    type="button"
                    onClick={() => {
                      setIsDetailDrawerOpen(false);
                      handleOpenPaymentModal(selectedOrder);
                    }}
                    className="px-5 py-2 rounded-full text-xs font-bold bg-wine hover:bg-wineDark text-white shadow-glow"
                  >
                    Verificar Pago
                  </button>
                )}

                {selectedOrder.status === 'PAGADO' && (
                  <button
                    type="button"
                    onClick={() => handleTransitionStatus(selectedOrder.id, 'PREPARANDO', 'Empaque iniciado')}
                    className="px-5 py-2 rounded-full text-xs font-bold bg-blue-600 hover:bg-blue-700 text-white"
                  >
                    Comenzar Preparación
                  </button>
                )}

                {selectedOrder.status === 'PREPARANDO' && (
                  <button
                    type="button"
                    onClick={() => handleTransitionStatus(selectedOrder.id, 'DESPACHADO', 'Despacho registrado')}
                    className="px-5 py-2 rounded-full text-xs font-bold bg-purple-600 hover:bg-purple-700 text-white"
                  >
                    Marcar Despachado
                  </button>
                )}

                {selectedOrder.status === 'DESPACHADO' && (
                  <button
                    type="button"
                    onClick={() => handleTransitionStatus(selectedOrder.id, 'ENTREGADO', 'Entrega confirmada')}
                    className="px-5 py-2 rounded-full text-xs font-bold bg-teal-600 hover:bg-teal-700 text-white"
                  >
                    Confirmar Entrega
                  </button>
                )}
              </div>
            </div>
          </div>
        </div>
      )}
    </AdminShell>
  );
}
