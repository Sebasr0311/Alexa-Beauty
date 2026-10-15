'use client';
import { useId } from 'react';
import type { ProductVariantDto } from '@/lib/api';
import { activeVariants, variantLabel } from '@/lib/shades';

export function ShadeSelector({ variants, selectedId, onSelect, disableExhausted = false }: {
  variants: ProductVariantDto[]; selectedId?: string; onSelect: (variant: ProductVariantDto) => void;
  disableExhausted?: boolean;
}) {
  const id = useId();
  const options = activeVariants(variants);
  const hasTone = options.some(variant => variant.shadeCode || (variant.shadeName && variant.shadeName.toLowerCase() !== 'default'));
  const hasOptions = options.length > 1 || hasTone || options.some(variant => variant.presentation);
  const selected = options.find(variant => variant.id === selectedId);
  if (!hasOptions) return null;
  return <section aria-labelledby={id} className="min-w-0 space-y-3">
    <h2 id={id} className="text-sm font-bold text-wine">{hasTone ? 'Tono' : 'Presentación'}
      {selected && <span className="block mt-1 text-ink font-normal break-words">{variantLabel(selected)} {selected.availableStock <= 0 ? '(Agotado - no disponible para compra)' : ''}</span>}
    </h2>
    <div className="flex flex-wrap gap-2" role="group" aria-label={hasTone ? 'Tonos disponibles' : 'Presentaciones disponibles'}>
      {options.map(variant => {
        const selected = variant.id === selectedId;
        const exhausted = variant.availableStock <= 0;
        return <button key={variant.id} type="button" aria-pressed={selected}
          aria-disabled={exhausted}
          disabled={disableExhausted && exhausted}
          aria-label={`${variantLabel(variant)}${exhausted ? ', Agotado - no disponible para compra' : ''}`}
          onClick={() => {
            if (disableExhausted && exhausted) return;
            onSelect(variant);
          }}
          className={`min-h-12 max-w-full rounded-2xl border-2 px-3 py-2 text-left focus-visible:outline focus-visible:outline-2 focus-visible:outline-offset-2 focus-visible:outline-wine motion-safe:transition-colors ${selected ? 'border-wine bg-blush/40 text-wine ring-1 ring-wine' : 'border-border bg-white text-ink hover:border-mauve'} ${exhausted ? 'opacity-60 bg-cream/30 cursor-not-allowed' : ''}`}>
          <span className="flex items-center gap-2">
            {variant.shadeHex && <span data-testid="shade-swatch" aria-hidden="true" className="h-6 w-6 shrink-0 rounded-full border border-black/25" style={{backgroundColor:variant.shadeHex}} />}
            <span className={`text-sm font-bold break-words ${exhausted ? 'line-through text-muted' : ''}`}>{variant.shadeCode || variantLabel(variant)}</span>
            {selected && <span aria-hidden="true" className="font-bold">✓</span>}
          </span>
          {variant.shadeCode && <span className="block text-xs mt-1 break-words text-muted">{[variant.shadeName,variant.presentation].filter(Boolean).join(' · ')}</span>}
          {exhausted && <span className="block text-xs mt-1 font-semibold text-rose-700">Agotado</span>}
        </button>;
      })}
    </div>
    <p className="text-xs text-muted">Las muestras de color son aproximadas y pueden variar según la pantalla y la piel.</p>
  </section>;
}
