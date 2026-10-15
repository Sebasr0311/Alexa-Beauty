# AUDITORÍA TÉCNICA DE ALEXA BEAUTY
**Fecha:** 15 de octubre de 2026  
**Fuente principal:** archivo aportado `Alexa-Beauty(1).zip` (405 entradas ZIP), extraído localmente sin ejecutar su código de producción.  
**Alcance:** Java 21/Spring Boot; Next.js 15/React 19; PostgreSQL/Supabase; migraciones V1-V6; inventario, tonos, Excel, ventas, pedidos, pagos, seguridad, rendimiento, experiencia móvil y despliegue.  
**Estado del repositorio y web publicada:** no hay verificación E2E de la URL de Vercel ni confirmación de que este ZIP coincida con el despliegue. La navegación web pública no fue accesible en esta sesión. No se efectuó ningún cambio en producción ni se subió código.

## 1. Dictamen

**NO APROBADA PARA DECLARAR «LISTA PARA PRODUCCIÓN» EN ESTA AUDITORÍA.** Esto no significa que la tienda necesariamente esté rota: significa que se identificaron defectos y faltan pruebas con la pila completa y el entorno real. La base técnica es reutilizable; NO recomiendo reescribir desde cero.

**Muy positivo:** arquitectura modular; stock físico y reservado diferenciados; bloqueos y revalidación de reservas; pago manual verificado; migraciones versionadas; alta/edición/desactivación de tonos y fotos por variante; catálogo mobile-first; idempotencia en pedidos; filtros de ventas de pagos verificados. Estas son conclusiones del código revisado, no de pruebas de producción.

**Resumen de hallazgos:** 3 ALTA, 7 MEDIA y 4 BAJA/MEJORA. Las severidades corresponden al impacto potencial en este emprendimiento; deben confirmarse contra la configuración y datos reales.

## 2. Resultado de las verificaciones efectivamente ejecutadas

| Control | Resultado | Alcance y limitación |
|---|---|---|
| Integridad/extracción del ZIP | COMPLETADO | 405 entradas, 5.249.934 bytes descomprimidos aprox. |
| Inspección manual estática | COMPLETADO | Servicios, controladores, entidades, migraciones, frontend, CI, Excel, variantes y configuraciones. |
| `cd frontend && npm test` | PASS, 3/3 | Prueba de `initialVariant`, etiquetas y selección de imágenes; no prueba componentes montados ni navegador. |
| TypeScript/TSX — `typescript.transpileModule` | PASS sintaxis, 28/28 | Solo sintaxis/transpilación; **NO** tipado global ni `next build`. |
| `npm ci --offline` | BLOQUEADO | Paquete `util-deprecate@1.0.2` ausente en caché. |
| `npm run build` y `npm run lint` | NO EJECUTADO | Dependencias completas ausentes. |
| Maven `test`, `package` y Flyway PostgreSQL | NO EJECUTADO | No está instalado `mvn`; el wrapper del ZIP solo aporta `maven-wrapper.properties`, no `maven-wrapper.jar`; sin dependencias Maven locales listas. |
| Playwright/E2E web real | NO EJECUTADO | No hay navegador/sesión del despliegue disponible; no se ejecutaron flujos reales. |
| Vercel/Render/Supabase/WhatsApp en producción | NO VERIFICADO | No se usaron credenciales ni se modificaron servicios. |
| Análisis SCA de dependencias, pentest y restauración de backup | NO EJECUTADO | Requieren entorno y herramientas adicionales. |

`documentation/PRODUCTION_READINESS_2026-10-13.md` relata pruebas de una versión anterior con PostgreSQL y navegador locales, y reconoce explícitamente que el despliegue real no se verificó. Estas afirmaciones documentales NO sustituyen una ejecución repetida sobre este ZIP actualizado y sus migraciones V5 y V6.

## 3. Hallazgos prioritarios

### AB-AUD-01 — ALTA — Costo interno visible desde catálogo público
**Evidencia:**
- `backend/src/main/java/com/alexabeauty/catalog/dto/ProductVariantResponse.java:9-25, 27-45`: el DTO serializa `referenceCost`, `stockOnHand`, `stockReserved` y `minStock`.
- `backend/src/main/java/com/alexabeauty/catalog/service/CatalogServiceImpl.java:533-537, 609-625`: tanto detalles como resúmenes públicos usan `ProductVariantResponse::from`.
- `backend/src/main/java/com/alexabeauty/catalog/controller/PublicCatalogController.java:38-63`: estos endpoints se publican sin login.
- `frontend/src/lib/api.ts:8-21` admite esos campos en datos del cliente.

