package simulation1;

public class Particle {
    private double position;
    private double velocity;
    private final double mass;

    public Particle(double initialPosition, double initialVelocity, double mass) {
        this.position = initialPosition;
        this.velocity = initialVelocity;
        this.mass = mass;
    }

    public double getPosition() {
        return position;
    }

    public double getVelocity() {
        return velocity;
    }

    public double getMass() {
        return mass;
    }


    public void setPosition(double position) {
        this.position = position;
    }

    public void setVelocity(double velocity) {
        this.velocity = velocity;
    }
}
