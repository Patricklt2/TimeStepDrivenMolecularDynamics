package com.simulation2.models;
import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Particle {
    private Vector3D position;
    private Vector3D velocity;
    private Vector3D acceleration;
    private Vector3D oldPosition;
    private Vector3D oldVelocity;
    private Vector3D oldAcceleration;
    private Vector3D force;
    private final double mass = 1.0; // masa unitaria
    private final int id;
    private final int galaxyId;


    public Particle(int id, int galaxyId, Vector3D initialPosition, Vector3D initialVelocity) {
        this.id = id;
        this.galaxyId = galaxyId;
        this.position = new Vector3D(initialPosition.getX(), 
                                     initialPosition.getY(), 
                                     initialPosition.getZ());
        this.velocity = new Vector3D(initialVelocity.getX(), 
                                     initialVelocity.getY(), 
                                     initialVelocity.getZ());
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

    public Vector3D calculateForceFrom(Particle other, double G, double h){

        Vector3D r12 = other.getPosition().subtract(position);
        double r12_soft = r12.getNormSq() + Math.pow(h, 2);
        double denominator = Math.pow(r12_soft, 3.0/2.0);
        double forceMag = - G * mass * other.getMass() / denominator;

        return r12.scalarMultiply(forceMag);
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

    public Vector3D getOldPosition() {
        return oldPosition;
    }

    public Vector3D getOldVelocity() {
        return oldVelocity;
    }

    public Vector3D getOldAcceleration() {
        return oldAcceleration;
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

    public void setOldPosition(Vector3D oldPosition) {
        this.oldPosition = new Vector3D(oldPosition.getX(), oldPosition.getY(), oldPosition.getZ());
    }

    public void setOldVelocity(Vector3D oldVelocity) {
        this.oldVelocity = new Vector3D(oldVelocity.getX(), oldVelocity.getY(), oldVelocity.getZ());
    }

    public void setOldAcceleration(Vector3D oldAcceleration) {
        this.oldAcceleration = new Vector3D(oldAcceleration.getX(), oldAcceleration.getY(), oldAcceleration.getZ());
    }

    @Override
    public String toString() {
        return String.format("Particle[id=%d, galaxyId=%d, pos=(%.3f,%.3f,%.3f), vel=(%.3f,%.3f,%.3f), m=%.3f]",
                id, galaxyId,
                position.getX(), position.getY(), position.getZ(),
                velocity.getX(), velocity.getY(), velocity.getZ(),
                mass);
    }
    
    // Método para guardar estado en archivo
    public String toFileString() {
        // Añadimos galaxyId al principio para facilitar el parsing y el coloreado
        return String.format("%d;%d;%.15e;%.15e;%.15e;%.15e;%.15e;%.15e",
                id,
                galaxyId,
                position.getX(), position.getY(), position.getZ(),
                velocity.getX(), velocity.getY(), velocity.getZ());
    }
}