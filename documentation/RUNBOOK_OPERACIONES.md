# RUNBOOK DE OPERACIONES Y CONTINGENCIA — ALEXA BEAUTY

Este documento detalla los procedimientos operativos estándar (SOP) para monitoreo, respuesta a incidentes, respaldos, rotación de credenciales y contingencias de la plataforma **Alexa Beauty**.

---

## 1. Monitoreo y Diagnóstico Rápido

### 1.1. Health Check y Estado del Backend
El backend expone un endpoint público de verificación de salud que no revela credenciales internas:
```bash
# Diagnóstico de disponibilidad
curl -i https://alexa-beauty-api.onrender.com/api/v1/health/public
```
**Respuesta esperada:**
```json
{
  "status": "UP",
  "appName": "Alexa Beauty API",
  "version": "1.0.0",
  "timestamp": "2026-10-12T20:00:00Z"
}
```

### 1.2. Invariantes de Inventario a Inspeccionar Regularmente
En PostgreSQL, ejecuta la siguiente consulta para verificar que no existan inconsistencias físicas ni sobreventa:
```sql
-- 1. Verificar que el stock físico sea siempre mayor o igual al reservado y nunca negativo
SELECT id, sku, shade_name, stock_on_hand, stock_reserved
FROM product_variants
WHERE stock_on_hand < 0 
   OR stock_reserved < 0 
   OR stock_reserved > stock_on_hand;
-- RESULTADO ESPERADO: 0 FILAS

-- 2. Verificar que no existan reservas 'ACTIVE' en pedidos ya pagados o cancelados
SELECT sr.id, sr.order_id, sr.status, o.status as order_status
FROM stock_reservations sr
JOIN orders o ON sr.order_id = o.id
WHERE sr.status = 'ACTIVE' 
  AND o.status IN ('PAGADO', 'PREPARANDO', 'DESPACHADO', 'ENTREGADO', 'CANCELADO', 'EXPIRADO');
-- RESULTADO ESPERADO: 0 FILAS
```

---

## 2. Protocolo de Respaldo y Restauración de Datos

### 2.1. Política de Copias de Seguridad
- **Frecuencia:** Diaria a las 02:00 AM (hora Colombia).
- **Destino:** Repositorio externo cifrado / Bucket seguro fuera del mismo proyecto de Supabase.
- **RPO (Recovery Point Objective):** Máximo 24 horas.
- **RTO (Recovery Time Objective):** Menor a 30 minutos.

### 2.2. Ejecución Manual de Respaldo
**En entornos Linux / macOS:**
```bash
export DATABASE_URL="postgresql://postgres:[PASSWORD]@[HOST]:5432/postgres"
./scripts/backup_db.sh ./backups
```

**En Windows (PowerShell):**
```powershell
$env:DATABASE_URL = "postgresql://postgres:[PASSWORD]@[HOST]:5432/postgres"
.\scripts\backup_db.ps1 -DestinationDir .\backups
```

### 2.3. Prueba de Restauración (Disaster Recovery)
Para restaurar el respaldo en un entorno de staging o recuperación:
```bash
./scripts/restore_db.sh ./backups/alexa_beauty_backup_YYYYMMDD_HHMMSS.sql.gz
```

---

## 3. Matriz de Incidentes y Planes de Acción

### Incidente 1: Backend Suspendido o Sin Respuesta (Cold Start / Caída)
- **Síntomas:** Frontend muestra error de red al consultar el catálogo o crear pedidos.
- **Causa común:** El plan gratuito de Render suspende el contenedor por inactividad tras 15 minutos.
- **Acción inmediata:**
  1. Ingresar a Render Dashboard y verificar el estado del servicio `alexa-beauty-api`.
  2. Forzar un reinicio manual con **Manual Deploy > Deploy latest commit**.
  3. Comprobar los logs para descartar fallos de conexión hacia Supabase PostgreSQL (`Connection refused` o timeout SSL).
  4. **Solución permanente:** Migrar al plan `Starter` ($7 USD/mes) para ejecución ininterrumpida del servidor y del cron de expiración.

