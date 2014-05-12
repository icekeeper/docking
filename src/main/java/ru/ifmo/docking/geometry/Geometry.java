package ru.ifmo.docking.geometry;

import org.apache.commons.math3.util.FastMath;

public class Geometry {

    public static double distance(Point p1, Point p2) {
        return FastMath.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y) + (p1.z - p2.z) * (p1.z - p2.z));
    }

    public static Vector vectorFromPoints(Point first, Point second) {
        return new Vector(first.x - second.x, first.y - second.y, first.z - second.z);
    }
}
