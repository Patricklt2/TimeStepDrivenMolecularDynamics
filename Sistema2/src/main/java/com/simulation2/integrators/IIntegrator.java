package com.simulation2.integrators;

import com.simulation2.models.Particle;

public interface IIntegrator {
    void step(Particle particle, double dt);
}