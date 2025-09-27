package com.simulation2.integrators;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.simulation2.models.Particle;

public class Verlet implements IIntegrator {
    private final Map<Integer, Vector3D> previousPositions;

    public Verlet(List<Particle> initialParticles, double dt) {
        this.previousPositions = new HashMap<>();
        for (Particle p : initialParticles) {
            // r(t-dt) = r(t) - v(t)*dt + a(t)*dt²/2
            Vector3D r0 = p.getPosition();
            Vector3D v0 = p.getVelocity();
            Vector3D a0 = p.getAcceleration();
            
            Vector3D prevPos = r0.subtract(v0.scalarMultiply(dt))
                                 .add(a0.scalarMultiply(0.5 * dt * dt));
            previousPositions.put(p.getId(), prevPos);
        }
    }

    @Override
    public void updatePositions(List<Particle> particles, double dt) {
        for (Particle p : particles) {
            Vector3D currentPos = p.getPosition();
            Vector3D prevPos = previousPositions.get(p.getId());
            Vector3D acceleration = p.getAcceleration();

            // r(t+dt) = 2*r(t) - r(t-dt) + a(t)*dt²
            Vector3D nextPos = currentPos.scalarMultiply(2)
                                       .subtract(prevPos)
                                       .add(acceleration.scalarMultiply(dt * dt));
            
            p.setPosition(nextPos);
            previousPositions.put(p.getId(), currentPos);
        }
    }

    @Override
    public void updateVelocities(List<Particle> particles, double dt) {
        for (Particle p : particles) {
            Vector3D nextPos = p.getPosition();
            Vector3D prevPos = previousPositions.get(p.getId());

            // v(t) = (r(t+dt) - r(t-dt)) / (2*dt)
            Vector3D currentPos = prevPos;
            Vector3D velocity = nextPos.subtract(currentPos).scalarMultiply(1.0 / (2.0 * dt));
            p.setVelocity(velocity);
        }
    }
}