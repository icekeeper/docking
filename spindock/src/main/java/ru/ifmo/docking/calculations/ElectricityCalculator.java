package ru.ifmo.docking.calculations;

import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.model.Protein;
import ru.ifmo.docking.util.PdbUtil;

import java.io.File;

public class ElectricityCalculator {

    private final Protein protein;

    public static ElectricityCalculator fromPqr(File pqrFile) {
        return new ElectricityCalculator(PdbUtil.readPqrFile(pqrFile));
    }

    public ElectricityCalculator(Protein protein) {
        this.protein = protein;
    }

    public double calculate(Point p) {
        return protein.getAtoms()
                .stream()
                .mapToDouble(atom -> {
                    double distance = Geometry.distance(atom.p, p);
                    return atom.charge / (Math.max(2.0, distance) * sigma(distance));
                })
                .sum();
    }

    public static double sigma(double r) {
        if (r <= 6) {
            return 4;
        } else if (6 < r && r < 8) {
            return 38 * r - 224;
        } else {
            return 80;
        }
    }
}
