package com.simulation2.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import com.simulation2.models.Galaxy;

public class CSVWriter implements AutoCloseable {
    private final BufferedWriter writer;
    public CSVWriter(String filename) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(filename, true));
        
    }

    public void writeData(double t, Galaxy g) throws IOException {
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
