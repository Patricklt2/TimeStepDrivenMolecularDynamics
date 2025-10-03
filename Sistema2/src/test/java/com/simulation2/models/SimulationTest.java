package com.simulation2.models;

import com.simulation2.integrators.IIntegrator;
import com.simulation2.integrators.IIntegrator2;
import com.simulation2.integrators.VelocityVerlet;
import com.simulation2.integrators.VelocityVerlet2;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class SimulationTest {

    private Simulation2 sim;

    private final int NUM_STARS = 2;
    private final int NUM_GALAXIES = 1;
    private final double GALAXY_DISTANCE = 0;
    private final double MAX_TIME = 5;
    private final String FILENAME = "test";
    private final IIntegrator2 VELOCITY_VERLET_INTEGRATION = new VelocityVerlet2();

    @BeforeEach
    void setUp(){
        sim = new Simulation2(NUM_STARS, NUM_GALAXIES, GALAXY_DISTANCE, MAX_TIME, 0.0001, FILENAME, VELOCITY_VERLET_INTEGRATION);
    }


    @Test
    public void testInitializeGalaxies(){
        sim.initializeGalaxies(NUM_GALAXIES, GALAXY_DISTANCE);
    }
}