**Impacto:** un visitante que inspeccione la respuesta HTTP de `/api/v1/catalog/products` o `/api/v1/catalog/products/{slug}` podría conocer los costos de referencia y las unidades físicas/reservadas, aunque la pantalla no los muestre. Esto revela información comercial competitiva.

**Corrección requerida:** crear `PublicProductVariantResponse` solo con ID, SKU visible si aplica, código/nombre/HEX del tono, presentación, precio, stock disponible (o disponibilidad simplificada) y estado; usar DTO administrativo independiente con costos y valores internos. Hacer prueba negativa que busque ausencia de claves `referenceCost`, `stockOnHand`, `stockReserved`, `minStock` en **ambos** endpoints públicos, y prueba positiva de disponibilidad en admin. Preferir no entregar `storagePath` interno en imágenes públicas.

### AB-AUD-02 — ALTA — Importación Excel no agrupa tonos del mismo producto
**Evidencia:**
- `backend/src/main/java/com/alexabeauty/reporting/excel/ExcelProductTemplateGenerator.java:30-46`: plantilla `ALEXA_PRODUCTS_V1` solo lleva `tono` genérico, no `producto_codigo`, `tono_codigo` ni `tono_hex`.
- `backend/src/main/java/com/alexabeauty/reporting/service/ExcelImportServiceImpl.java:484-568`: por **cada fila válida** crea un `Product` y un único `ProductVariant` con el mismo SKU; no agrupa ni enlaza varias filas a la misma ficha.
- `frontend/src/app/admin/importar/page.tsx:344-365`: anuncia fichas de productos y variantes pero sigue los contadores V1.

**Impacto:** importar cuatro tonos de una base puede crear cuatro fichas públicas independientes, mientras la creación manual sí admite un único producto con varios tonos. Duplicados comerciales, catálogo fragmentado, informes inconsistentes y operaciones administrativas más complejas.

**Corrección requerida:** Excel V2 explícito y con descarga oficial: `producto_codigo` estable, nombre, marca, categoría, `sku_variante`, `tono_codigo`, `tono_nombre`, `tono_hex` opcional, presentación, precio, costo, stock inicial, stock mínimo, publicado, descripción. Previsualizar agrupación (productos/variantes/errores), validar combinaciones y SKU; no tocar Excel V1 silenciosamente; migrar a importación compatible o rechazar versiones con mensaje claro. Nunca sumar stock dos veces por carga repetida. Probar un archivo con **1 producto + 4 tonos = 1 ficha + 4 variantes + 4 movimientos iniciales**.

### AB-AUD-03 — ALTA — Duplicación de número de recepción puede aceptar contenido diferente
**Evidencia:** `backend/src/main/java/com/alexabeauty/inventory/service/InventoryServiceImpl.java:123-142`.

El servicio comprueba el fingerprint cuando se repite `Idempotency-Key`; sin embargo, si encuentra un `receiptNumber` existente, devuelve directamente la recepción previa (`StockReceiptResponse.from(...)`) **sin comparar** huella, proveedor, variantes ni cantidades del nuevo envío.

**Reproducción conceptual, no ejecutada:** crear recepción `REC-123` con tono A +5; enviar otra solicitud con `receiptNumber=REC-123` y tono B +12, con distinta clave de idempotencia o sin ella. La segunda solicitud podría devolver una respuesta aparentemente correcta para la recepción antigua sin incorporar B ni indicar conflicto.

**Corrección requerida:** si se reutiliza número de recepción, comparar un fingerprint canónico de la solicitud; misma solicitud → replay idempotente; distinta solicitud → `409 Conflict`. Manejar carreras sobre restricciones únicas con resultado controlado. Test unitario y de integración de ambos casos, incluyendo solicitudes concurrentes. Verificar que ningún inventario cambie ante 409.

### AB-AUD-04 — MEDIA — Búsqueda anuncia tonos pero no los consulta
**Evidencia:** `frontend/src/app/(store)/catalogo/page.tsx:55` dice “nombre, marca, tono”; `CatalogServiceImpl.java:308-315` filtra solo nombre de producto, descripción y SKU de ficha (no `shadeName`, `shadeCode` ni SKU de variante).

