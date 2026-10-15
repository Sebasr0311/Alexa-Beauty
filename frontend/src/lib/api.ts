/**
 * Cliente HTTP tipado para la API de Alexa Beauty.
 * Soporta conexión contra el backend Spring Boot (/api/v1) con fallback resiliente.
 */

const API_BASE_URL = '/api/backend';

export interface ProductVariantDto {
  id: string;
  sku: string;
  shadeName?: string;
  shadeCode?: string;
  shadeHex?: string;
  presentation?: string;
  salePrice: number;
  referenceCost?: number;
  minStock: number;
  stockOnHand: number;
  stockReserved: number;
  availableStock: number;
  active: boolean;
}

export interface ProductImageDto {
  variantId?: string | null;
  id: string;
  storagePath: string;
  url: string;
  altText?: string;
  sortOrder: number;
  isCover: boolean;
}

export interface ProductSummaryDto {
  id: string;
  sku: string;
  name: string;
  slug: string;
  variants?: ProductVariantDto[];
  brandName: string;
  categoryName: string;
  brand?: { name: string };
  category?: { name: string };
  minPrice: number;
  maxPrice: number;
  totalAvailableStock: number;
  coverImageUrl?: string;
  featured: boolean;
  active: boolean;
}

export interface ProductDetailDto {
  id: string;
  sku: string;
  name: string;
  slug: string;
  description?: string;
  brand: { id: string; name: string; slug: string };
  category: { id: string; name: string; slug: string };
  featured: boolean;
  active: boolean;
  minPrice?: number;
  maxPrice?: number;
  totalStockOnHand?: number;
  images: ProductImageDto[];
  variants: ProductVariantDto[];
  totalAvailableStock: number;
}

export interface CategoryDto {
  id: string;
  name: string;
  slug: string;
  sortOrder: number;
  active: boolean;
}

export interface BrandDto {
  id: string;
  name: string;
  slug: string;
  active: boolean;
}

export interface PageDto<T> {
  content: T[];
  pageNumber: number;
  pageSize: number;
  totalElements: number;
  totalPages: number;
  last: boolean;
}

export interface LoginResponseDto {
  token: string;
  tokenType: string;
  user: {
    id: string;
    email: string;
    fullName: string;
    role: string;
  };
}

export interface VariantInput {
  sku?: string; shadeCode?: string; shadeName?: string; shadeHex?: string; presentation?: string;
  salePrice: number; referenceCost?: number | null; clearReferenceCost?: boolean; minStock?: number; stockOnHand?: number; active?: boolean;
}

class ApiClient {
  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const headers: Record<string, string> = {
      'X-Requested-With': 'XMLHttpRequest',
      ...(options.headers as Record<string, string>),
    };

