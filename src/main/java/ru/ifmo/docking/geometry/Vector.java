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

    public Vector cross(Vector that) {
        return new Vector(this.y * that.z - this.z * that.y, this.z * that.x - this.x * that.z, this.x * that.y - this.y * that.x);
    }

    public Vector mul(double k) {
        return new Vector(k * x, k * y, k * z);
    }

    public Vector sub(Vector that) {
        return new Vector(x - that.x, y - that.y, z - that.z);
    }

    public Vector add(Vector that) {
        return new Vector(x + that.x, y + that.y, z + that.z);
    }

    public double length() {
        return Math.sqrt(dot(this));
    }

    public double lengthSqr() {
        return dot(this);
    }

    public Point asPoint() {
        return new Point(x, y, z);
    }


}
