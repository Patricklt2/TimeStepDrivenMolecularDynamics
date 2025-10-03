package com.simulation2.models;

import com.simulation2.integrators.IIntegrator;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simulation2.utils.CSVWriter;
import java.io.IOException;

public class Simulation {

    private final double G = 1.0;
    private final double h = 0.05;
    private final int N; // número de partículas
    private final double maxTime;
    private final double timeStep = 1e-04;
    private final double printingStep = 1.0;

    private final IIntegrator integrator;
    private final Galaxy[] galaxies;
    private double totalTime = 0;
    private final String filename;
    private static final Logger logger = LoggerFactory.getLogger(Simulation.class);

    public Simulation(int n, int numGalaxies, double galaxyDistance, double maxTime, String filename, IIntegrator integrator) {
        this.N = n;
        this.galaxies = new Galaxy[numGalaxies];
        this.filename = filename;
        this.maxTime = maxTime;
        this.integrator = integrator;
        initializeGalaxies(numGalaxies, galaxyDistance);
    }

    private void initializeGalaxies(int numGalaxies, double galaxyDistance) {
        for (int i = 0; i < numGalaxies; i++) {
            String name = "Galaxy_" + (i + 1);
            int starsPerGalaxy = N / numGalaxies;
            Vector3D centerPosition = new Vector3D(i * galaxyDistance, 0, 0);
            galaxies[i] = new Galaxy(name, starsPerGalaxy, centerPosition);
        }
    }
    
    public void run(){
        // run simulation
        logger.info("Starting simulation...");
        logger.debug("Total particles: " + N);
        
        writeToFile(galaxies); // initial state

        while (totalTime < maxTime) {
                totalTime += timeStep; 
            for (Galaxy galaxy : galaxies) {
                galaxy.integratorMethod(integrator,timeStep, G, h);
            }
            writeToFile(galaxies);
        }
        logger.info("Simulation finished.");
        writeToFile(galaxies); // final state
    }

    private void writeToFile(Galaxy[] galaxies) {
        logger.debug("Writing simulation state to file: " + filename);
        CSVWriter writer = null;
        try {
            for (Galaxy galaxy : galaxies) {
                writer = new CSVWriter(filename);
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
    
