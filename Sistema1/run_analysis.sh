#!/bin/bash
VENV_PYTHON="venv/bin/python"

if [ ! -f "$VENV_PYTHON" ]; then
    echo "Error: Entorno virtual no encontrado. Ejecuta 'setup.sh' primero."
    exit 1
fi

$VENV_PYTHON src/python/main.py