package com.simulation2.integrators;

import java.util.List;
import com.simulation2.models.Particle;

@FunctionalInterface
public interface ForceCalculator {
    /**
     * Calcula y actualiza las fuerzas para todas las partículas del sistema
     * @param particles Lista de partículas
     */
    void calculateForces(List<Particle> particles);
}
