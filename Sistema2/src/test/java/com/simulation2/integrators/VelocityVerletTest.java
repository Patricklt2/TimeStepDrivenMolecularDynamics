package com.simulation2.integrators;

import com.simulation2.models.Particle;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests específicos para el integrador Velocity-Verlet
 */
@DisplayName("Velocity-Verlet Integrator Tests")
class VelocityVerletTest extends IntegratorTestBase {

    private VelocityVerlet velocityVerlet;

    @BeforeEach
    void setUpVelocityVerlet() {
        velocityVerlet = new VelocityVerlet();
    }

    @Test
    @DisplayName("Debe conservar energía en sistema de dos cuerpos")
    void testEnergyConservationTwoBody() {
        double initialEnergy = calculateTotalEnergy();

        // Ejecutar 1000 pasos de integración
        for (int i = 0; i < 1000; i++) {
            performIntegrationStep(velocityVerlet);
        }

        double finalEnergy = calculateTotalEnergy();
        double energyError = Math.abs((finalEnergy - initialEnergy) / initialEnergy);

        assertTrue(energyError < ENERGY_TOLERANCE,
            String.format("Error de energía demasiado grande: %.2e%% (tolerancia: %.2e%%)",
                energyError * 100, ENERGY_TOLERANCE * 100));
    }

    @Test
    @DisplayName("Debe conservar momento lineal")
    void testLinearMomentumConservation() {
        Vector3D initialMomentum = Vector3D.ZERO;
        for (Particle p : particles) {
            initialMomentum = initialMomentum.add(p.getVelocity().scalarMultiply(p.getMass()));
        }

        // Ejecutar varios pasos
        for (int i = 0; i < 100; i++) {
            performIntegrationStep(velocityVerlet);
        }

        Vector3D finalMomentum = Vector3D.ZERO;
        for (Particle p : particles) {
            finalMomentum = finalMomentum.add(p.getVelocity().scalarMultiply(p.getMass()));
        }

        double momentumError = finalMomentum.distance(initialMomentum);
        assertTrue(momentumError < TOLERANCE,
            String.format("Error en conservación de momento: %.2e", momentumError));
    }

    @Test
    @DisplayName("Debe ser reversible en el tiempo")
    void testTimeReversibility() {
        // Guardar estado inicial
        Vector3D initialPos = particles.get(0).getPosition();
        Vector3D initialVel = particles.get(0).getVelocity();

        // Ejecutar pasos hacia adelante
        for (int i = 0; i < 100; i++) {
            performIntegrationStep(velocityVerlet);
        }

        // Invertir velocidades
        for (Particle p : particles) {
            p.setVelocity(p.getVelocity().negate());
        }

        // Ejecutar mismo número de pasos hacia atrás
        for (int i = 0; i < 100; i++) {
            performIntegrationStep(velocityVerlet);
        }

        // Invertir velocidades de nuevo para comparar
        for (Particle p : particles) {
            p.setVelocity(p.getVelocity().negate());
        }

        Vector3D finalPos = particles.get(0).getPosition();
        Vector3D finalVel = particles.get(0).getVelocity();

        double posError = finalPos.distance(initialPos);
        double velError = finalVel.distance(initialVel);

        assertTrue(posError < TOLERANCE,
            String.format("Error en reversibilidad de posición: %.2e", posError));
        assertTrue(velError < TOLERANCE,
            String.format("Error en reversibilidad de velocidad: %.2e", velError));
    }

    @Test
    @DisplayName("Debe tener orden de precisión correcto para oscilador armónico")
    void testOrderOfAccuracyHarmonicOscillator() {
        // Configurar oscilador armónico simple
        Particle oscillator = createHarmonicOscillator();
        double k = 1.0; // constante de resorte
        double omega = Math.sqrt(k / oscillator.getMass());

        // Solución analítica: x(t) = cos(ωt), v(t) = -ω*sin(ωt)
        double t = DT * 100; // tiempo después de 100 pasos

        Vector3D expectedPos = new Vector3D(Math.cos(omega * t), 0, 0);
        Vector3D expectedVel = new Vector3D(-omega * Math.sin(omega * t), 0, 0);

        // Simular con Velocity-Verlet
        for (int i = 0; i < 100; i++) {
            setHarmonicOscillatorForce(oscillator, k);
            velocityVerlet.updatePositions(Arrays.asList(oscillator), DT);
            setHarmonicOscillatorForce(oscillator, k);
            velocityVerlet.updateVelocities(Arrays.asList(oscillator), DT);
        }

        double posError = oscillator.getPosition().distance(expectedPos);
        double velError = oscillator.getVelocity().distance(expectedVel);

        // Velocity-Verlet debería tener error O(dt^4) para oscilador armónico
        double expectedError = Math.pow(DT, 4);

        assertTrue(posError < 10 * expectedError,
            String.format("Error de posición mayor al esperado: %.2e (esperado: %.2e)",
                posError, expectedError));
        assertTrue(velError < 10 * expectedError,
            String.format("Error de velocidad mayor al esperado: %.2e (esperado: %.2e)",
                velError, expectedError));
    }

    @Test
    @DisplayName("Debe actualizar correctamente las aceleraciones")
    void testAccelerationUpdate() {
        Particle p = particles.get(0);
        Vector3D oldAcceleration = p.getAcceleration();

        // Ejecutar un paso
        performIntegrationStep(velocityVerlet);

        Vector3D newAcceleration = p.getAcceleration();

        // La aceleración debe haber cambiado (las partículas se mueven)
        double accelerationChange = newAcceleration.distance(oldAcceleration);
        assertTrue(accelerationChange > 0,
            "La aceleración debería cambiar cuando las partículas se mueven");
    }

    @Test
    @DisplayName("Debe mantener estabilidad numérica a largo plazo")
    void testLongTermStability() {
        double initialEnergy = calculateTotalEnergy();

        // Ejecutar muchos pasos para verificar estabilidad
        for (int i = 0; i < 10000; i++) {
            performIntegrationStep(velocityVerlet);

            // Verificar que las posiciones y velocidades no exploten
            for (Particle p : particles) {
                assertTrue(p.getPosition().getNorm() < 100,
                    "La posición creció demasiado, posible inestabilidad");
                assertTrue(p.getVelocity().getNorm() < 10,
                    "La velocidad creció demasiado, posible inestabilidad");
            }
        }

        double finalEnergy = calculateTotalEnergy();
        double energyDrift = Math.abs((finalEnergy - initialEnergy) / initialEnergy);

        // El drift de energía debe ser pequeño incluso después de muchos pasos
        assertTrue(energyDrift < 0.01,
            String.format("Drift de energía excesivo en simulación larga: %.4f%%", energyDrift * 100));
    }
}