# TODO DE IMPLEMENTACIÓN — ALEXA BEAUTY

> **Estado general:** En planificación inicial / Scaffold fundacional  
> **Última actualización:** 10 de octubre de 2026  
> **Fuente de verdad:** `documentation/PLAN_MAESTRO_ALEXA_BEAUTY.md` y `documentation/PROMPT_PARA_CODEX.md`

---

## Convenciones de Estado
- `[ ]` Pendiente
- `[~]` En progreso
- `[X]` Completado y verificado con pruebas reales
- `[!]` Bloqueado / Pendiente de configuración externa

---

## Épica 0: Descubrimiento, Scaffold y Fundaciones de Proyecto
**Objetivo:** Establecer la estructura del monorepo, contratos base, documentación viva y entornos reproducibles.

- [X] **AB-000** Auditoría de requerimientos, activos de branding y plantillas Excel.
- [X] **AB-001** Estructura del monorepo, `.gitignore`, `TODO_IMPLEMENTACION.md`, `DECISIONES.md` y `README.md`.
- [X] **AB-002** Backend Java 21 + Spring Boot 3.3.5 scaffold modular:
  - Dependencias: Web, Validation, Security, Data JPA, Flyway, PostgreSQL, Apache POI, OpenAPI (springdoc), Actuator.
  - Health check endpoint `/api/v1/health/public` y `/actuator/health`.
  - Manejo global de excepciones (`GlobalExceptionHandler`, RFC 7807 ProblemDetail).
  - Test smoke inicial (`AlexaBeautyApplicationTests` y `HealthControllerTest` passing).
- [X] **AB-003** Frontend Next.js 15+ (App Router) + TypeScript + Tailwind CSS scaffold:
  - Tokens visuales configurados (`wine: #711A38`, `wine-dark: #4C1126`, `mauve: #B7748C`, `cream: #FCF8F2`, `ink: #2B2025`).
  - Layout responsive base (360px – 1440px) y navegación mobile-first.
  - Vistas iniciales: Home (`/`), Catálogo (`/catalogo`), Carrito (`/carrito`), Admin preview (`/admin`).
  - Build verificado sin errores (`npm run build` PASS).
- [X] **AB-004** Configuración de variables de entorno (`.env.example`), Docker Compose para desarrollo local (Postgres 16) y GitHub Actions CI workflows (`ci-backend.yml`, `ci-frontend.yml`).

---

## Épica 1: Datos, Migraciones y Seguridad (Fases 2 & 3)
**Objetivo:** Modelo relacional completo e inmutable con Flyway, control de roles y acceso seguro.

- [X] **AB-005** Migración `V1__initial_schema.sql`:
  - Tablas: `users`, `brands`, `categories`, `products`, `product_variants`, `product_images`, `app_settings`.
  - Restricciones: UUIDs, checks de precios >= 0, stocks >= 0, slugs y SKUs únicos, `stock_reserved <= stock_on_hand`.
- [X] **AB-006** Migración `V2__inventory_and_orders.sql`:
  - Tablas: `inventory_movements`, `stock_receipts`, `stock_receipt_items`, `orders`, `order_items`, `stock_reservations`, `payments`, `order_status_history`, `audit_logs`, `excel_import_jobs`, `excel_import_errors`.
  - Índices de rendimiento en órdenes, reservas y movimientos.
- [X] **AB-007** Migración `V3__seed_data.sql`:
  - Usuario admin inicial (`admin@alexabeauty.com`, hash BCrypt).
  - Parámetros de negocio (`app_settings`), 4 categorías iniciales y 3 marcas semilla.
