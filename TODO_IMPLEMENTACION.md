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

- [ ] **AB-005** Migración `V1__initial_schema.sql`:
  - Tablas: `users`, `brands`, `categories`, `products`, `product_variants`, `product_images`, `app_settings`.
  - Restricciones: UUIDs, checks de precios >= 0, stocks >= 0, slugs y SKUs únicos.
- [ ] **AB-006** Migración `V2__inventory_and_orders.sql`:
  - Tablas: `inventory_movements`, `stock_receipts`, `stock_receipt_items`, `orders`, `order_items`, `stock_reservations`, `payments`, `order_status_history`, `audit_logs`, `excel_import_jobs`, `excel_import_errors`.
  - Check de integridad: `stock_reserved <= stock_on_hand`.
- [ ] **AB-007** Migración `V3__seed_admin_and_settings.sql`:
  - Usuario admin inicial (con hash Argon2/BCrypt configurable).
  - Parámetros iniciales de negocio (WhatsApp Business, moneda COP, TTL reserva 45m).
- [ ] **AB-008** Autenticación y Autorización (Spring Security + Cookie HttpOnly / JWT seguro):
  - Endpoint `POST /api/v1/auth/login`, `POST /api/v1/auth/logout`, `GET /api/v1/auth/me`.
  - RBAC: Roles `ADMIN` y `OPERADOR`. Política "Default Deny".
  - Rate limiting en login y protección contra fuerza bruta.

---

## Épica 2: Catálogo, Productos y Almacenamiento de Imágenes (Fase 4)
**Objetivo:** Gestión de marcas, categorías, productos, variantes y assets multimedia.

- [ ] **AB-009** Módulo Catálogo Backend:
  - Endpoints CRUD: `/api/v1/admin/products`, `/api/v1/catalog/products` (público paginado y filtrable).
  - Generación automática de variante `DEFAULT` si no se especifican tonos.
  - Registro de movimiento `ENTRADA_INICIAL` al crear producto con stock > 0.
- [ ] **AB-010** Servicio de Medios (`StorageGateway`):
  - Interfaz `StorageGateway` con implementación local para tests y Supabase Storage para producción.
  - Validación de imágenes (MIME `jpeg`, `png`, `webp`, rechazo de ejecutables/falsos binarios).
  - Endpoints: `POST /api/v1/admin/products/{id}/images`, `DELETE /api/v1/admin/products/{id}/images/{imageId}`.
- [ ] **AB-011** UI Catálogo y Ficha de Producto (Frontend):
  - Vista pública móvil `/` y `/catalogo` con búsqueda debounce, filtros y selector de tonos.
  - Ficha de producto `/producto/[slug]` con proporciones 1:1, estados agotado/disponible y accesibilidad WCAG AA.
  - Panel admin `/admin/productos` y `/admin/productos/nuevo`.

---

## Épica 3: Inventario Transaccional y Recepción de Mercancía (Fase 5)
**Objetivo:** Control estricto de existencias con la ecuación `available = on_hand - reserved`.

- [ ] **AB-012** Kardex y Ledger de Movimientos:
  - Registro inmutable en `inventory_movements` (tipos: `ENTRADA`, `VENTA`, `SALIDA`, `AJUSTE`, `DEVOLUCION`).
  - Endpoint `GET /api/v1/admin/inventory/movements`.
- [ ] **AB-013** Módulo de Recepción de Mercancía (`stock_receipts`):
  - Endpoint `POST /api/v1/admin/inventory/receipts` con clave de idempotencia.
  - Incremento atómico de `stock_on_hand` (no altera `reserved`).
  - Registro de proveedor, costo, factura y movimiento asociado.
- [ ] **AB-014** Ajuste Manual de Inventario con Causa:
  - Endpoint `POST /api/v1/admin/inventory/adjustments` con motivo obligatorio y auditoría.
  - Validación estricta: prohibido dejar `on_hand < reserved`.
- [ ] **AB-015** UI de Inventario y Recepciones en Panel Admin:
  - Buscador rápido por SKU/nombre, vista Antes/Entrada/Después.
  - Prevención de doble clic en UI.

---

## Épica 4: Importación Masiva por Excel (Fase 6)
**Objetivo:** Flujo seguro de importación de productos vía Apache POI y plantilla descargable.

- [ ] **AB-016** Generador de Plantilla `ALEXA_PRODUCTS_V1`:
  - Endpoint `GET /api/v1/admin/products/import/template`.
  - Hojas: `PRODUCTOS` (cabeceras idénticas al contrato), `INSTRUCCIONES` y `EJEMPLOS_NO_IMPORTAR`.
- [ ] **AB-017** Validador y Preview de Importación:
  - Endpoint `POST /api/v1/admin/products/imports/preview`.
  - Inspección de 5MB / 5.000 filas, sanitización de inyección de fórmulas (`=`, `+`, `-`, `@`).
  - Detección de SKUs duplicados dentro del archivo y contra la BD.
  - Respuesta con reporte de incidencias (fila, columna, valor, causa, solución).
