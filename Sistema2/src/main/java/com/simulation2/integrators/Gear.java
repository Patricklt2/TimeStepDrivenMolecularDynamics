package com.simulation2.integrators;

import com.simulation2.models.Particle;

public class Gear implements IIntegrator {
    // cte del resorte
    private final double k;

    // cte de amortiguamiento
    private final double gamma;  
    private final double mass;

    // r r1 r2 r3 r4 r5
    private double[] derivatives;

    private static final double[] FACTORIALS = {1.0, 1.0, 2.0, 6.0, 24.0, 120.0};

    // Sacado textualmente desde la tabla que hay en una diapo
    private static final double[] ALPHA_COEFFICIENTS = {
            3.0 / 16.0,
            251.0 / 360.0,
            1.0,
            11.0 / 18.0,
            1.0 / 6.0,
            1.0 / 60.0
    };

    // Se inicializacian las derivadas en orden, r r1 r2 r3 r4 r5
    public Gear(Particle p, double k, double gamma) {
        this.k = k;
        this.gamma = gamma;
        this.mass = p.getMass();
        this.derivatives = new double[6];

        derivatives[2] = (-k * derivatives[0] - gamma * derivatives[1]) / mass;
        derivatives[3] = (-k * derivatives[1] - gamma * derivatives[2]) / mass;
        derivatives[4] = (-k * derivatives[2] - gamma * derivatives[3]) / mass;
        derivatives[5] = (-k * derivatives[3] - gamma * derivatives[4]) / mass;
    }

    private double[] predict(double dt) {
        double[] predicted = new double[6];
        for (int i = 0; i < 6; i++) {
            double sum = 0;
            for (int j = i; j < 6; j++) {
                sum += derivatives[j] * (Math.pow(dt, j-i) / FACTORIALS[j-i]);
            }
            predicted[i] = sum;
        }
        return predicted;
    }

    private void correct(double[] predicted, double deltaR2, double dt) {
        for (int i = 0; i < 6; i++) {
            derivatives[i] = predicted[i] 
            + (ALPHA_COEFFICIENTS[i] * deltaR2 * FACTORIALS[i])
            / Math.pow(dt, i);
        }
    }

    @Override
    public void step(Particle p, double dt) { 
        double[] predictedDerivatives = predict(dt);

        double r0p = predictedDerivatives[0];
        double r1p = predictedDerivatives[1];
        double realForce = -k * r0p - gamma * r1p;
        double realAcceleration = realForce / mass;

        double deltaA = realAcceleration - predictedDerivatives[2];
        double deltaR2 = deltaA * (dt * dt) / FACTORIALS[2];

        correct(predictedDerivatives, deltaR2, dt);
    }
}
