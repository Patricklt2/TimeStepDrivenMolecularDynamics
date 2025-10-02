package com.simulation2.integrators;

import java.util.List;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import com.simulation2.models.Particle;

public class VelocityVerlet implements IIntegrator {
    @Override
    public void updatePositions(List<Particle> particles, double dt) {
        for (Particle p : particles) {
            // Guardar estado actual
            p.setOldPosition(p.getPosition());
            p.setOldVelocity(p.getVelocity());
            p.setOldAcceleration(p.getAcceleration());

            // Velocity-Verlet: nueva posición
            Vector3D newPosition = p.getPosition()
                .add(p.getVelocity().scalarMultiply(dt))
                .add(p.getAcceleration().scalarMultiply(0.5 * dt * dt));

            p.setPosition(newPosition);
        }
    }

    @Override
    public void updateVelocities(List<Particle> particles, double dt) {
        for (Particle p : particles) {
            // Nueva aceleración desde fuerzas recién calculadas
            Vector3D newAcceleration = p.getForce().scalarMultiply(1.0 / p.getMass());

            // Velocity-Verlet: nueva velocidad
            Vector3D newVelocity = p.getOldVelocity()
                .add(p.getOldAcceleration().add(newAcceleration).scalarMultiply(0.5 * dt));

            p.setVelocity(newVelocity);
            p.setAcceleration(newAcceleration);
        }
    }
}