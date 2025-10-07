package com.simulation2.models;

import java.io.IOException;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.simulation2.integrators.IIntegrator2;
import com.simulation2.utils.CSVWriter2;

public class Simulation2 {
    // Simulation Variables
    private final double maxTime;
    private final double timeStep;
    private final double printingInterval = 0.5;
    private final double printingStep;
    private final String filename;
    private static final Logger logger = LoggerFactory.getLogger(Simulation2.class);
    private double totalTime = 0;
    
    // Galaxy Info
    private final double G = 1.0;
    private final double h = 0.05;
    private final int starsPerGalaxy;
    private final int numGalaxies;
    private final Particle[] stars;
    private int starOffset;
    
    
    // Integrator for particle movement
    private final IIntegrator2 integrator;
    
    

    public Simulation2(int starsPerGalaxy, int numGalaxies, double maxTime, double timeStep, String filename, IIntegrator2 integrator) {
        this.starsPerGalaxy = starsPerGalaxy;
        this.numGalaxies = numGalaxies;
        this.stars = new Particle[starsPerGalaxy * numGalaxies];
        this.starOffset = 0;
        this.filename = filename;
        this.maxTime = maxTime;
        this.integrator = integrator;
        this.timeStep = timeStep;
        this.printingStep = (int) (printingInterval / timeStep);
    }

    /**
     * Adds a Galaxy to the simulation
     */
    public void addGalaxyToSimulation(Vector3D centerPosition, Vector3D initialVelocity) {
        if(starOffset == starsPerGalaxy * numGalaxies){
            logger.warn("Cannot add any more galaxies to the simulation.");
            return;
        }
        Particle[] newStars = Galaxy2.initializeStars(starOffset / starsPerGalaxy + 1, starOffset,  starsPerGalaxy, centerPosition, initialVelocity);
        System.arraycopy(newStars, 0, stars, starOffset, starsPerGalaxy);
        starOffset+=starsPerGalaxy;
    }

    /**
     * Prepares simulation to be run. Must be called after all galaxies are initialized
     */
    private void prepareSimulation(){
        if(starOffset < starsPerGalaxy*numGalaxies){
            logger.warn("Cannot prepare simulation, add {} remaining galaxies", (starsPerGalaxy*numGalaxies - starOffset) / starsPerGalaxy);
            return;
        }
        integrator.calculateForcesBetweenParticles(stars, G, h);
        for(Particle p: stars){
            p.updateAcceleration();
            p.setOldAcceleration(p.getAcceleration());
        }
    }


    /**
     * Ejecuta la simulación
     */
    public void run() {
        logger.info("Iniciando simulación con {} partículas totales", stars.length);

        prepareSimulation();

        writeToFile(); // Escribe el estado inicial (t=0)

        int stepCount = 0;

        while (totalTime < maxTime) {
            integrator.step(stars, timeStep, G, h);
            totalTime += timeStep;
            stepCount++;
            if (stepCount % printingStep == 0) {
                writeToFile();
            }
        }

        writeToFile();
        logger.info("Simulación finalizada en t={}", totalTime);
    }

    private void writeToFile() {
        logger.debug("Escribiendo estado en t={} al archivo: {}", totalTime, filename);
        // Usamos try-with-resources para asegurar que el writer se cierre siempre
        try (CSVWriter2 writer = new CSVWriter2(filename, true)) { // 'true' para modo append
            writer.writeTimeHeader(totalTime);
            for (Particle p : stars) {
                if (p != null) {
                    writer.writeLine(p.toFileString());
                }
            }
        } catch (IOException e) {
            logger.error("Error al escribir en el archivo: {}", e.getMessage(), e);
        }
    }

    /** ---------- Getters y Setters ----------**/
    public Particle[] getStars() {
        return stars;
    }
}
