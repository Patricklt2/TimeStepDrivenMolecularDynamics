package com.simulation2.integrators;

import java.util.List;
import java.util.Vector;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import com.simulation2.models.Particle;

public class Beeman implements IIntegrator {
   // cte del resorte
   private final double k;

   // cte de amortiguamiento
   private final double gamma;

   public Beeman(double k, double gamma) {
       this.k = k;
       this.gamma = gamma;
   }

    @Override
    public void updatePositions(List<Particle> particles, double dt){
        for (Particle p : particles) {

            // Guardamos la posición y velocidad anteriores
            Vector3D oldPosition = p.getPosition();
            Vector3D oldVelocity = p.getVelocity();
            Vector3D oldAcceleration = p.getAcceleration();

            p.setOldPosition(p.getPosition());
            p.setOldVelocity(p.getVelocity());
            p.setOldAcceleration(p.getAcceleration());


            // Actualizo la posicion
            Vector3D newPosition = p.getPosition().add(
                p.getVelocity().scalarMultiply(dt)
                .add(p.getAcceleration().scalarMultiply((2.0/3.0) * dt * dt))
                .subtract(oldAcceleration.scalarMultiply((1.0/6.0) * dt * dt))
            );
            p.setPosition(newPosition);
            
            Vector3D newAcceleration = p.getForce().scalarMultiply(1.0 / p.getMass());
            // Actualizo la velocidad
            Vector3D newVelocity = p.getVelocity().add(
                newAcceleration.scalarMultiply((1.0/3.0) * dt).add(
                p.getAcceleration().scalarMultiply((5.0/6.0) * dt)
                .subtract(oldAcceleration.scalarMultiply((1.0/6.0) * dt)))
            );
            p.setVelocity(newVelocity);
            p.setAcceleration(newAcceleration);
        }

    }

    @Override
    public void updateVelocities(List<Particle> particles, double dt){
        for (Particle p : particles) {
            Vector3D predictedVelocity = p.getVelocity();
            Vector3D currentAcceleration = p.getAcceleration();
            Vector3D previousAcceleration = p.getOldAcceleration();
            
            Vector3D newAcceleration = p.getForce().scalarMultiply(1.0 / p.getMass());
            
            Vector3D correctedVelocity = p.getOldVelocity()
                .add(newAcceleration.scalarMultiply((1.0/3.0) * dt))
                .add(currentAcceleration.scalarMultiply((5.0/6.0) * dt))
                .subtract(previousAcceleration.scalarMultiply((1.0/6.0) * dt));
            
            p.setVelocity(correctedVelocity);
            p.setAcceleration(newAcceleration);
        }
    }

}
