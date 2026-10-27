'use client';

import React, { useState, useEffect, useCallback } from 'react';
import Link from 'next/link';
import Image from 'next/image';
import {
  Package,
  Plus,
  Search,
  Upload,
  Eye,
  CheckCircle2,
  XCircle,
  RefreshCw,
  AlertTriangle,
  AlertCircle,
  X,
  Tag,
  Boxes,
  Image as ImageIcon,
  Trash2,
  PauseCircle,
  PlayCircle,
  Star,
  UploadCloud,
  Loader2,
  FileSpreadsheet,
} from 'lucide-react';
import { VariantFields, emptyVariant, variantPayload } from '@/components/admin/VariantFields';
import { BrandPicker } from '@/components/admin/BrandPicker';
import { ProductEditor } from '@/components/admin/ProductEditor';
import { VariantManager } from '@/components/admin/VariantManager';
import type { VariantInput } from '@/lib/api';
import AdminShell from '@/components/admin/AdminShell';
import { formatCOP } from '@/lib/utils';
import {
  api,
  ProductSummaryDto,
  ProductImageDto,
  CategoryDto,
  BrandDto,
} from '@/lib/api';

const EMPTY_PRODUCTS: ProductSummaryDto[] = [];

export default function AdminProductosPage() {
  const [products, setProducts] = useState<ProductSummaryDto[]>(EMPTY_PRODUCTS);
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [brands, setBrands] = useState<BrandDto[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [currentPage, setCurrentPage] = useState<number>(0);
  const [totalPages, setTotalPages] = useState<number>(1);
  const [totalElements, setTotalElements] = useState<number>(0);
  const [selectedCategory, setSelectedCategory] = useState<string>('TODAS');
  const [statusFilter, setStatusFilter] = useState<'ALL' | 'ACTIVE' | 'INACTIVE'>('ALL');
  const [loading, setLoading] = useState(false);
  const [exporting, setExporting] = useState(false);
  const [feedback, setFeedback] = useState<{ type: 'success' | 'error'; text: string } | null>(null);

  const [editingProductId, setEditingProductId] = useState<string | null>(null);
  const [imageError, setImageError] = useState('');
  const [newProductCost, setNewProductCost] = useState<number | undefined>(undefined);
  const [newVariants, setNewVariants] = useState<VariantInput[]>([]);
  const [variantsProductId, setVariantsProductId] = useState<string | null>(null);

  // Modal Nuevo Producto
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [creating, setCreating] = useState(false);
  const [modalError, setModalError] = useState<string | null>(null);
  const [newProductName, setNewProductName] = useState('');
  const [newProductSku, setNewProductSku] = useState('');
  const [newProductBrandId, setNewProductBrandId] = useState('');
  const [newProductCategoryId, setNewProductCategoryId] = useState('');
  const [newProductPrice, setNewProductPrice] = useState<number>(0);
  const [newProductStock, setNewProductStock] = useState<number>(10);
  const [newProductMinStock, setNewProductMinStock] = useState<number>(3);
  const [newProductShade, setNewProductShade] = useState('');
  const [newProductDescription, setNewProductDescription] = useState('');
  const [newProductImageFile, setNewProductImageFile] = useState<File | null>(null);
  const [newProductImagePreview, setNewProductImagePreview] = useState<string | null>(null);

  // Modal Gestionar Fotos
  const [imageModalProduct, setImageModalProduct] = useState<ProductSummaryDto | null>(null);
  const [productImages, setProductImages] = useState<ProductImageDto[]>([]);
  const [loadingImages, setLoadingImages] = useState(false);
  const [uploadingImage, setUploadingImage] = useState(false);
  const [selectedUploadFile, setSelectedUploadFile] = useState<File | null>(null);
  const [uploadFilePreview, setUploadFilePreview] = useState<string | null>(null);
  const [isCoverUpload, setIsCoverUpload] = useState(false);

  // Modal Confirmar Eliminación
  const [productToDelete, setProductToDelete] = useState<ProductSummaryDto | null>(null);
  const [deleting, setDeleting] = useState(false);
  const [lastSyncedAt, setLastSyncedAt] = useState<Date | null>(null);

  // Modal Categorías (Creación y Eliminación)
  const [isAddCategoryModalOpen, setIsAddCategoryModalOpen] = useState(false);
  const [newCategoryName, setNewCategoryName] = useState('');
  const [creatingCategory, setCreatingCategory] = useState(false);
  const [categoryModalError, setCategoryModalError] = useState<string | null>(null);
  const [categoryToDelete, setCategoryToDelete] = useState<CategoryDto | null>(null);
  const [deletingCategory, setDeletingCategory] = useState(false);

  useEffect(() => {
    setCurrentPage(0);
  }, [searchQuery]);

  const loadData = useCallback(async (pageOverride?: number) => {
    setLoading(true);
    const targetPage = pageOverride !== undefined ? pageOverride : currentPage;
    try {
      const [catsRes, brandsRes, prodsRes] = await Promise.allSettled([
        api.getCategories(),
        api.getAdminBrands(),
        api.getAdminProducts({ q: searchQuery || undefined, page: targetPage, size: 20, sort: 'createdAt,desc' }),
      ]);

      if (catsRes.status === 'fulfilled' && catsRes.value?.length > 0) {
        setCategories(catsRes.value);
        setNewProductCategoryId((prev) => prev || catsRes.value[0]?.id || '');
      }
      if (brandsRes.status === 'fulfilled' && brandsRes.value?.length > 0) {
        setBrands(brandsRes.value);
        setNewProductBrandId((prev) => prev || brandsRes.value[0]?.id || '');
      }
      if (prodsRes.status === 'fulfilled') {
        setProducts(prodsRes.value?.content || []);
        setTotalPages(prodsRes.value?.totalPages || 1);
        setTotalElements(prodsRes.value?.totalElements || 0);
        setLastSyncedAt(new Date());
      } else {
        setFeedback({
          type: 'error',
          text: 'Error al sincronizar el catálogo de productos desde el servidor.',
        });
      }
    } catch {
      setFeedback({ type: 'error', text: 'No se pudieron sincronizar los productos del servidor.' });
    } finally {
      setLoading(false);
    }
  }, [searchQuery, currentPage]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  useEffect(() => {
    const onFocus = () => void loadData();
    window.addEventListener('focus', onFocus);
    return () => window.removeEventListener('focus', onFocus);
  }, [loadData]);

  // Suspender / Activar Producto
  const handleToggleStatus = async (productId: string, currentStatus: boolean) => {
    try {
      await api.toggleProductStatus(productId, !currentStatus);
      const actionText = !currentStatus ? 'activado' : 'suspendido';
      setFeedback({
        type: 'success',
        text: `Producto ${actionText} con éxito.`,
      });
      setProducts((prev) =>
        prev.map((p) => (p.id === productId ? { ...p, active: !currentStatus } : p))
      );
    } catch {
      setFeedback({ type: 'error', text: 'Error al cambiar el estado del producto.' });
    }
  };

  // Eliminar Producto
  const handleDeleteProduct = async () => {
    if (!productToDelete) return;
    setDeleting(true);
    try {
      await api.deleteProduct(productToDelete.id);
      setProducts((prev) => prev.filter((p) => p.id !== productToDelete.id));
      setFeedback({
        type: 'success',
        text: `El producto "${productToDelete.name}" fue eliminado del catálogo.`,
      });
      setProductToDelete(null);
    } catch {
      setFeedback({
        type: 'error',
        text: 'No se pudo eliminar el producto. Revisa si tiene unidades reservadas en pedidos pendientes.',
      });
    } finally {
      setDeleting(false);
    }
  };

  // Gestión de Categorías
  const handleCreateCategory = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newCategoryName.trim()) {
      setCategoryModalError('El nombre de la categoría es obligatorio.');
      return;
    }
    setCreatingCategory(true);
    setCategoryModalError(null);
    try {
      const created = await api.createCategory({ name: newCategoryName.trim() });
      setCategories((prev) => [...prev, created]);
      setSelectedCategory(created.name);
      setIsAddCategoryModalOpen(false);
      setNewCategoryName('');
      setFeedback({ type: 'success', text: `Categoría "${created.name}" creada exitosamente.` });
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'Error al crear la categoría';
      setCategoryModalError(msg);
    } finally {
      setCreatingCategory(false);
    }
  };

  const handleDeleteCategory = async () => {
    if (!categoryToDelete) return;
    setDeletingCategory(true);
    try {
      await api.deleteCategory(categoryToDelete.id);
      setCategories((prev) => prev.filter((c) => c.id !== categoryToDelete.id));
      if (selectedCategory.toLowerCase() === categoryToDelete.name.toLowerCase()) {
        setSelectedCategory('TODAS');
      }
      setFeedback({
        type: 'success',
        text: `Categoría "${categoryToDelete.name}" eliminada exitosamente.`,
      });
      setCategoryToDelete(null);
    } catch (err: unknown) {
      const msg = err instanceof Error ? err.message : 'No se pudo eliminar la categoría';
      setFeedback({
        type: 'error',
        text: msg,
      });
      setCategoryToDelete(null);
    } finally {
      setDeletingCategory(false);
    }
  };

  // Abrir Modal de Fotos
  const handleOpenImagesModal = async (product: ProductSummaryDto) => {
    setImageModalProduct(product);
    setLoadingImages(true);
    setImageError('');
    setSelectedUploadFile(null);
    setUploadFilePreview(null);
    setIsCoverUpload(false);
    try {
      const detail = await api.getAdminProductById(product.id);
      setProductImages(detail.images || []);
    } catch {
      setProductImages([]);
      setFeedback({ type: 'error', text: 'No se pudieron cargar las fotos del producto.' });
    } finally {
      setLoadingImages(false);
    }
  };

  // Subir Nueva Foto
  const handleUploadImage = async () => {
    if (!imageModalProduct || !selectedUploadFile) return;
    setUploadingImage(true);
    setImageError('');
    try {
      const uploaded = await api.uploadProductImage(
        imageModalProduct.id,
        selectedUploadFile,
        isCoverUpload || productImages.length === 0
      );
      setProductImages((prev) => uploaded.isCover ? [...prev.map(img => ({...img,isCover:false})),uploaded] : [...prev,uploaded]);
      setSelectedUploadFile(null);
      setUploadFilePreview(null);
      setFeedback({ type: 'success', text: 'Foto adjuntada correctamente al producto.' });
      await loadData();
    } catch (error) {
      setImageError(error instanceof Error ? error.message : 'No se pudo guardar la foto. Reintenta sin cerrar esta ventana.');
    } finally {
      setUploadingImage(false);
    }
  };

  // Marcar Foto como Portada
  const handleSetCoverImage = async (imageId: string) => {
    if (!imageModalProduct) return;
    try {
      await api.setCoverProductImage(imageModalProduct.id, imageId);
      setProductImages((prev) =>
        prev.map((img) => ({ ...img, isCover: img.id === imageId }))
      );
      setFeedback({ type: 'success', text: 'Portada principal actualizada.' });
      await loadData();
    } catch {
      setFeedback({ type: 'error', text: 'No se pudo establecer como portada.' });
    }
  };

  // Eliminar Foto individual
  const handleDeleteImage = async (imageId: string) => {
    if (!imageModalProduct) return;
    try {
      await api.deleteProductImage(imageModalProduct.id, imageId);
      setProductImages((prev) => prev.filter((img) => img.id !== imageId));
      setFeedback({ type: 'success', text: 'Foto eliminada con éxito.' });
      await loadData();
    } catch {
      setFeedback({ type: 'error', text: 'No se pudo eliminar la imagen.' });
    }
  };

  // Manejo de archivo en modal de nuevo producto
  const handleNewProductImageChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setNewProductImageFile(file);
      const objectUrl = URL.createObjectURL(file);
      setNewProductImagePreview(objectUrl);
    } else {
      setNewProductImageFile(null);
      setNewProductImagePreview(null);
    }
  };

  // Manejo de archivo en modal de fotos
  const handleUploadFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0];
    if (file) {
      setSelectedUploadFile(file);
      const objectUrl = URL.createObjectURL(file);
      setUploadFilePreview(objectUrl);
    } else {
      setSelectedUploadFile(null);
      setUploadFilePreview(null);
    }
  };

  // Abrir Modal de Creación
  const handleOpenCreateModal = () => {
    setModalError(null);
    if (!newProductCategoryId && categories.length > 0) {
      setNewProductCategoryId(categories[0].id);
    }
    if (!newProductBrandId && brands.length > 0) {
      setNewProductBrandId(brands[0].id);
    }
    setIsModalOpen(true);
  };

  // Crear Producto
  const handleCreateProduct = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!newProductName.trim()) {
      setModalError('El nombre del producto es obligatorio.');
      return;
    }
    if (newVariants.length === 0 && (!newProductPrice || newProductPrice <= 0)) {
      setModalError('Debes ingresar un precio de venta mayor a 0.');
      return;
    }

    const brandIdToUse = newProductBrandId || brands[0]?.id;
    const categoryIdToUse = newProductCategoryId || categories[0]?.id;

    if (!brandIdToUse || !categoryIdToUse) {
      setModalError('Por favor selecciona una categoría y una marca válidas.');
      return;
    }

    setCreating(true);
    setModalError(null);

    try {
      const created = await api.createProduct({
        name: newProductName.trim(),
        sku: newProductSku.trim().toUpperCase() || undefined,
        brandId: brandIdToUse,
        categoryId: categoryIdToUse,
        salePrice: newVariants.length ? newVariants[0].salePrice : newProductPrice,
        referenceCost: newProductCost,
        stockOnHand: newProductStock,
        minStock: newProductMinStock,
        description: newProductDescription.trim() || undefined,
        variants: newVariants.length ? newVariants.map(variantPayload) : newProductShade.trim()
          ? [
              {
                sku: newProductSku.trim() ? `${newProductSku.trim().toUpperCase()}-V1` : undefined,
                shadeName: newProductShade.trim(),
                salePrice: newProductPrice,
                referenceCost: newProductCost,
                stockOnHand: newProductStock,
                minStock: newProductMinStock,
              },
            ]
          : undefined,
      });

      let coverUrl: string | undefined;
      let photoError = '';

      // Subir imagen si se adjuntó una
      if (newProductImageFile && created?.id) {
        try {
          const uploadedImg = await api.uploadProductImage(created.id, newProductImageFile, true);
          coverUrl = uploadedImg.url;
        } catch (error) {
          photoError = error instanceof Error ? error.message : 'No se pudo guardar la foto.';
        }
      }

      // Optimistic / Inmediato: reflejar el producto nuevo de inmediato en la tabla
      const createdSummary: ProductSummaryDto = {
        id: created.id,
        sku: created.sku,
        name: created.name,
        slug: created.slug,
        variants: created.variants,
        brandName: created.brand?.name || brands.find((b) => b.id === brandIdToUse)?.name || 'Alexa Beauty',
        categoryName: created.category?.name || categories.find((c) => c.id === categoryIdToUse)?.name || 'General',
        brand: created.brand,
        category: created.category,
        minPrice: created.minPrice ?? (newVariants[0]?.salePrice || newProductPrice || 0),
        maxPrice: created.maxPrice ?? (newVariants[0]?.salePrice || newProductPrice || 0),
        totalAvailableStock: created.totalAvailableStock,
        coverImageUrl: coverUrl || created.images?.find((img) => img.isCover)?.url || created.images?.[0]?.url,
        featured: created.featured,
        active: created.active,
      };

      setProducts((prev) => [createdSummary, ...prev.filter((p) => p.id !== created.id)]);
      setTotalElements((prev) => prev + 1);
      setCurrentPage(0);
      setSelectedCategory('TODAS');
      setStatusFilter('ALL');
      setSearchQuery('');
      setIsModalOpen(false);
      if (photoError && newProductImageFile) {
        setImageModalProduct(createdSummary);
        setProductImages(created.images || []);
        setSelectedUploadFile(newProductImageFile);
        setUploadFilePreview(newProductImagePreview);
        setIsCoverUpload(true);
        setImageError(`Producto creado. La foto no se guardó: ${photoError} Puedes reintentar aquí sin crear otro producto.`);
        setFeedback({type:'error',text:'Producto creado sin foto. Completa la carga desde la ventana de fotos.'});
      } else {
        setFeedback({ type: 'success', text: `Producto "${created.name}" creado correctamente.` });
      }
      resetForm();

      // Sincronizar en segundo plano con el servidor en la primera página
      void loadData(0);
    } catch (error) {
      setModalError(error instanceof Error ? error.message : 'No se pudo crear el producto.');
    } finally {
      setCreating(false);
    }
  };

  const resetForm = () => {
    setNewVariants([]);
    setNewProductName('');
    setNewProductSku('');
    setNewProductPrice(0);
    setNewProductCost(undefined);
    setNewProductStock(10);
    setNewProductMinStock(3);
    setNewProductShade('');
    setNewProductDescription('');
    setNewProductImageFile(null);
    setNewProductImagePreview(null);
    setModalError(null);
    if (categories.length > 0) setNewProductCategoryId(categories[0].id);
    if (brands.length > 0) setNewProductBrandId(brands[0].id);
  };

  const filteredProducts = products.filter((p) => {
    const brandName = p.brand?.name || p.brandName || '';
    const categoryName = p.category?.name || p.categoryName || '';
    const matchesSearch =
      p.name.toLowerCase().includes(searchQuery.toLowerCase()) ||
      p.sku.toLowerCase().includes(searchQuery.toLowerCase()) ||
      brandName.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesCategory =
      selectedCategory === 'TODAS' || categoryName.toLowerCase() === selectedCategory.toLowerCase();
    const matchesStatus =
      statusFilter === 'ALL' ||
      (statusFilter === 'ACTIVE' && p.active) ||
      (statusFilter === 'INACTIVE' && !p.active);
    return matchesSearch && matchesCategory && matchesStatus;
  });

  return (
    <AdminShell
      badge="Epic 2 · Catálogo Oficial"
      title="Catálogo de Productos"
      subtitle="Administración de fichas comerciales, imágenes, precios autoritativos, suspensión y bajas"
      actions={
        <>
          <button
            type="button"
            onClick={handleOpenCreateModal}
            className="inline-flex items-center gap-2 rounded-full bg-wine hover:bg-wineDark text-white font-bold text-xs sm:text-sm px-5 py-2.5 shadow-glow transition active:scale-95"
          >
            <Plus className="w-4 h-4" />
            <span>Nuevo Producto</span>
          </button>
          <Link
            href="/admin/importar"
            className="inline-flex items-center gap-2 rounded-full bg-white border border-border hover:bg-cream text-wine font-bold text-xs sm:text-sm px-4 py-2.5 shadow-xs transition"
          >
            <Upload className="w-4 h-4" />
            <span>Importar Excel</span>
          </Link>
        </>
      }
    >
      {/* Mensajes de feedback */}
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

      {/* Barra de Filtros y Estado */}
      <div className="bg-white border border-border rounded-2xl p-4 shadow-sm flex flex-col md:flex-row items-center justify-between gap-4">
        <div className="relative w-full md:w-80">
          <Search className="w-4 h-4 absolute left-3.5 top-1/2 -translate-y-1/2 text-muted" />
          <input
            type="text"
            value={searchQuery}
            onChange={(e) => setSearchQuery(e.target.value)}
            placeholder="Buscar por nombre o SKU..."
            className="w-full pl-9 pr-4 py-2 bg-cream/50 border border-border rounded-xl text-xs sm:text-sm focus:outline-none focus:ring-2 focus:ring-wine/20 text-ink"
          />
        </div>

        <div className="flex items-center gap-3 w-full md:w-auto flex-wrap">
          {/* Status Tabs */}
          <div className="flex items-center bg-cream rounded-xl p-1 border border-border/70 text-xs font-bold">
            <button
              type="button"
              onClick={() => setStatusFilter('ALL')} title="Total de referencias o productos en catálogo (no unidades de inventario)"
              className={`px-3 py-1 rounded-lg transition ${
                statusFilter === 'ALL'
                  ? 'bg-wine text-white shadow-xs'
                  : 'text-muted hover:text-ink'
              }`}
            >
              Todos ({selectedCategory === 'TODAS' ? totalElements : filteredProducts.length} refs)
            </button>
            <button
              type="button"
              onClick={() => setStatusFilter('ACTIVE')}
              className={`px-3 py-1 rounded-lg transition ${
                statusFilter === 'ACTIVE'
                  ? 'bg-emerald-700 text-white shadow-xs'
                  : 'text-muted hover:text-ink'
              }`}
            >
              Activos
            </button>
            <button
              type="button"
              onClick={() => setStatusFilter('INACTIVE')}
              className={`px-3 py-1 rounded-lg transition ${
                statusFilter === 'INACTIVE'
                  ? 'bg-rose-700 text-white shadow-xs'
                  : 'text-muted hover:text-ink'
              }`}
            >
              Suspendidos
            </button>
          </div>

          {/* Categorías */}
          <div className="flex items-center gap-1.5 overflow-x-auto no-scrollbar">
            <button
              type="button"
              onClick={() => setSelectedCategory('TODAS')}
              className={`px-2.5 py-1 rounded-lg text-xs font-bold transition whitespace-nowrap ${
                selectedCategory === 'TODAS'
                  ? 'bg-wine/10 text-wine font-extrabold border border-wine/30'
                  : 'bg-cream text-muted hover:text-ink'
              }`}
            >
              Todas
            </button>
            {categories.map((cat) => (
              <div
                key={cat.id}
                className={`inline-flex items-center rounded-lg text-xs font-bold transition whitespace-nowrap ${
                  selectedCategory.toLowerCase() === cat.name.toLowerCase()
                    ? 'bg-wine/10 text-wine font-extrabold border border-wine/30'
                    : 'bg-cream text-muted hover:text-ink'
                }`}
              >
                <button
                  type="button"
                  onClick={() => setSelectedCategory(cat.name)}
                  className="px-2.5 py-1"
                >
                  {cat.name}
                </button>
                <button
                  type="button"
                  onClick={(e) => {
                    e.stopPropagation();
                    setCategoryToDelete(cat);
                  }}
                  title={`Eliminar categoría ${cat.name}`}
                  className="pr-1.5 pl-0.5 py-1 hover:text-rose-600 transition opacity-60 hover:opacity-100"
                >
                  <X className="w-3 h-3" />
                </button>
              </div>
            ))}
            <button
              type="button"
              onClick={() => {
                setCategoryModalError(null);
                setNewCategoryName('');
                setIsAddCategoryModalOpen(true);
              }}
              title="Agregar nueva categoría"
              className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-bold bg-cream hover:bg-blush text-wine transition whitespace-nowrap border border-dashed border-wine/30"
            >
              <Plus className="w-3.5 h-3.5" />
              <span>Categoría</span>
            </button>


            <div className="flex items-center gap-1.5 ml-1">
              <button
                type="button"
                onClick={() => void loadData()}
                title="Recargar catálogo de productos"
                className="p-1.5 text-muted hover:text-wine rounded-lg bg-cream hover:bg-blush transition"
              >
                <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
              </button>
              {lastSyncedAt && (
                <span className="text-[10px] text-muted hidden md:inline-block whitespace-nowrap">
                  {lastSyncedAt.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit', second: '2-digit' })}
                </span>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* Tabla de Productos */}
      <div className="bg-white border border-border rounded-3xl p-6 shadow-soft overflow-hidden">
        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs sm:text-sm border-collapse">
            <thead>
              <tr className="bg-blush text-wine font-bold border-b border-border">
                <th className="py-3 px-3.5 rounded-l-xl">Producto</th>
                <th className="py-3 px-3.5">SKU</th>
                <th className="py-3 px-3.5 hidden sm:table-cell">Categoría</th>
                <th className="py-3 px-3.5">Precio COP</th>
                <th className="py-3 px-3.5" title="Unidades disponibles para venta inmediata / Físico en bodega / Reservado en pedidos WhatsApp">Stock (Disp. / Físico)</th>
                <th className="py-3 px-3.5">Estado</th>
                <th className="py-3 px-3.5 rounded-r-xl text-right">Acciones</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-border/60">
              {filteredProducts.length === 0 ? (
                <tr>
                  <td colSpan={8} className="py-12 text-center text-muted">
                    <Package className="w-8 h-8 mx-auto mb-2 text-mauve/60" />
                    <p className="font-medium">No se encontraron productos con los filtros seleccionados.</p>
                  </td>
                </tr>
              ) : (
                filteredProducts.map((p) => (
                  <tr key={p.id} className="hover:bg-cream/40 transition">
                    <td className="py-3.5 px-3.5">
                      <div className="flex items-center gap-3">
                        <div className="relative w-11 h-11 rounded-xl overflow-hidden bg-blush border border-border flex-shrink-0">
                          <Image unoptimized
                            src={p.coverImageUrl || '/branding/logo.png'}
                            alt={p.name}
                            fill
                            sizes="44px"
                            className="object-cover"
                          />
                        </div>
                        <div>
                          <span className="font-semibold text-ink block leading-tight">
                            {p.name}
                          </span>
                          <span className="text-[11px] text-muted">
                            {p.brand?.name || p.brandName || 'Alexa Beauty'}
                          </span>
                        </div>
                      </div>
                    </td>
                    <td className="py-3.5 px-3.5 font-mono text-xs font-semibold text-wine">
                      {p.sku}
                    </td>
                    <td className="py-3.5 px-3.5 hidden sm:table-cell text-muted font-medium">
                      {p.category?.name || p.categoryName || 'General'}
                    </td>
                    <td className="py-3.5 px-3.5 font-bold text-ink">
                      {formatCOP(p.minPrice)}
                    </td>
                    <td className="py-3.5 px-3.5">
                      <div className="flex flex-col">
                        <span
                          className={`inline-flex items-center gap-1 font-bold ${
                            p.totalAvailableStock <= 5
                              ? 'text-rose-600'
                              : 'text-emerald-700'
                          }`}
                          title="Unidades disponibles inmediatamente para venta"
                        >
                          <Boxes className="w-3.5 h-3.5 flex-shrink-0" />
                          {p.totalAvailableStock} disp.
                        </span>
                        {p.totalStockOnHand != null && (
                          <span className="text-[11px] text-muted tracking-tight mt-0.5">
                            {p.totalStockOnHand} físico
                            {p.totalStockOnHand > p.totalAvailableStock && (
                              <span className="text-amber-700 font-semibold ml-1" title="Unidades bloqueadas en pedidos de WhatsApp pendientes de confirmación">
                                ({p.totalStockOnHand - p.totalAvailableStock} reserv.)
                              </span>
                            )}
                          </span>
                        )}
                      </div>
                    </td>
                    <td className="py-3.5 px-3.5">
                      <span
                        className={`inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[11px] font-bold ${
                          p.active
                            ? 'bg-emerald-100 text-emerald-800'
                            : 'bg-rose-100 text-rose-800'
                        }`}
                      >
                        {p.active ? (
                          <>
                            <CheckCircle2 className="w-3 h-3" />
                            Activo
                          </>
                        ) : (
                          <>
                            <XCircle className="w-3 h-3" />
                            Suspendido
                          </>
                        )}
                      </span>
                    </td>
                    <td className="py-3.5 px-3.5 text-right">
                      <div className="inline-flex items-center justify-end gap-1.5 flex-wrap">
                        <button type="button" onClick={()=>setEditingProductId(p.id)} className="min-h-11 rounded-xl border border-border px-3 text-xs font-bold text-wine">Editar</button>
                        {/* Fotos Modal Trigger */}
                        <button type="button" onClick={() => setVariantsProductId(p.id)} className="min-h-11 rounded-xl border border-border px-3 text-xs font-bold text-wine" aria-label={`Gestionar tonos de ${p.name}`}>Tonos</button>
                        <button
                          type="button"
                          onClick={() => handleOpenImagesModal(p)}
                          className="inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-bold text-wine bg-blush/60 hover:bg-blush transition"
                          title="Gestionar fotos e imagen de portada"
                        >
                          <ImageIcon className="w-3.5 h-3.5" />
                          <span>Fotos</span>
                        </button>

                        {/* Suspender / Activar Button */}
                        <button
                          type="button"
                          onClick={() => handleToggleStatus(p.id, p.active)}
                          className={`inline-flex items-center gap-1 px-2.5 py-1 rounded-lg text-xs font-bold transition ${
                            p.active
                              ? 'text-amber-800 bg-amber-50 hover:bg-amber-100 border border-amber-200'
                              : 'text-emerald-800 bg-emerald-50 hover:bg-emerald-100 border border-emerald-200'
                          }`}
                          title={p.active ? 'Suspender producto (ocultar de la tienda)' : 'Activar producto en tienda'}
                        >
                          {p.active ? (
                            <>
                              <PauseCircle className="w-3.5 h-3.5" />
                              <span>Suspender</span>
                            </>
                          ) : (
                            <>
                              <PlayCircle className="w-3.5 h-3.5" />
                              <span>Activar</span>
                            </>
                          )}
                        </button>

                        {/* Ficha Pública */}
                        <Link
                          href={`/producto/${p.slug}`}
                          target="_blank"
                          className="inline-flex items-center gap-1 px-2 py-1 rounded-lg text-xs font-bold text-muted hover:text-ink hover:bg-cream transition"
                          title="Ver ficha comercial en la tienda"
                        >
                          <Eye className="w-3.5 h-3.5" />
                        </Link>

                        {/* Eliminar Producto */}
                        <button
                          type="button"
                          onClick={() => setProductToDelete(p)}
                          className="inline-flex items-center p-1.5 rounded-lg text-rose-500 hover:text-rose-700 hover:bg-rose-50 transition"
                          title="Eliminar producto"
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

          {/* Pagination Controls */}
          <div className="flex flex-col sm:flex-row items-center justify-between gap-3 border-t border-border pt-4 mt-4">
            <span className="text-xs text-muted">
              Página {currentPage + 1} de {Math.max(1, totalPages)} ({totalElements} productos en total)
            </span>
            <div className="flex items-center gap-2">
              <button
                type="button"
                disabled={currentPage === 0 || loading}
                onClick={() => setCurrentPage((p) => Math.max(0, p - 1))}
                className="px-3 py-1.5 rounded-lg border border-border text-xs font-semibold text-ink hover:bg-cream disabled:opacity-40 disabled:cursor-not-allowed transition"
              >
                Anterior
              </button>
              <button
                type="button"
                disabled={currentPage >= totalPages - 1 || loading}
                onClick={() => setCurrentPage((p) => p + 1)}
                className="px-3 py-1.5 rounded-lg border border-border text-xs font-semibold text-ink hover:bg-cream disabled:opacity-40 disabled:cursor-not-allowed transition"
              >
                Siguiente
              </button>
            </div>
          </div>
        </div>
      </div>

      {/* MODAL: GESTIONAR IMÁGENES */}
      {imageModalProduct && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4"
        >
          <div className="bg-white rounded-3xl max-w-2xl w-full p-6 sm:p-8 shadow-2xl space-y-6 animate-in fade-in zoom-in-95 duration-150 max-h-[90vh] overflow-y-auto">
            {imageError&&<p role="alert" className="bg-red-50 text-red-800 border border-red-200 p-3 rounded-xl">{imageError}</p>}
            <div className="flex items-center justify-between border-b border-border pb-3">
              <div className="flex items-center gap-2.5 text-wine">
                <ImageIcon className="w-5 h-5" />
                <div>
                  <h3 className="font-serif text-lg sm:text-xl font-bold">
                    Fotos de Producto
                  </h3>
                  <p className="text-xs text-muted font-sans font-medium">
                    {imageModalProduct.name} · SKU: {imageModalProduct.sku}
                  </p>
                </div>
              </div>
              <button
                type="button"
                disabled={uploadingImage}
                onClick={() => setImageModalProduct(null)}
                className="text-muted hover:text-ink p-1.5 rounded-full hover:bg-cream"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Galería de Fotos Existentes */}
            <div>
              <label className="font-bold text-ink text-xs uppercase tracking-wider block mb-2">
                Fotos Actuales ({productImages.length})
              </label>
              {loadingImages ? (
                <div className="py-10 text-center text-muted flex items-center justify-center gap-2 text-xs">
                  <Loader2 className="w-4 h-4 animate-spin text-wine" />
                  <span>Cargando fotos del producto...</span>
                </div>
              ) : productImages.length === 0 ? (
                <div className="py-8 text-center text-muted bg-cream/30 border border-dashed border-border rounded-2xl">
                  <ImageIcon className="w-8 h-8 mx-auto mb-2 text-mauve/40" />
                  <p className="text-xs font-semibold">Este producto aún no tiene fotos adjuntas.</p>
                  <p className="text-[11px] text-muted">Sube una imagen abajo para que aparezca en el catálogo.</p>
                </div>
              ) : (
                <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
                  {productImages.map((img) => (
                    <div
                      key={img.id}
                      className={`relative rounded-2xl overflow-hidden border p-2 bg-[#FCF9F7] flex flex-col items-center gap-2 transition ${
                        img.isCover ? 'border-wine ring-2 ring-wine/20 shadow-xs' : 'border-border'
                      }`}
                    >
                      <div className="relative w-full aspect-square rounded-xl overflow-hidden bg-white border border-border/50">
                        <Image unoptimized
                          src={img.url}
                          alt={img.altText || 'Foto del producto'}
                          fill
                          sizes="200px"
                          className="object-cover"
                        />
                        {img.isCover && (
                          <span className="absolute top-1.5 left-1.5 bg-wine text-white text-[10px] font-extrabold uppercase px-2 py-0.5 rounded-full shadow-xs flex items-center gap-1">
                            <Star className="w-3 h-3 fill-current" /> Portada
                          </span>
                        )}
                      </div>

                      <div className="flex items-center justify-between w-full pt-1 px-1">
                        {!img.isCover ? (
                          <button
                            type="button"
                            onClick={() => handleSetCoverImage(img.id)}
                            className="text-[11px] font-bold text-wine hover:underline inline-flex items-center gap-1"
                          >
                            <Star className="w-3 h-3" /> Hacer portada
                          </button>
                        ) : (
                          <span className="text-[11px] font-bold text-muted">Principal</span>
                        )}

                        <button
                          type="button"
                          onClick={() => handleDeleteImage(img.id)}
                          className="text-rose-500 hover:text-rose-700 p-1 rounded-md hover:bg-rose-50 transition"
                          title="Eliminar foto"
                        >
                          <Trash2 className="w-3.5 h-3.5" />
                        </button>
                      </div>
                    </div>
                  ))}
                </div>
              )}
            </div>

            {/* Zona para Subir Nueva Foto */}
            <div className="border-t border-border pt-4 space-y-3">
              <label htmlFor="image-file-input" className="font-bold text-ink text-xs uppercase tracking-wider block">
                Adjuntar Nueva Foto (JPG o PNG, máx 5MB)
              </label>

              <div className="flex flex-col sm:flex-row items-center gap-3">
                <input
                  type="file"
                  id="image-file-input"
                  accept="image/jpeg,image/png,image/jpg"
                  onChange={handleUploadFileChange}
                  className="block w-full text-xs text-muted file:mr-3 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-bold file:bg-wine file:text-white hover:file:bg-wineDark cursor-pointer"
                />

                <label className="flex items-center gap-2 text-xs font-semibold text-ink whitespace-nowrap cursor-pointer">
                  <input
                    type="checkbox"
                    checked={isCoverUpload}
                    onChange={(e) => setIsCoverUpload(e.target.checked)}
                    className="rounded text-wine focus:ring-wine/20 w-4 h-4"
                  />
                  <span>Establecer como portada</span>
                </label>
              </div>

              {uploadFilePreview && (
                <div className="flex items-center gap-3 p-3 bg-cream/40 rounded-xl border border-border">
                  <div className="relative w-12 h-12 rounded-lg overflow-hidden border border-border flex-shrink-0">
                    <Image unoptimized
                      src={uploadFilePreview}
                      alt="Vista previa"
                      fill
                      sizes="48px"
                      className="object-cover"
                    />
                  </div>
                  <div className="flex-1 min-w-0">
                    <p className="text-xs font-bold text-ink truncate">{selectedUploadFile?.name}</p>
                    <p className="text-[10px] text-muted">
                      {selectedUploadFile ? `${(selectedUploadFile.size / 1024).toFixed(0)} KB` : ''}
                    </p>
                  </div>
                  <button
                    type="button"
                    onClick={handleUploadImage}
                    disabled={uploadingImage}
                    className="px-4 py-2 rounded-xl bg-wine hover:bg-wineDark text-white text-xs font-bold transition shadow-xs disabled:opacity-50 inline-flex items-center gap-1.5"
                  >
                    {uploadingImage ? (
                      <>
                        <Loader2 className="w-3.5 h-3.5 animate-spin" />
                        <span>Subiendo...</span>
                      </>
                    ) : (
                      <>
                        <UploadCloud className="w-3.5 h-3.5" />
                        <span>Guardar Foto</span>
                      </>
                    )}
                  </button>
                </div>
              )}
            </div>

            <div className="flex justify-end pt-3 border-t border-border">
              <button
                type="button"
                disabled={uploadingImage}
                onClick={() => setImageModalProduct(null)}
                className="px-5 py-2.5 rounded-full border border-border text-muted font-bold text-xs hover:bg-cream transition"
              >
                Cerrar
              </button>
            </div>
          </div>
        </div>
      )}

      {editingProductId&&<ProductEditor id={editingProductId} onClose={()=>setEditingProductId(null)} onSaved={()=>{void loadData();}}/>}
      {/* MODAL: CONFIRMAR ELIMINACIÓN DE PRODUCTO */}
      {productToDelete && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4"
        >
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-7 shadow-2xl space-y-4 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center gap-3 text-rose-600">
              <div className="w-10 h-10 rounded-2xl bg-rose-100 flex items-center justify-center flex-shrink-0">
                <AlertTriangle className="w-5 h-5 text-rose-600" />
              </div>
              <div>
                <h3 className="font-serif text-lg font-bold text-ink">
                  ¿Eliminar Producto?
                </h3>
                <p className="text-xs text-muted">Esta acción afectará la disponibilidad del catálogo.</p>
              </div>
            </div>

            <div className="bg-cream/50 rounded-2xl p-4 border border-border space-y-2 text-xs">
              <p className="font-bold text-ink">{productToDelete.name}</p>
              <p className="text-muted">SKU: <span className="font-mono text-wine font-semibold">{productToDelete.sku}</span></p>
              <p className="text-muted leading-relaxed">
                Se eliminará de las listas y operaciones actuales. Los pedidos vendidos, recibos y movimientos históricos se conservarán. No se podrá reactivar. Si tiene unidades reservadas, primero debes finalizar o cancelar los pedidos pendientes.
              </p>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                disabled={deleting}
                onClick={() => setProductToDelete(null)}
                className="px-4 py-2.5 rounded-full border border-border text-xs font-bold text-muted hover:bg-cream transition"
              >
                Cancelar
              </button>
              <button
                type="button"
                disabled={deleting}
                onClick={handleDeleteProduct}
                className="px-5 py-2.5 rounded-full bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold transition shadow-xs disabled:opacity-50 inline-flex items-center gap-1.5"
              >
                {deleting ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    <span>Eliminando...</span>
                  </>
                ) : (
                  <>
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>Confirmar Baja</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: CREAR PRODUCTO */}
      {isModalOpen && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="product-modal-title"
          className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4"
        >
          <div className="bg-white rounded-3xl max-w-lg w-full p-6 sm:p-8 shadow-2xl space-y-5 animate-in fade-in zoom-in-95 duration-150 max-h-[90vh] overflow-y-auto">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <div className="flex items-center gap-2 text-wine">
                <Tag className="w-5 h-5" />
                <h3 id="product-modal-title" className="font-serif text-xl font-bold">Nuevo Producto</h3>
              </div>
              <button
                type="button"
                onClick={() => setIsModalOpen(false)}
                className="text-muted hover:text-ink p-1 rounded-full hover:bg-cream"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <form onSubmit={handleCreateProduct} className="space-y-4 text-xs sm:text-sm">
              {modalError && (
                <div
                  role="alert"
                  className="p-3.5 rounded-2xl bg-rose-50 border border-rose-200 text-rose-800 text-xs font-medium flex items-center gap-2.5"
                >
                  <AlertTriangle className="w-4 h-4 shrink-0 text-rose-600" />
                  <span>{modalError}</span>
                </div>
              )}

              <div>
                <label className="font-bold text-ink block mb-1">Nombre del producto *</label>
                <input aria-label="Nombre del producto"
                  type="text"
                  required
                  value={newProductName}
                  onChange={(e) => {
                    setNewProductName(e.target.value);
                    if (modalError) setModalError(null);
                  }}
                  placeholder="Ej. Labial Matte Velvet Red"
                  className="w-full px-3.5 py-2.5 rounded-xl border border-border bg-cream/40 focus:ring-2 focus:ring-wine/20 focus:outline-none"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-bold text-ink block mb-1">Categoría *</label>
                  <select
                    value={newProductCategoryId}
                    onChange={(e) => {
                      setNewProductCategoryId(e.target.value);
                      if (modalError) setModalError(null);
                    }}
                    required
                    className="w-full px-3.5 py-2.5 rounded-xl border border-border bg-cream/40 focus:ring-2 focus:ring-wine/20 focus:outline-none"
                  >
                    <option value="" disabled>Selecciona categoría</option>
                    {categories.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </div>
                <BrandPicker brands={brands} value={newProductBrandId} onChange={setNewProductBrandId} disabled={creating} onCreated={brand=>{setBrands(prev=>[...prev,brand]);setNewProductBrandId(brand.id);}}/>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-bold text-ink block mb-1">SKU Base (opcional)</label>
                  <input aria-label="SKU base"
                    type="text"
                    value={newProductSku}
                    onChange={(e) => setNewProductSku(e.target.value)}
                    placeholder="Ej. LAB-RED-01"
                    className="w-full px-3.5 py-2.5 rounded-xl border border-border bg-cream/40 font-mono uppercase focus:ring-2 focus:ring-wine/20 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="font-bold text-ink block mb-1">Precio Venta (COP) *</label>
                  <input aria-label="Precio general COP"
                    type="number"
                    required={newVariants.length === 0}
                    min={0}
                    step="any"
                    value={newProductPrice || ''}
                    onChange={(e) => {
                      setNewProductPrice(Number(e.target.value));
                      if (modalError) setModalError(null);
                    }}
                    placeholder="25000"
                    className="w-full px-3.5 py-2.5 rounded-xl border border-border bg-cream/40 focus:ring-2 focus:ring-wine/20 focus:outline-none"
                  />
                </div>
              </div>

              <label className="block font-bold">Costo de compra unitario COP (opcional)<input aria-label="Costo de compra general COP" type="number" min="0" step="0.01" value={newProductCost??''} onChange={e=>setNewProductCost(e.target.value===''?undefined:Number(e.target.value))} className="block w-full px-3.5 py-2.5 rounded-xl border border-border mt-1"/><span className="block text-xs text-muted font-normal">Vacío: costo no informado. Cero: costo cero confirmado. Para varios tonos, informa el costo en cada variante.</span></label>
              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="font-bold text-ink block mb-1">Stock Inicial</label>
                  <input aria-label="Stock inicial general"
                    type="number"
                    min={0}
                    value={newProductStock}
                    onChange={(e) => setNewProductStock(Number(e.target.value))}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-border bg-cream/40 focus:ring-2 focus:ring-wine/20 focus:outline-none"
                  />
                </div>
                <div>
                  <label className="font-bold text-ink block mb-1">Stock Mínimo Alerta</label>
                  <input aria-label="Stock mínimo general"
                    type="number"
                    min={0}
                    value={newProductMinStock}
                    onChange={(e) => setNewProductMinStock(Number(e.target.value))}
                    className="w-full px-3.5 py-2.5 rounded-xl border border-border bg-cream/40 focus:ring-2 focus:ring-wine/20 focus:outline-none"
                  />
                </div>
              </div>

              <div>
                <label className="font-bold text-ink block mb-1">Tono / Variante (Opcional)</label>
                <input
                  type="text"
                  value={newProductShade}
                  onChange={(e) => setNewProductShade(e.target.value)}
                  placeholder="Ej. Tono 01 Nude Rosé"
                  className="w-full px-3.5 py-2.5 rounded-xl border border-border bg-cream/40 focus:ring-2 focus:ring-wine/20 focus:outline-none"
                />
              </div>

              {/* Foto de Portada para nuevo producto */}
              <div>
                <label className="font-bold text-ink block mb-1">
                  Foto de Portada (Opcional · JPG/PNG máx 5MB)
                </label>
                <div className="flex items-center gap-3">
                  <input
                    type="file"
                    accept="image/jpeg,image/png,image/jpg"
                    aria-label="Foto del nuevo producto"
                    onChange={handleNewProductImageChange}
                    className="block w-full text-xs text-muted file:mr-3 file:py-2 file:px-4 file:rounded-xl file:border-0 file:text-xs file:font-bold file:bg-wine file:text-white hover:file:bg-wineDark cursor-pointer"
                  />
                </div>
                {newProductImagePreview && (
                  <div className="mt-2.5 flex items-center gap-3 p-2 bg-cream/40 rounded-xl border border-border">
                    <div className="relative w-12 h-12 rounded-lg overflow-hidden border border-border flex-shrink-0">
                      <Image unoptimized
                        src={newProductImagePreview}
                        alt="Previsualización"
                        fill
                        sizes="48px"
                        className="object-cover"
                      />
                    </div>
                    <span className="text-xs text-emerald-700 font-semibold flex items-center gap-1">
                      <CheckCircle2 className="w-3.5 h-3.5" /> Imagen lista para adjuntar
                    </span>
                  </div>
                )}
              </div>

              <div>
                <label className="font-bold text-ink block mb-1">Descripción</label>
                <textarea
                  rows={2}
                  value={newProductDescription}
                  onChange={(e) => setNewProductDescription(e.target.value)}
                  placeholder="Breve descripción para la ficha de producto..."
                  className="w-full px-3.5 py-2.5 rounded-xl border border-border bg-cream/40 focus:ring-2 focus:ring-wine/20 focus:outline-none"
                />
              </div>

              <div className="w-full space-y-3 mb-4">
                    <button type="button" onClick={() => setNewVariants(previous => [...previous,{...emptyVariant(),salePrice:newProductPrice}])} className="min-h-11 px-4 rounded-full border border-wine text-wine font-bold text-sm">Añadir tono al nuevo producto</button>
                    {newVariants.map((variant,index) => <fieldset key={index} className="p-3 border border-border rounded-xl space-y-2 min-w-0">
                      <legend className="text-sm font-bold text-wine">Variante {index+1}</legend>
                      <VariantFields value={variant} onChange={value=>setNewVariants(previous=>previous.map((item,i)=>i===index?value:item))}/>
                      <button type="button" className="min-h-11 text-sm text-red-700" onClick={()=>setNewVariants(previous=>previous.filter((_,i)=>i!==index))}>Quitar variante del formulario</button>
                    </fieldset>)}
                    {newVariants.length>0&&<p className="text-xs text-muted">El precio y stock de cada variante prevalecen sobre los campos generales. Puedes asociar fotos desde Tonos después del alta.</p>}
                  </div>
                  <div className="flex items-center justify-end gap-3 pt-3 border-t border-border">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-full border border-border text-muted font-bold hover:bg-cream transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={creating}
                  className="px-6 py-2 rounded-full bg-wine hover:bg-wineDark text-white font-bold transition shadow-glow disabled:opacity-50"
                >
                  {creating ? 'Guardando...' : 'Crear Producto'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
      {/* MODAL: AGREGAR CATEGORÍA */}
      {isAddCategoryModalOpen && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4"
        >
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-7 shadow-2xl space-y-4 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between border-b border-border pb-3">
              <div className="flex items-center gap-2.5">
                <div className="w-9 h-9 rounded-2xl bg-blush flex items-center justify-center text-wine">
                  <Tag className="w-4 h-4" />
                </div>
                <div>
                  <h3 className="font-serif text-lg font-bold text-ink">Nueva Categoría</h3>
                  <p className="text-[11px] text-muted">Añade una categoría para clasificar productos en la tienda</p>
                </div>
              </div>
              <button
                type="button"
                onClick={() => setIsAddCategoryModalOpen(false)}
                className="p-1 rounded-xl text-muted hover:text-ink hover:bg-cream transition"
              >
                <X className="w-4 h-4" />
              </button>
            </div>

            <form onSubmit={handleCreateCategory} className="space-y-4">
              {categoryModalError && (
                <div className="p-3 rounded-xl bg-rose-50 border border-rose-200 text-rose-700 text-xs font-semibold flex items-center gap-2">
                  <AlertCircle className="w-4 h-4 flex-shrink-0" />
                  <span>{categoryModalError}</span>
                </div>
              )}

              <div>
                <label className="text-xs font-bold text-ink block mb-1">
                  Nombre de la Categoría *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ej: Cuidado Facial, Brochas, Skincare..."
                  value={newCategoryName}
                  onChange={(e) => setNewCategoryName(e.target.value)}
                  className="w-full px-3.5 py-2.5 rounded-xl border border-border bg-cream/40 text-xs text-ink focus:ring-2 focus:ring-wine/20 focus:outline-none"
                  autoFocus
                />
              </div>

              <div className="flex items-center justify-end gap-3 pt-3 border-t border-border">
                <button
                  type="button"
                  onClick={() => setIsAddCategoryModalOpen(false)}
                  disabled={creatingCategory}
                  className="px-4 py-2 rounded-full border border-border text-muted font-bold text-xs hover:bg-cream transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={creatingCategory || !newCategoryName.trim()}
                  className="px-5 py-2 rounded-full bg-wine hover:bg-wineDark text-white text-xs font-bold transition shadow-xs disabled:opacity-50 inline-flex items-center gap-1.5"
                >
                  {creatingCategory ? (
                    <>
                      <Loader2 className="w-3.5 h-3.5 animate-spin" />
                      <span>Creando...</span>
                    </>
                  ) : (
                    <>
                      <Plus className="w-3.5 h-3.5" />
                      <span>Crear Categoría</span>
                    </>
                  )}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: CONFIRMAR ELIMINACIÓN DE CATEGORÍA */}
      {categoryToDelete && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 bg-black/50 backdrop-blur-xs flex items-center justify-center p-4"
        >
          <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-7 shadow-2xl space-y-4 animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center gap-3 text-rose-600">
              <div className="w-10 h-10 rounded-2xl bg-rose-100 flex items-center justify-center flex-shrink-0">
                <AlertTriangle className="w-5 h-5 text-rose-600" />
              </div>
              <div>
                <h3 className="font-serif text-lg font-bold text-ink">
                  ¿Eliminar Categoría?
                </h3>
                <p className="text-xs text-muted">Esta acción eliminará la categoría del catálogo.</p>
              </div>
            </div>

            <div className="bg-cream/50 rounded-2xl p-4 border border-border space-y-2 text-xs">
              <p className="font-bold text-ink text-sm">{categoryToDelete.name}</p>
              <p className="text-muted leading-relaxed">
                Si esta categoría tiene productos asociados en el catálogo, la base de datos bloqueará la eliminación para proteger la integridad de los productos.
              </p>
            </div>

            <div className="flex items-center justify-end gap-3 pt-2">
              <button
                type="button"
                disabled={deletingCategory}
                onClick={() => setCategoryToDelete(null)}
                className="px-4 py-2 rounded-full border border-border text-muted font-bold text-xs hover:bg-cream transition"
              >
                Cancelar
              </button>
              <button
                type="button"
                disabled={deletingCategory}
                onClick={handleDeleteCategory}
                className="px-5 py-2.5 rounded-full bg-rose-600 hover:bg-rose-700 text-white text-xs font-bold transition shadow-xs disabled:opacity-50 inline-flex items-center gap-1.5"
              >
                {deletingCategory ? (
                  <>
                    <Loader2 className="w-3.5 h-3.5 animate-spin" />
                    <span>Eliminando...</span>
                  </>
                ) : (
                  <>
                    <Trash2 className="w-3.5 h-3.5" />
                    <span>Eliminar Categoría</span>
                  </>
                )}
              </button>
            </div>
          </div>
        </div>
      )}

    {variantsProductId && <VariantManager productId={variantsProductId} onClose={() => setVariantsProductId(null)} onChanged={() => { void loadData(); }} />}
    </AdminShell>
  );
}
