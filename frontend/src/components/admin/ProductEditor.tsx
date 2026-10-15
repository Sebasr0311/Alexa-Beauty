"use client";
import { useEffect, useState } from "react";
import { api, BrandDto, CategoryDto, ProductDetailDto } from "@/lib/api";
import { BrandPicker } from "./BrandPicker";
export function ProductEditor({
  id,
  onClose,
  onSaved,
}: {
  id: string;
  onClose: () => void;
  onSaved: () => void;
}) {
  const [product, setProduct] = useState<ProductDetailDto | null>(null);
  const [brands, setBrands] = useState<BrandDto[]>([]);
  const [categories, setCategories] = useState<CategoryDto[]>([]);
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  useEffect(() => {
    let alive = true;
    Promise.all([
      api.getAdminProductById(id),
      api.getAdminBrands(),
      api.getCategories(),
    ])
      .then(([p, b, c]) => {
        if (alive) {
          setProduct(p);
          setBrands(b);
          setCategories(c);
        }
      })
      .catch((e) => {
        if (alive) setError(e.message);
      });
    return () => {
      alive = false;
    };
  }, [id]);
  return (
    <div className="fixed inset-0 z-50 bg-black/50 flex items-center justify-center p-4">
      <section
        role="dialog"
        aria-modal="true"
        aria-labelledby="product-editor-title"
        className="bg-white rounded-3xl p-6 max-w-xl w-full max-h-[90dvh] overflow-y-auto"
      >
        <div className="flex justify-between">
          <h2 id="product-editor-title" className="text-xl font-bold text-wine">
            Editar producto
          </h2>
          <button
            type="button"
            disabled={busy}
            onClick={onClose}
            aria-label="Cerrar edición"
          >
            ✕
          </button>
        </div>
        {error && (
          <p role="alert" className="text-red-700 my-3">
            {error}
          </p>
        )}
        {!product ? (
          <p>Cargando producto…</p>
        ) : (
          <form
            className="space-y-4 mt-4"
            onSubmit={async (e) => {
              e.preventDefault();
              setBusy(true);
              setError("");
              try {
                if (!product.name.trim()) throw new Error("El nombre del producto es obligatorio.");
                await api.updateProduct(id, {
                  name: product.name.trim(),
                  brandId: product.brand.id,
                  categoryId: product.category.id,
                  description: product.description || "",
                });
                onSaved();
                onClose();
              } catch (e) {
                setError(
                  e instanceof Error ? e.message : "No se pudo guardar.",
                );
              } finally {
                setBusy(false);
              }
            }}
          >
            <fieldset disabled={busy} className="space-y-4">
              <label className="block">
                Nombre del producto
                <input
                  required
                  maxLength={200}
                  className="block w-full border rounded-xl p-3"
                  value={product.name}
                  onChange={(e) =>
                    setProduct({ ...product, name: e.target.value })
                  }
                />
              </label>
              <BrandPicker
                brands={brands}
                value={product.brand.id}
                onChange={(brandId) =>
                  setProduct({
                    ...product,
                    brand: brands.find((b) => b.id === brandId)!,
                  })
                }
                onCreated={(brand) => {
                  setBrands((prev) => [...prev, brand]);
                  setProduct((prev) => (prev ? { ...prev, brand } : prev));
                }}
              />
              <label className="block">
                Categoría
                <select
                  required
                  className="block w-full border rounded-xl p-3"
                  value={product.category.id}
                  onChange={(e) =>
                    setProduct({
                      ...product,
                      category: categories.find(
                        (c) => c.id === e.target.value,
                      )!,
                    })
                  }
                >
                  {categories.map((c) => (
                    <option key={c.id} value={c.id}>
                      {c.name}
                    </option>
                  ))}
                </select>
              </label>
              <label className="block">
                Descripción
                <textarea
                  className="block w-full border rounded-xl p-3"
                  value={product.description || ""}
                  onChange={(e) =>
                    setProduct({ ...product, description: e.target.value })
                  }
                />
              </label>
            </fieldset>
            <p className="text-sm text-muted">
              El precio de venta y el costo de compra de cada presentación se
              editan desde Tonos y presentaciones.
            </p>
            <button
              disabled={busy}
              className="bg-wine text-white rounded-full px-5 min-h-11"
            >
              {busy ? "Guardando…" : "Guardar producto"}
            </button>
          </form>
        )}
      </section>
    </div>
  );
}
