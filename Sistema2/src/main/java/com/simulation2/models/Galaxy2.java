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
    public static Particle[] initializeStarsNoSuperposition(int galaxyId, int starOffset, int numberOfStars, Vector3D centerPosition, Vector3D initialGalaxyVelocity) {

        Particle[] stars = new Particle[numberOfStars];

        for (int i = 0; i < numberOfStars; i++) {

            Vector3D position;
            boolean validPosition;
            int maxAttempts = 5000;
            int attempts = 0;
            double minDistance = 0.05;

            do {
                double x = centerPosition.getX() + random.nextGaussian();
                double y = centerPosition.getY() + random.nextGaussian();
                double z = centerPosition.getZ() + random.nextGaussian();
                position = new Vector3D(x, y, z);

                validPosition = true;
                for (int j = 0; j < i; j++) {
                    if (position.distance(stars[j].getPosition()) < minDistance) {
                        validPosition = false;
                        break;
                    }
                }

                attempts++;

                if (!validPosition && attempts >= maxAttempts) {
                    logger.warn("Se superaron {} intentos para la partícula {}. Cambiando a distribución esférica uniforme.", maxAttempts, i);
                    position = generateRandomPositionInSphere(centerPosition, 3.0);

                    validPosition = true;
                    for (int j = 0; j < i; j++) {
                        if (position.distance(stars[j].getPosition()) < minDistance) {
                            validPosition = false;
                            break;
                        }
                    }

                    if (!validPosition) {
                        logger.error("No se pudo encontrar una posición sin superposición para la partícula {}. Se procederá con la última posición.", i);
                        validPosition = true;
                    }
                }

            } while (!validPosition);

            Vector3D randomDirection = generateRandomUnitVector();
            Vector3D velocity = randomDirection.scalarMultiply(INITIAL_STAR_VELOCITY_MAGNITUDE).add(initialGalaxyVelocity);

            stars[i] = new Particle(i + starOffset, galaxyId, position, velocity);
        }

        return stars;
    }

    public static Particle[] initializeStars(int galaxyId, int starOffset, int numberOfStars, Vector3D centerPosition, Vector3D initialGalaxyVelocity) {

    Particle[] stars = new Particle[numberOfStars];

    for (int i = 0; i < numberOfStars; i++) {
        // Posición aleatoria con distribución gaussiana centrada en centerPosition
        double x = centerPosition.getX() + random.nextGaussian();
        double y = centerPosition.getY() + random.nextGaussian();
        double z = centerPosition.getZ() + random.nextGaussian();
        Vector3D position = new Vector3D(x, y, z);

        // Velocidad con dirección aleatoria y magnitud fija
        Vector3D randomDirection = generateRandomUnitVector();
        Vector3D velocity = randomDirection.scalarMultiply(INITIAL_STAR_VELOCITY_MAGNITUDE)
                                          .add(initialGalaxyVelocity);

        stars[i] = new Particle(i + starOffset, galaxyId, position, velocity);
    }

    return stars;
}

    /**
     * Genera un vector unitario con dirección aleatoria y uniforme.
     */
    private static Vector3D generateRandomUnitVector() {
        double x = random.nextGaussian();
        double y = random.nextGaussian();
        double z = random.nextGaussian();
        // Al normalizar un vector de 3 componentes gaussianas, la dirección resultante
        // es uniformemente aleatoria en la superficie de una esfera.
        return new Vector3D(x, y, z).normalize();
    }

    /**
     * Genera una posición aleatoria dentro de una esfera con distribución uniforme por volumen.
     *
     * @param center El centro de la esfera.
     * @param radius El radio de la esfera.
     * @return Un Vector3D con la posición aleatoria.
     */
    private static Vector3D generateRandomPositionInSphere(Vector3D center, double radius) {
        Vector3D direction = generateRandomUnitVector();
        double randomRadius = radius * Math.cbrt(random.nextDouble());
        return center.add(direction.scalarMultiply(randomRadius));
    }
}
