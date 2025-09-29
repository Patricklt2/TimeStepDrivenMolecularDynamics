package simulation1.utils;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

import simulation1.Particle;

public class CSVWriter implements AutoCloseable {
    private final BufferedWriter writer;

    public CSVWriter(String filename) throws IOException {
        this.writer = new BufferedWriter(new FileWriter(filename));

        writer.write("t;pos;vel\n");
    }

    public void writeData(double t, Particle p) throws IOException {
        double pos = p.getPosition();
        double vel = p.getVelocity();

        writer.write(String.format(Locale.US, "%.15e;%.15e;%.15e\n", t, pos, vel));
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }
}
