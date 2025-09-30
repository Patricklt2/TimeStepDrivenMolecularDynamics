package com.simulation2;

import com.simulation2.models.Simulationv2;

public class Main {
    public static void main(String[] args) {

        System.out.println("Hello, World!");
        Simulationv2 s = new Simulationv2(200, 1, 100, 5, 0.00001, "sim.csv");
        s.run();
    }
}
