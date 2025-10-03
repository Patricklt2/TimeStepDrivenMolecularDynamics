package com.simulation2.models;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;
import com.simulation2.integrators.*;
import java.util.List;
import java.util.Random;
import java.util.Arrays;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Galaxy {
    private static final Logger logger = LoggerFactory.getLogger(Galaxy.class);
    private final String name;
    private int numberOfStars;
    private Vector3D centerPosition;
    private Particle[] stars;
    private final double G = 1.0;
    private final double h = 0.05;
    
    private final double initialVelocity = 0.1;
    private final Random random = new Random();


    ForceCalculator forceCalculator = (pList) -> {
        for (Particle p : pList) {
            p.resetForce();
        }

        for (int i = 0; i < pList.size(); i++) {
            for (int j = i + 1; j < pList.size(); j++) {
                Particle pi = pList.get(i);
                Particle pj = pList.get(j);
                Vector3D force = calculateForceFromP1ToP2(
                    pi.getPosition(), pi.getMass(),
                    pj.getPosition(), pj.getMass(),
                    G, h
                );
                pi.addForce(force);
                pj.addForce(force.negate());
            }
        }
    };
    
    public Galaxy(String name, int numberOfStars, Vector3D centerPosition) {
        this.name = name;
        this.numberOfStars = numberOfStars;
        this.centerPosition = centerPosition;
        initializeStars();
    }
    
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
    
    public void setNumberOfStars(int numberOfStars) {
        this.numberOfStars = numberOfStars;
        initializeStars();
    }
    
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
     */
    public void initializeStars() {
        stars = new Particle[numberOfStars];
        
        // Distancia mínima entre partículas (basada en el parámetro de suavizado)
        double minDistance = 0.1; // Un poco mayor que h = 0.05 del enunciado
        int maxAttempts = 1000; // máximo número de intentos por partícula
        
        for (int i = 0; i < numberOfStars; i++) {
            
            Vector3D position;
            boolean validPosition = false;
            int attempts = 0;
            
            do {
                double positionX = centerPosition.getX() + random.nextGaussian(); // desvío = 1
                double positionY = centerPosition.getY() + random.nextGaussian();
                double positionZ = centerPosition.getZ() + random.nextGaussian();
                
                position = new Vector3D(positionX, positionY, positionZ);
                
                validPosition = true;
                for (int j = 0; j < i; j++) {
                    double distance = position.distance(stars[j].getPosition());
                    if (distance < minDistance) {
                        validPosition = false;
                        break;
                    }
                }
                
                attempts++;
                if (attempts >= maxAttempts) {
                    logger.warn("Using spherical distribution for particle {} after {} failed normal distribution attempts.", i, maxAttempts);
                    position = generateRandomPositionInSphere(3.0);
                    validPosition = true;
                    for (int j = 0; j < i; j++) {
                        double distance = position.distance(stars[j].getPosition());
                        if (distance < minDistance) {
                            validPosition = false;
                            break;
                        }
                    }
                    
                    if (!validPosition && attempts >= maxAttempts * 2) {
                        System.out.println("Warning: Could not avoid overlap for particle " + i + 
                                         ". Proceeding with overlap (will be handled by smoothing parameter h).");
                        validPosition = true;
                    }
                }
                
            } while (!validPosition);
            
            Vector3D randomDirection = generateRandomUnitVector();
            Vector3D velocity = randomDirection.scalarMultiply(initialVelocity);
            
            stars[i] = new Particle(
                i,
                position,
                velocity
            );
        }
    }
    
    /**
     * Genera una posición aleatoria dentro de una esfera
     * Usado como fallback cuando la distribución normal falla
     */
    private Vector3D generateRandomPositionInSphere(double radius) {
        Vector3D randomDirection = generateRandomUnitVector();
        double randomRadius = radius * Math.cbrt(random.nextDouble()); // distribución uniforme en volumen
        return centerPosition.add(randomDirection.scalarMultiply(randomRadius));
    }
    
    /**
     * Genera un vector unitario con dirección aleatoria
     */
    private Vector3D generateRandomUnitVector() {
        double x1, x2, w;
        do {
            x1 = 2.0 * random.nextDouble() - 1.0;
            x2 = 2.0 * random.nextDouble() - 1.0;
            w = x1 * x1 + x2 * x2;
        } while (w >= 1.0);
        
        double sqrt = Math.sqrt(-2.0 * Math.log(w) / w);
        double z1 = x1 * sqrt;
        double z2 = x2 * sqrt;
        double z3 = random.nextGaussian();
        
        Vector3D vector = new Vector3D(z1, z2, z3);
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
     * Resetea las fuerzas de todas las estrellas de la galaxia
     */
    public void resetForces() {
        for (Particle star : stars) {
            star.resetForce();
        }
    }


    public void calculateForces(double G, double h) {
        // Mismo método para todos los integradores
        for (Particle p : stars) {
            p.resetForce();
        }
        
        for (int i = 0; i < stars.length; i++) {
            for (int j = i + 1; j < stars.length; j++) {
                Particle pi = stars[i];
                Particle pj = stars[j];

                Vector3D force = calculateForceFromP1ToP2(
                    pi.getPosition(), pi.getMass(),
                    pj.getPosition(), pj.getMass(),
                    G, h
                );
                
                pi.addForce(force);
                pj.addForce(force.negate());
            }
        }
    }

    /**
     * Calcula la fuerza que p1 ejerce sobre p2
     *
     *                                F12
     *                               <——-
     *                p1 ———---------———> p2
     *                          r
     *
     * @param pos1 posicion de particula 1
     * @param m1 masa de particula 1
     * @param pos2 posicion de particula 2
     * @param m2 masa de particula 2
     * @param G G
     * @param h h
     * @return fuerza ejercida sobre p2
     */
    public Vector3D calculateForceFromP1ToP2(
            Vector3D pos1, double m1, 
            Vector3D pos2, double m2,
            double G, double h) {
        
        Vector3D r = pos2.subtract(pos1);
        double r_soft = Math.sqrt(r.getNormSq() + h * h); 
        double denominator = Math.pow(r_soft, 3);          
        double forceMag = - G * m1 * m2 / denominator;

        return r.scalarMultiply(forceMag);
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
    
    /**
     * Método para obtener todas las partículas de la galaxia
     * Útil para integrar con el sistema de simulación principal
     */
    public Particle[] getAllParticles() {
        return stars.clone(); // retorna una copia para evitar modificaciones externas
    }
    
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
    
    public void integratorMethod(IIntegrator integrator, double dt, double G, double h){
        List<Particle> ls = Arrays.asList(this.stars);
        integrator.step(ls, dt, forceCalculator);
    }
}