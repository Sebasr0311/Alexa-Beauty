"use client";
import { useState } from "react";
import { api, BrandDto } from "@/lib/api";
export function BrandPicker({
  brands,
  value,
  onChange,
  onCreated,
  disabled = false,
}: {
  brands: BrandDto[];
  value: string;
  onChange: (id: string) => void;
  onCreated: (brand: BrandDto) => void;
  disabled?: boolean;
}) {
  const [name, setName] = useState("");
  const [busy, setBusy] = useState(false);
  const [error, setError] = useState("");
  return (
    <div className="space-y-2">
      <label className="block font-bold">
        Marca *
        <select
          aria-label="Marca"
          required
          disabled={disabled || busy}
          className="block w-full rounded-xl border border-border p-3"
          value={value}
          onChange={(e) => onChange(e.target.value)}
        >
          <option value="">Selecciona marca</option>
          {brands.map((b) => (
            <option key={b.id} value={b.id}>
              {b.name}
            </option>
          ))}
        </select>
      </label>
      <details>
        <summary className="cursor-pointer text-wine underline">
          Crear una marca
        </summary>
        <label className="block mt-2">
          Nombre de la nueva marca
          <input
            aria-label="Nombre de la nueva marca"
            maxLength={100}
            disabled={disabled || busy}
            className="block w-full rounded-xl border border-border p-3"
            value={name}
            onChange={(e) => setName(e.target.value)}
          />
        </label>
        <button
          type="button"
          disabled={disabled || busy || !name.trim()}
          className="min-h-11 text-wine font-bold"
          onClick={async () => {
            setBusy(true);
            setError("");
            try {
              const brand = await api.createBrand(name);
              onCreated(brand);
              setName("");
            } catch (e) {
              setError(
                e instanceof Error ? e.message : "No se pudo crear la marca.",
              );
            } finally {
              setBusy(false);
            }
          }}
        >
          {busy ? "Creando…" : "Crear y seleccionar marca"}
        </button>
      </details>
      {error && (
        <p role="alert" className="text-red-700">
          {error}
        </p>
      )}
    </div>
  );
}
