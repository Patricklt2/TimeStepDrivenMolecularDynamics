package models;

import org.apache.commons.math3.geometry.euclidean.threed.Vector3D;

public class Simulation {

    private static final double G = 1.0;
    private static final double h = 0.05;
    
    public static void main(String[] args) {
        // Crear vectores
        Vector3D pos1 = new Vector3D(1.0, 2.0, 3.0);
        Vector3D pos2 = new Vector3D(4.0, 5.0, 6.0);
        
        // Operaciones vectoriales
        Vector3D diff = pos2.subtract(pos1);
        double distance = diff.getNorm();
        Vector3D normalized = diff.normalize();
        
        // Producto punto y cruz
        double dotProduct = pos1.dotProduct(pos2);
        Vector3D crossProduct = pos1.crossProduct(pos2);
        
        // Operaciones con escalares
        Vector3D scaled = pos1.scalarMultiply(2.5);
        Vector3D sum = pos1.add(scaled);
    }
    
    // Cálculo de fuerza gravitacional
    public static Vector3D calculateForce(
            Vector3D pos1, double m1, 
            Vector3D pos2, double m2) {
        
        Vector3D r = pos2.subtract(pos1);
        double r2 = r.getNormSq() + h * h;
        double r_mag = Math.sqrt(r2);
        
        double forceMag = G * m1 * m2 / (r2 * r_mag);
        
        return r.normalize().scalarMultiply(forceMag);
    }
}
    
