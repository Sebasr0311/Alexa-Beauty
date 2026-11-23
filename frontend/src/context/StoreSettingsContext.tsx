'use client';

import React, { createContext, useCallback, useContext, useEffect, useState } from 'react';
import { api } from '@/lib/api';

interface StoreSettingsContextValue {
  whatsappNumber: string | null;
  refreshSettings: () => Promise<void>;
}

const StoreSettingsContext = createContext<StoreSettingsContextValue>({
  whatsappNumber: null,
  refreshSettings: async () => {},
});

export function StoreSettingsProvider({
  initialWhatsappNumber,
  children,
}: {
  initialWhatsappNumber: string | null;
  children: React.ReactNode;
}) {
  const [whatsappNumber, setWhatsappNumber] = useState<string | null>(initialWhatsappNumber ?? null);

  const refreshSettings = useCallback(async () => {
    try {
      const settings = await api.getPublicSettings();
      setWhatsappNumber(settings?.whatsappNumber ?? null);
    } catch {
      // Preserve initial snapshot when backend is transiently unreachable
    }
  }, []);

  useEffect(() => {
    let cancelled = false;
    api.getPublicSettings()
      .then((settings) => {
        if (!cancelled) {
          setWhatsappNumber(settings?.whatsappNumber ?? null);
        }
      })
      .catch(() => {
        // Keep initialWhatsappNumber intact on transient failure
      });
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <StoreSettingsContext.Provider value={{ whatsappNumber, refreshSettings }}>
      {children}
    </StoreSettingsContext.Provider>
  );
}

export function useStoreSettings(): StoreSettingsContextValue {
  return useContext(StoreSettingsContext);
}
