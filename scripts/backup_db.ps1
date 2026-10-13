<#
.SYNOPSIS
    Script de Respaldo de Base de Datos PostgreSQL para Alexa Beauty (PowerShell).
.DESCRIPTION
    Realiza un pg_dump de la base de datos configurada en $env:DATABASE_URL.
.PARAMETER DestinationDir
    Carpeta de destino para el archivo comprimido.
#>
param (
    [string]$DestinationDir = ".\backups"
)

$ErrorActionPreference = "Stop"

if (-not $env:DATABASE_URL) {
    Write-Error "La variable de entorno DATABASE_URL no está configurada. Ejemplo: postgresql://postgres:pass@host:5432/postgres"
}

if (-not (Test-Path $DestinationDir)) {
    New-Item -ItemType Directory -Path $DestinationDir | Out-Null
}

$timestamp = Get-Date -Format "yyyyMMdd_HHmmss"
$dumpFile = Join-Path $DestinationDir "alexa_beauty_backup_$timestamp.sql"

Write-Host "==================================================" -ForegroundColor Cyan
Write-Host "Iniciando respaldo de base de datos Alexa Beauty" -ForegroundColor Cyan
Write-Host "Fecha: $(Get-Date)"
Write-Host "Destino temporal: $dumpFile"
Write-Host "=================================================="

& pg_dump "$env:DATABASE_URL" --no-owner --no-acl --clean --if-exists --format=plain --file="$dumpFile"

if ($LASTEXITCODE -ne 0) {
    Write-Error "pg_dump falló con código de salida $LASTEXITCODE"
}

# Comprimir a zip
$zipFile = "$dumpFile.zip"
Compress-Archive -Path $dumpFile -DestinationPath $zipFile -Force
Remove-Item $dumpFile

$hash = (Get-FileHash -Path $zipFile -Algorithm SHA256).Hash
$size = (Get-Item $zipFile).Length / 1MB

Write-Host "==================================================" -ForegroundColor Green
Write-Host "Respaldo completado con éxito!" -ForegroundColor Green
Write-Host "Archivo: $zipFile ($([math]::Round($size, 2)) MB)"
Write-Host "SHA-256: $hash"
Write-Host "=================================================="
