package com.simulation2.integrators;

import java.util.List;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import com.simulation2.models.Particle;

public class Beeman implements IIntegrator {
    private final double k;
    private final double gamma;

    public Beeman(double k, double gamma) {
        this.k = k;
        this.gamma = gamma;
    }

    @Override
    public void updatePositions(List<Particle> particles, double dt) {
        for (Particle p : particles) {
            // Guardar estado actual
            p.setOldPosition(p.getPosition());
            p.setOldVelocity(p.getVelocity());
            p.setOldAcceleration(p.getAcceleration());

            Vector3D currentAcceleration = p.getAcceleration();
            Vector3D previousAcceleration = p.getOldAcceleration();

            // Manejo del primer paso
            if (previousAcceleration == null) {
                previousAcceleration = currentAcceleration;
            }

            // Beeman: predicción de posición
            Vector3D newPosition = p.getPosition()
                .add(p.getVelocity().scalarMultiply(dt))
                .add(currentAcceleration.scalarMultiply((2.0/3.0) * dt * dt))
                .subtract(previousAcceleration.scalarMultiply((1.0/6.0) * dt * dt));

            p.setPosition(newPosition);
        }
    }

    @Override
    public void updateVelocities(List<Particle> particles, double dt) {
        for (Particle p : particles) {
            Vector3D currentAcceleration = p.getAcceleration();
            Vector3D previousAcceleration = p.getOldAcceleration();

            // Manejo del primer paso
            if (previousAcceleration == null) {
                previousAcceleration = currentAcceleration;
            }

            // Nueva aceleración con fuerzas recién calculadas
            Vector3D newAcceleration = p.getForce().scalarMultiply(1.0 / p.getMass());

            // Beeman: corrección de velocidad
            Vector3D correctedVelocity = p.getOldVelocity()
                .add(newAcceleration.scalarMultiply((1.0/3.0) * dt))
                .add(currentAcceleration.scalarMultiply((5.0/6.0) * dt))
                .subtract(previousAcceleration.scalarMultiply((1.0/6.0) * dt));

            p.setVelocity(correctedVelocity);
            p.setAcceleration(newAcceleration);
        }
    }
}
