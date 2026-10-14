'use client';
import { useEffect, useRef, useState } from 'react';
import { api, ProductDetailDto, ProductVariantDto, VariantInput } from '@/lib/api';
import { variantLabel } from '@/lib/shades';
import { emptyVariant, variantPayload, VariantFields } from './VariantFields';

function VariantRow({product,variant,onReload}: {product:ProductDetailDto;variant:ProductVariantDto;onReload:()=>Promise<void>}) {
  const [value,setValue]=useState<VariantInput>({...variant,stockOnHand:undefined});
  const [busy,setBusy]=useState(false);const [message,setMessage]=useState('');
  useEffect(()=>setValue({...variant,stockOnHand:undefined}),[variant]);
  async function run(action:()=>Promise<unknown>,success:string) {
    setBusy(true);setMessage('');try{await action();await onReload();setMessage(success);}catch(error){setMessage(error instanceof Error?error.message:'No se pudo guardar.');}finally{setBusy(false);}
  }
  return <section className="p-4 border border-border rounded-2xl bg-white space-y-3 min-w-0" aria-label={`Variante ${variantLabel(variant)}`}>
    <h3 className="font-bold text-wine break-words">{variantLabel(variant)} · {variant.active?'Activa':'Inactiva'}</h3>
    <p className="text-xs text-muted">Existencias: {variant.stockOnHand} · Reservadas: {variant.stockReserved} · Disponibles: {variant.availableStock}. Las existencias se ajustan desde Inventario.</p>
    <form onSubmit={event=>{event.preventDefault();void run(()=>api.updateVariant(product.id,variant.id,variantPayload(value)),'Variante guardada.');}} className="space-y-3">
      <fieldset disabled={busy}><VariantFields value={value} onChange={setValue} editing/></fieldset>
      <div className="flex flex-wrap gap-2"><button disabled={busy} className="min-h-11 rounded-full px-4 bg-wine text-white text-sm font-bold">Guardar variante</button>
      <button type="button" disabled={busy} className="min-h-11 rounded-full border border-border px-4 text-sm" onClick={()=>{
        if(window.confirm(`${variant.active?'Desactivar':'Activar'} ${variantLabel(variant)}? Los pedidos y el stock se conservarán.`)) void run(()=>api.toggleVariantStatus(product.id,variant.id,!variant.active),'Estado actualizado.');
      }}>{variant.active?'Desactivar':'Activar'}</button></div>
    </form>
    <div className="flex flex-wrap gap-2">{product.images.filter(image=>image.variantId===variant.id).map(image=><img key={image.id} src={image.url} alt={image.altText||variantLabel(variant)} className="w-20 h-20 object-contain rounded-lg border border-border"/>)}</div>
    <label className="block text-xs font-semibold">Añadir foto para este tono (JPG/PNG, 5 MB)
      <input type="file" accept="image/jpeg,image/png" disabled={busy} className="block max-w-full text-xs mt-2" onChange={event=>{
        const file=event.target.files?.[0];event.target.value='';if(file)void run(()=>api.uploadProductImage(product.id,file,false,variant.id),'Foto vinculada al tono.');
      }}/></label>
    {message&&<p role="status" className="text-sm break-words">{message}</p>}
  </section>;
}
export function VariantManager({productId,onClose,onChanged}: {productId:string;onClose:()=>void;onChanged:()=>void}) {
  const [product,setProduct]=useState<ProductDetailDto|null>(null);const [value,setValue]=useState(emptyVariant());
  const [busy,setBusy]=useState(false);const [error,setError]=useState('');const [message,setMessage]=useState('');
  const panel=useRef<HTMLDivElement>(null);const close=useRef(onClose);close.current=onClose;
  async function reload(){setProduct(await api.getAdminProductById(productId));onChanged();}
  useEffect(()=>{api.getAdminProductById(productId).then(setProduct).catch(e=>setError(e.message));},[productId]);
  useEffect(()=>{
    const previous=document.activeElement as HTMLElement|null;const old=document.body.style.overflow;document.body.style.overflow='hidden';
    panel.current?.querySelector<HTMLButtonElement>('button')?.focus();
    const keyboard=(event:KeyboardEvent)=>{if(event.key==='Escape'){event.preventDefault();close.current();}
      if(event.key==='Tab'){const nodes=panel.current?.querySelectorAll<HTMLElement>('button:not(:disabled),input:not(:disabled):not([readonly]),select,textarea,[tabindex="0"]');if(!nodes?.length)return;
      const first=nodes[0],last=nodes[nodes.length-1];if(event.shiftKey&&document.activeElement===first){event.preventDefault();last.focus();}else if(!event.shiftKey&&document.activeElement===last){event.preventDefault();first.focus();}}};
    document.addEventListener('keydown',keyboard);return()=>{document.body.style.overflow=old;document.removeEventListener('keydown',keyboard);previous?.focus();};
  },[]);
  return <div className="fixed inset-0 z-50 bg-black/50 flex items-end sm:items-center justify-center sm:p-4" onClick={onClose}>
    <div ref={panel} role="dialog" aria-modal="true" aria-labelledby="variant-manager-title" className="w-full sm:max-w-3xl max-h-[92dvh] overflow-y-auto min-w-0 rounded-t-3xl sm:rounded-3xl bg-cream p-4 sm:p-6 space-y-4" onClick={event=>event.stopPropagation()}>
      <div className="flex justify-between gap-3"><h2 id="variant-manager-title" className="font-serif text-xl text-wine break-words">Tonos y presentaciones — {product?.name||'Cargando'}</h2><button type="button" aria-label="Cerrar gestión de tonos" onClick={onClose} className="h-11 w-11 shrink-0 rounded-full bg-white">✕</button></div>
      {error&&<p role="alert" className="text-red-700">{error}</p>}
      {product?.variants.map(variant=><VariantRow key={variant.id} product={product} variant={variant} onReload={reload}/>)}
      {product&&<form className="border border-border rounded-2xl p-4 space-y-3" onSubmit={async event=>{
        event.preventDefault();setBusy(true);setError('');setMessage('');try{await api.addVariant(product.id,variantPayload(value));setValue(emptyVariant());await reload();setMessage('Nuevo tono añadido.');}catch(e){setError(e instanceof Error?e.message:'No se pudo añadir.');}finally{setBusy(false);}
      }}><h3 className="font-bold text-wine">Añadir tono o presentación</h3><fieldset disabled={busy}><VariantFields value={value} onChange={setValue}/></fieldset><button disabled={busy} className="min-h-12 w-full bg-wine text-white rounded-full px-4 font-bold">{busy?'Guardando…':'Añadir variante'}</button>{message&&<p role="status">{message}</p>}</form>}
    </div>
  </div>;
}
