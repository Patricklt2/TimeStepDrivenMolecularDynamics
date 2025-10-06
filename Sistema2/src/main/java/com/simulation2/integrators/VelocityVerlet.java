package com.simulation2.integrators;

import java.util.List;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import com.simulation2.models.Particle;

public class VelocityVerlet implements IIntegrator {

    @Override
    public void step(List<Particle> particles, double dt, ForceCalculator forceCalculator) {
        // Paso 1: Calcular fuerzas y aceleraciones en t
        forceCalculator.calculateForces(particles);
        for (Particle p : particles) {
            p.updateAcceleration();
        }

        // Paso 2: Actualizar velocidades a t+dt/2 y posiciones a t+dt
        for (Particle p : particles) {
            Vector3D a_t = p.getAcceleration();

            // v(t+dt/2) = v(t) + a(t) * dt/2
            Vector3D v_half = p.getVelocity().add(a_t.scalarMultiply(dt / 2.0));

            // r(t+dt) = r(t) + v(t+dt/2) * dt
            Vector3D r_new = p.getPosition().add(v_half.scalarMultiply(dt));

            p.setPosition(r_new);
            p.setVelocity(v_half);
        }

        // Paso 3: Recalcular fuerzas y aceleraciones en t+dt
        forceCalculator.calculateForces(particles);
        for (Particle p : particles) {
            p.updateAcceleration();
        }

        // Paso 4: Actualizar velocidades a t+dt
        for (Particle p : particles) {
            Vector3D a_new = p.getAcceleration();

            // v(t+dt) = v(t+dt/2) + a(t+dt) * dt/2
            Vector3D v_new = p.getVelocity().add(a_new.scalarMultiply(dt / 2.0));

            p.setVelocity(v_new);
        }
    }
}