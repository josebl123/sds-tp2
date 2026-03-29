package ar.edu.itba.sds;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

import static ar.edu.itba.sds.Utils.*;

public class PostProcessing {
    public static void main(String[] args) {
        try (BufferedReader reader = new BufferedReader(new FileReader("file.txt"))) {
            String line;
            int currentIteration = -1;
            double velocityAccX = 0;
            double velocityAccY = 0;
            List<Double> orders = new ArrayList<>();

            while ((line = reader.readLine()) != null) {

                line = line.trim();

                if (line.isEmpty()) continue;

                String[] parts = line.split("\\s+");

                if (parts.length == 1) {
                    if (currentIteration >= 0) {
                        double order = calculateOrder(velocityAccX, velocityAccY);
                        orders.add(order);
                    }
                    currentIteration = Integer.parseInt(parts[0]);
                    velocityAccX = 0;
                    velocityAccY = 0;

                } else if (parts.length == 4) {
                    double x = Double.parseDouble(parts[0]);
                    double y = Double.parseDouble(parts[1]);
                    double vx = Double.parseDouble(parts[2]);
                    double vy = Double.parseDouble(parts[3]);
                    velocityAccX += vx;
                    velocityAccY += vy;

                } else {
                    System.out.println("Invalid line: " + line);
                }
            }
        } catch (Exception e) {
            throw new RuntimeException();
        }
    }
}
