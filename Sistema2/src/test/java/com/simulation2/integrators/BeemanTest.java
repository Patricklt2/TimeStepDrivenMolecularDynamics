package com.simulation2.integrators;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.simulation2.models.Particle;

class BeemanTest {

    private Beeman integrator;
    private static final double EPSILON = 1e-3;

    @BeforeEach
    void setUp() {
        integrator = new Beeman();
    }

    /**
     * Test 1: Movimiento rectilíneo uniforme (sin fuerzas)
     * Una partícula con velocidad inicial constante debe moverse sin aceleración
     */
    @Test
    void testUniformMotion() {
        List<Particle> particles = new ArrayList<>();
        Particle p = new Particle(1, Vector3D.ZERO, new Vector3D(1.0, 0.0, 0.0));
        particles.add(p);

        // Sin fuerzas
        ForceCalculator noForce = (pList) -> {
            for (Particle particle : pList) {
                particle.resetForce();
            }
        };

        double dt = 1e-4;
        double totalTime = 1.0;
        int steps = (int) (totalTime / dt);

        // Inicializar aceleración previa para Beeman
        p.setOldAcceleration(Vector3D.ZERO);

        for (int i = 0; i < steps; i++) {
            integrator.step(particles, dt, noForce);
        }

        // Posición esperada: x = v*t = 1.0 * 1.0 = 1.0
        assertEquals(1.0, p.getPosition().getX(), EPSILON);
        assertEquals(0.0, p.getPosition().getY(), EPSILON);
        assertEquals(0.0, p.getPosition().getZ(), EPSILON);

        // Velocidad debe mantenerse constante
        assertEquals(1.0, p.getVelocity().getX(), EPSILON);
        assertEquals(0.0, p.getVelocity().getY(), EPSILON);
        assertEquals(0.0, p.getVelocity().getZ(), EPSILON);
    }

    /**
     * Test 2: Oscilador armónico simple en 1D
     * x'' = -k*x con k=1, solución analítica: x(t) = A*cos(ωt + φ)
     */
    @Test
    void testSimpleHarmonicOscillator() {
        List<Particle> particles = new ArrayList<>();

        // Condiciones iniciales: x0 = 1.0, v0 = 0.0
        double x0 = 1.0;
        Particle p = new Particle(1, new Vector3D(x0, 0.0, 0.0), Vector3D.ZERO);
        particles.add(p);

        // Fuerza de resorte: F = -k*x con k = 1
        double k = 1.0;
        ForceCalculator springForce = (pList) -> {
            for (Particle particle : pList) {
                particle.resetForce();
                Vector3D force = particle.getPosition().scalarMultiply(-k);
                particle.addForce(force);
            }
        };

        double dt = 1e-4;
        double period = 2 * Math.PI; // Período para ω = sqrt(k/m) = 1
        int stepsPerPeriod = (int) (period / dt);

        // Inicializar aceleración previa
        springForce.calculateForces(particles);
        p.updateAcceleration();
        p.setOldAcceleration(p.getAcceleration());

        // Simular un período completo
        for (int i = 0; i < stepsPerPeriod; i++) {
            integrator.step(particles, dt, springForce);
        }

        // Después de un período completo, debe volver a la posición inicial
        assertEquals(x0, p.getPosition().getX(), 0.01);
        assertEquals(0.0, p.getVelocity().getX(), 0.01);
    }

