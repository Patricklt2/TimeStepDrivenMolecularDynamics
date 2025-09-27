package com.simulation2.integrators;

import com.simulation2.models.Particle;

public class Verlet implements IIntegrator {
    // cte del resorte
    private final double k;

    // cte de amortiguamiento
    private final double gamma;  
    private double previousPosition;

    /*
     * Aclaro para cuando lean esto, esto es para el verlet original
     * creo que esta bien implementado
     */
    public Verlet(double k, double gamma) {
        this.k = k;
        this.gamma = gamma;
        this.previousPosition = Double.NaN; // inicialmente no se cuanto es XD
    }

    @Override
    public void step(Particle p, double dt){
        // Hay que inicializar
        if (Double.isNaN(previousPosition)) {
            initializePreviousPosition(p, dt);
        }

    }

    private void initializePreviousPosition(Particle p0, double dt) {

    }
}
