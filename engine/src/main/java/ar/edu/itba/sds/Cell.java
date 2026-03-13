package ar.edu.itba.sds;


import java.util.ArrayList;
import java.util.List;

public class Cell {
    private final int row;
    private final int col;
    private final List<Particle> particles;

    public Cell(int row, int col) {
        this.row = row;
        this.col = col;
        this.particles = new ArrayList<>();
    }

    public void addParticle(Particle p) {
        this.particles.add(p);
    }

    public List<Particle> getParticles() {
        return particles;
    }

    public int getRow() { return row; }
    public int getCol() { return col; }
}