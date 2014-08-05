package ru.ifmo.docking.model;

import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.geometry.Vector;

public class SpinImage {
    private final int pointIndex;
    private final double[][] bins;

    private SpinImage(int pointIndex, double[][] bins) {
        this.pointIndex = pointIndex;
        this.bins = bins;
    }

    public int getPointIndex() {
        return pointIndex;
    }

    public static SpinImage compute(int pointIndex, Surface surface, double radius, double binSize) {
        int iMax = (int) Math.round(Math.floor(2 * radius / binSize)) + 1;
        int jMax = (int) Math.round(Math.floor(radius / binSize)) + 1;

        double[][] bins = new double[iMax + 1][jMax + 1];

        Point basePoint = surface.points.get(pointIndex);
        Vector normal = surface.normals.get(pointIndex);

        for (int index = 0; index < surface.points.size(); index++) {
            Point point = surface.points.get(index);

            if (Geometry.distance(basePoint, point) < radius) {
                Vector p = Geometry.vectorFromPoints(basePoint, point);
                double b = normal.dot(p);
                double a = Math.sqrt(p.length() * p.length() - b * b);

                int i = (int) Math.round(Math.floor((radius - b) / binSize));
                int j = (int) Math.round(Math.floor(a / binSize));

                double bilA = (radius - b) / binSize - i;
                double bilB = a / binSize - j;

                bins[i][j] += (1 - bilA) * (1 - bilB);
                bins[i + 1][j] += bilA * (1 - bilB);
                bins[i][j + 1] += (1 - bilA) * bilB;
                bins[i + 1][j + 1] += bilA * bilB;
            }
        }

        return new SpinImage(pointIndex, bins);
    }

    public double correlation(SpinImage that) {
        int n = 0;
        double sumP = 0;
        double sumQ = 0;
        double sumPP = 0;
        double sumQQ = 0;
        double sumPQ = 0;

        int maxI = bins.length;
        int maxJ = bins[0].length;

        for (int i = 0; i < maxI; i++) {
            for (int j = 0; j < maxJ; j++) {
                double p = this.bins[i][j];
                double q = that.bins[maxI - i - 1][j];

                if (p != 0 || q != 0) {
                    n += 1;
                    sumP += p;
                    sumQ += q;
                    sumPP += p * p;
                    sumQQ += q * q;
                    sumPQ += p * q;
                }
            }
        }

        if (sumP == 0 || sumQ == 0) {
            return 0;
        } else {
            return (n * sumPQ - sumP * sumQ) / Math.sqrt((n * sumPP - sumP * sumP) * (n * sumQQ - sumQ * sumQ));
        }
    }

}
