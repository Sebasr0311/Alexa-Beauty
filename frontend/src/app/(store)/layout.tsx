import React from 'react';
import StoreShell from '@/components/store/StoreShell';
import { StoreSettingsProvider } from '@/context/StoreSettingsContext';
import { fetchPublicSettings } from '@/lib/server-catalog';

export default async function StoreLayout({
  children,
}: {
  children: React.ReactNode;
}) {
  const settings = await fetchPublicSettings();

  return (
    <StoreSettingsProvider initialWhatsappNumber={settings?.whatsappNumber ?? null}>
      <StoreShell>{children}</StoreShell>
    </StoreSettingsProvider>
  );
}
