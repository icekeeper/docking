package ru.ifmo.docking.geometry;

public class Vector {
    public final double x;
    public final double y;
    public final double z;

    public Vector(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public Vector unite() {
        double length = length();
        return new Vector(x / length, y / length, z / length);
    }

    public double dot(Vector that) {
        return this.x * that.x + this.y * that.y + this.z * that.z;
    }

    public double length() {
        return Math.sqrt(dot(this));
    }

}
