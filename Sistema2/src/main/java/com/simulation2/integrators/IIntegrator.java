package com.simulation2.integrators;

import java.util.List;

import com.simulation2.models.Particle;

public interface IIntegrator {

    void updatePositions(List<Particle> particles, double dt);

    void updateVelocities(List<Particle> particles, double dt);
}