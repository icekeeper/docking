package ru.ifmo.docking.model;

import ru.ifmo.docking.geometry.Point;

public class Atom {
    final public String s;
    final public Point p;
    final public double fi;

    public Atom(double x, double y, double z, double fi) {
        this.p = new Point(x, y, z);
        this.fi = fi;
        this.s = null;
    }

    public Atom(double x, double y, double z, String s) {
        this.p = new Point(x, y, z);
        this.s = s;
        this.fi = 0;
    }

}