- [X] **AB-008** Autenticación y Autorización (Spring Security 6 + JWT y HttpOnly Cookie):
  - Endpoints `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `GET /api/v1/auth/me`.
  - RBAC: Roles `ADMIN` y `OPERADOR`. Manejo RFC 7807 de 401/403.
  - Verificado con `AuthIntegrationTest` y `FlywayMigrationTest` (13 tests PASS).

---

## Épica 2: Catálogo, Productos y Almacenamiento de Imágenes (Fase 4)
**Objetivo:** Gestión de marcas, categorías, productos, variantes y assets multimedia.

- [X] **AB-009** Módulo Catálogo Backend:
  - Endpoints CRUD: `/api/v1/admin/products`, `/api/v1/catalog/products` (público paginado y filtrable), `/catalog/products/{slug}`, `/catalog/categories`, `/catalog/brands`.
  - Generación automática de variante `DEFAULT` si no se especifican tonos.
  - Registro atómico de movimiento `ENTRADA_INICIAL` en el kardex al crear producto con stock > 0.
  - Detección de SKU duplicado (HTTP 409) y validación de precios negativos (HTTP 400).
- [X] **AB-010** Servicio de Medios (`StorageGateway`):
  - Interfaz `StorageGateway` con implementación `LocalStorageGateway` y `SupabaseStorageGateway`.
  - Validación estricta de imágenes (MIME `jpeg`, `png`, `webp`, rechazo de otros formatos con HTTP 400).
  - Endpoints: `POST /api/v1/admin/products/{id}/images`, `DELETE /api/v1/admin/products/{id}/images/{imageId}`, `PATCH /api/v1/admin/products/{id}/images/{imageId}/cover`.
- [X] **AB-011** UI Catálogo y Ficha de Producto (Frontend):
  - Cliente API tipado `src/lib/api.ts` con manejo de tokens y errores.
  - Ficha de producto `/producto/[slug]` con proporciones 1:1, selector interactivo de tonos/variantes, indicador de existencias disponibles y botón de consulta por WhatsApp.
  - `ProductCard` interactivo enlazado a la ficha de producto. Build de producción verificado (`npm run build` PASS).

---

## Épica 3: Inventario Transaccional y Recepción de Mercancía (Fase 5)
**Objetivo:** Control estricto de existencias con la ecuación `available = on_hand - reserved`.

- [X] **AB-012** Kardex y Ledger de Movimientos:
  - Registro inmutable en `inventory_movements` (tipos: `ENTRADA_INICIAL`, `ENTRADA`, `VENTA`, `SALIDA`, `AJUSTE`, `DEVOLUCION`).
  - Endpoint `GET /api/v1/admin/inventory/movements` con filtros dinámicos por variante, tipo y rango de fechas.
- [X] **AB-013** Módulo de Recepción de Mercancía (`stock_receipts`):
  - Endpoint `POST /api/v1/admin/inventory/receipts` con soporte para header `Idempotency-Key` (garantía matriz P10).
  - Bloqueo pesimista determinista libre de interbloqueos (`findByIdInOrderByIdForUpdate`).
  - Incremento atómico de `stock_on_hand` preservando rigurosamente `stock_reserved` (matriz P09).
  - Endpoints `GET /receipts` y `GET /receipts/{id}` con trazabilidad completa de costos e ítems.
- [X] **AB-014** Ajuste Manual de Inventario con Causa:
  - Endpoint `POST /api/v1/admin/inventory/adjustments` con motivo obligatorio y registro en `audit_logs`.
  - Validación de regla inviolable: rechazo con HTTP 422 si se intenta dejar `newStockOnHand < stock_reserved`.
- [X] **AB-015** Resumen de Inventario y Existencias:
  - Endpoint `GET /api/v1/admin/inventory/summary` con cálculo `availableStock = on_hand - reserved` y filtro `lowStockOnly`.
  - Verificado con suite de pruebas `InventoryIntegrationTest` (26 tests pasando en total).

---

## Épica 4: Importación Masiva por Excel (Fase 6)
**Objetivo:** Flujo seguro de importación de productos vía Apache POI y plantilla descargable.

- [X] **AB-016** Generador de Plantilla `ALEXA_PRODUCTS_V1`:
  - Endpoint `GET /api/v1/admin/products/import/template`.
  - Hojas: `PRODUCTOS` (12 columnas exactas del contrato estilizadas con paleta de marca), `INSTRUCCIONES` y `EJEMPLOS_NO_IMPORTAR`.
- [X] **AB-017** Validador y Preview de Importación:
  - Endpoint `POST /api/v1/admin/products/imports/preview`.
  - Protección contra formula injection (`ExcelSanitizer`, matriz P07).
  - Detección de duplicados internos y contra BD, cálculo de SHA-256 para prevenir reimportaciones.
  - Tabla de errores persistida en `excel_import_errors` y descarga de rechazadas vía `GET /imports/{jobId}/rejected.xlsx`.
- [X] **AB-018** Commit Atómico de Importación:
  - Endpoint `POST /api/v1/admin/products/imports/{jobId}/commit`.
  - Idempotencia comprobada (P06: segundo commit lanza 409 Conflict sin duplicar stock).
  - Creación de movimientos `ENTRADA_INICIAL` en el ledger para cada producto con stock inicial > 0.
- [X] **AB-019** UI de Importación en Panel Admin (`/admin/importar`):
  - Flujo completo: descarga de plantilla, drag-and-drop, vista previa de métricas/errores, selector de políticas y confirmación. Build verificado (`npm run build` PASS).

---

## Épica 5: Carrito, Checkout y Gateway WhatsApp (Fases 7 & 8)
**Objetivo:** Checkout transaccional con reserva de inventario y generación del enlace de conversación.

- [X] **AB-020** Carrito de Compras en Frontend:
  - Persistencia en `localStorage`, cálculo de subtotales, soporte para variantes y tonos.
- [X] **AB-021** Checkout Transaccional (`POST /api/v1/orders`):
  - Recálculo autoritativo de precios en backend (precios congelados en `order_items`).
  - Bloqueo pesimista determinista en orden ascendente de UUID (`findByIdInOrderByIdForUpdate`, previene deadlocks).
  - Comprobación estricta de disponibilidad `available >= requested_qty` (P11 matrix: sin sobreventa ni stock negativo).
  - Incremento atómico de `stock_reserved` y creación de `stock_reservations` con TTL de 45m.
  - Generación de código público único `AB-YYYY-NNNNNN` y token seguro de seguimiento hasheado (SHA-256).
  - Soporte para `Idempotency-Key` (P13 matrix: reintentos no duplican reservas ni pedidos).
- [X] **AB-022** Interfaz `WhatsAppGateway`:
  - `ClickToChatGateway` (MVP): Generación de URL `https://wa.me/{number}?text={encodedText}` con desglose del pedido.
  - Fallback en UI: botón "Copiar mensaje de pedido" con retroalimentación instantánea (P24 matrix).