### Incidente 2: Reservas Bloqueadas tras un Reinicio del Backend (Matriz P25)
- **Síntomas:** Productos aparecen con "Agotado" o stock disponible bajo, a pesar de que los clientes no pagaron en WhatsApp.
- **Causa:** El backend estuvo inactivo durante el período de expiración de 45 minutos.
- **Recuperación automática:** Al reiniciar, el `ReservationExpirationJob` se ejecuta de inmediato (dentro del primer minuto) y procesa todas las órdenes con `expires_at < NOW()`, liberando el stock reservado de forma atómica.
- **Recuperación manual (si el job fallase):**
  Ejecutar el script SQL de reconciliación:
  ```sql
  BEGIN;

  -- 1. Bloquear variantes afectadas para evitar carreras concurrentes
  SELECT id FROM product_variants
  WHERE id IN (
    SELECT DISTINCT variant_id FROM stock_reservations
    WHERE status = 'ACTIVE' AND expires_at < NOW()
  )
  FOR UPDATE;

  -- 2. Restar la suma total agrupada de reservas vencidas por variante
  WITH expired_aggregates AS (
    SELECT variant_id, SUM(quantity) AS total_expired_qty
    FROM stock_reservations
    WHERE status = 'ACTIVE' AND expires_at < NOW()
    GROUP BY variant_id
  )
  UPDATE product_variants pv
  SET stock_reserved = GREATEST(0, pv.stock_reserved - ea.total_expired_qty)
  FROM expired_aggregates ea
  WHERE pv.id = ea.variant_id;

  -- 3. Marcar reservas como liberadas
  UPDATE stock_reservations
  SET status = 'RELEASED', updated_at = NOW()
  WHERE status = 'ACTIVE' AND expires_at < NOW();

  -- 4. Expirar las órdenes asociadas
  UPDATE orders
  SET status = 'EXPIRADO', updated_at = NOW()
  WHERE status IN ('PENDIENTE_CONTACTO', 'PENDIENTE_PAGO') AND expires_at < NOW();

  COMMIT;
  ```

### Incidente 3: Caída de la Red de WhatsApp (Matriz P24)
- **Síntomas:** El cliente final no puede abrir el enlace `wa.me` tras confirmar el checkout.
- **Procedimiento de contingencia:**
  1. La pantalla `/pedido/confirmado/[reference]` cuenta con el botón de respaldo **"Copiar mensaje de pedido"**.
  2. El cliente copia el texto formateado al portapapeles y puede enviarlo por mensaje directo de Instagram, SMS o correo de soporte.
  3. El operador busca el pedido en `/admin/pedidos` por su referencia `AB-YYYY-NNNNNN` para atenderlo.

### Incidente 4: Error en Importación Masiva de Excel
- **Síntomas:** Al importar un archivo Excel, el preview muestra filas rechazadas o la importación se detiene.
- **Procedimiento:**
  1. Descargar el reporte de inconsistencias en `/api/v1/admin/products/imports/{jobId}/rejected.xlsx`.
  2. La columna adicional `ERROR_MOTIVO` explica exactamente qué celda falló (ej. stock negativo, SKU duplicado, precio inválido).
  3. El administrador corrige únicamente las filas rechazadas y vuelve a subir el archivo.
  4. Gracias a la idempotencia por SHA-256, los productos ya existentes no duplicarán stock físico.

---

## 4. Procedimiento de Rotación de Secretos

1. **Rotación de JWT_SECRET:**
   - Generar un nuevo token de 256 bits: `openssl rand -base64 64`.
   - Actualizar la variable en Render.
   - El servicio reiniciará; las sesiones anteriores caducarán y los administradores deberán iniciar sesión nuevamente.

2. **Rotación de Claves de Supabase:**
   - En Supabase > **API Settings**, regenerar la clave `service_role`.
   - Actualizar inmediatamente `SUPABASE_SERVICE_ROLE_KEY` en Render.
