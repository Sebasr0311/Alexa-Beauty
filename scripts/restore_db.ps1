<#
.SYNOPSIS
    Script de Restauración de Base de Datos PostgreSQL para Alexa Beauty (PowerShell).
.DESCRIPTION
    Restaura un dump SQL a la base de datos configurada en $env:DATABASE_URL.
.PARAMETER BackupFile
    Ruta al archivo .zip o .sql de respaldo.
#>
param (
    [Parameter(Mandatory=$true)]
    [string]$BackupFile
)

$ErrorActionPreference = "Stop"

if (-not (Test-Path $BackupFile)) {
    Write-Error "El archivo de respaldo '$BackupFile' no existe."
}

if (-not $env:DATABASE_URL) {
    Write-Error "La variable de entorno DATABASE_URL no está configurada."
}

Write-Host "==================================================" -ForegroundColor Yellow
Write-Host "ADVERTENCIA: Esta operación sobreescribirá la base de datos destino." -ForegroundColor Yellow
Write-Host "Destino: $env:DATABASE_URL"
Write-Host "Archivo: $BackupFile"
Write-Host "=================================================="

$confirm = Read-Host "¿Desea continuar? (Escriba 'SI' en mayúsculas)"
if ($confirm -ne "SI") {
    Write-Host "Operación cancelada." -ForegroundColor Gray
    exit 0
}

$tempSql = $BackupFile
if ($BackupFile.EndsWith(".zip")) {
    $tempFolder = Join-Path $env:TEMP "alexa_restore_$(Get-Random)"
    Expand-Archive -Path $BackupFile -DestinationPath $tempFolder -Force
    $tempSql = (Get-ChildItem -Path $tempFolder -Filter "*.sql")[0].FullName
}

try {
    Write-Host "Ejecutando restauración con psql..." -ForegroundColor Cyan
    & psql "$env:DATABASE_URL" -v ON_ERROR_STOP=1 -f "$tempSql"
    if ($LASTEXITCODE -ne 0) {
        Write-Error "psql falló con código $LASTEXITCODE"
    }
    Write-Host "==================================================" -ForegroundColor Green
    Write-Host "Restauración completada exitosamente." -ForegroundColor Green
    Write-Host "=================================================="
}
finally {
    if ($BackupFile.EndsWith(".zip") -and (Test-Path $tempFolder)) {
        Remove-Item -Recurse -Force $tempFolder
    }
}
