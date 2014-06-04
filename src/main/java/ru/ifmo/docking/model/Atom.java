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
    final public String pdbCharge;
    final public double charge;
    final public double r;
    private String resUid = null;

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
                String pdbCharge,
                double charge,
                double r) {
        this(
                recordName,
                serial,
                name,
                altLock,
                resName,
                chainId,
                resSeq,
                iCode,
                new Point(x, y, z),
                occupancy,
                tempFactor,
                element,
                pdbCharge,
                charge,
                r
        );
    }

    public Atom(String recordName,
                int serial,
                String name,
                char altLock,
                String resName,
                char chainId,
                int resSeq,
                char iCode,
                Point p,
                double occupancy,
                double tempFactor,
                String element,
                String pdbCharge,
                double charge,
                double r) {
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
        this.pdbCharge = pdbCharge;
        this.charge = charge;
        this.r = r;
        this.p = p;
    }

    public Point getPoint() {
        return p;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Atom atom = (Atom) o;

        return chainId == atom.chainId
                && resSeq == atom.resSeq
                && serial == atom.serial
                && name.equals(atom.name)
                && recordName.equals(atom.recordName)
                && resName.equals(atom.resName);

    }

    @Override
    public int hashCode() {
        int result = recordName.hashCode();
        result = 31 * result + serial;
        result = 31 * result + name.hashCode();
        result = 31 * result + resName.hashCode();
        result = 31 * result + (int) chainId;
        result = 31 * result + resSeq;
        return result;
    }

    @Override
    public String toString() {
        return "Atom{"
                + "\nrecordName='" + recordName + '\''
                + ",\nserial=" + serial
                + ",\nname='" + name + '\''
                + ",\naltLock=" + altLock
                + ",\nresName='" + resName + '\''
                + ",\nchainId=" + chainId
                + ",\nresSeq=" + resSeq
                + ",\niCode=" + iCode
                + ",\noccupancy=" + occupancy
                + ",\ntempFactor=" + tempFactor
                + ",\nelement='" + element + '\''
                + ",\npdbCharge='" + pdbCharge + '\''
                + ",\ncharge=" + charge
                + ",\nr=" + r
                + ",\np=" + p
                + "\n}";
    }

    public String getResidueUid() {
        if (resUid == null) {
            resUid = resName + "_" + resSeq + "_" + iCode;
        }
        return resUid;
    }
}
