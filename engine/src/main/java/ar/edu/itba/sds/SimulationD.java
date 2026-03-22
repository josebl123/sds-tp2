package ar.edu.itba.sds;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static ar.edu.itba.sds.Utils.*;

/**
 * Point (d): Runs all 3 scenarios with the SAME initial conditions
 * so the comparison is fair.
 */
public class SimulationD {
    public static void main(String[] args) {
        String baseFilename;

        if (args.length >= 1) {
            baseFilename = args[0];
        } else {
            baseFilename = String.valueOf(System.currentTimeMillis() / 1000);
        }

        // Generate base particles once in STANDARD mode (same for all scenarios)
        SCENARIO = Scenario.STANDARD;
        List<Particle> baseParticles = generateParticles();

        System.out.println("Partículas generadas: " + (int) N);
        System.out.println("Usando mismas condiciones iniciales para los 3 escenarios.\n");

        // Run all 3 scenarios sequentially with the same initial conditions
        for (int scenario = 0; scenario < 3; scenario++) {
            SCENARIO = Scenario.values()[scenario];
            System.out.println("Corriendo escenario " + scenario + " (" + SCENARIO + ")...");

            Map<Double, Double> resultsOrder = new TreeMap<>();
            Map<Double, Double> resultsError = new TreeMap<>();
            CellIndexMethod cim = new CellIndexMethod(M, INTERACTION_RADIUS);
            Map<Integer, Set<Particle>> neighbors;

            for (double eta = 0.0; eta <= Math.PI * 2; eta += Math.PI * 2 / STEPS_C) {
                List<Double> orders = new ArrayList<>();
                double orderAvg = 0;
                double desvioSum = 0;

                // Clone base particles — same starting positions for every scenario+eta
                List<Particle> particles = baseParticles.stream()
                        .map(Particle::new)
                        .collect(Collectors.toList());
                CIRCULAR_SCENARIO_STEP = 0;
                cim.populateGrid(particles);
                neighbors = cim.calculateNeighbors();

                for (int i = 0; i < TRANSITION_ITERATIONS + ITERATIONS_C; i++) {
                    for (Particle p : particles) {
                        updateParticle(p, neighbors.get(p.getId()), eta);
                    }
                    cim.populateGrid(particles);
                    if (i >= TRANSITION_ITERATIONS) {
                        double order = calculateOrder(particles);
                        orders.add(order);
                        orderAvg += order;
                    }
                    neighbors = cim.calculateNeighbors();
                }
                orderAvg = orderAvg / ITERATIONS_C;

                for (double o : orders) {
                    desvioSum += Math.pow((o - orderAvg), 2);
                }
                double desvio = Math.sqrt(desvioSum / ITERATIONS_C);
                double error = desvio / Math.sqrt(ITERATIONS_C);
                resultsOrder.put(eta, orderAvg);
                resultsError.put(eta, error);
            }

            new java.io.File(DATA_DIR).mkdirs();
            String outputFile = DATA_DIR + "/" + baseFilename + "C-scenario" + scenario + ".txt";
            try (PrintWriter c = new PrintWriter(new FileWriter(outputFile))) {
                resultsOrder.forEach((key, value) -> {
                    c.println(key + " " + value + " " + resultsError.get(key));
                });
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            System.out.println("  -> " + outputFile);
        }

        System.out.println("\nListo! Los 3 archivos usan las mismas condiciones iniciales.");
        System.out.println("Para graficar: python3 graphics/plot_comparison.py --timestamp " + baseFilename);
    }
}
