package com.simulation2.integrators;

import com.simulation2.models.Particle;

public class Beeman implements IIntegrator {
    // cte del resorte
    private final double k;

    // cte de amortiguamiento
    private final double gamma;  
    private double previousAcceleration;

    public Beeman(double k, double gamma) {
        this.k = k;
        this.gamma = gamma;
        this.previousAcceleration = Double.NaN; // inicialmente no se cuanto es XD2
    }

    @Override
    public void step(Particle p, double dt) {
        double mass = p.getMass();

        double force = -k * p.getPosition() - gamma* p.getVelocity();
        double currentAcceleration = force / mass;

        if (Double.isNaN(previousAcceleration)) {
            this.previousAcceleration = currentAcceleration;
        }

        double nextPosition = p.getPosition() 
        + p.getVelocity() * dt 
        + (2.0/3.0) * currentAcceleration * dt * dt
        - (1.0/6.0) * this.previousAcceleration * dt * dt;

        double predictedVelocity = p.getVelocity() 
        + (3.0/2.0) * currentAcceleration * dt 
        - (1.0/2.0) * this.previousAcceleration * dt;

        double nextForce = -k * nextPosition - gamma * predictedVelocity;
        double nextAcceleration = nextForce / mass;

        double correctedVelocity = p.getVelocity() 
        + (1.0/3.0) * nextAcceleration * dt 
        + (5.0/6.0) * currentAcceleration * dt
        - (1.0/6.0) * this.previousAcceleration * dt;

        this.previousAcceleration = currentAcceleration;

        p.setPosition(nextPosition);
        p.setVelocity(correctedVelocity);
    }
}
