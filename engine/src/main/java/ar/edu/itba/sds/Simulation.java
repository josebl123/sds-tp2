package ar.edu.itba.sds;

import java.io.*;
import java.util.*;

public class Simulation {

    static final double N = 400;
    static final int L = 10;
    static final double VELOCITY = 0.03;
    static final double CIRCULAR_SCENARIO_RADIUS = 5;
    static final double[] CIRCULAR_SCENARIO_CENTER = {2, 2};
    static final double ANGULAR_VELOCITY = VELOCITY / CIRCULAR_SCENARIO_RADIUS;
    static int CIRCULAR_SCENARIO_STEP = 0;
    static int CIRCULAR_SCENARIO_MAX_STEP = (int) Math.ceil((2 * Math.PI) / ANGULAR_VELOCITY);
    private static final String DATA_DIR = "data";
    static final int LEADER_ID = 1;
    enum Scenario {
        STANDARD,
        LEADER,
        CIRCULAR_LEADER
    }
    static Scenario SCENARIO = Scenario.CIRCULAR_LEADER;

    public static void main(String[] args) {
        List<Particle> particles = null;
        double rc = 0;
        double eta = 0;
        int M = 0;
        String method = "CIM";
        String baseFilename = String.valueOf(System.currentTimeMillis() / 1000);

        try {
            if (args.length == 2) {
                String staticFile = args[0];
                String dynamicFile = args[1];
                double[] lArr = new double[1];
                Scanner scanner = new Scanner(System.in);
                System.out.print("M: "); M = scanner.nextInt();
                System.out.print("rc: "); rc = scanner.nextDouble();
                particles = loadParticles(staticFile, dynamicFile, lArr);
                baseFilename = new File(staticFile).getName().replace(".txt", "");

            } else {
                    Scanner scanner = new Scanner(System.in);
                    System.out.print("r_c: "); rc = scanner.nextDouble();
                    System.out.print("M: "); M = scanner.nextInt();
                    System.out.print("Eta: "); eta = scanner.nextDouble();
                    scanner.close();

                particles = generateParticles();
                saveMapFiles(particles, baseFilename);
            }

            long startTime = System.nanoTime();
            Map<Integer, Set<Particle>> neighbors;


            CellIndexMethod cim = new CellIndexMethod(M, rc);
            cim.populateGrid(particles);
            neighbors = cim.calculateNeighbors();

            writeDynamicFrame(baseFilename, 0, particles, false); // initial frame

            for (int i = 0; i < 1000; i++) {
                saveOutputs(neighbors, baseFilename, i);
                if (i > 0) {
                    writeDynamicFrame(baseFilename, i, particles, true);
                }
                for (Particle p : particles) {
                    updateParticle(p, neighbors.get(p.getId()), eta);
                }
                neighbors = cim.calculateNeighbors();
            }

            long endTime = System.nanoTime();
            double timeMs = (endTime - startTime) / 1000000.0;

            double order = calculateOrder(particles);

            System.out.println("Tiempo de ejecucion: " + timeMs + " ms");
            System.out.println("Archivos generados con timestamp: " + baseFilename);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static double calculateOrder(List<Particle> particles) {
        double velocityAccX = 0;
        double velocityAccY = 0;
        for (Particle p : particles) {
            velocityAccX += Math.cos(p.getAngle());
            velocityAccY += Math.sin(p.getAngle());
        }

        return Math.sqrt(Math.pow(velocityAccX, 2) + Math.pow(velocityAccY, 2)) / N;
    }

    private static void updateParticle(Particle p, Set<Particle> neighbors, double eta) {
        if (SCENARIO.equals(Scenario.STANDARD) || p.getId() != LEADER_ID) {
            double sinAvg = 0.0;
            double cosAvg = 0.0;
            Random rand = new Random();
            for (Particle particle : neighbors) {
                sinAvg += Math.sin(particle.getAngle());
                cosAvg += Math.cos(particle.getAngle());
            }
            p.setAngle(Math.atan2(sinAvg, cosAvg) + ((rand.nextDouble() * 2) - 1) * eta / 2);
        }

        double nx, ny;

        if (SCENARIO.equals(Scenario.CIRCULAR_LEADER) && p.getId() == LEADER_ID) {
            nx = CIRCULAR_SCENARIO_CENTER[0] + CIRCULAR_SCENARIO_RADIUS * Math.cos(ANGULAR_VELOCITY * CIRCULAR_SCENARIO_STEP);
            ny = CIRCULAR_SCENARIO_CENTER[1] + CIRCULAR_SCENARIO_RADIUS * Math.sin(ANGULAR_VELOCITY * CIRCULAR_SCENARIO_STEP);
            CIRCULAR_SCENARIO_STEP = (CIRCULAR_SCENARIO_STEP + 1) % CIRCULAR_SCENARIO_MAX_STEP;
        } else {
            nx = p.getX() + Math.cos(p.getAngle()) * VELOCITY;
            ny = p.getY() + Math.sin(p.getAngle()) * VELOCITY;
        }
        nx = ((nx % L) + L) % L;
        ny = ((ny % L) + L) % L;
        p.setX(nx);
        p.setY(ny);
    }

    private static List<Particle> generateParticles() {
        List<Particle> particles = new ArrayList<>();
        Random rand = new Random();

        int ids = 1;

        if (!SCENARIO.equals(Scenario.STANDARD)) {
            particles.add(new Particle(LEADER_ID, CIRCULAR_SCENARIO_CENTER[0], CIRCULAR_SCENARIO_CENTER[1], rand.nextDouble() * 2 * Math.PI));
        }

        while (particles.size() < N) {
            double x = L * rand.nextDouble();
            double y = L * rand.nextDouble();
            boolean overlap = false;

            for (Particle p : particles) {
                if (x == p.getX() && y == p.getY()) {
                    overlap = true;
                    break;
                }
            }

            if (!overlap) {
                double angle = rand.nextDouble() * 2 * Math.PI;
                if (ids == LEADER_ID) ids++;
                particles.add(new Particle(ids++, x, y, angle));
            }
        }
        return particles;
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
    private static void writeDynamicFrame(String baseFilename, int iteration, List<Particle> particles, boolean append) {
        String path = DATA_DIR + "/" + baseFilename + "-Dynamic.txt";
        try (PrintWriter writer = new PrintWriter(new FileWriter(path, append))) {
            writer.println(iteration);
            for (Particle p : particles) {
                double vx = Math.cos(p.getAngle()) * VELOCITY;
                double vy = Math.sin(p.getAngle()) * VELOCITY;
                writer.println(p.getX() + " " + p.getY() + " " + vx + " " + vy);
            }
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
    private static void saveOutputs(Map<Integer, Set<Particle>> neighbors, String baseFilename, int iteration) {
        String outputPath = DATA_DIR + "/" + baseFilename + "-output.txt";
        try (PrintWriter outWriter = new PrintWriter(new FileWriter(outputPath, true))) {
            outWriter.println("Iteration: " + iteration);
            for (Map.Entry<Integer, Set<Particle>> entry : neighbors.entrySet()) {
                outWriter.print("[" + entry.getKey());
                List<Particle> sortedNeighbors = new ArrayList<>(entry.getValue());
                sortedNeighbors.sort(Comparator.comparing(Particle::getId));
                for (Particle neighbor : sortedNeighbors) {
                    outWriter.print(" " + neighbor.getId());
                }
                outWriter.println("]");
            }
            outWriter.println();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

