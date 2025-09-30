// @Sergi
// Lo arme asi porque no queria pisarte el codigo, despues comparamos
// Voy a poner la logica del integrator aca, 
package com.simulation2.models;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simulation2.integrators.IIntegrator;
import com.simulation2.integrators.VelocityVerlet;

public class Simulationv2 {
    // --- Physical and Simulation Parameters ---
    private final double G = 1.0;
    private final double h = 0.05; // Gravitational smoothing parameter
    private final double timeStep;
    private final double maxTime;
    private final String outputFilename;

    // --- Simulation State ---
    private final List<Particle> allParticles;
    private final Galaxy[] galaxies;
    private final IIntegrator integrator;
    private double totalTime = 0;
    private int printCount = 0;

    private static final Logger logger = LoggerFactory.getLogger(Simulation.class);

    public Simulationv2(int n, int numGalaxies, double galaxyDistance, double maxTime, double timeStep, String outputFilename) {
        this.maxTime = maxTime;
        this.timeStep = timeStep;
        this.outputFilename = outputFilename;
        this.galaxies = new Galaxy[numGalaxies];

        initializeGalaxies(n, numGalaxies, galaxyDistance);

        // Create a single, unified list of all particles for efficient calculations.
        this.allParticles = new ArrayList<>();
        for (Galaxy g : galaxies) {
            this.allParticles.addAll(Arrays.asList(g.getStars()));
        }

        // The Velocity-Verlet integrator is the ideal choice for this problem.
        this.integrator = new VelocityVerlet();

        // Calculate initial forces and accelerations (at t=0) before the first step.
        logger.info("Calculating initial forces and accelerations...");
        calculateAllForces();
        updateAllAccelerations();
    }

    private void initializeGalaxies(int n, int numGalaxies, double galaxyDistance) {
        for (int i = 0; i < numGalaxies; i++) {
            String name = "Galaxy_" + (i + 1);
            int starsPerGalaxy = n / numGalaxies;
            Vector3D centerPosition = new Vector3D(i * galaxyDistance, 0, 0);
            galaxies[i] = new Galaxy(name, starsPerGalaxy, centerPosition);
        }
    }

    /**
     * Runs the main simulation loop until maxTime is reached.
     */
    public void run() {
        logger.info("Starting simulation with {} particles and dt={}...", allParticles.size(), timeStep);
        
        // Write the initial state of the system to the output file.
        writeToFile();

        while (totalTime < maxTime) {
            // --- The Velocity-Verlet Integration Loop ---

            // 1. Update positions and the first half of the velocity update (kick-drift).
            integrator.updatePositions(allParticles, timeStep);

            // 2. Recalculate all forces based on the new particle positions.
            calculateAllForces();
            updateAllAccelerations();

            // 3. Complete the velocity update using the newly calculated forces (kick).
            integrator.updateVelocities(allParticles, timeStep);

            totalTime += timeStep;
            
            // Write the current state of the system to the output file.
            writeToFile();
        }
        logger.info("Simulation finished at t = {}", String.format(Locale.US, "%.2f", totalTime));
    }

    private void calculateAllForces() {
        for (Particle p : allParticles) {
            p.resetForce();
        }

        for (int i = 0; i < allParticles.size(); i++) {
            for (int j = i + 1; j < allParticles.size(); j++) {
                Particle p1 = allParticles.get(i);
                Particle p2 = allParticles.get(j);

                Vector3D forceOnP1 = Galaxy.calculateForce(p1, p2, G, h);

                p1.addForce(forceOnP1);
                p2.addForce(forceOnP1.negate());
            }
        }
    }

    private void updateAllAccelerations() {
        for (Particle p : allParticles) {
            p.updateAcceleration();
        }
    }

    private double calculateTotalSystemEnergy() {
        double totalKineticEnergy = 0;
        double totalPotentialEnergy = 0;

        for (Particle p : allParticles) {
            totalKineticEnergy += p.getKineticEnergy();
        }

        for (int i = 0; i < allParticles.size(); i++) {
            for (int j = i + 1; j < allParticles.size(); j++) {
                Particle p1 = allParticles.get(i);
                Particle p2 = allParticles.get(j);
                
                Vector3D r_ij = p2.getPosition().subtract(p1.getPosition());
                double distance = Math.sqrt(r_ij.getNormSq() + (h * h));
                
                totalPotentialEnergy += -G * p1.getMass() * p2.getMass() / distance;
            }
        }
        return totalKineticEnergy + totalPotentialEnergy;
    }

    private void writeToFile() {
        boolean append = (printCount > 0);
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(outputFilename, append))) {
            writer.write(allParticles.size() + "\n");
            writer.write(String.format(Locale.US, "Frame: %d, Time: %.5f, Total Energy: %.5e\n", 
                                       printCount++, totalTime, calculateTotalSystemEnergy()));
            
            for (Particle p : allParticles) {
                writer.write(String.format(Locale.US, "%s %.5f %.5f %.5f\n",
                        p.getGalaxyName(),
                        p.getPosition().getX(),
                        p.getPosition().getY(),
                        p.getPosition().getZ()));
            }
        } catch (IOException e) {
            logger.error("Error writing to output file '{}'", outputFilename, e);
        }
    }
}