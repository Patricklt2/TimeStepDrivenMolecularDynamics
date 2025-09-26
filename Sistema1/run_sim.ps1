Write-Host "Creando directorio de salida..."
New-Item -ItemType Directory -Force -Path "out/java"

Write-Host "Compilando archivos Java..."
javac -d out/java (Get-ChildItem -Path "src/java" -Recurse -Filter "*.java" | ForEach-Object { $_.FullName })

if ($LASTEXITCODE -ne 0) {
    Write-Host "Error de compilación. Abortando."
    exit 1
}

Write-Host "Ejecutando la simulación..."
java -cp out/java simulation1.Main

Write-Host "Script finalizado."