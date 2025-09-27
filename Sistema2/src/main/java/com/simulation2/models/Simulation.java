package com.simulation2.models;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Simulation {

    private final double G = 1.0;
    private final double h = 0.05;
    private final int N; // número de partículas
    private final double maxTime;
    private final double timeStep = 0.01;
    private final double printingStep = 1.0;

    private final Galaxy[] galaxies;
    private double totalTime = 0;


    private static final Logger logger = LoggerFactory.getLogger(Simulation.class);

    private Simulation(int n, int numGalaxies, double galaxyDistance, double maxTime) {
        this.N = n;
        this.galaxies = new Galaxy[numGalaxies];
        this.maxTime = maxTime;
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
                galaxy.resetForces();
                
                galaxy.calculateForces(G, h);

                galaxy.updateStarPositions(timeStep);
                
                galaxy.calculateNewCenterPosition();
            }
            writeToFile(galaxies);
        }
        logger.info("Simulation finished.");
        writeToFile(galaxies); // final state
    }
    private void writeToFile(Galaxy[] galaxies) {
    }

    private List<Particle> updateStarPositions(List<Particle> stars) {
        for (Particle star : stars) {
            star.updateAcceleration();
            star.setVelocity(star.getVelocity().add(star.getAcceleration().scalarMultiply(timeStep)));
            star.setPosition(star.getPosition().add(star.getVelocity().scalarMultiply(timeStep)));
        }
        return stars;
    }

}
    
