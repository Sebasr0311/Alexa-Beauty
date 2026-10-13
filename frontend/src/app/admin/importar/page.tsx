'use client';

import React, { useState } from 'react';
import Link from 'next/link';
import {
  ArrowLeft,
  Download,
  Upload,
  FileSpreadsheet,
  AlertTriangle,
  CheckCircle2,
  XCircle,
  AlertCircle,
  ArrowRight,
  ShieldAlert,
} from 'lucide-react';
import { formatCOP } from '@/lib/utils';

interface ErrorRow {
  rowNumber: number;
  columnName: string;
  rawValue: string;
  errorMessage: string;
  suggestion: string;
}

interface PreviewItem {
  nombre: string;
  marca: string;
  sku?: string;
  precio: number;
  stock: number;
  categoria?: string;
  tono?: string;
}

interface PreviewResponse {
  jobId: string;
  fileName: string;
  fileHash: string;
  totalRows: number;
  validRows: number;
  rejectedRows: number;
  duplicateSkuCount: number;
  isDuplicateFile: boolean;
  errors: ErrorRow[];
  previewSample: PreviewItem[];
}

export default function AdminImportPage() {
  const [file, setFile] = useState<File | null>(null);
  const [analyzing, setAnalyzing] = useState(false);
  const [preview, setPreview] = useState<PreviewResponse | null>(null);
  const [conflictPolicy, setConflictPolicy] = useState<'CANCEL_ON_ERROR' | 'SKIP_DUPLICATES'>('CANCEL_ON_ERROR');
  const [committing, setCommitting] = useState(false);
  const [commitResult, setCommitResult] = useState<{
    success: boolean;
    importedCount: number;
    initialStockAdded: number;
  } | null>(null);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  const API_BASE = process.env.NEXT_PUBLIC_API_BASE_URL || 'http://localhost:8080/api/v1';

  const handleDownloadTemplate = () => {
    window.open(`${API_BASE}/admin/products/import/template`, '_blank');
  };

  const handleFileChange = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files[0]) {
      const selected = e.target.files[0];
      if (!selected.name.endsWith('.xlsx')) {
        alert('Solo se admiten archivos en formato Excel (.xlsx).');
        return;
      }
      setFile(selected);
      setPreview(null);
      setCommitResult(null);
      setErrorMsg(null);
    }
  };

  const handleAnalyzeFile = async () => {
    if (!file) return;
    setAnalyzing(true);
    setErrorMsg(null);

    const formData = new FormData();
    formData.append('file', file);

    try {
      const token = typeof window !== 'undefined' ? localStorage.getItem('alexa_auth_token') : null;
      const res = await fetch(`${API_BASE}/admin/products/imports/preview`, {
        method: 'POST',
        headers: {
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: formData,
      });

      if (!res.ok) {
        const errJson = await res.json().catch(() => ({}));
        throw new Error(errJson.detail || errJson.message || 'Error al analizar el archivo Excel.');
      }

      const data = await res.json();
      setPreview(data.data || data);
    } catch (err: any) {
      setErrorMsg(err.message || 'Fallo de conexión al analizar el archivo.');
    } finally {
      setAnalyzing(false);
    }
  };

  const handleCommitImport = async () => {
    if (!preview) return;
    setCommitting(true);
    setErrorMsg(null);

    try {
      const token = typeof window !== 'undefined' ? localStorage.getItem('alexa_auth_token') : null;
      const res = await fetch(`${API_BASE}/admin/products/imports/${preview.jobId}/commit`, {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          ...(token ? { Authorization: `Bearer ${token}` } : {}),
        },
        body: JSON.stringify({ policy: conflictPolicy }),
      });

      if (!res.ok) {
        const errJson = await res.json().catch(() => ({}));
        throw new Error(errJson.detail || errJson.message || 'Error al confirmar la importación.');
      }

      const data = await res.json();
      const resultData = data.data || data;
      setCommitResult({
        success: true,
        importedCount: resultData.importedProductsCount || preview.validRows,
        initialStockAdded: resultData.totalInitialStockAdded || 0,
      });
    } catch (err: any) {
      setErrorMsg(err.message || 'No fue posible completar la importación.');
    } finally {
      setCommitting(false);
    }
  };

  const handleDownloadRejected = () => {
    if (!preview) return;
    window.open(`${API_BASE}/admin/products/imports/${preview.jobId}/rejected.xlsx`, '_blank');
  };

  return (
    <div className="min-h-screen bg-cream text-ink pb-16">
      {/* Header */}
      <header className="bg-white border-b border-border sticky top-0 z-20">
        <div className="max-w-6xl mx-auto px-4 py-4 flex items-center justify-between">
          <div className="flex items-center gap-3">
            <Link
              href="/admin"
              className="w-9 h-9 rounded-full border border-border flex items-center justify-center text-muted hover:text-wine hover:border-wine transition"
            >
              <ArrowLeft className="w-4 h-4" />
            </Link>
            <div>
              <h1 className="font-serif text-xl sm:text-2xl text-wine font-normal">
                Importación Masiva de Productos
              </h1>
              <p className="text-muted text-xs">Plantilla oficial ALEXA_PRODUCTS_V1</p>
            </div>
          </div>

          <button
            onClick={handleDownloadTemplate}
            className="inline-flex items-center gap-2 bg-white border border-wine text-wine hover:bg-blush/60 px-4 py-2 rounded-full text-xs font-bold transition shadow-sm"
          >
            <Download className="w-3.5 h-3.5" /> Descargar Plantilla (.xlsx)
          </button>
        </div>
      </header>

      <main className="max-w-5xl mx-auto px-4 py-8 space-y-8">
        {/* Step 1: Upload Card */}
        <div className="bg-white border border-border rounded-3xl p-6 sm:p-8 shadow-[0_9px_26px_rgba(58,26,39,0.03)]">
          <div className="flex items-center gap-3 mb-4">
            <span className="w-7 h-7 rounded-full bg-wine text-white font-bold text-xs flex items-center justify-center">
              1
            </span>
            <h2 className="font-serif text-xl text-wineDark">Selecciona el archivo Excel completado</h2>
          </div>

          <p className="text-muted text-xs sm:text-sm leading-relaxed mb-6">
            Utiliza únicamente el archivo generado por la plataforma. El sistema analizará las filas de la hoja{' '}
            <code className="bg-blush px-2 py-0.5 rounded text-wine font-bold">PRODUCTOS</code>, validará que los datos
            sean coherentes, comprobará que no haya SKUs repetidos y protegerá contra inyecciones de fórmulas.
          </p>

          <div className="border-2 border-dashed border-border rounded-2xl p-8 text-center bg-cream/40 hover:bg-cream/70 transition">
            <FileSpreadsheet className="w-12 h-12 text-mauve mx-auto mb-3" />
            <input
              type="file"
              accept=".xlsx"
              id="excel-upload"
              className="hidden"
              onChange={handleFileChange}
            />
            <label
              htmlFor="excel-upload"
              className="inline-block bg-wine text-white hover:bg-wineDark px-6 py-3 rounded-full text-xs sm:text-sm font-bold cursor-pointer transition shadow-md"
            >
              Seleccionar archivo (.xlsx)
            </label>
            <p className="text-muted text-xs mt-3">
              {file ? (
                <span className="font-semibold text-ink">Archivo seleccionado: {file.name} ({(file.size / 1024).toFixed(1)} KB)</span>
              ) : (
                'Máximo 5MB o 5.000 filas. No uses archivos con macros ni hojas modificadas.'
              )}
            </p>
          </div>

          {file && !preview && !commitResult && (
            <div className="mt-6 flex justify-end">
              <button
                onClick={handleAnalyzeFile}
                disabled={analyzing}
                className="inline-flex items-center gap-2 bg-wine hover:bg-wineDark text-white px-7 py-3 rounded-full text-sm font-bold transition shadow-md disabled:opacity-50"
              >
                {analyzing ? (
                  <>
                    <div className="w-4 h-4 border-2 border-white/20 border-t-white rounded-full animate-spin" />
                    Analizando filas...
                  </>
                ) : (
                  <>
                    Analizar y previsualizar <ArrowRight className="w-4 h-4" />
                  </>
                )}
              </button>
            </div>
          )}

          {errorMsg && (
            <div className="mt-4 p-4 rounded-2xl bg-red-50 border border-red-200 text-red-700 text-xs flex items-center gap-3">
              <AlertCircle className="w-5 h-5 shrink-0" />
              <span>{errorMsg}</span>
            </div>
          )}
        </div>

        {/* Step 2: Preview Results */}
        {preview && !commitResult && (
          <div className="bg-white border border-border rounded-3xl p-6 sm:p-8 shadow-[0_9px_26px_rgba(58,26,39,0.03)] space-y-6">
            <div className="flex items-center gap-3">
              <span className="w-7 h-7 rounded-full bg-wine text-white font-bold text-xs flex items-center justify-center">
                2
              </span>
              <h2 className="font-serif text-xl text-wineDark">Resultado del análisis previo</h2>
            </div>

            {preview.isDuplicateFile && (
              <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200 text-amber-800 text-xs flex items-center gap-3">
                <AlertTriangle className="w-5 h-5 shrink-0 text-amber-600" />
                <span>
                  <strong>Aviso de archivo ya importado:</strong> Este archivo exacto (o uno con el mismo contenido) ya fue procesado con éxito anteriormente. Si continúas, podrías generar duplicados no deseados.
                </span>
              </div>
            )}

            {/* Metrics Grid */}
            <div className="grid grid-cols-2 sm:grid-cols-4 gap-4">
              <div className="p-4 rounded-2xl bg-cream/70 border border-border">
                <span className="text-muted text-[11px] font-bold uppercase tracking-wider block">Total filas</span>
                <span className="font-serif text-2xl text-ink font-bold mt-1 block">{preview.totalRows}</span>
              </div>
              <div className="p-4 rounded-2xl bg-green-50 border border-green-200">
                <span className="text-green-800 text-[11px] font-bold uppercase tracking-wider block">Válidas</span>
                <span className="font-serif text-2xl text-green-700 font-bold mt-1 block">{preview.validRows}</span>
              </div>
              <div className="p-4 rounded-2xl bg-red-50 border border-red-200">
                <span className="text-red-800 text-[11px] font-bold uppercase tracking-wider block">Rechazadas</span>
                <span className="font-serif text-2xl text-red-700 font-bold mt-1 block">{preview.rejectedRows}</span>
              </div>
              <div className="p-4 rounded-2xl bg-amber-50 border border-amber-200">
                <span className="text-amber-800 text-[11px] font-bold uppercase tracking-wider block">SKUs en conflicto</span>
                <span className="font-serif text-2xl text-amber-700 font-bold mt-1 block">{preview.duplicateSkuCount}</span>
              </div>
            </div>

            {/* Errors List */}
            {preview.errors && preview.errors.length > 0 && (
              <div className="border border-red-200 rounded-2xl overflow-hidden bg-red-50/40">
                <div className="p-4 bg-red-100/70 border-b border-red-200 flex items-center justify-between">
                  <div className="flex items-center gap-2 text-red-900 font-bold text-xs sm:text-sm">
                    <ShieldAlert className="w-4 h-4 text-red-600" />
                    <span>Incidencias encontradas ({preview.errors.length})</span>
                  </div>
                  <button
                    onClick={handleDownloadRejected}
                    className="text-xs font-bold text-red-700 hover:underline flex items-center gap-1"
                  >
                    <Download className="w-3.5 h-3.5" /> Descargar filas rechazadas (.xlsx)
                  </button>
                </div>
                <div className="max-h-60 overflow-y-auto divide-y divide-red-100 text-xs">
                  {preview.errors.map((err, idx) => (
                    <div key={idx} className="p-3 grid grid-cols-1 sm:grid-cols-4 gap-2">
                      <span className="font-bold text-red-800">Fila {err.rowNumber} · {err.columnName}</span>
                      <span className="text-muted font-mono">{err.rawValue ? `"${err.rawValue}"` : '(Vacío)'}</span>
                      <span className="text-red-600 sm:col-span-2">
                        {err.errorMessage}. <em className="text-muted font-normal">Sugerencia: {err.suggestion}</em>
                      </span>
                    </div>
                  ))}
                </div>
              </div>
            )}

            {/* Policy choice and Commit button */}
            <div className="p-5 rounded-2xl bg-blush/40 border border-border flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
              <div>
                <label className="text-xs font-extrabold uppercase text-wine tracking-wider block mb-1">
                  Política ante conflictos:
                </label>
                <div className="flex items-center gap-4 text-xs font-medium">
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="policy"
                      value="CANCEL_ON_ERROR"
                      checked={conflictPolicy === 'CANCEL_ON_ERROR'}
                      onChange={() => setConflictPolicy('CANCEL_ON_ERROR')}
                      className="accent-wine"
                    />
                    <span>Cancelar todo si hay errores (Recomendado)</span>
                  </label>
                  <label className="flex items-center gap-2 cursor-pointer">
                    <input
                      type="radio"
                      name="policy"
                      value="SKIP_DUPLICATES"
                      checked={conflictPolicy === 'SKIP_DUPLICATES'}
                      onChange={() => setConflictPolicy('SKIP_DUPLICATES')}
                      className="accent-wine"
                    />
                    <span>Omitir duplicados e importar las válidas</span>
                  </label>
                </div>
              </div>

              <button
                onClick={handleCommitImport}
                disabled={committing || (preview.rejectedRows > 0 && conflictPolicy === 'CANCEL_ON_ERROR')}
                className="bg-wine hover:bg-wineDark text-white px-8 py-3.5 rounded-full text-xs sm:text-sm font-bold transition shadow-lg disabled:opacity-40"
              >
                {committing ? 'Procesando en base de datos...' : `Confirmar e importar ${preview.validRows} productos`}
              </button>
            </div>
          </div>
        )}

        {/* Step 3: Success Screen */}
        {commitResult && (
          <div className="bg-white border border-border rounded-3xl p-8 text-center space-y-4 shadow-soft">
            <div className="w-16 h-16 rounded-full bg-green-100 text-green-700 flex items-center justify-center mx-auto">
              <CheckCircle2 className="w-8 h-8" />
            </div>
            <h2 className="font-serif text-2xl sm:text-3xl text-wine font-normal">
              ¡Importación completada con éxito!
            </h2>
            <p className="text-muted text-sm max-w-md mx-auto leading-relaxed">
              Se crearon exitosamente <strong>{commitResult.importedCount}</strong> fichas de productos y sus variantes correspondientes. Se generaron los movimientos iniciales de inventario por un total de <strong>{commitResult.initialStockAdded}</strong> unidades físicas.
            </p>
            <div className="pt-4 flex justify-center gap-4">
              <Link
                href="/admin"
                className="bg-wine hover:bg-wineDark text-white px-6 py-2.5 rounded-full text-xs font-bold transition shadow-md"
              >
                Ir al panel de control
              </Link>
              <button
                onClick={() => {
                  setFile(null);
                  setPreview(null);
                  setCommitResult(null);
                }}
                className="border border-border bg-white hover:bg-cream text-ink px-6 py-2.5 rounded-full text-xs font-bold transition"
              >
                Importar otro archivo
              </button>
            </div>
          </div>
        )}
      </main>
    </div>
  );
}
