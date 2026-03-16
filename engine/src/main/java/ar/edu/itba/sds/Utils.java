package ar.edu.itba.sds;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

public class Utils {
    static final double N = 400;
    static final int L = 10;
    static final double VELOCITY = 0.03;
    static final double CIRCULAR_SCENARIO_RADIUS = 5;
    static final int INTERACTION_RADIUS = 1;
    static final int M = L / INTERACTION_RADIUS;
    static final double[] CIRCULAR_SCENARIO_CENTER = {2, 2};
    static final double ANGULAR_VELOCITY = VELOCITY / CIRCULAR_SCENARIO_RADIUS;
    static int CIRCULAR_SCENARIO_STEP = 0;
    static int CIRCULAR_SCENARIO_MAX_STEP = (int) Math.ceil((2 * Math.PI) / ANGULAR_VELOCITY);
    static final String DATA_DIR = "data";
    static final int LEADER_ID = 1;
    static final int ITERATIONS = 50;
    static final int ITERATIONS_B = 50;
    enum Scenario {
        STANDARD,
        LEADER,
        CIRCULAR_LEADER
    }
    static Scenario SCENARIO = Scenario.STANDARD;

    static void updateParticle(Particle p, Set<Particle> neighbors, double eta) {
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
            p.setAngle((ANGULAR_VELOCITY * CIRCULAR_SCENARIO_STEP) + (Math.PI / 2.0));
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

    static double calculateOrder(List<Particle> particles) {
        double velocityAccX = 0;
        double velocityAccY = 0;
        for (Particle p : particles) {
            velocityAccX += Math.cos(p.getAngle());
            velocityAccY += Math.sin(p.getAngle());
        }

        return Math.sqrt(Math.pow(velocityAccX, 2) + Math.pow(velocityAccY, 2)) / N;
    }

    static List<Particle> generateParticles() {
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

    static void writeDynamicFrame(String baseFilename, int iteration, List<Particle> particles, boolean append) {
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

    static void saveOutputs(Map<Integer, Set<Particle>> neighbors, String baseFilename, int iteration) {
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
