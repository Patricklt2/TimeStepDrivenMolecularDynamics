package com.simulation2.integrators;

import com.simulation2.models.Particle;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests específicos para verificar la conservación de energía
 * en diferentes integradores y configuraciones
 */
@DisplayName("Energy Conservation Tests")
class EnergyConservationTest extends IntegratorTestBase {

    /**
     * Proporciona diferentes integradores para tests parametrizados
     */
    static Stream<IIntegrator> integrators() {
        return Stream.of(
            new VelocityVerlet(),
            new Beeman(1.0, 0.0)
        );
    }

    @ParameterizedTest
    @MethodSource("integrators")
    @DisplayName("Conservación de energía en sistema de dos cuerpos")
    void testEnergyConservationTwoBody(IIntegrator integrator) {
        double initialEnergy = calculateTotalEnergy();

        // Ejecutar simulación
        for (int i = 0; i < 1000; i++) {
            performIntegrationStep(integrator);
        }

        double finalEnergy = calculateTotalEnergy();
        double relativeError = Math.abs((finalEnergy - initialEnergy) / initialEnergy);

        assertTrue(relativeError < ENERGY_TOLERANCE,
            String.format("Integrador %s: Error de energía %.2e%% > tolerancia %.2e%%",
                integrator.getClass().getSimpleName(),
                relativeError * 100,
                ENERGY_TOLERANCE * 100));
    }

    @ParameterizedTest
    @MethodSource("integrators")
    @DisplayName("Conservación de energía con diferentes pasos de tiempo")
    void testEnergyConservationDifferentTimesteps(IIntegrator integrator) {
        double[] timesteps = {1e-5, 5e-5, 1e-4, 5e-4};

        for (double dt : timesteps) {
            // Reinicializar sistema
            setupParticles();
            double initialEnergy = calculateTotalEnergy();

            // Simular con dt específico
            int steps = (int)(0.1 / dt); // Simular hasta t=0.1
            for (int i = 0; i < steps; i++) {
                integrator.updatePositions(particles, dt);
                calculateForces();
                integrator.updateVelocities(particles, dt);
            }

            double finalEnergy = calculateTotalEnergy();
            double relativeError = Math.abs((finalEnergy - initialEnergy) / initialEnergy);

            assertTrue(relativeError < ENERGY_TOLERANCE * 10,
                String.format("Integrador %s con dt=%.1e: Error de energía %.2e%%",
                    integrator.getClass().getSimpleName(),
                    dt,
                    relativeError * 100));
        }
    }

    @Test
    @DisplayName("Conservación de energía en sistema de múltiples partículas")
    void testEnergyConservationMultipleParticles() {
        // Crear sistema de 4 partículas
        List<Particle> multiSystem = Arrays.asList(
            new Particle(0, new Vector3D(0, 0, 0), new Vector3D(0.1, 0, 0)),
            new Particle(1, new Vector3D(1, 0, 0), new Vector3D(-0.1, 0, 0)),
            new Particle(2, new Vector3D(0, 1, 0), new Vector3D(0, -0.1, 0)),
            new Particle(3, new Vector3D(1, 1, 0), new Vector3D(0, 0.1, 0))
        );

        // Inicializar fuerzas y aceleraciones
        calculateForcesForSystem(multiSystem);
        for (Particle p : multiSystem) {
            Vector3D acceleration = p.getForce().scalarMultiply(1.0 / p.getMass());
            p.setAcceleration(acceleration);
            p.setOldAcceleration(acceleration);
        }

        double initialEnergy = calculateEnergyForSystem(multiSystem);

        VelocityVerlet integrator = new VelocityVerlet();

        // Simular sistema de múltiples partículas
        for (int i = 0; i < 500; i++) {
            integrator.updatePositions(multiSystem, DT);
            calculateForcesForSystem(multiSystem);
            integrator.updateVelocities(multiSystem, DT);
        }

        double finalEnergy = calculateEnergyForSystem(multiSystem);
        double relativeError = Math.abs((finalEnergy - initialEnergy) / initialEnergy);

        assertTrue(relativeError < ENERGY_TOLERANCE * 5,
            String.format("Error de energía en sistema múltiple: %.2e%%", relativeError * 100));
    }

    @Test
    @DisplayName("Verificar que energía cinética y potencial cambian pero total se conserva")
    void testEnergyComponentsEvolution() {
        double initialKE = calculateKineticEnergy();
        double initialPE = calculatePotentialEnergy();
        double initialTotal = initialKE + initialPE;

        VelocityVerlet integrator = new VelocityVerlet();

        // Arrays para rastrear la evolución
        double[] kineticEnergies = new double[100];
        double[] potentialEnergies = new double[100];
        double[] totalEnergies = new double[100];

        for (int i = 0; i < 100; i++) {
            performIntegrationStep(integrator);

            kineticEnergies[i] = calculateKineticEnergy();
            potentialEnergies[i] = calculatePotentialEnergy();
            totalEnergies[i] = kineticEnergies[i] + potentialEnergies[i];
        }

        // Verificar que las componentes cambian
        boolean keChanged = Math.abs(kineticEnergies[99] - initialKE) > TOLERANCE;
        boolean peChanged = Math.abs(potentialEnergies[99] - initialPE) > TOLERANCE;

        assertTrue(keChanged, "La energía cinética debe cambiar durante la evolución");
        assertTrue(peChanged, "La energía potencial debe cambiar durante la evolución");

        // Verificar conservación de energía total
        for (int i = 0; i < 100; i++) {
            double relativeError = Math.abs((totalEnergies[i] - initialTotal) / initialTotal);
            assertTrue(relativeError < ENERGY_TOLERANCE,
                String.format("Error de energía en paso %d: %.2e%%", i, relativeError * 100));
        }
    }

