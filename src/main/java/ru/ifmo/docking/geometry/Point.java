package ru.ifmo.docking.geometry;

import org.apache.commons.math3.util.FastMath;

public class Point {
    public final double x;
    public final double y;
    public final double z;

    public Point(double x, double y, double z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    public double distance(Point to) {
        return FastMath.sqrt((x - to.x) * (x - to.x) + (y - to.y) * (y - to.y) + (z - to.z) * (z - to.z));
    }

    public Vector asVector() {
        return new Vector(x, y, z);
    }

    @Override
    public String toString() {
        return "(" + x + ", " + y + ", " + z + ")";
    }
}
