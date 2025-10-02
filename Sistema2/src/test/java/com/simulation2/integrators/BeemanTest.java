package com.simulation2.integrators;

import com.simulation2.models.Particle;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests específicos para el integrador Beeman
 */
@DisplayName("Beeman Integrator Tests")
class BeemanTest extends IntegratorTestBase {

    private Beeman beeman;

    @BeforeEach
    void setUpBeeman() {
        beeman = new Beeman(1.0, 0.0); // k=1, gamma=0 (sin fricción)
    }

    @Test
    @DisplayName("Debe conservar energía en sistema conservativo")
    void testEnergyConservationTwoBody() {
        double initialEnergy = calculateTotalEnergy();

        // Ejecutar pasos de integración
        for (int i = 0; i < 1000; i++) {
            performIntegrationStep(beeman);
        }

        double finalEnergy = calculateTotalEnergy();
        double energyError = Math.abs((finalEnergy - initialEnergy) / initialEnergy);

        assertTrue(energyError < ENERGY_TOLERANCE,
            String.format("Error de energía demasiado grande: %.2e%% (tolerancia: %.2e%%)",
                energyError * 100, ENERGY_TOLERANCE * 100));
    }

    @Test
    @DisplayName("Debe manejar correctamente el primer paso sin aceleración previa")
    void testFirstStepWithoutPreviousAcceleration() {
        // Crear nueva partícula sin aceleración previa
        Particle p = new Particle(0, new Vector3D(1, 0, 0), new Vector3D(0.1, 0, 0));
        p.resetForce();
        p.addForce(new Vector3D(-1, 0, 0)); // Fuerza constante

        Vector3D initialAcceleration = p.getForce().scalarMultiply(1.0 / p.getMass());
        p.setAcceleration(initialAcceleration);
        // Deliberadamente NO establecer oldAcceleration

        Vector3D initialPos = p.getPosition();
        Vector3D initialVel = p.getVelocity();

        // Ejecutar primer paso
        beeman.updatePositions(Arrays.asList(p), DT);

        // La posición debe haber cambiado de manera razonable
        Vector3D newPos = p.getPosition();
        double displacement = newPos.distance(initialPos);

        assertTrue(displacement > 0, "La partícula debe moverse");
        assertTrue(displacement < 1.0, "El desplazamiento debe ser razonable para dt pequeño");
    }

    @Test
    @DisplayName("Debe usar aceleración previa correctamente en pasos subsecuentes")
    void testUsesOldAcceleration() {
        Particle p = particles.get(0);

        // Ejecutar un paso para establecer aceleración previa
        performIntegrationStep(beeman);
        Vector3D oldAcceleration = p.getOldAcceleration();

        assertNotNull(oldAcceleration, "La aceleración previa debe estar establecida");

        // Cambiar la fuerza para el siguiente paso
        p.resetForce();
        p.addForce(new Vector3D(1, 1, 0)); // Nueva fuerza diferente

        Vector3D positionBefore = p.getPosition();
        beeman.updatePositions(Arrays.asList(p), DT);
        Vector3D positionAfter = p.getPosition();

        // La posición debe cambiar basándose en la combinación de aceleraciones
        assertNotEquals(positionBefore, positionAfter,
            "La posición debe cambiar con el algoritmo Beeman");
    }

    @Test
    @DisplayName("Debe conservar momento lineal")
    void testLinearMomentumConservation() {
        Vector3D initialMomentum = Vector3D.ZERO;
        for (Particle p : particles) {
            initialMomentum = initialMomentum.add(p.getVelocity().scalarMultiply(p.getMass()));
        }

        for (int i = 0; i < 100; i++) {
            performIntegrationStep(beeman);
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
    @DisplayName("Debe tener precisión correcta para oscilador armónico")
    void testHarmonicOscillatorAccuracy() {
        Particle oscillator = createHarmonicOscillator();
        double k = 1.0;
        double omega = Math.sqrt(k / oscillator.getMass());

        // Establecer aceleración inicial
        setHarmonicOscillatorForce(oscillator, k);

        double t = DT * 50;
        Vector3D expectedPos = new Vector3D(Math.cos(omega * t), 0, 0);

        for (int i = 0; i < 50; i++) {
            setHarmonicOscillatorForce(oscillator, k);
            beeman.updatePositions(Arrays.asList(oscillator), DT);
            setHarmonicOscillatorForce(oscillator, k);
            beeman.updateVelocities(Arrays.asList(oscillator), DT);
        }

        double posError = oscillator.getPosition().distance(expectedPos);

        // Beeman debería tener buena precisión para oscilador armónico
        assertTrue(posError < 1e-3,
            String.format("Error de posición para oscilador armónico: %.2e", posError));
    }

    @Test
    @DisplayName("Debe ser estable numéricamente")
    void testNumericalStability() {
        double initialEnergy = calculateTotalEnergy();

        for (int i = 0; i < 5000; i++) {
            performIntegrationStep(beeman);

            // Verificar que no hay crecimiento explosivo
            for (Particle p : particles) {
                assertTrue(p.getPosition().getNorm() < 50,
                    "La posición creció demasiado, posible inestabilidad");
                assertTrue(p.getVelocity().getNorm() < 5,
                    "La velocidad creció demasiado, posible inestabilidad");
            }
        }

        double finalEnergy = calculateTotalEnergy();
        double energyDrift = Math.abs((finalEnergy - initialEnergy) / initialEnergy);

        assertTrue(energyDrift < 0.1,
            String.format("Drift de energía excesivo: %.4f%%", energyDrift * 100));
    }

    @Test
    @DisplayName("Debe actualizar velocidades usando predictor-corrector")
    void testVelocityUpdate() {
        Particle p = particles.get(0);
        Vector3D oldVelocity = p.getVelocity();
        Vector3D oldAcceleration = p.getAcceleration();

        // Ejecutar un paso completo
        performIntegrationStep(beeman);

        Vector3D newVelocity = p.getVelocity();
        Vector3D newAcceleration = p.getAcceleration();

        // La velocidad debe cambiar
        assertNotEquals(oldVelocity, newVelocity,
            "La velocidad debe cambiar después de la integración");

        // La nueva aceleración debe estar establecida
        assertNotNull(newAcceleration, "La nueva aceleración debe estar establecida");

        // La aceleración antigua debe estar guardada
        assertNotNull(p.getOldAcceleration(), "La aceleración antigua debe estar guardada");
    }
}