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

    }
}
