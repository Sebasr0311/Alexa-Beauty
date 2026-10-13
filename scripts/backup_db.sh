#!/usr/bin/env bash
# ==============================================================================
# Script de Backup para Base de Datos PostgreSQL (Supabase / Producción)
# Proyecto: Alexa Beauty
# Uso: ./scripts/backup_db.sh [ruta_destino_opcional]
# ==============================================================================

set -euo pipefail

TIMESTAMP=$(date +"%Y%m%d_%H%M%S")
BACKUP_DIR="${1:-./backups}"
mkdir -p "$BACKUP_DIR"

BACKUP_FILE="${BACKUP_DIR}/alexa_beauty_backup_${TIMESTAMP}.sql.gz"

echo "=================================================="
echo "Iniciando respaldo de base de datos Alexa Beauty"
echo "Fecha y hora: $(date)"
echo "Archivo destino: ${BACKUP_FILE}"
echo "=================================================="

if [ -z "${DATABASE_URL:-}" ]; then
  echo "Error: La variable DATABASE_URL no está definida."
  echo "Ejemplo: export DATABASE_URL='postgresql://postgres:[PASSWORD]@[HOST]:5432/postgres'"
  exit 1
fi

# Ejecución de pg_dump con compresión gzip
pg_dump "$DATABASE_URL" \
  --no-owner \
  --no-acl \
  --clean \
  --if-exists \
  --format=plain \
  | gzip -9 > "$BACKUP_FILE"

FILESIZE=$(du -h "$BACKUP_FILE" | cut -f1)

echo "=================================================="
echo "Respaldo completado exitosamente!"
echo "Archivo: ${BACKUP_FILE} (${FILESIZE})"
echo "SHA-256: $(sha256sum "$BACKUP_FILE" | cut -d' ' -f1)"
echo "=================================================="
