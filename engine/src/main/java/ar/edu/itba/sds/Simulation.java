package ar.edu.itba.sds;

import java.io.*;
import java.util.*;

import static ar.edu.itba.sds.Utils.*;

public class Simulation {

    public static void main(String[] args) {
        List<Particle> particles = null;
        double eta = 0;
        String baseFilename = String.valueOf(System.currentTimeMillis() / 1000);
        int scenario = 0; // 0: standard, 1: leader, 2: circular leader
        int iterations = ITERATIONS_A;

        try {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Eta: "); eta = scanner.nextDouble();
            System.out.print("Iterations: "); iterations = scanner.nextInt();
            System.out.print("Scenario (0: estandar, 1: lider, 2: lider circular): "); scenario = scanner.nextInt();
            scanner.close();
            SCENARIO = Scenario.values()[scenario];
            particles = generateParticles();
            saveMapFiles(baseFilename);

            long startTime = System.nanoTime();
            Map<Integer, Set<Particle>> neighbors;


            CellIndexMethod cim = new CellIndexMethod(M, INTERACTION_RADIUS);
            cim.populateGrid(particles);
            neighbors = cim.calculateNeighbors();

            writeDynamicFrame(baseFilename, 0, particles, false);
            for (int i = 0; i < iterations; i++) {
                saveOutputs(neighbors, baseFilename, i);
                if (i > 0) {
                    writeDynamicFrame(baseFilename, i, particles, true);
                }


                List<Particle> nextParticles = new ArrayList<>();
                for (Particle p : particles) {
                    Particle nextP = new Particle(p);
                    updateParticle(nextP, neighbors.get(p.getId()), eta);
                    nextParticles.add(nextP);
                }
                if (SCENARIO == Scenario.CIRCULAR_LEADER) {
                    CIRCULAR_SCENARIO_STEP = (CIRCULAR_SCENARIO_STEP + 1) % CIRCULAR_SCENARIO_MAX_STEP;
                }
                particles = nextParticles;
                cim.populateGrid(particles);
                neighbors = cim.calculateNeighbors();
            }

            long endTime = System.nanoTime();
            double timeMs = (endTime - startTime) / 1000000.0;

            System.out.println("Tiempo de ejecucion: " + timeMs + " ms");
            System.out.println("Archivos generados con timestamp: " + baseFilename);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveMapFiles(String baseFilename) {
        new File(DATA_DIR).mkdirs();
        try (PrintWriter staticWriter = new PrintWriter(new FileWriter(DATA_DIR + "/" + baseFilename + ".txt", true))) {
            staticWriter.println(N);
            staticWriter.println(L);
            String metadata = "SCENARIO " + SCENARIO + " LEADER_ID " + LEADER_ID;
            if (SCENARIO == Scenario.CIRCULAR_LEADER) {
                metadata += " CIRCLE_CENTER " + CIRCULAR_SCENARIO_CENTER[0] + " " + CIRCULAR_SCENARIO_CENTER[1] + " CIRCLE_RADIUS " + CIRCULAR_SCENARIO_RADIUS;
            }
            staticWriter.println(metadata);

        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}

