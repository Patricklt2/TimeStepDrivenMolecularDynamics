package simulation1;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import simulation1.integrators.Beeman;
import simulation1.integrators.Gear;
import simulation1.integrators.IIntegrator;
import simulation1.integrators.Verlet;
import simulation1.utils.CSVWriter;

public class Main {
    private static final double K = 10000;
    private static final double GAMMA = 100;
    private static final double MASS = 70;
    private static final double TOTAL_TIME = 5.0;

    private static final List<Double> DT_VALUES = List.of(1e-1, 1e-2,
            1e-3, 1e-4, 1e-5, 1e-6, 1e-7
    );

    public static void main(String[] args) {
        String[] algorithms = {"Verlet", "Beeman", "Gear"};

        for (String algorithmName : algorithms) {
            for (double dt : DT_VALUES) {
                runSimulation(algorithmName, dt);
            }
        }
    }

    /**
     * Realiza una simulación completa.
     * @param algorithmName El nombre del integrador.
     * @param dt El paso temporal para esta simulación.
     */
    private static void runSimulation(String algorithmName, double dt) {
        String filename = String.format(Locale.US, "./data/raw/%s_sim_%.0e.csv",
                algorithmName.toLowerCase(), dt).replace("e-0", "e-");

        double initialPosition = 1.0;
        double initialAmplitude = 1.0;
        double initialVelocity = -initialAmplitude * GAMMA / (2 * MASS);
        Particle particle = new Particle(initialPosition, initialVelocity, MASS);

        IIntegrator integrator;
        switch (algorithmName) {
            case "Beeman":
                integrator = new Beeman(K, GAMMA);
                break;
            case "Gear":
                integrator = new Gear(particle, K, GAMMA);
                break;
            case "Verlet":
            default:
                integrator = new Verlet(K, GAMMA);
                break;
        }

        try (CSVWriter writer = new CSVWriter(filename)) {
            writer.writeData(0, particle);
            for (double t = dt; t <= TOTAL_TIME; t += dt) {
                integrator.step(particle, dt);
                writer.writeData(t, particle);
            }
        } catch (IOException e) {
            System.err.println("Error al escribir el archivo " + filename + ": " + e.getMessage());
        }
    }
}