    if (!(options.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
    }


    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      credentials: 'include',
      headers,
    });

    if (!response.ok) {
      let errorMsg = `HTTP Error ${response.status}`;
      try {
        const errorJson = await response.json();
        errorMsg = errorJson.detail || errorJson.message || errorMsg;
        if (Array.isArray(errorJson.errors)) errorMsg += ': ' + errorJson.errors.map((field: {field:string;message:string}) => `${field.field}: ${field.message}`).join('; ');
      } catch {
        // Ignorar error de parsing
      }
      throw new Error(errorMsg);
    }

    // Si es 204 No Content
    if (response.status === 204) {
      return {} as T;
    }

    const json = await response.json();
    const data = json.data !== undefined ? json.data : json;
    if (data && Array.isArray(data.content)) {
      data.content = data.content.map((item: any) => ({ ...item,
        brandName: item.brandName ?? item.brand?.name,
        categoryName: item.categoryName ?? item.category?.name,
      }));
      data.pageNumber ??= data.number;
      data.pageSize ??= data.size;
    }
    return data;
  }

  // --- Catálogo Público ---
  async getCatalogProducts(params?: {
    q?: string;
    category?: string;
    brand?: string;
    minPrice?: number;
    maxPrice?: number;
    inStockOnly?: boolean;
    page?: number;
    size?: number;
    sort?: string;
  }): Promise<PageDto<ProductSummaryDto>> {
    const searchParams = new URLSearchParams();
    if (params?.q) searchParams.set('query', params.q);
    if (params?.category) searchParams.set('category', params.category);
    if (params?.brand) searchParams.set('brand', params.brand);
    if (params?.minPrice !== undefined) searchParams.set('minPrice', params.minPrice.toString());
    if (params?.maxPrice !== undefined) searchParams.set('maxPrice', params.maxPrice.toString());
    if (params?.inStockOnly) searchParams.set('inStockOnly', 'true');
    if (params?.page !== undefined) searchParams.set('page', params.page.toString());
    if (params?.size !== undefined) searchParams.set('size', params.size.toString());
    if (params?.sort) searchParams.set('sort', params.sort);

    const query = searchParams.toString();
    return this.request<PageDto<ProductSummaryDto>>(`/catalog/products${query ? `?${query}` : ''}`);
  }

  async getProductBySlug(slug: string): Promise<ProductDetailDto> {
    return this.request<ProductDetailDto>(`/catalog/products/${slug}`);
  }

  async getCategories(): Promise<CategoryDto[]> {
    return this.request<CategoryDto[]>('/catalog/categories');
  }

  async getBrands(): Promise<BrandDto[]> {
    return this.request<BrandDto[]>('/catalog/brands');
  }

  // --- Autenticación ---
  async login(email: string, password: string): Promise<LoginResponseDto> {
    const res = await this.request<LoginResponseDto>('/auth/login', {
      method: 'POST',
      body: JSON.stringify({ email, password }),
    });
    return res;
  }

  async logout(): Promise<void> {
    await this.request('/auth/logout', { method: 'POST' });
  }

  async getMe(): Promise<LoginResponseDto['user']> {
    return this.request<LoginResponseDto['user']>('/auth/me');
  }

  // --- Admin Catálogo ---
  async getAdminProducts(params?: { q?: string; page?: number; size?: number; sort?: string }): Promise<PageDto<ProductSummaryDto>> {
    const searchParams = new URLSearchParams();
    if (params?.q) searchParams.set('query', params.q);
    if (params?.page !== undefined) searchParams.set('page', params.page.toString());
    if (params?.size !== undefined) searchParams.set('size', params.size.toString());
    if (params?.sort) searchParams.set('sort', params.sort);
    const query = searchParams.toString();
    return this.request<PageDto<ProductSummaryDto>>(`/admin/products${query ? `?${query}` : ''}`);
  }

  async createProduct(data: {
    name: string;
    brandId: string;
    categoryId: string;
    description?: string;
    sku?: string;
    salePrice: number;
    referenceCost?: number;
    stockOnHand: number;
    minStock?: number;
    featured?: boolean;
    variants?: Array<{
      sku?: string;
      shadeCode?: string;
      shadeHex?: string;
      shadeName?: string;
      presentation?: string;
      salePrice: number;
      referenceCost?: number | null;
      stockOnHand?: number;
      minStock?: number;
    }>;
  }): Promise<ProductDetailDto> {
    return this.request<ProductDetailDto>('/admin/products', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async createBrand(name: string): Promise<BrandDto> {
    return this.request('/admin/brands', {method: 'POST', body: JSON.stringify({name: name.trim()})});
  }
  async getAdminBrands(): Promise<BrandDto[]> { return this.request('/admin/brands'); }
  async updateProduct(id: string, data: {name: string; brandId: string; categoryId: string; description?: string}): Promise<ProductDetailDto> {
    return this.request(`/admin/products/${id}`, {method: 'PUT', body: JSON.stringify(data)});
  }

  async addVariant(productId: string, data: VariantInput): Promise<ProductVariantDto> {
    return this.request(`/admin/products/${productId}/variants`, { method: 'POST', body: JSON.stringify(data) });
  }
  async updateVariant(productId: string, variantId: string, data: Omit<VariantInput, 'stockOnHand'>): Promise<ProductVariantDto> {
    return this.request(`/admin/products/${productId}/variants/${variantId}`, { method: 'PUT', body: JSON.stringify(data) });
  }
  async toggleVariantStatus(productId: string, variantId: string, active: boolean): Promise<ProductVariantDto> {
    return this.request(`/admin/products/${productId}/variants/${variantId}/status`, { method: 'PATCH', body: JSON.stringify({ active }) });
  }

  async toggleProductStatus(id: string, active: boolean): Promise<ProductSummaryDto> {
    return this.request<ProductSummaryDto>(`/admin/products/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ active }),
    });
  }

  async getAdminProductById(id: string): Promise<ProductDetailDto> {
    return this.request<ProductDetailDto>(`/admin/products/${id}`);
  }

  async deleteProduct(id: string): Promise<void> {
    return this.request<void>(`/admin/products/${id}`, {
      method: 'DELETE',
    });
  }

  async uploadProductImage(productId: string, file: File, isCover: boolean = false, variantId?: string): Promise<ProductImageDto> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('isCover', isCover.toString());
    if (variantId) formData.append('variantId', variantId);
    return this.request<ProductImageDto>(`/admin/products/${productId}/images`, {
      method: 'POST',
      body: formData,
    });
  }

  async setCoverProductImage(productId: string, imageId: string): Promise<ProductImageDto> {
    return this.request<ProductImageDto>(`/admin/products/${productId}/images/${imageId}/cover`, {
      method: 'PATCH',
    });
  }

  async deleteProductImage(productId: string, imageId: string): Promise<void> {
    return this.request<void>(`/admin/products/${productId}/images/${imageId}`, {
      method: 'DELETE',
    });
  }

  // --- Pedidos y Checkout Público ---
  async createOrder(data: CreateOrderRequestDto, idempotencyKey?: string): Promise<OrderCheckoutResponseDto> {
    const headers: Record<string, string> = {};
    if (idempotencyKey) {
      headers['Idempotency-Key'] = idempotencyKey;
    }
    return this.request<OrderCheckoutResponseDto>('/orders', {
      method: 'POST',
      body: JSON.stringify(data),
      headers,
      credentials: 'include',
    });
  }

  async trackOrder(reference: string, token: string): Promise<OrderTrackingResponseDto> {
    const searchParams = new URLSearchParams({
      reference,
      token,
    });
    return this.request<OrderTrackingResponseDto>(`/orders/track?${searchParams.toString()}`);
  }

  // --- Admin Pedidos y Pagos ---
  async getAdminOrders(params?: {
    status?: string;
    search?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Promise<PageDto<AdminOrderSummaryDto>> {
    const searchParams = new URLSearchParams();
    if (params?.status) searchParams.set('status', params.status);
    if (params?.search) searchParams.set('search', params.search);
    if (params?.from) searchParams.set('from', params.from);
    if (params?.to) searchParams.set('to', params.to);
    if (params?.page !== undefined) searchParams.set('page', params.page.toString());
    if (params?.size !== undefined) searchParams.set('size', params.size.toString());

    const query = searchParams.toString();
    return this.request<PageDto<AdminOrderSummaryDto>>(`/admin/orders${query ? `?${query}` : ''}`);
  }

  async getAdminOrderDetail(id: string): Promise<AdminOrderDetailDto> {
    return this.request<AdminOrderDetailDto>(`/admin/orders/${id}`);
  }

  async confirmAdminOrderPayment(id: string, data: ConfirmPaymentDto, idempotencyKey?: string): Promise<PaymentDto> {
    const headers: Record<string, string> = {};
    if (idempotencyKey) {
      headers['Idempotency-Key'] = idempotencyKey;
    }
    return this.request<PaymentDto>(`/admin/orders/${id}/confirm-payment`, {
      method: 'POST',
      body: JSON.stringify(data),
      headers,
      credentials: 'include',
    });
  }

  async transitionAdminOrderStatus(id: string, newStatus: string, notes?: string): Promise<AdminOrderDetailDto> {
    return this.request<AdminOrderDetailDto>(`/admin/orders/${id}/transition`, {
      method: 'POST',
      body: JSON.stringify({ newStatus, notes }),
    });
  }

  async extendAdminOrderReservation(id: string, additionalMinutes: number, notes?: string): Promise<AdminOrderDetailDto> {
    return this.request<AdminOrderDetailDto>(`/admin/orders/${id}/extend-reservation`, {
      method: 'POST',
      body: JSON.stringify({ additionalMinutes, notes }),
    });
  }

  // --- Admin Ventas y Analítica ---
  async getAdminSales(params?: {
    from?: string;
    to?: string;
    method?: string;
    page?: number;
    size?: number;
  }): Promise<SalesListResponseDto> {
    const searchParams = new URLSearchParams();
    if (params?.from) searchParams.set('from', params.from);
    if (params?.to) searchParams.set('to', params.to);
    if (params?.method && params.method !== 'TODOS') searchParams.set('method', params.method);
    if (params?.page !== undefined) searchParams.set('page', params.page.toString());
    if (params?.size !== undefined) searchParams.set('size', params.size.toString());

    const query = searchParams.toString();
    return this.request<SalesListResponseDto>(`/admin/sales${query ? `?${query}` : ''}`);
  }

  async downloadAdminSalesExcel(params: { from?: string; to?: string; method?: string }): Promise<void> {
    const searchParams = new URLSearchParams();
    if (params.from) searchParams.set('from', params.from);
    if (params.to) searchParams.set('to', params.to);
    if (params.method && params.method !== 'TODOS') searchParams.set('method', params.method);
    const query = searchParams.toString();
    const headers: Record<string, string> = { 'X-Requested-With': 'XMLHttpRequest' };

    const response = await fetch(`${API_BASE_URL}/admin/sales/export.xlsx${query ? `?${query}` : ''}`, {
      headers,
      credentials: 'include',
    });

    if (!response.ok) {
      let errorMsg = `Error ${response.status} al descargar reporte Excel`;
      try {
        const errorJson = await response.json();
        errorMsg = errorJson.detail || errorJson.message || errorMsg;
      } catch {}
      throw new Error(errorMsg);
    }

    const blob = await response.blob();
    const blobUrl = window.URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = blobUrl;
    const fromStr = params.from || 'inicio';
    const toStr = params.to || 'fin';
    link.download = `AlexaBeauty_Reporte_Ventas_${fromStr}_${toStr}.xlsx`;
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    window.URL.revokeObjectURL(blobUrl);
  }

  async getAdminAnalytics(params?: { from?: string; to?: string }): Promise<SalesAnalyticsResponseDto> {
    const searchParams = new URLSearchParams();
    if (params?.from) searchParams.set('from', params.from);
    if (params?.to) searchParams.set('to', params.to);
    const query = searchParams.toString();
    return this.request<SalesAnalyticsResponseDto>(`/admin/analytics/overview${query ? `?${query}` : ''}`);
  }

  async getAdminTopProducts(params?: { from?: string; to?: string }): Promise<TopProductDto[]> {
    const searchParams = new URLSearchParams();
    if (params?.from) searchParams.set('from', params.from);
    if (params?.to) searchParams.set('to', params.to);
    const query = searchParams.toString();
    return this.request<TopProductDto[]>(`/admin/analytics/top-products${query ? `?${query}` : ''}`);
  }

  // --- Admin Inventario ---
  async getInventorySummary(params?: {
    query?: string;
    lowStockOnly?: boolean;
    page?: number;
    size?: number;
  }): Promise<PageDto<InventorySummaryDto>> {
    const searchParams = new URLSearchParams();
    if (params?.query) searchParams.set('query', params.query);
    if (params?.lowStockOnly !== undefined) searchParams.set('lowStockOnly', params.lowStockOnly.toString());
    if (params?.page !== undefined) searchParams.set('page', params.page.toString());
    if (params?.size !== undefined) searchParams.set('size', params.size.toString());
    const q = searchParams.toString();
    return this.request<PageDto<InventorySummaryDto>>(`/admin/inventory/summary${q ? `?${q}` : ''}`);
  }

  async getInventoryMovements(params?: {
    variantId?: string;
    type?: string;
    from?: string;
    to?: string;
    page?: number;
    size?: number;
  }): Promise<PageDto<InventoryMovementDto>> {
    const searchParams = new URLSearchParams();
    if (params?.variantId) searchParams.set('variantId', params.variantId);
    if (params?.type) searchParams.set('type', params.type);
    if (params?.from) searchParams.set('from', params.from);
    if (params?.to) searchParams.set('to', params.to);
    if (params?.page !== undefined) searchParams.set('page', params.page.toString());
    if (params?.size !== undefined) searchParams.set('size', params.size.toString());
    const q = searchParams.toString();
    return this.request<PageDto<InventoryMovementDto>>(`/admin/inventory/movements${q ? `?${q}` : ''}`);
  }

  async adjustStock(data: {
    variantId: string;
    newStockOnHand: number;
    reason: string;
    notes?: string;
  }): Promise<any> {
    return this.request('/admin/inventory/adjustments', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async createStockReceipt(
    data: {
      receiptNumber?: string;
      providerName?: string;
      invoiceNumber?: string;
      notes?: string;
      items: Array<{
        variantId: string;
        quantity: number;
        unitCost?: number;
        notes?: string;
      }>;
    },
    idempotencyKey?: string
  ): Promise<any> {
    const headers: Record<string, string> = {};
    if (idempotencyKey) headers['Idempotency-Key'] = idempotencyKey;
    return this.request('/admin/inventory/receipts', {
      method: 'POST',
      body: JSON.stringify(data),
      headers,
      credentials: 'include',
    });
  }
}



export interface OrderItemRequestDto {
  variantId: string;
  quantity: number;
}

export interface CreateOrderRequestDto {
  customerName: string;
  customerPhone: string;
  deliveryType: string;
  city?: string;
  address?: string;
  customerNotes?: string;
  items: OrderItemRequestDto[];
}

export interface OrderItemResponseDto {
  id: string;
  variantId: string;
  sku: string;
  productName: string;
  variantName?: string;
  unitPrice: number;
  quantity: number;
  subtotal: number;
}

export interface OrderCheckoutResponseDto {
  orderId: string;
  reference: string;
  trackingToken: string;
  status: string;
  expiresAt: string;
  subtotal: number;
  shippingFee: number;
  total: number;
  whatsappUrl: string;
  whatsappMessageText: string;
  items: OrderItemResponseDto[];
}

export interface OrderTrackingResponseDto {
  reference: string;
  status: string;
  expiresAt: string;
  total: number;
  itemCount: number;
  createdAt: string;
  customerName?: string;
  deliveryType?: string;
  whatsappUrl?: string;
  whatsappMessageText?: string;
  items?: OrderItemResponseDto[];
}

export interface PaymentDto {
  id: string;
  orderId: string;
  paymentMethod: 'NEQUI' | 'BANCOLOMBIA' | 'DAVIPLATA' | 'EFECTIVO' | 'OTRO';
  transferReference: string;
  amount: number;
  status: 'PENDIENTE' | 'VERIFICADO' | 'RECHAZADO';
  verifiedByEmail?: string;
  verifiedAt?: string;
  notes?: string;
  receiptImageUrl?: string;
  createdAt?: string;
}

export interface ConfirmPaymentDto {
  paymentMethod: 'NEQUI' | 'BANCOLOMBIA' | 'DAVIPLATA' | 'EFECTIVO' | 'OTRO';
  transferReference?: string;
  amount: number;
  notes?: string;
  receiptImageUrl?: string;
}

export interface StockReservationDto {
  id: string;
  variantId?: string;
  sku?: string;
  productName?: string;
  quantity: number;
  expiresAt: string;
  status: string;
  createdAt: string;
}

export interface OrderStatusHistoryDto {
  id: string;
  previousStatus?: string;
  newStatus: string;
  userEmail?: string;
  notes?: string;
  createdAt: string;
}

export interface AdminOrderSummaryDto {
  id: string;
  reference: string;
  customerName: string;
  customerPhone: string;
  deliveryType: string;
  total: number;
  status: string;
  channel: string;
  createdAt: string;
  paidAt?: string;
  expiresAt: string;
}

export interface AdminOrderDetailDto {
  id: string;
  reference: string;
  customerName: string;
  customerPhone: string;
  deliveryType: string;
  city?: string;
  address?: string;
  customerNotes?: string;
  subtotal: number;
  discount: number;
  shippingFee: number;
  total: number;
  status: string;
  channel: string;
  expiresAt: string;
  paidAt?: string;
  createdAt: string;
  updatedAt: string;
  items: OrderItemResponseDto[];
  payments: PaymentDto[];
  reservations: StockReservationDto[];
  statusHistory: OrderStatusHistoryDto[];
}

export const api = new ApiClient();

// --- Ventas y Analítica DTOs ---

export interface SalesSummaryDto {
  totalSales: number;
  paidOrdersCount: number;
  totalUnitsSold: number;
  averageTicket: number;
  startDate: string;
  endDate: string;
  merchandiseRevenue?: number;
  coveredMerchandiseRevenue?: number;
  knownCostTotal?: number;
  grossProfit?: number;
  grossMarginPercentage?: number | null;
  unitsWithoutCost?: number;
}

export interface SaleOrderItemDto {
  id: string;
  sku: string;
  productName: string;
  brandName?: string;
  shadeName?: string;
  quantity: number;
  unitPrice: number;
  subtotal: number;
  costSnapshot?: number;
}

export interface SaleOrderDto {
  id: string;
  reference: string;
  customerName: string;
  customerPhone: string;
  paymentMethod?: string;
  subtotal: number;
  discount: number;
  shippingFee: number;
  total: number;
  verifiedBy?: string;
  status: string;
  channel: string;
  paidAt?: string;
  localPaidAt?: string;
  items: SaleOrderItemDto[];
}

export interface SalesListResponseDto {
  summary: SalesSummaryDto;
  orders: SaleOrderDto[];
  page: number;
  size: number;
  totalElements: number;
  totalPages: number;
}

export interface TopProductDto {
  rank: number;
  sku: string;
  productName: string;
  brandName?: string;
  unitsSold: number;
  grossRevenue: number;
  revenueSharePercentage: number;
}

export interface DailySalesDto {
  date: string;
  totalSales: number;
  ordersCount: number;
  unitsCount: number;
}

export interface PaymentMethodBreakdownDto {
  paymentMethod: string;
  totalAmount: number;
  orderCount: number;
  percentage: number;
}

export interface SalesAnalyticsResponseDto {
  summary: SalesSummaryDto;
  dailySales: DailySalesDto[];
  topProducts: TopProductDto[];
  paymentMethods: PaymentMethodBreakdownDto[];
}

export interface InventorySummaryDto {
  variantId: string;
  productId: string;
  sku: string;
  productName: string;
  brandName?: string;
  categoryName?: string;
  shadeName?: string;
  stockOnHand: number;
  stockReserved: number;
  availableStock: number;
  minStock: number;
  isLowStock: boolean;
  active: boolean;
}

export interface InventoryMovementDto {
  id: string;
  variantId: string;
  sku: string;
  productName: string;
  shadeName?: string;
  movementType: string;
  quantityDelta: number;
  qtyBefore: number;
  qtyAfter: number;
  reference?: string;
  reason?: string;
  actorEmail?: string;
  createdAt: string;
}


