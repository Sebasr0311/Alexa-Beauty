'use client';
import { useEffect, useRef, useState, type RefObject } from 'react';
import { ProductDetailDto, ProductVariantDto } from '@/lib/api';
import { formatCOP } from '@/lib/utils';
import { variantImages, variantLabel } from '@/lib/shades';
import { ShadeSelector } from './ShadeSelector';

export function ShadeSelectorModal({product,isOpen,onClose,onSelectVariant,returnFocusRef}: {
  returnFocusRef?:RefObject<HTMLButtonElement|null>;
  product:ProductDetailDto|null;isOpen:boolean;onClose:()=>void;onSelectVariant:(variant:ProductVariantDto)=>void;
}) {
  const [selected,setSelected]=useState<ProductVariantDto|null>(null);
  const panel=useRef<HTMLDivElement>(null);
  const closeRef=useRef(onClose); closeRef.current=onClose;
  useEffect(()=>{
    if (!isOpen) return;
    setSelected(null);
    const previous=returnFocusRef?.current || document.activeElement as HTMLElement|null;
    const oldOverflow=document.body.style.overflow;
    document.body.style.overflow='hidden';
    panel.current?.querySelector<HTMLButtonElement>('button')?.focus();
    function keyboard(event:KeyboardEvent) {
      if(event.key==='Escape') { event.preventDefault();closeRef.current(); }
      if(event.key==='Tab') {
        const nodes=panel.current?.querySelectorAll<HTMLElement>('button:not(:disabled),a[href],input:not(:disabled),[tabindex="0"]');
        if(!nodes?.length)return;
        const first=nodes[0],last=nodes[nodes.length-1];
        if(event.shiftKey && document.activeElement===first){event.preventDefault();last.focus();}
        if(!event.shiftKey && document.activeElement===last){event.preventDefault();first.focus();}
      }
    }
    document.addEventListener('keydown',keyboard);
    return ()=>{document.body.style.overflow=oldOverflow;document.removeEventListener('keydown',keyboard);previous?.focus();};
  },[isOpen,returnFocusRef]);
  if(!isOpen || !product)return null;
  const image=variantImages(product.images,selected?.id)[0];
  return <div className="fixed inset-0 z-50 flex items-end sm:items-center justify-center bg-black/50 p-0 sm:p-4" onClick={onClose}>
    <div ref={panel} role="dialog" aria-modal="true" aria-labelledby="shade-selector-title" onClick={event=>event.stopPropagation()}
      className="w-full min-w-0 max-h-[90dvh] overflow-y-auto bg-cream p-4 sm:p-6 rounded-t-3xl sm:rounded-3xl sm:max-w-lg shadow-xl space-y-4">
      <div className="flex gap-3 justify-between items-start">
        <div className="min-w-0"><p className="text-xs text-muted">{product.brand.name}</p><h2 id="shade-selector-title" className="font-serif text-xl text-wine break-words">{product.name}</h2></div>
        <button type="button" onClick={onClose} aria-label="Cerrar selector de tonos" className="h-11 w-11 shrink-0 rounded-full bg-white text-wine focus-visible:outline-wine">✕</button>
      </div>
      {image && <img src={image.url} alt={image.altText||product.name} className="h-28 w-full object-contain" />}
      <p className="text-sm text-muted">Elige un tono o presentación antes de agregarlo.</p>
      <ShadeSelector variants={product.variants} selectedId={selected?.id} onSelect={setSelected} disableExhausted={true}/>
      <div aria-live="polite" className="min-h-20 text-sm">
        {selected ? <><p className="font-bold text-wine">{formatCOP(selected.salePrice)}</p><p>{variantLabel(selected)}</p><p>{selected.availableStock>0?`${selected.availableStock} unidades disponibles`:'Agotado'}</p><p className="text-xs text-muted break-all">SKU: {selected.sku}</p></>:<p>Selecciona una opción.</p>}
      </div>
      <button type="button" disabled={!selected||!selected.active||selected.availableStock<=0} onClick={()=>selected&&onSelectVariant(selected)}
        className="w-full min-h-12 rounded-full bg-wine text-white font-bold px-4 py-3 disabled:opacity-50 focus-visible:outline focus-visible:outline-offset-2 focus-visible:outline-wine">Agregar al carrito</button>
    </div>
  </div>;
}