**Corrección requerida:** agregar subconsulta/EXISTS de variantes activas para `shadeCode`, `shadeName`, `sku`, más marca si corresponde; `SELECT` paginado sin duplicar productos. Prueba de búsqueda por `110`, `Classic Ivory`, SKU de variante y nombre de marca.

### AB-AUD-05 — MEDIA — Se aceptan variantes sin identificación comercial
**Evidencia:** `ProductVariantRequest.java:10-17,31-34` deja opcionales `shadeCode`, `shadeName` y `presentation`; `VariantRules.java:25-38` construye clave `|` si todos son vacíos; `VariantManagementService.java:42-57` permite crear tal variante.

**Impacto:** opciones ambiguas con etiqueta genérica y precios/stock distintos sin poder distinguirlas.

**Corrección requerida:** al crear una variante adicional, exigir al menos `shadeCode` o `shadeName` o `presentation` no vacío; validar duplicado normalizado en backend y formularios. Permitir únicamente la variante por defecto cuando el producto realmente no tiene variaciones.

### AB-AUD-06 — MEDIA — Se pueden seleccionar tonos agotados
**Evidencia:** `frontend/src/components/catalog/ShadeSelector.tsx:20-34` aplica estilo tachado a opciones agotadas pero sus botones siguen teniendo `onClick`; carrito y checkout sí bloquean compras sin stock.

**Corrección requerida:** según UX acordada, mostrar tono agotado y estado “Agotado”, pero impedir su selección o permitir solo inspección de ficha diferenciada de compra. Si se desactiva botón, incluir información accesible y leyenda. Test UI de teclado/táctil y cantidad.

### AB-AUD-07 — MEDIA — Promesa de WhatsApp no corresponde a un envío automático
**Evidencia:** `frontend/src/app/(store)/pedido/page.tsx:196-212` dice “A este número enviaremos los datos de seguimiento”; el checkout usa `wa.me`/click-to-chat asistido y en la línea 100 abre ventana tras completar una solicitud asíncrona.

**Impacto:** el cliente puede esperar un mensaje automático que no se envía; además algunos navegadores móviles bloquean `window.open` cuando ocurre después del `await`.

**Corrección requerida:** declarar “Nos comunicaremos por WhatsApp para coordinar” sin prometer automatización inexistente; mantener CTA visible de “Enviar pedido por WhatsApp” en pantalla de confirmación; usar navegación iniciada por el usuario con fallback y no depender del popup automático. Para mensajes automáticos futuros, integrar oficialmente la Cloud API y sus plantillas/consentimientos.

### AB-AUD-08 — MEDIA — Protección de solicitudes públicas es global por instancia
**Evidencia:** `backend/src/main/java/com/alexabeauty/security/PublicWriteRateLimitFilter.java:12-46` cuenta logins y checkouts totales por minuto en memoria local (no por IP/cliente/usuario) y la documentación reconoce que falta límite distribuido de borde.

**Impacto:** un actor puede consumir la cuota de todos los compradores; en varias réplicas no hay una cuota coherente. **No constituye por sí solo un bypass de autenticación**.

**Corrección requerida:** aplicar controles por cliente en un proxy confiable (Render/CDN/edge) y considerar rate limit distribuido; mantener un presupuesto global de emergencia sin bloquear clientes legítimos. Probar abuso de checkout, reintentos e idempotencia.

### AB-AUD-09 — MEDIA — Las comprobaciones CI no ejecutan todo el conjunto
**Evidencia:** `.github/workflows/ci-frontend.yml:15-39` ejecuta `npm ci` + `npm run build`, pero no `npm test` ni tests de interacción; `ci-backend.yml:15-32` ejecuta `mvn clean test`, sin levantar PostgreSQL real. 

**Corrección requerida:** incluir pruebas de Node, lint real, build, análisis de dependencias, PostgreSQL efímero (Testcontainers o servicio de Actions), migración V1→V6 y E2E Playwright con viewport móvil. Publicar artefactos de evidencia y bloquear merge sin CI verde.

### AB-AUD-10 — MEDIA — Persistencia de fotos y variantes tiene una ruta de recuperación incompleta
**Evidencia:** `MediaServiceImpl.java:69-99`: se sube el archivo a Supabase antes de persistir la fila de BD; si el guardado falla puede quedar un archivo huérfano. `MediaServiceImpl.java:101-123` borra storage antes de BD y podría dejar inconsistencias si falla la transacción posterior.

