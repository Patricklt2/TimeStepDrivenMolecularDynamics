# Script para configurar el entorno virtual de Python con autodetección en PowerShell.
$PythonCmd = ""
if (Get-Command python3 -ErrorAction SilentlyContinue) {
    $PythonCmd = "python3"
} elseif (Get-Command python -ErrorAction SilentlyContinue) {
    $PythonCmd = "python"
} elseif (Get-Command py -ErrorAction SilentlyContinue) {
    $PythonCmd = "py"
} else {
    Write-Host "Error: No se pudo encontrar un ejecutable de Python ('python3', 'python', o 'py')." -ForegroundColor Red
    Write-Host "Por favor, asegúrate de que Python esté instalado y en tu PATH."
    exit 1
}

& $PythonCmd --version

if (-not (Test-Path -Path "venv")) {
    Write-Host "Creando entorno virtual en la carpeta 'venv'..."
    & $PythonCmd -m venv venv
} else {
    Write-Host "El entorno virtual 'venv' ya existe."
}

.\venv\Scripts\python.exe -m pip install -r src\python\requirements.txt
