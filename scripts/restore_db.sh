#!/usr/bin/env bash
# ==============================================================================
# Script de Restauración para Base de Datos PostgreSQL (Staging / Recuperación)
# Proyecto: Alexa Beauty
# Uso: ./scripts/restore_db.sh <ruta_al_archivo_backup.sql.gz>
# ==============================================================================

set -euo pipefail

if [ "$#" -lt 1 ]; then
  echo "Uso: $0 <archivo_backup.sql.gz>"
  echo "Ejemplo: $0 ./backups/alexa_beauty_backup_20261012_120000.sql.gz"
  exit 1
fi

BACKUP_FILE="$1"

if [ ! -f "$BACKUP_FILE" ]; then
  echo "Error: El archivo de respaldo '${BACKUP_FILE}' no existe."
  exit 1
fi

if [ -z "${DATABASE_URL:-}" ]; then
  echo "Error: La variable DATABASE_URL no está definida."
  echo "Ejemplo: export DATABASE_URL='postgresql://postgres:[PASSWORD]@[HOST]:5432/postgres'"
  exit 1
fi

echo "=================================================="
echo "ADVERTENCIA: Esta operación sobreescribirá la base de datos destino."
echo "Destino: ${DATABASE_URL}"
echo "Origen:  ${BACKUP_FILE}"
echo "=================================================="
read -p "¿Está seguro de continuar? (escriba 'SI' en mayúsculas): " CONFIRM

if [ "$CONFIRM" != "SI" ]; then
  echo "Operación cancelada por el usuario."
  exit 0
fi

echo "Descomprimiendo y restaurando base de datos..."
gunzip -c "$BACKUP_FILE" | psql "$DATABASE_URL" -v ON_ERROR_STOP=1

echo "=================================================="
echo "Restauración completada con éxito."
echo "Ejecute las pruebas de verificación para confirmar integridad."
echo "=================================================="
