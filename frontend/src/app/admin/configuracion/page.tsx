'use client';

import React, { useEffect, useState } from 'react';
import AdminShell from '@/components/admin/AdminShell';
import { WhatsAppSettingsForm } from '@/components/admin/WhatsAppSettingsForm';
import { AdminSettingsDto, api } from '@/lib/api';

export default function AdminConfiguracionPage() {
  const [settings, setSettings] = useState<AdminSettingsDto | null>(null);
  const [canEdit, setCanEdit] = useState(false);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    let cancelled = false;

    async function loadSettings() {
      setLoading(true);
      setError(null);
      try {
        const [adminSettings, currentUser] = await Promise.all([
          api.getAdminSettings(),
          api.getMe(),
        ]);
        if (cancelled) return;
        setSettings(adminSettings);
        setCanEdit(currentUser.role === 'ADMIN');
      } catch (err: unknown) {
        if (cancelled) return;
        const message =
          err instanceof Error && err.message
            ? err.message
            : 'No pudimos cargar la configuración de la tienda.';
        setError(message);
      } finally {
        if (!cancelled) setLoading(false);
      }
    }

    loadSettings();
    return () => {
      cancelled = true;
    };
  }, []);

  return (
    <AdminShell
      badge="Ajustes de la tienda"
      title="Configuración"
      subtitle="Administra el número oficial de WhatsApp al que llegan los mensajes y pedidos."
    >
      {loading ? (
        <div
          role="status"
          aria-label="Cargando configuración"
          className="bg-white border border-border rounded-3xl p-8 shadow-soft flex items-center gap-3 text-sm text-muted"
        >
          <div className="w-5 h-5 border-2 border-wine/25 border-t-wine rounded-full animate-spin flex-shrink-0" />
          <span>Cargando configuración actual...</span>
        </div>
      ) : error ? (
        <div
          role="alert"
          className="bg-red-50 border border-red-200 text-red-800 rounded-3xl p-6 text-sm font-medium"
        >
          {error}
        </div>
      ) : (
        <WhatsAppSettingsForm
          initialNumber={settings?.whatsappNumber ?? null}
          initialUpdatedAt={settings?.updatedAt ?? null}
          canEdit={canEdit}
        />
      )}
    </AdminShell>
  );
}
