package com.simulation2.models;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class ParticleTest {

    @Test
    public void testCalculateForces(){
        Particle p1 = new Particle(1, new Vector3D(0, 0, 0), Vector3D.ZERO);
        Particle p2 = new Particle(2, new Vector3D(1, 0, 0), Vector3D.ZERO);

        double G = 1.0;
        double h = 0.05;

        // Calculamos la fuerza que p2 ejerce sobre p1
        Vector3D forceOnP1 = p1.calculateForceFrom(p2, G, h);

        double expectedForceX = -0.9962616;

        // Fuerza atractiva
        double delta = 1e-5;
        assertEquals(expectedForceX, forceOnP1.getX(), delta);
        assertEquals(0.0, forceOnP1.getY(), delta);
        assertEquals(0.0, forceOnP1.getZ(), delta);
    }
}
