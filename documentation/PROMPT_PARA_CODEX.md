# PROMPT MAESTRO PARA CODEX — ALEXA BEAUTY

Copia y pega este texto a Codex **junto con todo este paquete ZIP**. El plan técnico completo está en `documentacion/PLAN_MAESTRO_ALEXA_BEAUTY.md` y es la fuente de verdad inicial.

---

Actúa como arquitecto de software senior, diseñador UX/UI mobile-first, ingeniero Java/Spring Boot, experto en PostgreSQL, Next.js, pruebas y DevOps. Tu objetivo es **IMPLEMENTAR y VERIFICAR**, no únicamente generar pantallas, el sistema «Alexa Beauty», tienda de cosméticos con inventario, pedidos asistidos por WhatsApp Business y panel de ventas. Todo el comportamiento debe corresponder fielmente a `PLAN_MAESTRO_ALEXA_BEAUTY.md`. Incluye las imágenes de marca suministradas en `branding/`; el logo refinado es una **propuesta** y debe poder reemplazarse fácilmente tras su aprobación.

## Instrucciones no negociables

1. Inspecciona los archivos suministrados y resume requisitos confirmados, decisiones pendientes, arquitectura, dependencias y riesgos. **Antes de crear código**, genera un `TODO_IMPLEMENTACION.md` con épicas, tareas atómicas, dependencias, pruebas y estados; un `DECISIONES.md` con supuestos reversibles y un `README.md` de instalación.
2. Si el repositorio ya contiene archivos o historial, **NO los borres ni sobreescribas ciegamente**. Inspecciona `git status`, estructura, tests, entornos, versiones y migraciones. Respeta cambios existentes. No ejecutar operaciones destructivas sin autorización explícita.
3. Implementa en el stack: Java 21 LTS y Spring Boot estable compatible, PostgreSQL Supabase, Supabase Storage, Next.js + TypeScript en Vercel y API en Render. Usa arquitectura modular por función, DTOs, servicios transaccionales, repositorios, validaciones, errores consistentes, SQL Flyway, OpenAPI y configuración por entorno. Mantén toda credencial de servicio en backend.
4. **NO simules integración, ventas, pagos, stock ni reportes en producción.** Si algo depende de cuentas o secretos que aún no existen, construye su adaptador testeable, deja instrucciones exactas para configurar el servicio y marca claramente `PENDIENTE DE CONFIGURACIÓN EXTERNA`, sin falsos «completado».
5. Implementa mobile-first pensando primero en 360–430 px. Inspírate en los colores reales del logo: vino oscuro, malva y crema. Usa diseño de boutique de cosméticos con un tono cercano, tipografía elegante, fotografías 1:1, CTA claros, estados vacíos y accesibilidad WCAG 2.2 AA. Evita dashboards genéricos que no se puedan usar en móvil.
6. La **lógica de inventario y pagos** es de máximo riesgo: `available = on_hand - reserved`; reservas por checkout transaccional, confirmación manual de pago y consumo del stock sólo una vez, liberación de pedidos expirados/cancelados, ledger auditado, idempotencia y concurrencia con bloqueo PostgreSQL. No implementar simples restas de stock desde frontend.
7. El pedido debe persistir con código único y totales calculados en servidor **antes** de abrir WhatsApp. `wa.me` sólo prellena el mensaje, no certifica envío ni pago. Construye la interfaz `WhatsAppGateway` con `ClickToChatGateway` MVP, y documenta un `CloudApiGateway` futuro sujeto a credenciales/Meta Business. No afirmar que usar un MCP produce envío automático sin proveedor/autorización verificable.
8. Importación XLSX: el sistema genera `ALEXA_PRODUCTS_V1` y descarga plantilla descargable con fila 1 exacta; valida filas, muestra preview y errores, detecta SKU repetido, exige confirmación y registra auditoría e idempotencia; crea movimientos de stock inicial. El Excel entregado en `plantillas/` es un **contrato de referencia inicial**, no sustituye al generador de backend. Altas individuales funcionan sin Excel. Recepciones de mercancía son proceso distinto: suman a SKU ya existente, jamás recrean producto.
9. Reportes: filtrar **ventas con pago confirmado** por fecha real de verificación en `America/Bogota`, otros filtros y exportar XLSX profesional con `RESUMEN`, `VENTAS`, `DETALLE_PRODUCTOS`, `TOP_PRODUCTOS`, `METADATOS`. Los totales deben cuadrar desde la BD al Excel y el gráfico. No computar pendientes como ventas.
10. Protección: autorización por rol en API, hash de password, sesiones/cookies seguras, CSRF adecuado, CORS limitado, XSS/IDOR/SQL injection/file upload controls, auditoría, backups, rate limiting, logs sin secretos, errores amigables. Diseña una recuperación de cuenta segura. Nunca usar `ddl-auto=update` en producción.
11. Crea y ejecuta pruebas JUnit/Mockito, tests integrados Testcontainers PostgreSQL, API, servicios críticos y E2E Playwright móvil; **incluye concurrencia**: dos clientes comprando la última unidad y doble confirmación de pago. Donde no haya Docker/test credentials, indica no ejecutado y por qué, no inventes evidencias.
12. Configura CI, Dockerfile backend, despliegues Vercel/Render, variables `.env.example`, instrucciones Supabase PostgreSQL/Storage, conexión TLS y cron/worker fiable para expiraciones. Distingue dev/staging/prod. Agrega runbook para incidencias y pruebas de restauración.
13. No introduzcas características extra, complejidad de microservicios ni costos innecesarios sin justificar. Mantén el MVP enfocado pero el modelo extensible para variantes de tono, múltiples fotos y futura automatización/pasarelas.

