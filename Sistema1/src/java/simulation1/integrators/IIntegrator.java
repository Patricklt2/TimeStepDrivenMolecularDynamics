package simulation1.integrators;

import simulation1.Particle;

public interface IIntegrator {
    void step(Particle particle, double dt);
}