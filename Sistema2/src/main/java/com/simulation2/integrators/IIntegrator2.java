package com.simulation2.integrators;

import com.simulation2.models.Particle;

import java.util.List;

public interface IIntegrator2 {
    void step(Particle[] particles, double dt, double G, double h);

    void calculateForcesBetweenParticles(Particle[] particles, double G, double h);
}