**Corrección requerida:** compensación explícita si falla el registro, limpieza programada de objetos huérfanos y pruebas de fallos inyectados. No presupone una falla hoy, identifica una ventana de inconsistencia en servicios externos no transaccionales.

### AB-AUD-11 — BAJA/MEJORA — SVG/imagen de tonos y leyenda visual
**Evidencia:** `ShadeSelector.tsx:26-37`, `VariantFields.tsx:16-21`. El diseño actual sí tiene muestra HEX, código y nombre. No hay prueba en navegador de contraste, desbordamiento o comparación con la referencia exacta. El componente no muestra imagen miniatura del tono como parte de cada opción; fotos por variante se ven en galería.

**Corrección requerida:** UX mobile-first con swatches amplios y etiquetas completas, estados accesibles, colores opcionales y aviso de aproximación, sin inventar HEX; snapshot/capturas a 360, 390, 768 y 1440 px.

### AB-AUD-12 — BAJA/MEJORA — SKU automático no sigue una convención legible de negocio
**Evidencia:** `VariantRules.java:18-23` genera `ABV-<UUID sin guiones>` si falta SKU. Es único de forma muy probable pero no recoge marca/categoría/tono, a diferencia de la convención humana deseada.

**Corrección requerida:** mantener ID inmutable y generar SKU legible con contador/clave robusta o usar el código actual como identificador interno y un código comercial aparte; nunca renombrar SKU históricos automáticamente.

### AB-AUD-13 — BAJA/MEJORA — Branding público pesado y optimización aún incompleta
**Evidencia:** `frontend/public/branding/logo.png` pesa aprox. 1,7 MB. `ProductCard.tsx:112-117` y la ficha `producto/[slug]/page.tsx:115-117` utilizan `<img>` sin un esquema homogéneo de optimización y tamaños responsive.

**Corrección requerida:** generar WebP/AVIF para branding donde corresponda, usar `next/image` con `sizes` razonable y estrategia de dominio remota para Supabase; medir Core Web Vitals en 4G/móvil real. No alterar calidad original sin revisión de marca.

### AB-AUD-14 — BAJA/MEJORA — Documentación y estado de lanzamiento
**Evidencia:** `README.md` prescribe Playwright pero no hay una suite E2E localizada en `frontend`; `documentation/PRODUCTION_READINESS_2026-10-13.md` reconoce que no hay evidencia del entorno real y se refiere a V1-V4, mientras el ZIP ya contiene V5-V6.

**Corrección requerida:** actualizar matriz de pruebas con versión, fecha, comando, resultado y plataforma; no marcar PASS si no se ejecutó. Agregar checklist de variables de entorno, permisos Storage, backup/restore, rollback, CORS, cookies Secure y WhatsApp real.

## 4. Funcionalidades con implementación encontrada (no equivalen a PASS E2E)

| Módulo | En el código | Acción |
|---|---|---|
| Catálogo público / fichas | Sí | Corregir DTO público, búsqueda y mejorar rendimiento móvil. |
| Swatches HEX + código/nombre de tono | Sí | Afinar agotados, accesibilidad y experiencia móvil. |
| Fotos por variante | Sí | Validar extremo a extremo con Storage. |
| Admin de tonos: alta/edición/estado | Sí | Restringir variantes vacías, verificar errores y validación. |
| SKUs automáticos | Sí | Mejorar legibilidad si el negocio lo requiere. |
| Stock físico y reservado | Sí | Conservar modelo transaccional y revalidar en PG real. |
| Carrito/checkout | Sí | Verificar móvil, popup WhatsApp e idempotencia. |
| Confirmación manual de pago | Sí | Prueba real de carrera y vencimiento en staging. |
| Recepción mercancía | Sí | Reparar reuso conflictivo de número. |
| Excel V1 | Sí | Diseñar V2 agrupada por producto, sin destruir V1. |
| Reportes/ventas filtradas | Sí | Conciliar BD, pantallas y XLSX para variantes. |
| Seguridad cookie, origen, JWT | Sí | Eliminar filtración de costos y hacer pruebas externas. |
| Deploy Vercel/Render/Supabase | Configurado en código | No verificado en servicios reales. |

## 5. Plan de corrección ordenado para Codex

