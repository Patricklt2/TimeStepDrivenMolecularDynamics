package com.simulation2.models;

import java.util.List;
import java.util.logging.Logger;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Simulation {

    private final double G = 1.0;
    private final double h = 0.05;
    private final int N; // número de partículas
    private final Galaxy[] galaxies;
    private final double timeStep = 0.01;
    private final double printingStep = 1.0;
    private double totalTime = 0;
    private final double maxTime;

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
        logger.info("Starting simulation with {} galaxies and {} particles.", galaxies.length, N);
        
        writeToFile(galaxies); // initial state

        while (totalTime < maxTime) {
                totalTime += timeStep; 
            for (Galaxy galaxy : galaxies) {
                galaxy.resetForces();
                
                galaxy.calculateForces();

                galaxy.updateStarPositions(timeStep);
                
                galaxy.calculateNewCenterPosition();
            }
            writeToFile(galaxies);
        }
        logger.info("Simulation finished.");
        writeToFile(galaxies); // final state
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
    