- [X] **AB-023** Job de Expiración y Reconciliación de Reservas:
  - Tarea programada confiable `ReservationExpirationJob` cada 60s con `@EnableScheduling`.
  - Liberación transaccional de reservas vencidas (`stock_reserved` vuelve a 0, `stock_on_hand` intacto, matriz P14).
  - Transición a `EXPIRADO` con registro en `order_status_history`.
- [X] **AB-024** UI de Checkout y Confirmación de Pedido:
  - Pantallas `/pedido` y `/pedido/confirmado/[reference]` con cuenta regresiva en tiempo real y mensaje honesto sin proclamar pagos falsos. Build verificado (`npm run build` PASS).

---

## Épica 6: Verificación de Pagos y Ventas (Fases 8 & 9)
**Objetivo:** Confirmación administrativa humana de transferencias, consumo de stock y reporte de ventas.

- [X] **AB-025** Verificación de Pago (`POST /api/v1/admin/orders/{id}/confirm-payment`):
  - Operación atómica e idempotente con bloqueo pesimista de orden y variantes (`orderRepository.findByIdForUpdate`).
  - Consumo irreversible de stock: `stock_reserved -= qty` y `stock_on_hand -= qty` en transacción única.
  - Creación del movimiento `VENTA` en el ledger kardex con referencia de pago.
  - Garantía matrices P15 y P16: confirmaciones repetidas o concurrentes no duplican el descuento ni la venta.
  - Garantía matriz P17: rechazo de pagos sobre pedidos expirados o cancelados (409 Conflict).
