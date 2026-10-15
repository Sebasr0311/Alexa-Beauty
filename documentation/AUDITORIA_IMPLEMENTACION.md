# Seguimiento de Implementación — Corrección Auditoría Alexa Beauty (2026-10-15)

## 1. Estado Inicial y Baseline
- **Fecha:** 2026-10-15
- **Rama base:** `fix/auditoria-alexa-20261015` (derivada de `main` en commit `d50ba3a`)
- **Backend:** Maven Java 21 — `CatalogIntegrationTest` y suite completa pasan en verde.
- **Frontend:** Next.js 15 — `npm test` (3/3 pass), `npx tsc --noEmit` (0 errors), `npm run build` (14/14 static pages).

---

## 2. Matriz de Hallazgos y Plan de Acción

| ID | Severidad | Descripción | Estado |
|---|---|---|---|
| **AB-AUD-01** | **P0 - ALTA** | Filtración de `referenceCost`, `stockOnHand`, `stockReserved`, `minStock` en API pública. | **Resuelto y verificado** (Commit `e8bb2bd`) |
| **AB-AUD-03** | **P0 - ALTA** | Reutilización de `receiptNumber` con contenido distinto debe responder 409 Conflict y no alterar stock. | **Resuelto y verificado** (Commit `e8bb2bd`) |
| **AB-AUD-04** | **P1 - MEDIA** | Búsqueda pública en catálogo no consulta código, nombre ni SKU de variantes. | **Resuelto y verificado** |
| **AB-AUD-05** | **P1 - MEDIA** | Variantes adicionales sin identificación comercial (código, nombre y presentación vacíos). | **Resuelto y verificado** |
| **AB-AUD-06** | **P1 - MEDIA** | Tonos agotados seleccionables para compra en `ShadeSelector`. | **Resuelto y verificado** |
| **AB-AUD-02** | **P1 - ALTA** | Excel V2: importar múltiples tonos bajo la misma ficha de producto con preview agrupada. | **Resuelto y verificado** |
| **AB-AUD-07** | **P2 - MEDIA** | Checkout WhatsApp asistido: botón manual explícito para evitar bloqueo de popups tras llamada async. | **Resuelto y verificado** |
| **AB-AUD-09** | **P2 - MEDIA** | Workflow CI: incorporar suite completa de tests de frontend y backend. | **Resuelto y verificado** |

---

## 3. Evidencia de Pruebas y Verificación

- **Backend (77/77 tests pasando en verde):**
  - `CatalogIntegrationTest`: 9/9 tests pass (búsqueda por tono, código, SKU sin duplicar filas, aislamiento de DTO público).
  - `InventoryIntegrationTest`: 12/12 tests pass (idempotencia canónica, rechazo 409 ante payload discrepante sin alterar stock).
  - `ExcelImportIntegrationTest`: 8/8 tests pass (1 producto + 4 tonos = 1 ficha + 4 variantes + 4 movimientos iniciales Kardex; escala 500 filas en <15s; validación de errores por fila; compatibilidad retroactiva V1 intacta).
  - Suite completa: 77 tests run, 0 failures, 0 errors, 0 skipped.
- **Frontend (3/3 tests unitarios, TypeScript limpio y build exitoso):**
  - `npm test`: 3/3 tests passing en `tests/shades.test.cjs`.
  - `npx tsc --noEmit`: 0 errores de tipado TypeScript.
  - `npm run build`: 14 páginas estáticas y dinámicas compiladas exitosamente.
- **Flujo de Checkout WhatsApp:**
  - `frontend/src/app/(store)/pedido/page.tsx`: Eliminado `window.open` tras `await` que causaba bloqueo por bloqueador de ventanas emergentes. Texto del formulario actualizado a modelo de atención asistida sin promesas falsas de automatización.
  - `frontend/src/app/(store)/pedido/confirmado/[reference]/page.tsx`: Botón CTA principal explícito "Enviar pedido por WhatsApp ↗" con enlace `wa.me` directo y fallback de copiado al portapapeles.
- **Workflow CI:**
  - `.github/workflows/ci-frontend.yml`: Agregados pasos de `npm test` y `npx tsc --noEmit` previos a `npm run build`.
