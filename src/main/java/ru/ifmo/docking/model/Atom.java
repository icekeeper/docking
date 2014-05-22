package ru.ifmo.docking.model;

import ru.ifmo.docking.geometry.Point;

public class Atom {
    final public String recordName;
    final public int serial;
    final public String name;
    final public char altLock;
    final public String resName;
    final public char chainId;
    final public int resSeq;
    final public char iCode;
    final public double occupancy;
    final public double tempFactor;
    final public String element;
    final public String charge;

    final public Point p;

    public Atom(String recordName,
                int serial,
                String name,
                char altLock,
                String resName,
                char chainId,
                int resSeq,
                char iCode,
                double x,
                double y,
                double z,
                double occupancy,
                double tempFactor,
                String element,
                String charge) {
        this.recordName = recordName;
        this.serial = serial;
        this.name = name;
        this.altLock = altLock;
        this.resName = resName;
        this.chainId = chainId;
        this.resSeq = resSeq;
        this.iCode = iCode;
        this.occupancy = occupancy;
        this.tempFactor = tempFactor;
        this.element = element;
        this.charge = charge;
        this.p = new Point(x, y, z);
    }

    public Point getPoint() {
        return p;
    }
}