    /**
     * Test 3: Conservación de energía en oscilador armónico
     * E = (1/2)*m*v^2 + (1/2)*k*x^2 debe mantenerse constante
     */
    @Test
    void testEnergyConservation() {
        List<Particle> particles = new ArrayList<>();

        double x0 = 1.0;
        Particle p = new Particle(1, new Vector3D(x0, 0.0, 0.0), Vector3D.ZERO);
        particles.add(p);

        double k = 1.0;
        ForceCalculator springForce = (pList) -> {
            for (Particle particle : pList) {
                particle.resetForce();
                Vector3D force = particle.getPosition().scalarMultiply(-k);
                particle.addForce(force);
            }
        };

        // Inicializar aceleración previa
        springForce.calculateForces(particles);
        p.updateAcceleration();
        p.setOldAcceleration(p.getAcceleration());

        // Energía inicial
        double kineticEnergy0 = p.getKineticEnergy();
        double potentialEnergy0 = 0.5 * k * p.getPosition().getNormSq();
        double totalEnergy0 = kineticEnergy0 + potentialEnergy0;

        double dt = 1e-4;
        int steps = 1000;

        // Simular y verificar energía en varios puntos
        for (int i = 0; i < steps; i++) {
            integrator.step(particles, dt, springForce);

            double kineticEnergy = p.getKineticEnergy();
            double potentialEnergy = 0.5 * k * p.getPosition().getNormSq();
            double totalEnergy = kineticEnergy + potentialEnergy;

            // La energía total debe conservarse (con pequeña tolerancia por errores numéricos)
            assertEquals(totalEnergy0, totalEnergy, 0.05);
        }
    }

    /**
     * Test 4: Caída libre con gravedad constante
     * y(t) = y0 + v0*t + (1/2)*g*t^2
     */
    @Test
    void testConstantAcceleration() {
        List<Particle> particles = new ArrayList<>();

        double y0 = 100.0;
        double v0 = 0.0;
        Particle p = new Particle(1, new Vector3D(0.0, y0, 0.0), new Vector3D(0.0, v0, 0.0));
        particles.add(p);

        double g = 9.81;
        ForceCalculator gravity = (pList) -> {
            for (Particle particle : pList) {
                particle.resetForce();
                Vector3D force = new Vector3D(0.0, -g * particle.getMass(), 0.0);
                particle.addForce(force);
            }
        };

        // Inicializar aceleración previa
        gravity.calculateForces(particles);
        p.updateAcceleration();
        p.setOldAcceleration(p.getAcceleration());

        double dt = 1e-4;
        double t = 1.0;
        int steps = (int) (t / dt);

        for (int i = 0; i < steps; i++) {
            integrator.step(particles, dt, gravity);
        }

        // Posición esperada: y = y0 + v0*t - (1/2)*g*t^2
        double expectedY = y0 + v0 * t - 0.5 * g * t * t;
        assertEquals(expectedY, p.getPosition().getY(), 0.1);

        // Velocidad esperada: v = v0 - g*t
        double expectedVy = v0 - g * t;
        assertEquals(expectedVy, p.getVelocity().getY(), 0.1);
    }

    /**
     * Test 5: Movimiento circular uniforme (fuerza centrípeta)
     * Para v = ω*r, la fuerza centrípeta F = m*ω^2*r debe mantener órbita circular
     */
    @Test
    void testCircularMotion() {
        List<Particle> particles = new ArrayList<>();

        double r = 1.0;
        double omega = 1.0;

        // Posición inicial en (r, 0, 0) con velocidad tangencial (0, ω*r, 0)
        Particle p = new Particle(1, new Vector3D(r, 0.0, 0.0), new Vector3D(0.0, omega * r, 0.0));
        particles.add(p);

        // Fuerza centrípeta apuntando hacia el centro
        ForceCalculator centripetalForce = (pList) -> {
            for (Particle particle : pList) {
                particle.resetForce();
                Vector3D position = particle.getPosition();
                double forceMagnitude = particle.getMass() * omega * omega * position.getNorm();
                Vector3D force = position.normalize().scalarMultiply(-forceMagnitude);
                particle.addForce(force);
            }
        };

        // Inicializar aceleración previa
        centripetalForce.calculateForces(particles);
        p.updateAcceleration();
        p.setOldAcceleration(p.getAcceleration());

        double dt = 1e-4;
        double period = 2 * Math.PI / omega;
        int stepsPerPeriod = (int) (period / dt);

        // Simular un período completo
        for (int i = 0; i < stepsPerPeriod; i++) {
            integrator.step(particles, dt, centripetalForce);
        }

        // El radio debe mantenerse constante (aproximadamente)
        double finalRadius = p.getPosition().getNorm();
        assertEquals(r, finalRadius, 0.05);

        // Después de un período, debe volver aproximadamente a la posición inicial
        assertEquals(r, p.getPosition().getX(), 0.05);
        assertEquals(0.0, p.getPosition().getY(), 0.05);
    }
}
