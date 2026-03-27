package ar.edu.itba.sds;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static ar.edu.itba.sds.Utils.*;

public class SimulationC {
    public static void main(String[] args) {
        // Accept scenario as argument or from stdin
        int scenario = 0;
        String baseFilename;

        if (args.length >= 1) {
            scenario = Integer.parseInt(args[0]);
        } else {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Scenario (0: estandar, 1: lider, 2: lider circular): ");
            scenario = scanner.nextInt();
            scanner.close();
        }

        // Optional second argument: shared base filename for parallel runs
        if (args.length >= 2) {
            baseFilename = args[1] + "C";
        } else {
            baseFilename = String.valueOf(System.currentTimeMillis() / 1000).concat("C");
        }

        SCENARIO = Scenario.values()[scenario];

        List<Particle> baseParticles = generateParticles();
        List<Particle> particles;
        CellIndexMethod cim = new CellIndexMethod(M, INTERACTION_RADIUS);
        Map<Integer, Set<Particle>> neighbors;
        Map<Double, Double> resultsOrder = new TreeMap<>();
        Map<Double, Double> resultsError = new TreeMap<>();

        for (double eta = 0.0; eta <= Math.PI * 2; eta += Math.PI * 2 / STEPS_C) {
            List<Double> orders = new ArrayList<>();
            double orderAvg = 0;
            double desvioSum = 0;

            particles = baseParticles.stream().map(Particle::new).collect(Collectors.toList());
            CIRCULAR_SCENARIO_STEP = 0;
            cim.populateGrid(particles);
            neighbors = cim.calculateNeighbors();
            for (int i = 0; i < TRANSITION_ITERATIONS + ITERATIONS_C; i++) {
                List<Particle> nextParticles = new ArrayList<>();
                for (Particle p : particles) {
                    Particle nextP = new Particle(p);
                    updateParticle(nextP, neighbors.get(p.getId()), eta);
                    nextParticles.add(nextP);
                }
                particles = nextParticles;
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
            double error = Math.sqrt(desvioSum / ITERATIONS_C);
            resultsOrder.put(eta, orderAvg);
            resultsError.put(eta, error);
        }

        String outputFile = DATA_DIR + "/" + baseFilename + "-scenario" + scenario + ".txt";
        new java.io.File(DATA_DIR).mkdirs();
        try (PrintWriter c = new PrintWriter(new FileWriter(outputFile))) {
            resultsOrder.forEach((key, value) -> {
                c.println(key + " " + value + " " + resultsError.get(key));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Archivo generado con timestamp: " + baseFilename.substring(0, baseFilename.length() - 1));
    }
}