    @Test
    @DisplayName("Verificar conservación en trayectorias cercanas a colisión")
    void testEnergyConservationNearCollision() {
        // Configurar partículas que se acercan mucho
        Particle p1 = new Particle(0, new Vector3D(-0.5, 0, 0), new Vector3D(1, 0, 0));
        Particle p2 = new Particle(1, new Vector3D(0.5, 0, 0), new Vector3D(-1, 0, 0));

        List<Particle> collisionSystem = Arrays.asList(p1, p2);

        // Inicializar
        calculateForcesForSystem(collisionSystem);
        for (Particle p : collisionSystem) {
            Vector3D acceleration = p.getForce().scalarMultiply(1.0 / p.getMass());
            p.setAcceleration(acceleration);
            p.setOldAcceleration(acceleration);
        }

        double initialEnergy = calculateEnergyForSystem(collisionSystem);

        VelocityVerlet integrator = new VelocityVerlet();

        // Simular hasta que las partículas se acerquen y se alejen
        for (int i = 0; i < 1000; i++) {
            integrator.updatePositions(collisionSystem, DT * 0.1); // dt más pequeño
            calculateForcesForSystem(collisionSystem);
            integrator.updateVelocities(collisionSystem, DT * 0.1);
        }

        double finalEnergy = calculateEnergyForSystem(collisionSystem);
        double relativeError = Math.abs((finalEnergy - initialEnergy) / initialEnergy);

        assertTrue(relativeError < ENERGY_TOLERANCE * 10,
            String.format("Error de energía en encuentro cercano: %.2e%%", relativeError * 100));
    }

    @Test
    @DisplayName("Verificar conservación con condiciones iniciales aleatorias")
    void testEnergyConservationRandomInitialConditions() {
        VelocityVerlet integrator = new VelocityVerlet();

        // Probar múltiples configuraciones aleatorias
        for (int trial = 0; trial < 10; trial++) {
            // Crear sistema aleatorio
            List<Particle> randomSystem = createRandomSystem(trial);

            double initialEnergy = calculateEnergyForSystem(randomSystem);

            // Simular
            for (int i = 0; i < 500; i++) {
                integrator.updatePositions(randomSystem, DT);
                calculateForcesForSystem(randomSystem);
                integrator.updateVelocities(randomSystem, DT);
            }

            double finalEnergy = calculateEnergyForSystem(randomSystem);
            double relativeError = Math.abs((finalEnergy - initialEnergy) / initialEnergy);

            assertTrue(relativeError < ENERGY_TOLERANCE * 5,
                String.format("Trial %d: Error de energía %.2e%%", trial, relativeError * 100));
        }
    }

    // Métodos auxiliares

    private void calculateForcesForSystem(List<Particle> system) {
        for (Particle p : system) {
            p.resetForce();
        }

        for (int i = 0; i < system.size(); i++) {
            for (int j = i + 1; j < system.size(); j++) {
                Particle pi = system.get(i);
                Particle pj = system.get(j);

                Vector3D r = pj.getPosition().subtract(pi.getPosition());
                double r_norm = r.getNorm();
                double r_soft = Math.sqrt(r_norm * r_norm + H * H);
                double denominator = r_soft * r_soft * r_soft;
                double forceMag = -G * pi.getMass() * pj.getMass() / denominator;

                Vector3D force = r.scalarMultiply(forceMag);
                pi.addForce(force);
                pj.addForce(force.negate());
            }
        }
    }

    private double calculateEnergyForSystem(List<Particle> system) {
        double ke = 0.0;
        for (Particle p : system) {
            ke += 0.5 * p.getMass() * p.getVelocity().getNormSq();
        }

        double pe = 0.0;
        for (int i = 0; i < system.size(); i++) {
            for (int j = i + 1; j < system.size(); j++) {
                Particle pi = system.get(i);
                Particle pj = system.get(j);
                Vector3D r = pj.getPosition().subtract(pi.getPosition());
                double distance = Math.sqrt(r.getNormSq() + H * H);
                pe += -G * pi.getMass() * pj.getMass() / distance;
            }
        }

        return ke + pe;
    }

    private List<Particle> createRandomSystem(int seed) {
        // Usar seed para reproducibilidad
        java.util.Random random = new java.util.Random(seed);

        Particle p1 = new Particle(0,
            new Vector3D(random.nextGaussian(), random.nextGaussian(), 0),
            new Vector3D(random.nextGaussian() * 0.1, random.nextGaussian() * 0.1, 0));

        Particle p2 = new Particle(1,
            new Vector3D(random.nextGaussian(), random.nextGaussian(), 0),
            new Vector3D(random.nextGaussian() * 0.1, random.nextGaussian() * 0.1, 0));

        List<Particle> system = Arrays.asList(p1, p2);

        // Inicializar fuerzas y aceleraciones
        calculateForcesForSystem(system);
        for (Particle p : system) {
            Vector3D acceleration = p.getForce().scalarMultiply(1.0 / p.getMass());
            p.setAcceleration(acceleration);
            p.setOldAcceleration(acceleration);
        }

        return system;
    }
}