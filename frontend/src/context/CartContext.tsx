'use client';
import React, { createContext, useContext, useEffect, useState } from 'react';
import type { Product } from '@/lib/data';
export interface CartItem { product: Product; quantity: number; variantId: string; variantName?: string }
interface CartContextType {
  items: CartItem[];
  addItem: (product: Product, quantity?: number, variantId?: string, variantName?: string) => { success: boolean; message?: string };
  updateQuantity: (variantId: string, delta: number) => void;
  removeItem: (variantId: string) => void;
  clearCart: () => void; totalCount: number; totalAmount: number;
}
const CartContext = createContext<CartContextType | undefined>(undefined);
const STORAGE_KEY = 'alexa_beauty_cart_v2';
const uuid = /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i;
export function CartProvider({ children }: { children: React.ReactNode }) {
  const [items, setItems] = useState<CartItem[]>([]);
  const [hydrated, setHydrated] = useState(false);
  useEffect(() => {
    try {
      const parsed: unknown = JSON.parse(localStorage.getItem(STORAGE_KEY) || '[]');
      if (Array.isArray(parsed)) setItems(parsed.filter((item): item is CartItem =>
        item && uuid.test(item.variantId) && item.product && uuid.test(item.product.id)
        && Number.isInteger(item.quantity) && item.quantity > 0
        && Number.isFinite(item.product.price) && item.product.price >= 0
        && Number.isInteger(item.product.stock) && item.product.stock >= item.quantity));
      localStorage.removeItem('alexa_beauty_cart_v1');
      localStorage.removeItem('alexa_auth_token');
    } catch { /* Invalid local storage is not authoritative inventory. */ }
    setHydrated(true);
  }, []);
  useEffect(() => {
    if (hydrated) { try { localStorage.setItem(STORAGE_KEY, JSON.stringify(items)); } catch { /* Storage may be unavailable. */ } }
  }, [items, hydrated]);
  const addItem = (product: Product, quantity = 1, variantId = product.variantId, variantName?: string) => {
    if (!hydrated || !variantId || !uuid.test(variantId) || !uuid.test(product.id)
        || !Number.isInteger(quantity) || quantity < 1 || product.stock < quantity) {
      return { success: false, message: 'Selecciona una variante disponible en la ficha del producto.' };
    }
    const existing = items.find(item => item.variantId === variantId);
    if ((existing?.quantity || 0) + quantity > product.stock) {
      if (existing && existing.quantity >= product.stock) {
        return {
          success: false,
          message: product.stock === 1
            ? 'Ya agregaste la única unidad disponible al carrito. Puedes finalizar tu pedido desde tu bolsa.'
            : `Ya agregaste todas las unidades disponibles (${product.stock}) a tu carrito.`
        };
      }
      return { success: false, message: 'No hay suficientes unidades disponibles.' };
    }
    setItems(previous => {
      const current = previous.find(item => item.variantId === variantId);
      if ((current?.quantity || 0) + quantity > product.stock) return previous;
      return current ? previous.map(item => item.variantId === variantId ? { ...item, product, quantity: item.quantity + quantity } : item)
        : [...previous, { product, quantity, variantId, variantName }];
    });
    return { success: true };
  };
  const updateQuantity = (variantId: string, delta: number) => setItems(previous => previous.flatMap(item => {
    if (item.variantId !== variantId) return [item];
    const quantity = item.quantity + delta;
    return quantity <= 0 ? [] : quantity > item.product.stock ? [item] : [{ ...item, quantity }];
  }));
  return <CartContext.Provider value={{ items, addItem, updateQuantity,
    removeItem: id => setItems(previous => previous.filter(item => item.variantId !== id)),
    clearCart: () => setItems([]), totalCount: items.reduce((sum, item) => sum + item.quantity, 0),
    totalAmount: items.reduce((sum, item) => sum + item.product.price * item.quantity, 0) }}>{children}</CartContext.Provider>;
}
export function useCart() {
  const context = useContext(CartContext);
  if (!context) throw new Error('useCart must be used within a CartProvider');
  return context;
}
