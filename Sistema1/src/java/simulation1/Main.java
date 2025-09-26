package simulation1;

import java.io.IOException;
import simulation1.integrators.*;
import simulation1.utils.CSVWriter;

public class Main {
    private static final double K = 10000;
    private static final double GAMMA = 100;
    private static final double MASS = 70;

    private static final double INITIAL_POSITION = 1.0;
    private static final double INITIAL_AMPLITUDE = 1.0;
    private static final double INITIAL_VELOCITY = -INITIAL_AMPLITUDE * GAMMA / (2 * MASS);

    private static final double TOTAL_TIME = 6.0;
    private static final double DT = 0.00001;

    public static void main(String[] args) {
        Particle particle = new Particle(INITIAL_POSITION, INITIAL_VELOCITY, MASS);

        IIntegrator integrator = new Verlet(K, GAMMA);
        // IIntegrator integrator = new Beeman(K, GAMMA);
        // IIntegrator integrator = new Gear(particle, K, GAMMA);

        String outputFilename = "./data/raw/" + integrator.getClass().getSimpleName().toLowerCase() + "_sim.csv";

        try (CSVWriter writer = new CSVWriter(outputFilename)) {
            writer.writeData(0, particle);

            for (double t = DT; t <= TOTAL_TIME; t += DT) {
                integrator.step(particle, DT);
                writer.writeData(t, particle);
            }
        } catch (IOException e) {
            System.err.println("Error al escribir en el archivo de salida: " + e.getMessage());
            e.printStackTrace();
        }
    }
}