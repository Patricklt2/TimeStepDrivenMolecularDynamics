package com.simulation2.utils;

import com.simulation2.models.Galaxy2;

import java.io.BufferedWriter;
import java.io.Closeable;
import java.io.FileWriter;
import java.io.IOException;

public class CSVWriter2 implements AutoCloseable {
    private final BufferedWriter writer;
    public CSVWriter2(String filename) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(filename, true));

    }

    public void writeData(double t, Galaxy2 g) throws IOException {
        String[] starLines = g.toFileGalaxyStars();
        writer.write(t + ";" + g.toFileGalaxyHeader() + "\n");
        for (String line : starLines) {
            writer.write(line + "\n");
        }
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
