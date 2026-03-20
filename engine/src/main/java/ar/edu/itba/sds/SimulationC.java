package ar.edu.itba.sds;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static ar.edu.itba.sds.Utils.*;

public class SimulationC {
    public static void main(String[] args) {
        List<Particle> baseParticles = generateParticles();
        List<Particle> particles;
        CellIndexMethod cim = new CellIndexMethod(M, INTERACTION_RADIUS);
        Map<Integer, Set<Particle>> neighbors;
        String baseFilename = String.valueOf(System.currentTimeMillis() / 1000).concat("C");
        Map<Double, Double> resultsOrder = new TreeMap<>();
        Map<Double, Double> resultsError = new TreeMap<>();
        List<Double> orders = new ArrayList<>();

        final int steps = 10;
        double orderAvg = 0;
        double order = 0;
        double desvioSum = 0;
        double desvio = 0;
        double error = 0;

        for (double eta = 0.0; eta <= Math.PI * 2; eta += Math.PI * 2 / steps) {
            particles = baseParticles.stream().map(Particle::new).collect(Collectors.toList());
            cim.populateGrid(particles);
            neighbors = cim.calculateNeighbors();
            for (int i = 0; i < 200; i++) {
                for (Particle p : particles) {
                    updateParticle(p, neighbors.get(p.getId()), eta);
                }
                cim.populateGrid(particles);
                if (i >= 50) {
                    order = calculateOrder(particles);
                    orders.add(order);
                    orderAvg += order;
                }
                neighbors = cim.calculateNeighbors();
            }
            orderAvg = orderAvg / 150;

            for (double o : orders) {
                desvioSum += Math.pow((o - orderAvg), 2);
            }
            desvio = Math.sqrt(desvioSum / 150);
            error = desvio / Math.sqrt(150);
            resultsOrder.put(eta, order);
            resultsError.put(eta, error);
        }

        try (PrintWriter c = new PrintWriter(new FileWriter(DATA_DIR + "/" + baseFilename + "-C.txt"))) {
            resultsOrder.forEach((key, value) -> {
                c.println(key + " " + value + " " + resultsError.get(key));
            });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
