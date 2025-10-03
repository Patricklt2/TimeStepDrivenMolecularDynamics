package com.simulation2.integrators;

import java.util.List;
import com.simulation2.models.Particle;

public interface IIntegrator {
    /**
     * Ejecuta un paso completo de integración numérica
     * @param particles Lista de partículas del sistema
     * @param dt Paso de tiempo
     * @param forceCalculator Función que calcula las fuerzas del sistema
     */
    void step(List<Particle> particles, double dt, ForceCalculator forceCalculator);
}