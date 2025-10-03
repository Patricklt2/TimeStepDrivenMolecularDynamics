package com.simulation2;

import com.simulation2.integrators.*;
import com.simulation2.models.Simulation;
import com.simulation2.models.Simulation2;

public class Main {
    public static void main(String[] args) {
        double K = 10000;
        final double GAMMA = 100;

        System.out.println("Hello, World!");
        IIntegrator2 integrator = new VelocityVerlet2();
        double timeStep = 0.0001;
        String filename = "sim_dt_" + timeStep + ".csv";

        Simulation2 s = new Simulation2(2, 1, 100, 5, timeStep, filename, integrator);
        s.run();
    }
}
