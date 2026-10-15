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
| **AB-AUD-01** | **P0 - ALTA** | Filtración de `referenceCost`, `stockOnHand`, `stockReserved`, `minStock` en API pública. | En progreso |
| **AB-AUD-03** | **P0 - ALTA** | Reutilización de `receiptNumber` con contenido distinto debe responder 409 Conflict y no alterar stock. | En progreso |
| **AB-AUD-04** | **P1 - MEDIA** | Búsqueda pública en catálogo no consulta código, nombre ni SKU de variantes. | Pendiente |
| **AB-AUD-05** | **P1 - MEDIA** | Variantes adicionales sin identificación comercial (código, nombre y presentación vacíos). | Pendiente |
| **AB-AUD-06** | **P1 - MEDIA** | Tonos agotados seleccionables para compra en `ShadeSelector`. | Pendiente |
| **AB-AUD-02** | **P1 - ALTA** | Excel V2: importar múltiples tonos bajo la misma ficha de producto con preview agrupada. | Pendiente |
| **AB-AUD-07** | **P2 - MEDIA** | Checkout WhatsApp asistido: botón manual explícito para evitar bloqueo de popups tras llamada async. | Pendiente |
| **AB-AUD-09** | **P2 - MEDIA** | Workflow CI: incorporar suite completa de tests de frontend y backend. | Pendiente |
