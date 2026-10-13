'use client';

import React, { createContext, useContext, useEffect, useState, useMemo } from 'react';
import { Product, PRODUCTS } from '@/lib/data';

export interface CartItem {
  product: Product;
  quantity: number;
  variantId?: string;
  variantName?: string;
}

interface CartContextType {
  items: CartItem[];
  addItem: (
    product: Product,
    quantity?: number,
    variantId?: string,
    variantName?: string
  ) => { success: boolean; message?: string };
  updateQuantity: (productId: number, delta: number) => void;
  removeItem: (productId: number) => void;
  clearCart: () => void;
  totalCount: number;
  totalAmount: number;
}

const CartContext = createContext<CartContextType | undefined>(undefined);

const STORAGE_KEY = 'alexa_beauty_cart_v1';

export function CartProvider({ children }: { children: React.ReactNode }) {
  // Start with demo initial cart (items 1 and 2) or empty
  const [items, setItems] = useState<CartItem[]>(() => [
    { product: PRODUCTS[0], quantity: 1 },
    { product: PRODUCTS[1], quantity: 1 },
  ]);
  const [hasHydrated, setHasHydrated] = useState(false);

  useEffect(() => {
    try {
      const stored = localStorage.getItem(STORAGE_KEY);
      if (stored) {
        const parsed = JSON.parse(stored);
        if (Array.isArray(parsed) && parsed.length > 0) {
          setItems(parsed);
        }
      }
    } catch {
      // ignore JSON errors or localStorage limits
    } finally {
      setHasHydrated(true);
    }
  }, []);

  useEffect(() => {
    if (hasHydrated) {
      try {
        localStorage.setItem(STORAGE_KEY, JSON.stringify(items));
      } catch {
        // ignore
      }
    }
  }, [items, hasHydrated]);

  const addItem = (
    product: Product,
    quantityToAdd: number = 1,
    variantId?: string,
    variantName?: string
  ) => {
    if (product.stock <= 0) {
      return { success: false, message: 'Producto sin existencias disponibles.' };
    }

    const qty = Math.max(1, quantityToAdd);
    const resolvedVariantId = variantId || product.variantId;
    let success = true;
    let message = '';

    setItems((prev) => {
      const existing = prev.find(
        (i) => i.product.id === product.id && (!resolvedVariantId || i.variantId === resolvedVariantId)
      );
      if (existing) {
        if (existing.quantity + qty > product.stock) {
          success = false;
          message = `No hay más unidades disponibles (${product.stock} máx).`;
          return prev;
        }
        return prev.map((i) =>
          (i.product.id === product.id && (!resolvedVariantId || i.variantId === resolvedVariantId))
            ? { ...i, quantity: i.quantity + qty }
            : i
        );
      }
      return [
        ...prev,
        {
          product,
          quantity: Math.min(qty, product.stock),
          variantId: resolvedVariantId,
          variantName,
        },
      ];
    });

    return { success, message };
  };

  const updateQuantity = (productId: number, delta: number) => {
    setItems((prev) => {
      return prev
        .map((item) => {
          if (item.product.id === productId) {
            const newQty = item.quantity + delta;
            if (newQty <= 0) return null;
            if (newQty > item.product.stock) return item;
            return { ...item, quantity: newQty };
          }
          return item;
        })
        .filter((item): item is CartItem => item !== null);
    });
  };

  const removeItem = (productId: number) => {
    setItems((prev) => prev.filter((i) => i.product.id !== productId));
  };

  const clearCart = () => {
    setItems([]);
  };

  const totalCount = useMemo(
    () => items.reduce((acc, curr) => acc + curr.quantity, 0),
    [items]
  );

  const totalAmount = useMemo(
    () => items.reduce((acc, curr) => acc + curr.product.price * curr.quantity, 0),
    [items]
  );

  return (
    <CartContext.Provider
      value={{
        items,
        addItem,
        updateQuantity,
        removeItem,
        clearCart,
        totalCount,
        totalAmount,
      }}
    >
      {children}
    </CartContext.Provider>
  );
}

export function useCart() {
  const context = useContext(CartContext);
  if (!context) {
    throw new Error('useCart must be used within a CartProvider');
  }
  return context;
}
