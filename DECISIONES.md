# REGISTRO DE DECISIONES Y SUPUESTOS — ALEXA BEAUTY

> **Fecha:** 10 de octubre de 2026  
> **Estado:** Documento vivo de arquitectura y supuestos de negocio.

---

## 1. Supuestos Reversibles Iniciales (No Bloqueantes)

Para avanzar con agilidad sin detener el desarrollo técnico en aspectos que dependen de acuerdos comerciales futuros con los dueños del negocio, se establecen los siguientes supuestos configurables y reversibles:

| ID | Área | Supuesto Inicial Establecido | Mecanismo de Reversibilidad / Configuración |
|---|---|---|---|
| **SUP-01** | **Moneda y Formato** | Moneda legal colombiana `COP`, sin decimales en interfaz (`$ 25.000`). | Parámetro en tabla `app_settings` y variable de entorno. |
| **SUP-02** | **Zona Horaria de Negocio** | `America/Bogota` (UTC-5) para todas las agrupaciones de días de venta y reportes. En BD se almacena siempre `TIMESTAMPTZ` en UTC. | Configurable en `application.yml` (`app.timezone`). |
| **SUP-03** | **Duración de Reservas de Stock** | **45 minutos** de vigencia desde la creación del pedido (`expires_at = now() + 45 min`). | Parámetro `RESERVATION_MINUTES` en `app_settings` / `.env`. |
| **SUP-04** | **Canal de Pago MVP** | Pagos manuales mediante transferencias bancarias o billeteras electrónicas locales (Nequi, Daviplata, Bancolombia). No se integra pasarela de cobro automático con comisiones en el MVP. | Diseñado bajo interfaz `PaymentVerificationService` extensible a webhooks de pasarelas. |
| **SUP-05** | **Canal de WhatsApp** | Enlace `https://wa.me/{number}` prellenado (`ClickToChatGateway`). No requiere credenciales ni cuenta aprobada de Meta en etapa inicial. | Abstracción `WhatsAppGateway` lista para incorporar `CloudApiGateway` cuando se disponga de Meta Business Platform. |
| **SUP-06** | **Autenticación de Clientes** | Clientes públicos compran sin necesidad de crear cuenta ni ingresar contraseñas (checkout express tipo boutique). | Los pedidos se asocian a nombre + teléfono y se protegen con un token seguro de seguimiento (`tracking_token`). |
| **SUP-07** | **Identidad Visual / Logo** | Se incorpora la propuesta refinada (`Pareja chibi con gafas y destellos.png`) optimizada a WebP para la interfaz, manteniendo el logotipo original preservado en `branding/original/` hasta aprobación final de los propietarios. | Componente de Logo modular en Frontend que lee la ruta configurada en `app_settings`. |
| **SUP-08** | **Variantes de Producto** | Todo producto en el modelo de base de datos se asocia a variantes (`product_variants`). Si un producto no tiene tonos o tamaños, el sistema genera automáticamente una variante `DEFAULT`. | Permite añadir tonos o presentaciones en el futuro sin modificar esquemas relacionales ni migraciones. |
| **SUP-09** | **Costo de Adquisición** | La captura del costo de compra (`costo_unitario_cop`) es opcional. El cálculo de margen bruto sólo se proyectará para los artículos donde se haya registrado dicho costo. | Campos anulables (`NULL`) en `order_items.cost_snapshot`. |
| **SUP-10** | **Tarifas de Envío** | Entrega coordinada por WhatsApp como "Por coordinar" o valor 0 en MVP. | El administrador puede asignar el valor del flete durante la atención directa antes de confirmar el total a pagar. |

---

## 2. Decisiones Arquitectónicas Principales (ADR)

### ADR-01: Monorepo Modular
- **Decisión:** Mantener `backend/` (Spring Boot Java 21) y `frontend/` (Next.js 15) en un único repositorio con CI independiente.
- **Razón:** Facilita la sincronización de contratos de datos, OpenAPI, tipados TypeScript y pruebas E2E sin desincronización de repositorios.

### ADR-02: Transaccionalidad de Inventario con Bloqueo Pesimista
- **Decisión:** Toda reserva de pedido ejecuta `SELECT ... FOR UPDATE` ordenado por `variant_id` sobre las filas de `product_variants`.
- **Razón:** Prevenir condiciones de carrera (*race conditions* / sobreventa) bajo solicitudes concurrentes concurrentes. Garantiza `available = on_hand - reserved >= 0`.

### ADR-03: Consumo Atómico de Stock e Idempotencia en Confirmación de Pago
- **Decisión:** Al marcar un pedido como `PAGADO`, se verifica en una única transacción que el estado anterior sea `PENDIENTE_PAGO` o `PAGO_EN_REVISION`, se descuenta simultáneamente `stock_reserved` y `stock_on_hand`, se genera el movimiento `VENTA` y se bloquean solicitudes duplicadas mediante clave de idempotencia.
- **Razón:** Evita ventas duplicadas ante dobles clics o confirmaciones concurrentes de múltiples administradores.

### ADR-04: Contrato de Plantilla Excel Server-Side
- **Decisión:** La plantilla descargable de productos y el reporte de ventas son generados directamente por el backend con Apache POI respetando las cabeceras exactas de `ALEXA_PRODUCTS_V1`.
- **Razón:** Asegura que cualquier cambio en las validaciones se refleje directamente en la plantilla descargable y previene inyecciones maliciosas de fórmulas al exportar.

### ADR-05: Entornos e Infraestructura Desacoplada
- **Decisión:** Backend desplegable en Render (o contenedor Docker estándar) y Frontend en Vercel, con base de datos PostgreSQL y Storage alojados en Supabase.
- **Razón:** Aprovecha el poder de edge caching y SSR de Vercel con el rendimiento transaccional robusto de Spring Boot y PostgreSQL.