- [ ] **AB-018** Commit Atómico de Importación:
  - Endpoint `POST /api/v1/admin/products/imports/{jobId}/commit`.
  - Políticas ante conflictos: Omitir o Cancelar todo.
  - Creación de movimientos `ENTRADA_INICIAL` en transacción única.
- [ ] **AB-019** UI de Importación en Panel Admin (`/admin/importar`):
  - Drag-and-drop de `.xlsx`, previsualización de errores y confirmación explícita.

---

## Épica 5: Carrito, Checkout y Gateway WhatsApp (Fases 7 & 8)
**Objetivo:** Checkout transaccional con reserva de inventario y generación del enlace de conversación.

- [ ] **AB-020** Carrito de Compras en Frontend:
  - Persistencia en `localStorage`, cálculo de subtotales, validación reactiva de existencias.
- [ ] **AB-021** Checkout Transaccional (`POST /api/v1/orders`):
  - Recalculo autoritativo de precios en backend (el cliente nunca envía el total final).
  - Bloqueo pesimista determinista (`SELECT ... FOR UPDATE` ordenado por variant_id).
  - Comprobación: `available >= requested_qty`.
  - Incremento de `stock_reserved` y creación de `stock_reservations` con TTL de 45m.
  - Generación de código público único `AB-YYYY-NNNNNN` y token seguro de seguimiento.
  - Clave de idempotencia `Idempotency-Key` en headers.
- [ ] **AB-022** Interfaz `WhatsAppGateway`:
  - `ClickToChatGateway` (MVP): Generación de URL `https://wa.me/{number}?text={encodedText}` con desglose del pedido.
  - Fallback en UI: botón "Copiar mensaje" si falla la apertura de la app.
  - Especificación documentada para futuro `CloudApiGateway`.
- [ ] **AB-023** Job de Expiración y Reconciliación de Reservas:
  - Tarea programada confiable cada 5 minutos (`ScheduledJob` transaccional).
  - Detección de órdenes en `PENDIENTE_CONTACTO` o `PENDIENTE_PAGO` con `expires_at < now()`.
  - Transición a `EXPIRADO` y decremento de `stock_reserved`.
- [ ] **AB-024** UI de Checkout y Confirmación de Pedido:
  - Pantalla `/pedido` y `/pedido/confirmado/[reference]`. Mensajes honestos (sin proclamar pago falso).

---

## Épica 6: Verificación de Pagos y Ventas (Fases 8 & 9)
**Objetivo:** Confirmación administrativa humana de transferencias, consumo de stock y reporte de ventas.

- [ ] **AB-025** Verificación de Pago (`POST /api/v1/admin/orders/{id}/confirm-payment`):
  - Operación atómica e idempotente.
  - Transición a `PAGADO`.
  - Consumo de reserva: `stock_reserved -= qty`, `stock_on_hand -= qty`.
  - Creación de movimiento `VENTA` en el ledger.
  - Registro de datos del pago (método Nequi/Daviplata/Bancolombia, referencia, comprobante, usuario auditor).
- [ ] **AB-026** Gestión de Estados y Cancelaciones:
  - Transiciones válidas: `PREPARANDO`, `DESPACHADO`, `ENTREGADO`, `CANCELADO`.
  - Cancelación de pedido antes del pago libera únicamente `stock_reserved`.
- [ ] **AB-027** Bandeja de Pedidos Admin (`/admin/pedidos`):
  - Filtros por estado, fecha, buscador por cliente o referencia.
  - Formulario modal de verificación de pago con confirmación de seguridad.

---

## Épica 7: Reportes, Analítica y Exportación Excel (Fases 9 & 10)
**Objetivo:** Cuadre 100% exacto entre BD, interfaz web y libro Excel exportado.

- [ ] **AB-028** Agregación de Ventas en Backend:
  - Endpoint `GET /api/v1/admin/sales` filtrado por fecha real de pago verificado en `America/Bogota`.
  - Exclusión estricta de pedidos pendientes o cancelados.
  - Cálculo de KPIs: Ingresos cobrados, unidades vendidas, ticket promedio.
- [ ] **AB-029** Exportador Profesional de Ventas XLSX:
  - Endpoint `GET /api/v1/admin/sales/export.xlsx` con los mismos filtros aplicados.
  - 5 Hojas: `RESUMEN` (con estilo bordó/malva), `VENTAS`, `DETALLE_PRODUCTOS`, `TOP_PRODUCTOS`, `METADATOS`.
  - Fórmulas de suma para totales y anchos legibles.
- [ ] **AB-030** Dashboard de Analítica y Top Productos:
  - Endpoint `GET /api/v1/admin/analytics/overview` y `/top-products`.
  - UI `/admin/ventas`, `/admin/analitica` y `/admin/reportes`.

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
