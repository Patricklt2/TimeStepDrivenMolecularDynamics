package com.simulation2;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import com.simulation2.integrators.IIntegrator2;
import com.simulation2.integrators.VelocityVerlet2;
import com.simulation2.models.Simulation2;

public class Main {
    public static void main(String[] args) {
        defaultRun();
    }

    public static void defaultRun(){
        IIntegrator2 integrator = new VelocityVerlet2();
        Simulation2 s = new Simulation2(100, 1, 100, 1, 0.001, "sim.csv", integrator);
        s.run();
    }

    public static void dtRun(){
        IIntegrator2 integrator = new VelocityVerlet2();
        double timeStep = 0.0001;
        String filename = "sim_dt_" + timeStep + ".csv";
        Simulation2 s = new Simulation2(2, 1, 100, 5, timeStep, filename, integrator);
        s.run();
    }


    public static void runfor2() {
        IIntegrator2 integrator = new VelocityVerlet2();

        int numberOfThreads = Runtime.getRuntime().availableProcessors();
        ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);

        for (int i = 100; i <= 2000; i += 200) {
            for (int j = 0; j < 10; j++) {
                final int currentI = i;
                final int currentJ = j;

                Runnable simulationTask = () -> {
                    Simulation2 s = new Simulation2(currentI, 1, 100, 1, 0.001,
                            String.format("sim_%d_%d.csv", currentI, currentJ), integrator);
                    s.run();
                };
                executor.submit(simulationTask);
            }
        }

        System.out.println("All tasks submitted. Waiting for completion...");
        executor.shutdown();

        try {
            if (!executor.awaitTermination(60, TimeUnit.MINUTES)) {
                System.err.println("Tasks did not complete in 60 minutes. Forcing shutdown.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            System.err.println("Main thread was interrupted. Forcing shutdown.");
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }

        System.out.println("All simulations finished.");
    }
}