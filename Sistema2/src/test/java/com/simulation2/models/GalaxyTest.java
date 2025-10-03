package com.simulation2.models;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class GalaxyTest {

    private Galaxy2 galaxy;
    private final String GALAXY_NAME = "galaxy";
    private final int NUM_STARS = 2;
    private final Vector3D CENTER_POSITION = new Vector3D(0, 0, 0);

    @BeforeEach
    public void setUp(){
        galaxy = new Galaxy2(GALAXY_NAME, NUM_STARS, CENTER_POSITION);
    }

    @Test
    public void testInitializeStars(){
        galaxy.initializeStars();

        assertEquals(NUM_STARS, galaxy.getStars().length);
    }


    @Test
    public void testCalculateForces_GetResultantForceOnEachParticle(){
//        Particle p1 = new Particle(1, new Vector3D(0, 0, 0), Vector3D.ZERO);
//        Particle p2 = new Particle(2, new Vector3D(1, 0, 0), Vector3D.ZERO);
//
//        Particle[] particles = {p1, p2};
//
//        galaxy.setStars(particles);
//
//        double G = 1.0;
//        double h = 0.05;
//        calculateForces(G, h);
//
//        double expectedForceXModule = 0.9962616;
//
//        // checks for p1
//        assertEquals(expectedForceXModule, p1.getForce().getX(), 1e-5);
//        assertEquals(0.0, p1.getForce().getY());
//        assertEquals(0.0, p1.getForce().getZ());
//
//        // checks for p2
//        assertEquals(expectedForceXModule * (-1), p2.getForce().getX(), 1e-5);
//        assertEquals(0.0, p2.getForce().getY());
//        assertEquals(0.0, p2.getForce().getZ());
    }
}
