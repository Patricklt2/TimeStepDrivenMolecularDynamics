#!/bin/bash
if command -v python3 &> /dev/null; then
    PYTHON_CMD="python3"
elif command -v python &> /dev/null; then
    PYTHON_CMD="python"
else
    echo "Error: No se pudo encontrar un ejecutable de Python ('python3' o 'python')."
    echo "Por favor, asegúrate de que Python esté instalado y en tu PATH."
    exit 1
fi

$PYTHON_CMD --version

if [ ! -d "venv" ]; then
    echo "Creando entorno virtual en la carpeta 'venv'..."
    $PYTHON_CMD -m venv venv
else
    echo "El entorno virtual 'venv' ya existe."
fi

./venv/bin/python -m pip install -r src/python/requirements.txt
