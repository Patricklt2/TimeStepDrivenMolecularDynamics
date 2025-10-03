package com.simulation2.models;

import com.simulation2.integrators.IIntegrator;
import com.simulation2.integrators.IIntegrator2;
import com.simulation2.utils.CSVWriter;
import com.simulation2.utils.CSVWriter2;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;

public class Simulation2 {
    private final double G = 1.0;
    private final double h = 0.05;
    private final int N; // número de partículas
    private final double maxTime;
    private final double timeStep;
    private final double printingStep = 1.0;

    private final IIntegrator2 integrator;
    private final Galaxy2[] galaxies;
    private double totalTime = 0;
    private final String filename;
    private static final Logger logger = LoggerFactory.getLogger(Simulation.class);

    public Simulation2(int n, int numGalaxies, double galaxyDistance, double maxTime, double timeStep, String filename, IIntegrator2 integrator) {
        this.N = n;
        this.galaxies = new Galaxy2[numGalaxies];
        this.filename = filename;
        this.maxTime = maxTime;
        this.integrator = integrator;
        this.timeStep = timeStep;
        initializeGalaxies(numGalaxies, galaxyDistance);
        initializeStarsAcceleration();
    }

    /**
     * Inicializa las galaxias de la simulación
     * @param numGalaxies cantidad de galaxias
     * @param galaxyDistance distancia relativa entre las galaxias
     */
    public void initializeGalaxies(int numGalaxies, double galaxyDistance) {
        for (int i = 0; i < numGalaxies; i++) {
            String name = "Galaxy_" + (i + 1);
            int starsPerGalaxy = N / numGalaxies;
            Vector3D centerPosition = new Vector3D(i * galaxyDistance, 0, 0);
            galaxies[i] = new Galaxy2(name, starsPerGalaxy, centerPosition);
        }
    }


    /**
     * Calcula la aceleración inicial de las partículas
     */
    public void initializeStarsAcceleration(){
        for (Galaxy2 galaxy : galaxies) {
            integrator.calculateForcesBetweenParticles(galaxy.getStars(), G, h);
            for(Particle p: galaxy.getStars()){
                p.updateAcceleration();
                p.setOldAcceleration(p.getAcceleration());
            }
        }
    }


    /**
     * Ejecuta la simulación
     */
    public void run(){
        logger.info("Starting simulation...");
        logger.debug("Total particles: " + N);

        writeToFile(galaxies); // initial state

        while (totalTime < maxTime) {
            totalTime += timeStep;
            for (Galaxy2 galaxy : galaxies) {
                integrator.step(galaxy.getStars(), timeStep, G, h);
            }
            writeToFile(galaxies);
        }
        logger.info("Simulation finished.");
        writeToFile(galaxies); // final state
    }

    private void writeToFile(Galaxy2[] galaxies) {
        logger.debug("Writing simulation state to file: " + filename);
        CSVWriter2 writer = null;
        try {
            for (Galaxy2 galaxy : galaxies) {
                writer = new CSVWriter2(filename);
                writer.writeData(totalTime, galaxy);
                writer.close();
            }
        } catch (IOException e) {
            logger.error("Error writing to file: " + e.getMessage());
        } finally {
            if (writer != null) {
                try {
                    writer.close();
                } catch (IOException e) {
                    logger.error("Error closing writer: " + e.getMessage());
                }
            }
        }
    }
}
