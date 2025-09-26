package simulation1.integrators;

import simulation1.Particle;

public class Verlet implements IIntegrator {
    // cte del resorte
    private final double k;

    // cte de amortiguamiento
    private final double gamma;  
    private double previousPosition;

    /*
     * Aclaro para cuando lean esto, esto es para el verlet original
     * creo que esta bien implementado
     */
    public Verlet(double k, double gamma) {
        this.k = k;
        this.gamma = gamma;
        this.previousPosition = Double.NaN; // inicialmente no se cuanto es XD
    }

    @Override
    public void step(Particle p, double dt){
        // Hay que inicializar
        if (Double.isNaN(previousPosition)) {
            initializePreviousPosition(p, dt);
        }

        double currentPosition = p.getPosition();
        double mass = p.getMass();

        double force = -k * currentPosition - gamma * p.getVelocity();
        double acceleration = force / mass;
        double nextPosition = 2 * currentPosition - previousPosition + acceleration * dt * dt;

        double nextVelocity = (nextPosition - previousPosition) / (2 * dt);

        this.previousPosition = currentPosition;

        p.setPosition(nextPosition);
        p.setVelocity(nextVelocity);
    }

    private void initializePreviousPosition(Particle p0, double dt) {
        double r0 = p0.getPosition();
        double v0 = p0.getVelocity();
        double m = p0.getMass();

        double f0 = -k * r0 - gamma * v0;
        double a0 = f0 / m;

        this.previousPosition = r0 - v0 * dt + 0.5 * a0 * dt * dt;
    }
}