- [X] **AB-026** Gestión de Estados y Cancelaciones:
  - Máquina de estados: `PAGADO -> PREPARANDO -> DESPACHADO -> ENTREGADO`.
  - Prórroga de reserva (`/extend-reservation`) auditada.
  - Cancelación antes del pago (matriz P18): libera `stock_reserved` manteniendo intacto `stock_on_hand`.
- [X] **AB-027** Bandeja de Pedidos Admin (`/admin/pedidos`):
  - Tabla responsive con filtros por estado, búsqueda y fecha.
  - Drawer con detalle del pedido, historial cronológico y pagos.
  - Modal de verificación de transferencias (Nequi, Daviplata, Bancolombia, etc.) y botones de avance de estado. Build verificado (`npm run build` PASS).

---

## Épica 7: Reportes, Analítica y Exportación Excel (Fases 9 & 10)
**Objetivo:** Cuadre 100% exacto entre BD, interfaz web y libro Excel exportado.

- [X] **AB-028** Agregación de Ventas en Backend:
  - Endpoint `GET /api/v1/admin/sales` filtrado por fecha real de pago verificado en `America/Bogota` (garantía matriz P19: 23:59 vs 00:01).
  - Exclusión estricta de pedidos pendientes, expirados o cancelados.
  - Cálculo consistente de KPIs: Ingresos cobrados, pedidos pagados, unidades vendidas, ticket promedio.
- [X] **AB-029** Exportador Profesional de Ventas XLSX:
  - Endpoint `GET /api/v1/admin/sales/export.xlsx` con los mismos filtros aplicados y garantía matriz P20 (coincidencia 100% con la API).
  - 5 Hojas exactas del contrato `AlexaBeauty_Ejemplo_Reporte_Ventas.xlsx`: `VENTAS`, `DETALLE_PRODUCTOS`, `TOP_PRODUCTOS`, `RESUMEN` (con estilo bordó `#711A38`/malva) y `METADATOS`.
  - Fórmulas dinámicas de suma `=SUM(H6:H{N})`, formato de moneda `$ #,##0`, porcentajes `0.0%`, paneles congelados y autoajuste de columnas.
- [X] **AB-030** Dashboard de Analítica y Top Productos:
  - Endpoints `GET /api/v1/admin/analytics/overview` y `GET /api/v1/admin/analytics/top-products`.
  - UI interactiva `/admin/ventas` con filtros por fecha/método, modal de items y descarga de Excel.
  - UI interactiva `/admin/analitica` con gráfico de tendencia diaria, ranking con medallas y desglose de canales de pago. Build verificado (`npm run build` PASS con 12 rutas).
  - Verificado con suite `SalesAndReportingIntegrationTest` (48 tests pasando en backend).

---

## Épica 8: Endurecimiento, Pruebas y Despliegue (Fases 10, 11 & 12)
**Objetivo:** Verificación contra la matriz P01–P26, seguridad OWASP, CI/CD y despliegue en Vercel/Render.

- [ ] **AB-031** Suite de Pruebas Automatizadas:
  - Unitarias (JUnit 5 + Mockito).
  - Integración con Testcontainers PostgreSQL (pruebas de concurrencia de reservas y doble verificación).
  - E2E Playwright móvil (flujo completo catálogo → checkout → whatsapp).
- [ ] **AB-032** Dockerfile backend, Workflows de GitHub Actions (`ci-backend.yml`, `ci-frontend.yml`).
- [ ] **AB-033** Configuración y guías de despliegue en Render y Vercel + Runbook de contingencias.
- [ ] **AB-034** Generación del informe final de verificación `INFORME_FINAL_VERIFICACION.md`.
