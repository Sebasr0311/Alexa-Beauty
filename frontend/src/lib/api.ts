/**
 * Cliente HTTP tipado para la API de Alexa Beauty.
 * Soporta conexión contra el backend Spring Boot (/api/v1) con fallback resiliente.
 */

const API_BASE_URL = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api/v1';

export interface ProductVariantDto {
  id: string;
  sku: string;
  shadeName?: string;
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
  brandName: string;
  categoryName: string;
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

class ApiClient {
  private token: string | null = null;

  setToken(token: string | null) {
    this.token = token;
    if (typeof window !== 'undefined') {
      if (token) {
        localStorage.setItem('alexa_auth_token', token);
      } else {
        localStorage.removeItem('alexa_auth_token');
      }
    }
  }

  getToken(): string | null {
    if (!this.token && typeof window !== 'undefined') {
      this.token = localStorage.getItem('alexa_auth_token');
    }
    return this.token;
  }

  private async request<T>(endpoint: string, options: RequestInit = {}): Promise<T> {
    const headers: Record<string, string> = {
      'Content-Transformed': 'true',
      ...(options.headers as Record<string, string>),
    };

    if (!(options.body instanceof FormData)) {
      headers['Content-Type'] = 'application/json';
    }

    const token = this.getToken();
    if (token) {
      headers['Authorization'] = `Bearer ${token}`;
    }

    const response = await fetch(`${API_BASE_URL}${endpoint}`, {
      ...options,
      headers,
    });

    if (!response.ok) {
      let errorMsg = `HTTP Error ${response.status}`;
      try {
        const errorJson = await response.json();
        errorMsg = errorJson.detail || errorJson.message || errorMsg;
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
    return json.data !== undefined ? json.data : json;
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
    if (params?.q) searchParams.set('q', params.q);
    if (params?.category) searchParams.set('category', params.category);
    if (params?.brand) searchParams.set('brand', params.brand);
    if (params?.minPrice !== undefined) searchParams.set('minPrice', params.minPrice.toString());
    if (params?.maxPrice !== undefined) searchParams.set('maxPrice', params.maxPrice.toString());
    if (params?.inStockOnly) searchParams.set('inStock', 'true');
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
    if (res.token) {
      this.setToken(res.token);
    }
    return res;
  }

  async logout(): Promise<void> {
    try {
      await this.request('/auth/logout', { method: 'POST' });
    } finally {
      this.setToken(null);
    }
  }

  async getMe(): Promise<LoginResponseDto['user']> {
    return this.request<LoginResponseDto['user']>('/auth/me');
  }

  // --- Admin Catálogo ---
  async getAdminProducts(params?: { q?: string; page?: number; size?: number }): Promise<PageDto<ProductSummaryDto>> {
    const searchParams = new URLSearchParams();
    if (params?.q) searchParams.set('q', params.q);
    if (params?.page !== undefined) searchParams.set('page', params.page.toString());
    if (params?.size !== undefined) searchParams.set('size', params.size.toString());
    const query = searchParams.toString();
    return this.request<PageDto<ProductSummaryDto>>(`/admin/products${query ? `?${query}` : ''}`);
  }

  async createProduct(data: {
    name: string;
    brandId: string;
    categoryId: string;
    description?: string;
    sku: string;
    salePrice: number;
    stockOnHand: number;
    minStock?: number;
    featured?: boolean;
    variants?: Array<{
      sku: string;
      shadeName?: string;
      presentation?: string;
      salePrice: number;
      stockOnHand: number;
      minStock?: number;
    }>;
  }): Promise<ProductDetailDto> {
    return this.request<ProductDetailDto>('/admin/products', {
      method: 'POST',
      body: JSON.stringify(data),
    });
  }

  async toggleProductStatus(id: string, active: boolean): Promise<ProductSummaryDto> {
    return this.request<ProductSummaryDto>(`/admin/products/${id}/status`, {
      method: 'PATCH',
      body: JSON.stringify({ active }),
    });
  }

  async uploadProductImage(productId: string, file: File, isCover: boolean = false): Promise<ProductImageDto> {
    const formData = new FormData();
    formData.append('file', file);
    formData.append('isCover', isCover.toString());
    return this.request<ProductImageDto>(`/admin/products/${productId}/images`, {
      method: 'POST',
      body: formData,
    });
  }

  async deleteProductImage(productId: string, imageId: string): Promise<void> {
    return this.request<void>(`/admin/products/${productId}/images/${imageId}`, {
      method: 'DELETE',
    });
  }
}

export const api = new ApiClient();
