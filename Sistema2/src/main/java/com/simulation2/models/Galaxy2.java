package com.simulation2.models;

import com.simulation2.integrators.IIntegrator;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Galaxy2 {
    private static final Logger logger = LoggerFactory.getLogger(Galaxy2.class);
    private static final double INITIAL_STAR_VELOCITY_MAGNITUDE = 0.1; // velocidad inicial de las estrellas
    private static final Random random = new Random();

    private Galaxy2() {}

    /**
     * Inicializa las estrellas según los requisitos del enunciado y les
     * suma a cada una la velocidad neta de la galaxia
     */
    public static Particle[] initializeStars(int galaxyId, int particleOffset, int numStars, Vector3D centerPosition, Vector3D initialGalaxyVelocity) {
        Particle[] stars = new Particle[numStars];

        Particle p1 = new Particle(1, 1, new Vector3D(0, 0, 0), new Vector3D(0,0,0));
        Particle p2 = new Particle(2, 1, new Vector3D(0.2, 0, 0), new Vector3D(0,0,0));

        stars[0] = p1;
        stars[1] = p2;

        /*final double minDistance = 0.05;
        // Un radio "efectivo" para la galaxia, actúa como un multiplicador del desvío estándar.
        final double galaxyRadiusScale = 1;
        final int maxAttemptsPerStar = 5000;

        for (int i = 0; i < numStars; i++) {
            Vector3D position;
            boolean validPosition;
            int attempts = 0;

            do {
                // 1. Generar una dirección aleatoria y uniforme.
                Vector3D direction = generateRandomUnitVector();

                // 2. Generar un radio usando una distribución gaussiana.
                //    Usamos Math.abs() porque el radio no puede ser negativo.
                //    Multiplicamos por 'galaxyRadiusScale' para controlar la dispersión.
                double radius = Math.abs(random.nextGaussian() * galaxyRadiusScale);

                // 3. Calcular la posición final.
                position = centerPosition.add(direction.scalarMultiply(radius));

                // 4. Validar que no haya superposición (este paso es ahora mucho más rápido).
                validPosition = true;
                for (int j = 0; j < i; j++) {
                    if (position.distance(stars[j].getPosition()) < minDistance) {
                        validPosition = false;
                        break;
                    }
                }
                attempts++;

            } while (!validPosition && attempts < maxAttemptsPerStar);

            if (attempts >= maxAttemptsPerStar) {
                logger.warn("Could not find a valid non-overlapping position for star {} after {} attempts. " +
                                "The simulation might be unstable. Consider increasing galaxyRadiusScale or decreasing numberOfStars.",
                        i, maxAttemptsPerStar);
                // Si fallamos, aceptamos la última posición para no entrar en un bucle infinito.
            }

            // La inicialización de la velocidad no cambia.
            Vector3D randomDirectionForVelocity = generateRandomUnitVector();
            Vector3D velocity = randomDirectionForVelocity.scalarMultiply(INITIAL_STAR_VELOCITY_MAGNITUDE).add(initialGalaxyVelocity);

            stars[i] = new Particle(i+particleOffset, galaxyId, position, velocity);
        }*/

        return stars;
    }

    /**
     * Genera un vector unitario con dirección aleatoria
     */
    private static Vector3D generateRandomUnitVector() {
        // Genera tres números aleatorios de una distribución normal.
        double x = random.nextGaussian();
        double y = random.nextGaussian();
        double z = random.nextGaussian();

        Vector3D vector = new Vector3D(x, y, z);

        return vector.normalize();
    }
}
