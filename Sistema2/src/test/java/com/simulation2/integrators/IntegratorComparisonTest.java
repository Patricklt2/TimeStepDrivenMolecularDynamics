package com.simulation2.integrators;

import com.simulation2.models.Particle;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.BeforeEach;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests de comparación entre diferentes integradores
 */
@DisplayName("Integrator Comparison Tests")
class IntegratorComparisonTest extends IntegratorTestBase {

    private VelocityVerlet velocityVerlet;
    private Beeman beeman;

    @BeforeEach
    void setUpIntegrators() {
        velocityVerlet = new VelocityVerlet();
        beeman = new Beeman(1.0, 0.0);
    }

    @Test
    @DisplayName("Todos los integradores deben conservar energía similarmente")
    void testEnergyConservationComparison() {
        // Crear copias independientes del sistema para cada integrador
        List<Particle> systemVV = createParticlesCopy();
        List<Particle> systemBeeman = createParticlesCopy();

        double initialEnergy = calculateTotalEnergyForSystem(systemVV);

        // Simular 1000 pasos con cada integrador
        for (int i = 0; i < 1000; i++) {
            performIntegrationStepForSystem(velocityVerlet, systemVV);
            performIntegrationStepForSystem(beeman, systemBeeman);
        }

        double finalEnergyVV = calculateTotalEnergyForSystem(systemVV);
        double finalEnergyBeeman = calculateTotalEnergyForSystem(systemBeeman);

        double errorVV = Math.abs((finalEnergyVV - initialEnergy) / initialEnergy);
        double errorBeeman = Math.abs((finalEnergyBeeman - initialEnergy) / initialEnergy);

        // Ambos deben conservar energía dentro de tolerancia
        assertTrue(errorVV < ENERGY_TOLERANCE,
            String.format("Velocity-Verlet error de energía: %.2e%%", errorVV * 100));
        assertTrue(errorBeeman < ENERGY_TOLERANCE,
            String.format("Beeman error de energía: %.2e%%", errorBeeman * 100));

        // Velocity-Verlet debería ser más preciso o similar
        assertTrue(errorVV <= errorBeeman * 10,
            String.format("Velocity-Verlet debería ser competitivo: VV=%.2e%%, Beeman=%.2e%%",
                errorVV * 100, errorBeeman * 100));
    }

    @Test
    @DisplayName("Comparar precisión en oscilador armónico")
    void testHarmonicOscillatorComparison() {
        // Crear osciladores independientes
        Particle oscillatorVV = createHarmonicOscillator();
        Particle oscillatorBeeman = createHarmonicOscillator();

        double k = 1.0;
        double omega = Math.sqrt(k / oscillatorVV.getMass());
        double t = DT * 100;

        Vector3D expectedPos = new Vector3D(Math.cos(omega * t), 0, 0);
        Vector3D expectedVel = new Vector3D(-omega * Math.sin(omega * t), 0, 0);

        // Simular con cada integrador
        for (int i = 0; i < 100; i++) {
            // Velocity-Verlet
            setHarmonicOscillatorForce(oscillatorVV, k);
            velocityVerlet.updatePositions(Arrays.asList(oscillatorVV), DT);
            setHarmonicOscillatorForce(oscillatorVV, k);
            velocityVerlet.updateVelocities(Arrays.asList(oscillatorVV), DT);

            // Beeman
            setHarmonicOscillatorForce(oscillatorBeeman, k);
            beeman.updatePositions(Arrays.asList(oscillatorBeeman), DT);
            setHarmonicOscillatorForce(oscillatorBeeman, k);
            beeman.updateVelocities(Arrays.asList(oscillatorBeeman), DT);
        }

        double errorVV = oscillatorVV.getPosition().distance(expectedPos);
        double errorBeeman = oscillatorBeeman.getPosition().distance(expectedPos);

        // Ambos deben tener errores razonables
        assertTrue(errorVV < 1e-2,
            String.format("Error Velocity-Verlet: %.2e", errorVV));
        assertTrue(errorBeeman < 1e-2,
            String.format("Error Beeman: %.2e", errorBeeman));

        System.out.printf("Precisión oscilador armónico - VV: %.2e, Beeman: %.2e%n",
            errorVV, errorBeeman);
    }

