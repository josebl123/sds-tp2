package ar.edu.itba.sds;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static ar.edu.itba.sds.Utils.*;

public class SimulationB {

    public static void main(String[] args) {
        List<Particle> particles = generateParticles();
        List<Particle> particles2 = particles.stream().map(Particle::new).collect(java.util.stream.Collectors.toList());;
        List<Particle> particles3 = particles.stream().map(Particle::new).collect(java.util.stream.Collectors.toList());;

        CellIndexMethod cim = new CellIndexMethod(M, INTERACTION_RADIUS);
        Map<Integer, Set<Particle>> neighbors;

        String baseFilename = String.valueOf(System.currentTimeMillis() / 1000).concat("B");

        double[] etas = {0, Math.PI, Math.PI * 2};
        Map<Double, List<Particle>> map = new HashMap<>();
        map.put(etas[0], particles);
        map.put(etas[1], particles2);
        map.put(etas[2], particles3);

        try (PrintWriter b = new PrintWriter(new FileWriter(DATA_DIR + "/" + baseFilename + "-B.txt"))) {

            for (double val : etas) {
                cim.populateGrid(map.get(val));
                neighbors = cim.calculateNeighbors();

                b.println("Eta: " + val);

                for (int i = 0; i < ITERATIONS; i++) {

                    b.println(i + " " + calculateOrder(map.get(val)));

                    for (Particle p : map.get(val)) {
                        updateParticle(p, neighbors.get(p.getId()), val);
                    }
                    neighbors = cim.calculateNeighbors();
                }

                b.println(ITERATIONS + " " + calculateOrder(map.get(val)));
                b.println();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
