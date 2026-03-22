package ar.edu.itba.sds;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static ar.edu.itba.sds.Utils.*;

public class SimulationB {

    public static void main(String[] args) {
        // Accept scenario as argument or from stdin
        int scenario = 0;
        if (args.length >= 1) {
            scenario = Integer.parseInt(args[0]);
        } else {
            Scanner scanner = new Scanner(System.in);
            System.out.print("Scenario (0: estandar, 1: lider, 2: lider circular): ");
            scenario = scanner.nextInt();
            scanner.close();
        }
        SCENARIO = Scenario.values()[scenario];

        List<Particle> particles = generateParticles();
        List<Particle> particles2 = particles.stream().map(Particle::new).collect(Collectors.toList());
        List<Particle> particles3 = particles.stream().map(Particle::new).collect(Collectors.toList());

        CellIndexMethod cim = new CellIndexMethod(M, INTERACTION_RADIUS);
        Map<Integer, Set<Particle>> neighbors;

        String baseFilename;
        if (args.length >= 2) {
            baseFilename = args[1] + "B";
        } else {
            baseFilename = String.valueOf(System.currentTimeMillis() / 1000).concat("B");
        }

        double[] etas = {0, Math.PI, Math.PI * 2};
        Map<Double, List<Particle>> map = new HashMap<>();
        map.put(etas[0], particles);
        map.put(etas[1], particles2);
        map.put(etas[2], particles3);

        new java.io.File(DATA_DIR).mkdirs();
        String outputFile = DATA_DIR + "/" + baseFilename + "-scenario" + scenario + ".txt";
        try (PrintWriter b = new PrintWriter(new FileWriter(outputFile))) {

            for (double val : etas) {
                CIRCULAR_SCENARIO_STEP = 0; // Reset for each eta
                cim.populateGrid(map.get(val));
                neighbors = cim.calculateNeighbors();

                b.println("Eta: " + String.format("%.2f", val));

                for (int i = 0; i < ITERATIONS_B; i++) {

                    b.println(i + " " + calculateOrder(map.get(val)));

                    for (Particle p : map.get(val)) {
                        updateParticle(p, neighbors.get(p.getId()), val);
                    }
                    cim.populateGrid(map.get(val));
                    neighbors = cim.calculateNeighbors();
                }

                b.println(ITERATIONS_B + " " + calculateOrder(map.get(val)));
                b.println();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        System.out.println("Archivo generado: " + outputFile);
    }
}
