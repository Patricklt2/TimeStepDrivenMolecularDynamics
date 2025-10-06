package com.simulation2.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;

public class CSVWriter2 implements AutoCloseable {
    private final BufferedWriter writer;

    public CSVWriter2(String filename, boolean append) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(filename, append));
    }

    public void writeTimeHeader(double time) throws IOException {
        // Usamos formato científico para mantener la consistencia y precisión
        writer.write(String.format("t=%.15e\n", time));
    }

    /**
     * Escribe una línea de texto genérica en el archivo, seguida de un salto de línea.
     * Se usa para escribir la representación de cada partícula obtenida de `particle.toFileString()`.
     *
     * @param line La línea de texto a escribir.
     * @throws IOException Si ocurre un error de escritura.
     */
    public void writeLine(String line) throws IOException {
        writer.write(line + "\n");
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
