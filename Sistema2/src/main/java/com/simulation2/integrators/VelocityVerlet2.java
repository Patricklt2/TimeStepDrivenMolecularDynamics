package com.simulation2.integrators;

import com.simulation2.models.Particle;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

import java.util.List;

public class VelocityVerlet2 implements IIntegrator2{
    @Override
    public void step(Particle[] particles, double dt, double G, double h) {
        for(Particle particle: particles){
            // 1. backupeo la aceleración actual, se va a usar en el cálculo de la velocidad
            particle.setOldAcceleration(particle.getAcceleration());

            // 2. actualizo la posición
            Vector3D newPosition = particle.getPosition() // x(t)
                    .add(particle.getVelocity().scalarMultiply(dt)) // + v(t) * ∆t
                    .add(particle.getAcceleration().scalarMultiply(Math.pow(dt, 2)/2.0)); // + a(t) * ∆t^2 * 1/2
            particle.setPosition(newPosition);
        }

        // 3. recalculo las fuerzas entre las partículas
        calculateForcesBetweenParticles(particles, G, h);

        // 4. con la nueva fuerza calculada, actualizo las aceleraciones (tengo backupeada la anterior) y las velocidades
        for(Particle particle: particles){
            particle.updateAcceleration(); // a(t+∆t)
            Vector3D accelerationSum = particle.getOldAcceleration().add(particle.getAcceleration()); // a(t) + a(t+∆t)

            Vector3D newVelocity = particle.getVelocity() // v(t)
                    .add(accelerationSum.scalarMultiply(dt/2.0)); // + 1/2 * ( a(t) + a(t+∆t) ) * ∆t
            particle.setVelocity(newVelocity);
        }
    }

    @Override
    public void calculateForcesBetweenParticles(Particle[] particles, double G, double h){
        for(Particle particle : particles){
            particle.resetForce();
        }

        for(int i = 0; i < particles.length; i++){
            for(int j = i + 1; j < particles.length; j++) {
                Particle pi = particles[i];
                Particle pj = particles[j];

                Vector3D force = pi.calculateForceFrom(pj, G, h);

                pi.addForce(force.negate());
                pj.addForce(force);
            }
        }
    }
}
