package com.simulation2.integrators;

import java.util.List;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import com.simulation2.models.Particle;

public class Beeman implements IIntegrator {

    @Override
    public void step(List<Particle> particles, double dt, ForceCalculator forceCalculator) {

        forceCalculator.calculateForces(particles);

        for (Particle p : particles) {
            p.setOldAcceleration(p.getAcceleration());
        }

        for (Particle p : particles) {
            p.updateAcceleration();
        }

        for (Particle p : particles) {
            Vector3D a_t = p.getAcceleration();
            Vector3D a_prev = p.getOldAcceleration();

            // r(t+dt) = r(t) + v(t)*dt + (2/3)*a(t)*dt^2 - (1/6)*a(t-dt)*dt^2
            Vector3D r_new = p.getPosition()
                .add(p.getVelocity().scalarMultiply(dt))
                .add(a_t.scalarMultiply((2.0/3.0) * dt * dt))
                .subtract(a_prev.scalarMultiply((1.0/6.0) * dt * dt));

            p.setPosition(r_new);
        }

        forceCalculator.calculateForces(particles);

        for (Particle p : particles) {
            Vector3D a_t = p.getOldAcceleration();
            Vector3D a_prev = p.getOldAcceleration();
            Vector3D a_new = p.getForce().scalarMultiply(1.0 / p.getMass());

            // v(t+dt) = v(t) + (1/3)*a(t+dt)*dt + (5/6)*a(t)*dt - (1/6)*a(t-dt)*dt
            Vector3D v_new = p.getVelocity()
                .add(a_new.scalarMultiply((1.0/3.0) * dt))
                .add(a_t.scalarMultiply((5.0/6.0) * dt))
                .subtract(a_prev.scalarMultiply((1.0/6.0) * dt));

            p.setVelocity(v_new);
            p.setAcceleration(a_new);
        }
    }
}
