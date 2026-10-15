'use client';
import type { VariantInput } from '@/lib/api';
export function emptyVariant(): VariantInput { return {shadeCode:'',shadeName:'',shadeHex:'',presentation:'',salePrice:0,stockOnHand:0,minStock:3,active:true}; }
export function variantPayload(value:VariantInput):VariantInput {
  return {...value,clearReferenceCost:value.referenceCost===null,sku:value.sku?.trim()||undefined,shadeCode:value.shadeCode?.trim()||undefined,
    shadeName:value.shadeName?.trim()||undefined,shadeHex:value.shadeHex?.trim()||undefined,presentation:value.presentation?.trim()||undefined};
}
export function VariantFields({value,onChange,editing=false}: {value:VariantInput;onChange:(value:VariantInput)=>void;editing?:boolean}) {
  const change=(key:keyof VariantInput,next:string|number)=>onChange({...value,[key]:next});
  const input='block w-full min-w-0 min-h-11 rounded-xl border border-border bg-white px-3 py-2 mt-1 text-sm focus:outline-wine';
  return <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 min-w-0">
    <label className="text-xs font-semibold">Código de tono<input className={input} value={value.shadeCode||''} onChange={e=>change('shadeCode',e.target.value)} maxLength={50}/></label>
    <label className="text-xs font-semibold">Nombre del tono<input className={input} value={value.shadeName||''} onChange={e=>change('shadeName',e.target.value)} maxLength={100}/></label>
    <label className="text-xs font-semibold">Presentación<input className={input} value={value.presentation||''} onChange={e=>change('presentation',e.target.value)} maxLength={100}/></label>
    <label className="text-xs font-semibold">SKU {editing?'(conservado)':'(opcional, se genera automáticamente)'}<input className={input} value={value.sku||''} readOnly={editing} onChange={e=>change('sku',e.target.value)} maxLength={50} pattern="[A-Za-z0-9][A-Za-z0-9._-]*" /></label>
    <div className="text-xs font-semibold">Muestra aproximada (opcional)
      <div className="flex items-center gap-2 mt-1">
        <label className="shrink-0"><span className="sr-only">Elegir color aproximado</span><input type="color" aria-label="Elegir color aproximado" value={/^#[0-9a-f]{6}$/i.test(value.shadeHex||'')?value.shadeHex!:'#FFFFFF'} onChange={e=>change('shadeHex',e.target.value.toUpperCase())} className="w-11 h-11"/></label>
        <label className="min-w-0 flex-1"><span className="sr-only">Color HEX</span><input aria-label="Color HEX" placeholder="#RRGGBB" className={input+' !mt-0'} value={value.shadeHex||''} onChange={e=>change('shadeHex',e.target.value.toUpperCase())} pattern="#[0-9a-fA-F]{6}" maxLength={7}/></label>
        <button type="button" className="min-h-11 px-2 text-wine underline" onClick={()=>change('shadeHex','')}>Sin color</button>
      </div><p className="font-normal text-muted mt-1">Dejar vacío si no conoces el color; no se inventará una muestra.</p>
    </div>
    <label className="text-xs font-semibold">Precio de venta COP<input type="number" required min="0" step="0.01" className={input} value={value.salePrice} onChange={e=>change('salePrice',Number(e.target.value))}/></label>
    <label className="text-xs font-semibold">Costo de compra unitario COP (opcional)<input type="number" min="0" step="0.01" className={input} value={value.referenceCost??''} onChange={e=>onChange({...value,referenceCost:e.target.value===''?null:Number(e.target.value)})}/><span className="font-normal text-muted">Vacío: costo no informado; 0: costo cero confirmado.</span></label>
    {!editing&&<label className="text-xs font-semibold">Stock inicial<input type="number" min="0" step="1" required className={input} value={value.stockOnHand??0} onChange={e=>change('stockOnHand',Number(e.target.value))}/></label>}
    <label className="text-xs font-semibold">Stock mínimo<input type="number" min="0" step="1" required className={input} value={value.minStock??3} onChange={e=>change('minStock',Number(e.target.value))}/></label>
  </div>;
}
