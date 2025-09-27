package com.simulation2.integrators;

import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import com.simulation2.models.Particle;

public class VelocityVerlet implements IIntegrator {
    @Override
    public void updatePositions(List<Particle> particles, double dt) {
        for (Particle p : particles) {
            // v(t + dt/2) = v(t) + a(t) * (dt/2)
            Vector3D halfStepVelocity = p.getVelocity().add(
                p.getAcceleration().scalarMultiply(dt / 2.0)
            );
            
            // r(t + dt) = r(t) + v(t + dt/2) * dt
            Vector3D nextPosition = p.getPosition().add(
                halfStepVelocity.scalarMultiply(dt)
            );
            
            p.setPosition(nextPosition);
            p.setVelocity(halfStepVelocity); 
        }
    }

    @Override
    public void updateVelocities(List<Particle> particles, double dt) {
        for (Particle p : particles) {
            
            // v(t + dt) = v(t + dt/2) + a(t + dt) * (dt/2)
            Vector3D finalVelocity = p.getVelocity().add(
                p.getAcceleration().scalarMultiply(dt / 2.0)
            );
            p.setVelocity(finalVelocity);
        }
    }
}