# Script para ejecutar el análisis de Python usando el entorno virtual.

$VENV_PYTHON = ".\venv\Scripts\python.exe"

if (-not (Test-Path -Path $VENV_PYTHON)) {
    Write-Host "Error: Entorno virtual no encontrado. Ejecuta 'setup.ps1' primero." -ForegroundColor Red
    exit 1
}

& $VENV_PYTHON src\python\main.py