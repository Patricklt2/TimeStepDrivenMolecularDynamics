#!/bin/bash

echo "Creando directorio de salida..."
mkdir -p out/java

echo "Compilando archivos Java..."
javac -d out/java $(find src/java -name "*.java")

if [ $? -ne 0 ]; then
    echo "Error de compilación. Abortando."
    exit 1
fi

echo "Ejecutando la simulación..."
java -cp out/java simulation1.Main

echo "Script finalizado."