## Método de ejecución exigido

**Primero:** muestra un plan de las 12 fases del documento y mapea tareas a archivos/clases concretos y criterios de aceptación. Anota cualquier discrepancia. Pregunta sólo por bloqueos que realmente impidan avanzar; para asuntos no bloqueantes fija supuestos explícitos y reversibles.

**Después:** implementa en iteraciones pequeñas, de preferencia una fase por PR o grupo de commits. Para cada tarea registra:

- Objetivo y requisito asociado.
- Archivos creados/editados (rutas exactas).
- SQL/migraciones y efectos sobre stock.
- Comandos de construcción/test ejecutados y resultados **reales**.
- Prueba manual en móvil cuando corresponda.
- Riesgos, pendientes y cómo revertir.

**Al finalizar cada fase:** compila, ejecuta los tests pertinentes, confirma que no hay cambios accidentales, actualiza `TODO_IMPLEMENTACION.md`, documentación técnica, `CHANGELOG.md` y una tabla de cumplimiento por criterio. Nunca digas 100% sin verificación. Si detectas un bug anterior, corrígelo antes de seguir si afecta seguridad, datos o compilación.

**Al finalizar el proyecto:** entrega un informe `INFORME_FINAL_VERIFICACION.md` con: matriz de 26 pruebas P01–P26 del plan, estado PASS/FAIL/NO_EJECUTADO y pruebas concretas, estado del despliegue, instrucciones del admin, auditoría de seguridad, pruebas de reservas/pagos con transacciones simultáneas, control de backup y restauración, enlaces y variables por configurar, puntos pendientes y evidencia de no falsear ventas.

## Prioridad de comienzo

Comienza con auditoría y scaffold, migraciones/roles, productos y Storage; después inventario transaccional y Excel; luego catálogo, checkout/WhatsApp, verificación de pagos; finalmente ventas, exportación, analíticas, pruebas/deploy. Puedes preparar el diseño en paralelo, sin simular datos reales.

## Criterio final de éxito

Un administrador carga un Excel de 100 productos, añade imágenes a algunos, recibe 10 unidades más de una variante, un cliente móvil realiza un pedido por WhatsApp, el administrador verifica el pago exactamente una vez, el inventario registra cada movimiento, las ventas y el Top Productos se actualizan y un Excel filtrado cuadra con la base de datos. La solución resiste pagos repetidos, intentos de sobreventa, pedidos caducados y usuarios no autorizados. El proyecto está desplegado, documentado y testeado hasta donde lo permiten las cuentas realmente proporcionadas.
