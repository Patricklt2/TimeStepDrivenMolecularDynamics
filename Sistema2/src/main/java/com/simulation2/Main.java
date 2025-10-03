package com.simulation2;

import com.simulation2.integrators.Beeman;
import com.simulation2.integrators.IIntegrator;
import com.simulation2.integrators.VelocityVerlet;
import com.simulation2.models.Simulation;

public class Main {
    public static void main(String[] args) {
        double K = 10000;
        final double GAMMA = 100;

        System.out.println("Hello, World!");
        IIntegrator integrator = new Beeman();

        Simulation s = new Simulation(4, 1, 0, 15, "sim.csv", integrator);
        s.run();
    }
}
