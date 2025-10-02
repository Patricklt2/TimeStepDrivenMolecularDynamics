package com.simulation2.models;

import com.simulation2.integrators.IIntegrator;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class Galaxy2 {
    private static final Logger logger = LoggerFactory.getLogger(Galaxy.class);
    private final String name;
    private int numberOfStars;
    private Vector3D centerPosition; // cambia cuando se mueve la galaxia
    private Particle[] stars;

    private final double initialVelocity = 0.1; // velocidad inicial de las estrellas
    private final Random random = new Random();

    public Galaxy2(String name, int numberOfStars, Vector3D centerPosition) {
        this.name = name;
        this.numberOfStars = numberOfStars;
        this.centerPosition = centerPosition;
        initializeStars();
    }

    /** ----------------- for testing purposes ----------------- **/

    public void setStars(Particle[] stars){
        this.stars = stars;
    }


    /** ----------------- Getters ----------------- **/

    public String getName() {
        return name;
    }

    public Vector3D getCenterPosition() {
        return centerPosition;
    }

    public int getNumberOfStars() {
        return numberOfStars;
    }

    public Particle[] getStars() {
        return stars;
    }

    /** ----------------- Setters ----------------- **/

    public void setNumberOfStars(int numberOfStars) {
        this.numberOfStars = numberOfStars;
        initializeStars();
    }

    /** ----------------- Proper Methods ----------------- **/

    public void calculateNewCenterPosition() {
        Vector3D sumPositions = Vector3D.ZERO;
        for (Particle star : stars) {
            sumPositions = sumPositions.add(star.getPosition());
        }
        this.centerPosition = sumPositions.scalarMultiply(1.0 / numberOfStars);
    }

    /**
     * Calcula la velocidad del centro de masa de la galaxia
     * v_cm = (Σ m_i * v_i) / (Σ m_i)
     * Como todas las masas son unitarias: v_cm = (Σ v_i) / N
     */
    public Vector3D getCenterVelocity() {
        Vector3D sumVelocities = Vector3D.ZERO;
        for (Particle star : stars) {
            sumVelocities = sumVelocities.add(star.getVelocity());
        }
        return sumVelocities.scalarMultiply(1.0 / numberOfStars);
    }

    /**
     * Inicializa las estrellas según los requisitos del enunciado:
     * - Masa unitaria (mi = 1)
     * - Posiciones distribuidas normalmente con centro en el origen y desvío unitario
     * - Velocidades con dirección aleatoria y módulo |v| = 0.1
     * - Evita superposiciones iniciales para condiciones físicamente realistas
     *
     * A CHEQUEAR (creía que esto estaba mal, así que no darle bola)
     */
    public void initializeStars() {
        stars = new Particle[numberOfStars];

        final double minDistance = 0.1;
        // Un radio "efectivo" para la galaxia, actúa como un multiplicador del desvío estándar.
        final double galaxyRadiusScale = 1.5;
        final int maxAttemptsPerStar = 5000;

        for (int i = 0; i < numberOfStars; i++) {
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
            Vector3D velocity = randomDirectionForVelocity.scalarMultiply(initialVelocity);

            stars[i] = new Particle(i, position, velocity);
        }
    }

    /**
     * Genera un vector unitario con dirección aleatoria
     */
    private Vector3D generateRandomUnitVector() {
        // Genera tres números aleatorios de una distribución normal.
        double x = random.nextGaussian();
        double y = random.nextGaussian();
        double z = random.nextGaussian();

        Vector3D vector = new Vector3D(x, y, z);

        return vector.normalize();
    }

    /**
     * Método para mover toda la galaxia a una nueva posición
     * Útil para configurar colisiones entre galaxias
     */
    public void moveGalaxy(Vector3D displacement) {
        this.centerPosition = this.centerPosition.add(displacement);
        for (Particle star : stars) {
            Vector3D newPosition = star.getPosition().add(displacement);
            star.setPosition(newPosition);
        }
    }


    /**
     * Método para agregar una velocidad inicial a toda la galaxia
     * Útil para simular colisiones entre galaxias
     * Esta velocidad se suma a la velocidad individual de cada estrella
     */
    public void addGalaxyVelocity(Vector3D galaxyVelocity) {
        for (Particle star : stars) {
            Vector3D newVelocity = star.getVelocity().add(galaxyVelocity);
            star.setVelocity(newVelocity);
        }
    }

    /** ----------------- Aux Methods ----------------- **/


    @Override
    public String toString() {
        return String.format("Galaxy{name='%s', numberOfStars=%d, centerPosition=%s}",
                name, numberOfStars, centerPosition);
    }

    public String toFileGalaxyHeader(){
        return String.format("%s;%.5e;%.5e;%.5e",
                name, centerPosition.getX(), centerPosition.getY(), centerPosition.getZ());
    }

    public String[] toFileGalaxyStars(){
        String[] starLines = new String[numberOfStars];
        for (int i = 0; i < numberOfStars; i++) {
            starLines[i] = stars[i].toFileString();
        }
        return starLines;
    }
}
