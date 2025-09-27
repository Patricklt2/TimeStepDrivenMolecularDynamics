package com.simulation2.models;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Particle {
    private Vector3D position;
    private Vector3D velocity;
    private Vector3D acceleration;
    private Vector3D force;
    private final double mass = 1.0; // masa unitaria
    private final int id;


    public Particle(int id, Vector3D initialPosition, Vector3D initialVelocity) {
        this.id = id;
        this.position = new Vector3D(initialPosition.getX(), 
                                     initialPosition.getY(), 
                                     initialPosition.getZ());
        this.velocity = new Vector3D(initialVelocity.getX(), 
                                     initialVelocity.getY(), 
                                     initialVelocity.getZ());
        this.acceleration = Vector3D.ZERO;
        this.force = Vector3D.ZERO;
    }

    // TODO: Posición aleatoria; ver de incluir los limites de rand de posicion
    public Particle(int id) {
        this.id = id;
        this.position = Vector3D.ZERO;
        this.velocity = Vector3D.ZERO;
        this.acceleration = Vector3D.ZERO;
        this.force = Vector3D.ZERO;
    }
    
    // Métodos para la simulación
    public void resetForce() {
        this.force = Vector3D.ZERO;
    }
    
    // TODO: ver si hay que calcular las fuerzas resultantes no solo agregar
    public void addForce(Vector3D newForce) {
        this.force = this.force.add(newForce);
    }
    
    public void updateAcceleration() {
        this.acceleration = this.force.scalarMultiply(1.0 / mass);
    }

    public double getKineticEnergy() {
        return 0.5 * mass * velocity.getNormSq();
    }
    
    public double distanceTo(Particle other) {
        return position.distance(other.position);
    }
    

    // Getters
    public Vector3D getPosition() {
        return position;
    }
    
    public Vector3D getVelocity() {
        return velocity;
    }
    
    public Vector3D getAcceleration() {
        return acceleration;
    }
    
    public Vector3D getForce() {
        return force;
    }
    
    public double getMass() {
        return mass;
    }
    
    public int getId() {
        return id;
    }
    
    // Setters (usar con cuidado)
    public void setPosition(Vector3D position) {
        this.position = new Vector3D(position.getX(), position.getY(), position.getZ());
    }
    
    public void setVelocity(Vector3D velocity) {
        this.velocity = new Vector3D(velocity.getX(), velocity.getY(), velocity.getZ());
    }
    
    public void setAcceleration(Vector3D acceleration) {
        this.acceleration = new Vector3D(acceleration.getX(), acceleration.getY(), acceleration.getZ());
    }

    @Override
    public String toString() {
        return String.format("Particle[id=%d, pos=(%.3f,%.3f,%.3f), vel=(%.3f,%.3f,%.3f), m=%.3f]",
                id,
                position.getX(), position.getY(), position.getZ(),
                velocity.getX(), velocity.getY(), velocity.getZ(),
                mass);
    }
    
    // Método para guardar estado en archivo
    public String toFileString() {
        return String.format("%d %.15e %.15e %.15e %.15e %.15e %.15e %.15e",
                id,
                position.getX(), position.getY(), position.getZ(),
                velocity.getX(), velocity.getY(), velocity.getZ(),
                mass);
    }
}