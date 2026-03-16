package ar.edu.itba.sds;

import java.util.Objects;

public class Particle {
    private final int id;
    private double x;
    private double y;
    private double angle;

    public Particle(int id, double x, double y, double angle) {
        this.id = id;
        this.x = x;
        this.y = y;
        this.angle = angle;
    }

    public Particle(Particle other) {
        this(other.id, other.x, other.y, other.angle);
    }

    public double getDistance(Particle other, double L) {
        double dx = Math.abs(this.x - other.x);
        double dy = Math.abs(this.y - other.y);

        if (dx > L / 2) dx = L - dx;
        if (dy > L / 2) dy = L - dy;

        return Math.sqrt(dx * dx + dy * dy);
    }

    public int getId() { return id; }
    public double getX() { return x; }
    public double getY() { return y; }

    public void setX(double x) {
        this.x = x;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getAngle() {
        return angle;
    }

    public void setAngle(double angle) {
        this.angle = angle;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Particle particle = (Particle) o;
        return id == particle.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}