**Fase 0 — Contención (bloqueante):** rama `fix/auditoria-alexa-20261015` sin push/merge/deploy no autorizado; añadir pruebas negativas del DTO público; corregir filtración de `referenceCost`; impedir replay diferente de `receiptNumber`; verificar ausencia de modificación de stock. Ejecutar unit/integration con BD aislada.

**Fase 1 — Variantes consistentes:** corregir validación de variante vacía, búsqueda por tono/código/SKU, selector agotado, galería e imágenes propias; pruebas frontend accesibles + integración JUnit PostgreSQL. No cambiar SKUs ni movimientos históricos.

**Fase 2 — Excel V2, bloque importante:** diseño de contrato oficial + plantilla generada por servidor, preview agrupada, reporte de filas fallidas, validación de duplicados, importación atómica, rechazo de reimportación; pruebas de varias variantes con stock, importación repetida y reversión.

**Fase 3 — Operación y calidad móvil:** checkout con CTA manual de WhatsApp confiable, copy correcto, optimización de fotos, pruebas de carga/CPU en móvil, control de rate limit de borde, alertas, CI con PG y E2E. Recorrido completo en HTTPS staging con Supabase Storage.

**Fase 4 — Decisión de lanzamiento:** evidencia real de Vercel/Render/Supabase, restore y rollback probados, 4 flujos comerciales completos, claves segregadas, sin datos demo, WhatsApp correcto, aprobación explícita del administrador del negocio antes de publicar.

## 6. Matriz de aceptación obligatoria

1. GET público catálogo/detalle: **NO aparece** `referenceCost`, `stockReserved` ni stock físico interno; solo disponibilidad comercial segura.
2. Admin: costos, inventario y reservas siguen disponibles bajo rol apropiado.
3. Alta de base con tonos 107, 110 y 115: una ficha, tres variantes, tres SKU únicos; 115 agotado visible pero no comprable.
4. Búsquedas por `107`, `Classic Ivory`, SKU de variante y marca devuelven ficha correcta sin duplicados ni inflación del contador.
5. Variante sin código, nombre ni presentación se rechaza con mensaje explícito si es adicional.
6. Excel V2: 1 producto/4 tonos importados crea 1 producto/4 variantes con movimientos y stock exactos; preview anticipa agrupación.
7. Archivo Excel duplicado: no crea SKU, fichas ni movimientos nuevos; error comprensible.
8. Recepción número `REC-123` idéntica: replay seguro; `REC-123` con cantidades/tonos diferentes: 409 y stock intacto.
9. Dos compras de última unidad: no sobreventa; cancelación y vencimiento no descuenta stock físico; pago verificado descuenta una sola vez.
10. Doble confirmación concurrente de pago: un pago + un conjunto único de movimientos.
11. Foto de tono A no aparece como foto exclusiva del tono B; fallback general sin filtraciones.
12. Checkout móvil hace disponible enlace manual de WhatsApp aun si popup está bloqueado y dice claramente que la clienta debe pulsar enviar.
13. Mobile 360, 390, 768 px: sin desbordamientos; selector táctil y navegación por teclado accesibles.
14. Reporte XLSX: cada venta distingue SKU/tono; totales conciliados con pagos realmente confirmados.
15. Migración V4→V5→V6 probada sobre copia de datos realista con backup y rollback documentado; nunca editar V1-V6 aplicadas.
16. CI verde con Next build/TS, tests frontend, JUnit, PostgreSQL y E2E mínimo; staging HTTPS probado.

## 7. Riesgos no resueltos por esta auditoría

No se determinó si el despliegue web usa exactamente este ZIP, si existen clientes/pedidos/productos reales, ni el estado de V5/V6 en Supabase. **Antes de migrar datos existentes hay que hacer copia de seguridad, inspección de integridad, conteos y simulación en staging**. No se comprobó accesibilidad WCAG real, rendimiento Lighthouse, escaneo de dependencias, secretos del entorno desplegado ni end-to-end con WhatsApp real.

## 8. Conclusión

La implementación del selector visual de tonos **sí está presente y aprovecha bien el modelo de variantes**. El sistema tiene una estructura sensata para continuar mejorando. Las tres correcciones prioritarias son: **no exponer costos al público**, **agrupar tonos en Excel** y **rechazar reutilizaciones contradictorias de recepciones de mercancía**. Solo después debe darse prioridad al pulido visual y al lanzamiento.