    @Test
    @DisplayName("Comparar estabilidad a largo plazo")
    void testLongTermStabilityComparison() {
        List<Particle> systemVV = createParticlesCopy();
        List<Particle> systemBeeman = createParticlesCopy();

        double initialEnergyVV = calculateTotalEnergyForSystem(systemVV);
        double initialEnergyBeeman = calculateTotalEnergyForSystem(systemBeeman);

        // Simulación muy larga
        for (int i = 0; i < 10000; i++) {
            performIntegrationStepForSystem(velocityVerlet, systemVV);
            performIntegrationStepForSystem(beeman, systemBeeman);
        }

        double finalEnergyVV = calculateTotalEnergyForSystem(systemVV);
        double finalEnergyBeeman = calculateTotalEnergyForSystem(systemBeeman);

        double driftVV = Math.abs((finalEnergyVV - initialEnergyVV) / initialEnergyVV);
        double driftBeeman = Math.abs((finalEnergyBeeman - initialEnergyBeeman) / initialEnergyBeeman);

        // El drift debe ser aceptable para ambos
        assertTrue(driftVV < 0.05,
            String.format("Drift Velocity-Verlet excesivo: %.4f%%", driftVV * 100));
        assertTrue(driftBeeman < 0.05,
            String.format("Drift Beeman excesivo: %.4f%%", driftBeeman * 100));

        System.out.printf("Drift energía (10k pasos) - VV: %.4f%%, Beeman: %.4f%%%n",
            driftVV * 100, driftBeeman * 100);
    }

    @Test
    @DisplayName("Verificar que las trayectorias son físicamente razonables")
    void testPhysicallyReasonableTrajectories() {
        List<Particle> systemVV = createParticlesCopy();
        List<Particle> systemBeeman = createParticlesCopy();

        // Medir distancia entre partículas al inicio
        double initialDistance = systemVV.get(0).getPosition().distance(systemVV.get(1).getPosition());

        for (int i = 0; i < 1000; i++) {
            performIntegrationStepForSystem(velocityVerlet, systemVV);
            performIntegrationStepForSystem(beeman, systemBeeman);

            // Las partículas no deben alejarse infinitamente
            double distVV = systemVV.get(0).getPosition().distance(systemVV.get(1).getPosition());
            double distBeeman = systemBeeman.get(0).getPosition().distance(systemBeeman.get(1).getPosition());

            assertTrue(distVV < initialDistance * 5,
                "Las partículas con VV no deben alejarse demasiado");
            assertTrue(distBeeman < initialDistance * 5,
                "Las partículas con Beeman no deben alejarse demasiado");
        }
    }

    // Métodos auxiliares para trabajar con sistemas independientes

    private List<Particle> createParticlesCopy() {
        Particle p1 = new Particle(0, new Vector3D(0, 0, 0), new Vector3D(0.1, 0, 0));
        Particle p2 = new Particle(1, new Vector3D(1, 0, 0), new Vector3D(-0.1, 0, 0));

        List<Particle> system = Arrays.asList(p1, p2);

        // Calcular fuerzas e inicializar aceleraciones
        calculateForcesForSystem(system);
        for (Particle p : system) {
            Vector3D acceleration = p.getForce().scalarMultiply(1.0 / p.getMass());
            p.setAcceleration(acceleration);
            p.setOldAcceleration(acceleration);
        }

        return system;
    }

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

    private double calculateTotalEnergyForSystem(List<Particle> system) {
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

    private void performIntegrationStepForSystem(IIntegrator integrator, List<Particle> system) {
        integrator.updatePositions(system, DT);
        calculateForcesForSystem(system);
        integrator.updateVelocities(system, DT);
    }
}