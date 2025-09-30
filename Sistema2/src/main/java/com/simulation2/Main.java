package com.simulation2;

import com.simulation2.integrators.IIntegrator;
import com.simulation2.integrators.VelocityVerlet;
import com.simulation2.models.Simulation;

public class Main {
    public static void main(String[] args) {
        IIntegrator integrator = new VelocityVerlet();
        Simulation s = new Simulation(100, 1, 0, 5, "sim.csv", integrator);
        s.run();
    }
}
