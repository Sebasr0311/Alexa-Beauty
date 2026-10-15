# INSTRUCCIONES PARA CODEX — CORRECCIÓN AUDITORÍA ALEXA BEAUTY

Trabaja sobre el repositorio EXISTENTE `Sebasr0311/Alexa-Beauty`. Recibirás `AUDITORIA_TECNICA_ALEXA_BEAUTY.md` como fuente de hallazgos de revisión estática. **Confirma cada defecto ejecutando pruebas sobre el código actual antes de modificarlo.** Los hallazgos basados en estática son hipótesis sustentadas, no resultados de pruebas E2E.

## Protocolo previo
1. Verifica `git status`, rama, commit y si hay archivos del usuario sin confirmar. No sobrescribas ni borres trabajo no confirmado.
2. Crea rama `fix/auditoria-alexa-20261015` a partir de la base apropiada, sin push/merge/deploy automáticos.
3. Ejecuta baseline real: `cd frontend && npm ci && npm test && npm run build`; `cd backend && mvn test`; con PostgreSQL aislado repetir migraciones y pruebas de transacción. No inventes PASS ante bloqueos.
4. Registra estado inicial en `AUDITORIA_IMPLEMENTACION.md`, con hallazgos, evidencia reproducible y tareas.

## Orden estricto

**P0 seguridad/confianza**: corregir `ProductVariantResponse` compartido entre respuestas públicas y privadas. La API pública NO debe serializar `referenceCost`, `stockOnHand`, `stockReserved`, `minStock`; usa DTO público pequeño y DTO admin completo. Implementa tests `MockMvc` de catálogo paginado y detalle, sin autenticar y autenticados; verifica bytes JSON, no solo tipado TS. Corregir idempotencia de `InventoryServiceImpl.createReceipt` para que el mismo `receiptNumber` con diferente payload devuelva HTTP 409 y deje stock intacto. Guardar fingerprint canónico de solicitud; replay idéntico seguro; carreras concurrentes controladas.

**P1 variantes**: validar al menos un identificador no vacío entre código/nombre/presentación en cualquier variante adicional; mantener default para productos simples. Buscar por código/nombre/SKU de variante en catálogo, usando EXISTS/sin duplicar productos. Ajustar `ShadeSelector` según referencia original: color HEX opcional, código/nombre, selected/in-stock/agotado, sin elegir variante agotada para compra, estados accesibles. Conservar snapshots de órdenes, imágenes por variante, SKU y existencias.

**P1 Excel V2**: ampliar plantilla (mantener compatibilidad clara con V1) incluyendo `producto_codigo`, `sku_variante`, `tono_codigo`, `tono_nombre`, `tono_hex`, presentación, precio, marca/categoría, stock inicial/mínimo. Implementar preview agrupada por producto y atómica; registrar movimientos una vez; no sumar stock por reimportación ni modificar históricos. Crear tests de 1 producto y 4 tonos, 500 filas, errores por fila y archivos duplicados. No usar la importación V1 actual para agrupar por coincidencia solo de texto sin identificador estable.

**P2 experiencia/operación**: checkout WhatsApp con botón explícito de “Enviar pedido” que funcione incluso con bloqueo de popups; corregir promesas de mensajería automática; pruebas móviles de 360/390/768 px. Optimizar imágenes. Incorporar `npm test`, lint/TS, JUnit sobre PostgreSQL y E2E Playwright a CI; vigilancia edge de rate limits y objetos huérfanos en Storage.

## Reglas
- No editar migraciones Flyway existentes V1–V6; crear V7+ si es imprescindible. Hacer simulación de upgrade y rollback en staging con snapshot.
- Nunca actualizar en bloque SKUs existentes ni saldos/costos, ni reescribir pedidos históricos.
- Evita usar los servicios de producción y sus claves; no ejecutes scripts destructivos.
- Reserva y pago deben mantener `0 <= stockReserved <= stockOnHand` y un solo movimiento VENTA por unidad confirmada.
- Usa 100% datos de API para la tienda; no introducir fixtures demo en vistas reales.
- Adopta pruebas *RED -> GREEN*, complementa con casos límite/concurrencia y captura resultados.
- No atribuyas a MCP ni a WhatsApp Cloud API capacidades que no están implementadas: wa.me es asistido.
- Presenta commits por fase y PR revisable (solo si el usuario autoriza push), informe de archivos tocados, tests exactos, antes/después, capturas responsivas y pendientes.
- La condición para cerrar es que cada criterio de aceptación del informe pase con evidencia; si faltan credenciales, Docker, red, navegadores o datos, marca `NO VERIFICADO` y explica cómo probarlo en staging.

**Resultado esperado:** cambios reales, probados y preservando la arquitectura existente, NO una simple lista de sugerencias. Espera autorización antes de merge o deploy a producción.
