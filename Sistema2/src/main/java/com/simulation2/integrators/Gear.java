package com.simulation2.integrators;
import com.simulation2.models.Particle;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import java.util.List;

public class Gear implements IIntegrator {
    
    private static final double[] ALPHA = {3.0/20.0, 251.0/360.0, 1.0, 11.0/18.0, 1.0/6.0, 1.0/60.0};
    
    // Derivadas
    private Vector3D[] r3;
    private Vector3D[] r4;
    private Vector3D[] r5;
    
    private int numParticles;
    private boolean initialized = false;

    public Gear() {
    }

    private void initialize(List<Particle> particles) {
        numParticles = particles.size();
        r3 = new Vector3D[numParticles];
        r4 = new Vector3D[numParticles];
        r5 = new Vector3D[numParticles];
        
        for (int i = 0; i < numParticles; i++) {
            r3[i] = Vector3D.ZERO;
            r4[i] = Vector3D.ZERO;
            r5[i] = Vector3D.ZERO;
        }
        
        initialized = true;
    }

    @Override
    public void updatePositions(List<Particle> particles, double dt) {
        if (!initialized) {
            initialize(particles);
        }
        predict(particles, dt); // Prediccion
    }

    @Override
    public void updateVelocities(List<Particle> particles, double dt) {
        correct(particles, dt); // Corrección
    }

    private void predict(List<Particle> particles, double dt) {
        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            
            Vector3D r0 = p.getPosition();
            Vector3D r1 = p.getVelocity();
            Vector3D r2 = p.getAcceleration();
            
            double dt2 = dt * dt;
            double dt3 = dt2 * dt;
            double dt4 = dt3 * dt;
            double dt5 = dt4 * dt;
            
            Vector3D r0p = r0
                .add(r1.scalarMultiply(dt))
                .add(r2.scalarMultiply(dt2 / 2.0))
                .add(r3[i].scalarMultiply(dt3 / 6.0))
                .add(r4[i].scalarMultiply(dt4 / 24.0))
                .add(r5[i].scalarMultiply(dt5 / 120.0));
            
            Vector3D r1p = r1
                .add(r2.scalarMultiply(dt))
                .add(r3[i].scalarMultiply(dt2 / 2.0))
                .add(r4[i].scalarMultiply(dt3 / 6.0))
                .add(r5[i].scalarMultiply(dt4 / 24.0));
            
            Vector3D r2p = r2
                .add(r3[i].scalarMultiply(dt))
                .add(r4[i].scalarMultiply(dt2 / 2.0))
                .add(r5[i].scalarMultiply(dt3 / 6.0));
            
            Vector3D r3p = r3[i]
                .add(r4[i].scalarMultiply(dt))
                .add(r5[i].scalarMultiply(dt2 / 2.0));
            
            Vector3D r4p = r4[i]
                .add(r5[i].scalarMultiply(dt));
            
            Vector3D r5p = r5[i];
            
            p.setPosition(r0p);
            p.setVelocity(r1p);
            p.setAcceleration(r2p);
            r3[i] = r3p;
            r4[i] = r4p;
            r5[i] = r5p;
        }
    }

    private void correct(List<Particle> particles, double dt) {
        for (int i = 0; i < particles.size(); i++) {
            Particle p = particles.get(i);
            
            Vector3D r2p = new Vector3D(p.getAcceleration().getX(), 
                                        p.getAcceleration().getY(), 
                                        p.getAcceleration().getZ());
            
            p.updateAcceleration();
            Vector3D r2_real = p.getAcceleration();
            
            Vector3D deltaA = r2_real.subtract(r2p);
            
            double dt2 = dt * dt;
            Vector3D deltaR2 = deltaA.scalarMultiply(dt2 / 2.0);
            
            Vector3D r0c = p.getPosition().add(deltaR2.scalarMultiply(ALPHA[0]));
            Vector3D r1c = p.getVelocity().add(deltaR2.scalarMultiply(ALPHA[1] / dt));
            Vector3D r2c = r2p.add(deltaR2.scalarMultiply(ALPHA[2] * 2.0 / dt2));
            Vector3D r3c = r3[i].add(deltaR2.scalarMultiply(ALPHA[3] * 6.0 / (dt * dt2)));
            Vector3D r4c = r4[i].add(deltaR2.scalarMultiply(ALPHA[4] * 24.0 / (dt2 * dt2)));
            Vector3D r5c = r5[i].add(deltaR2.scalarMultiply(ALPHA[5] * 120.0 / (dt * dt2 * dt2)));
            
            p.setPosition(r0c);
            p.setVelocity(r1c);
            p.setAcceleration(r2c);
            r3[i] = r3c;
            r4[i] = r4c;
            r5[i] = r5c;
        }
    }
}