package com.simulation2.integrators;

import com.simulation2.models.Particle;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.List;

/**
 * Clase base para tests de integradores.
 * Contiene métodos utilitarios y configuraciones comunes para todas las pruebas.
 */
public abstract class IntegratorTestBase {

    protected static final double TOLERANCE = 1e-10;
    protected static final double ENERGY_TOLERANCE = 1e-8;
    protected static final double G = 1.0;
    protected static final double H = 0.05;
    protected static final double DT = 1e-4;

    protected List<Particle> particles;

    @BeforeEach
    void setUp() {
        setupParticles();
    }

    /**
     * Configura partículas de prueba para los tests
     */
    protected void setupParticles() {
        // Sistema simple de dos partículas
        Particle p1 = new Particle(0, new Vector3D(0, 0, 0), new Vector3D(0.1, 0, 0));
        Particle p2 = new Particle(1, new Vector3D(1, 0, 0), new Vector3D(-0.1, 0, 0));

        particles = Arrays.asList(p1, p2);

        // Calcular fuerzas iniciales
        calculateForces();

        // Inicializar aceleraciones
        for (Particle p : particles) {
            Vector3D acceleration = p.getForce().scalarMultiply(1.0 / p.getMass());
            p.setAcceleration(acceleration);
            p.setOldAcceleration(acceleration);
        }
    }

    /**
     * Calcula las fuerzas gravitacionales entre partículas
     */
    protected void calculateForces() {
        // Resetear fuerzas
        for (Particle p : particles) {
            p.resetForce();
        }

        // Calcular fuerzas por pares
        for (int i = 0; i < particles.size(); i++) {
            for (int j = i + 1; j < particles.size(); j++) {
                Particle pi = particles.get(i);
                Particle pj = particles.get(j);

                Vector3D force = calculateForceFromP1ToP2(
                    pi.getPosition(), pi.getMass(),
                    pj.getPosition(), pj.getMass(),
                    G, H
                );

                pi.addForce(force);
                pj.addForce(force.negate());
            }
        }
    }

    /**
     * Calcula la fuerza gravitacional entre dos partículas
     */
    private Vector3D calculateForceFromP1ToP2(Vector3D pos1, double m1,
                                              Vector3D pos2, double m2,
                                              double G, double h) {
        Vector3D r = pos2.subtract(pos1);
        double r_norm = r.getNorm();
        double r_soft = Math.sqrt(r_norm * r_norm + h * h);
        double denominator = r_soft * r_soft * r_soft;
        double forceMag = -G * m1 * m2 / denominator;

        return r.scalarMultiply(forceMag);
    }

    /**
     * Calcula la energía cinética total del sistema
     */
    protected double calculateKineticEnergy() {
        double totalKE = 0.0;
        for (Particle p : particles) {
            Vector3D velocity = p.getVelocity();
            double speedSquared = velocity.getNormSq();
            totalKE += 0.5 * p.getMass() * speedSquared;
        }
        return totalKE;
    }

    /**
     * Calcula la energía potencial total del sistema
     */
    protected double calculatePotentialEnergy() {
        double totalPE = 0.0;
        for (int i = 0; i < particles.size(); i++) {
            for (int j = i + 1; j < particles.size(); j++) {
                Particle pi = particles.get(i);
                Particle pj = particles.get(j);

                Vector3D r = pj.getPosition().subtract(pi.getPosition());
                double distance = Math.sqrt(r.getNormSq() + H * H);
                totalPE += -G * pi.getMass() * pj.getMass() / distance;
            }
        }
        return totalPE;
    }

    /**
     * Calcula la energía total del sistema
     */
    protected double calculateTotalEnergy() {
        return calculateKineticEnergy() + calculatePotentialEnergy();
    }

    /**
     * Simula un paso de integración completo
     */
    protected void performIntegrationStep(IIntegrator integrator) {
        // 1. Actualizar posiciones
        integrator.updatePositions(particles, DT);

        // 2. Calcular nuevas fuerzas
        calculateForces();

        // 3. Actualizar velocidades
        integrator.updateVelocities(particles, DT);
    }

    /**
     * Crea una partícula oscilador armónico simple para tests analíticos
     */
    protected Particle createHarmonicOscillator() {
        // Partícula en x=1, con velocidad 0, bajo fuerza F = -kx con k=1
        return new Particle(0, new Vector3D(1, 0, 0), Vector3D.ZERO);
    }

    /**
     * Calcula la fuerza del oscilador armónico F = -kx
     */
    protected void setHarmonicOscillatorForce(Particle p, double k) {
        Vector3D force = p.getPosition().scalarMultiply(-k * p.getMass());
        p.resetForce();
        p.addForce(force);

        Vector3D acceleration = force.scalarMultiply(1.0 / p.getMass());
        p.setAcceleration(acceleration);
    }
}