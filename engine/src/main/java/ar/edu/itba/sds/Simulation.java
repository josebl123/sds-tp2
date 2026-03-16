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

        try {
            if (args.length == 2) {
                String staticFile = args[0];
                String dynamicFile = args[1];
                double[] lArr = new double[1];
                particles = loadParticles(staticFile, dynamicFile, lArr);
                baseFilename = new File(staticFile).getName().replace(".txt", "");

            } else {
                    Scanner scanner = new Scanner(System.in);
                    System.out.print("Eta: "); eta = scanner.nextDouble();
                    System.out.print("Scenario (0: estandar, 1: lider, 2: lider circular): "); scenario = scanner.nextInt();
                    scanner.close();
                    SCENARIO = Scenario.values()[scenario];
                particles = generateParticles();
                saveMapFiles(particles, baseFilename);
            }

            long startTime = System.nanoTime();
            Map<Integer, Set<Particle>> neighbors;


            CellIndexMethod cim = new CellIndexMethod(M, INTERACTION_RADIUS);
            cim.populateGrid(particles);
            neighbors = cim.calculateNeighbors();

            writeDynamicFrame(baseFilename, 0, particles, false); // initial frame
            PrintWriter orderWriter = new PrintWriter(new FileWriter(DATA_DIR + "/" + baseFilename + "-order.txt"));
            for (int i = 0; i < ITERATIONS; i++) {
                saveOutputs(neighbors, baseFilename, i);
                if (i > 0) {
                    writeDynamicFrame(baseFilename, i, particles, true);
                }

                double currentOrder = calculateOrder(particles);
                orderWriter.println(i + " " + currentOrder);

                for (Particle p : particles) {
                    updateParticle(p, neighbors.get(p.getId()), eta);
                }
                neighbors = cim.calculateNeighbors();
            }

            orderWriter.close();
            long endTime = System.nanoTime();
            double timeMs = (endTime - startTime) / 1000000.0;

            double order = calculateOrder(particles);

            System.out.println("Tiempo de ejecucion: " + timeMs + " ms");
            System.out.println("Archivos generados con timestamp: " + baseFilename);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void saveMapFiles(List<Particle> particles, String baseFilename) {
        new File(DATA_DIR).mkdirs();
        try {
            PrintWriter staticWriter = new PrintWriter(new FileWriter(DATA_DIR + "/" + baseFilename + ".txt", true));
            staticWriter.println(N);
            staticWriter.println(L);
            // Metadata line to help the animation highlight the leader when applicable
            staticWriter.println("SCENARIO " + SCENARIO + " LEADER_ID " + LEADER_ID);
            for (Particle p : particles) {
                staticWriter.println(0 /*radius*/ + " " + 1.0);
            }
            staticWriter.close();

            // Initial frame is now written in writeDynamicFrame, so no work here
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }

    private static List<Particle> loadParticles(String staticPath, String dynamicPath, double[] L_out) throws Exception {
        List<Particle> particles = new ArrayList<>();
        Scanner staticScanner = new Scanner(new File(staticPath)).useLocale(java.util.Locale.US);
        Scanner dynamicScanner = new Scanner(new File(dynamicPath)).useLocale(java.util.Locale.US);

        int N = staticScanner.nextInt();
        L_out[0] = staticScanner.nextDouble();
        if (staticScanner.hasNextLine()) {
            staticScanner.nextLine();
        }

        // If there is a metadata line starting with SCENARIO, consume it; otherwise, keep scanning radii
        if (staticScanner.hasNext("SCENARIO")) {
            if (staticScanner.hasNextLine()) {
                staticScanner.nextLine();
            }
        }

        for (int i = 0; i < N; i++) {
            String line = staticScanner.nextLine().trim();
            while (line.isEmpty() && staticScanner.hasNextLine()) {
                line = staticScanner.nextLine().trim();
            }
        }

        dynamicScanner.nextDouble();

        if (dynamicScanner.hasNextLine()) {
            dynamicScanner.nextLine();
        }

        for (int i = 0; i < N; i++) {
            String line = dynamicScanner.nextLine().trim();
            while (line.isEmpty() && dynamicScanner.hasNextLine()) {
                line = dynamicScanner.nextLine().trim();
            }

            String[] parts = line.split("\\s+");
            double x = Double.parseDouble(parts[0]);
            double y = Double.parseDouble(parts[1]);
            double angle = Double.parseDouble(parts[2]);
            particles.add(new Particle(i + 1, x, y, angle));
        }

        staticScanner.close();
        dynamicScanner.close();
        return particles;
    }
}

