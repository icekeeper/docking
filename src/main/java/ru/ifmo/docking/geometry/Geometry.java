package ru.ifmo.docking.geometry;

import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.SingularValueDecomposition;
import org.apache.commons.math3.util.FastMath;
import ru.ifmo.docking.model.Surface;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class Geometry {

    public static double distance(Point p1, Point p2) {
        return FastMath.sqrt((p1.x - p2.x) * (p1.x - p2.x) + (p1.y - p2.y) * (p1.y - p2.y) + (p1.z - p2.z) * (p1.z - p2.z));
    }

    public static Vector vectorFromPoints(Point first, Point second) {
        return new Vector(first.x - second.x, first.y - second.y, first.z - second.z);
    }

    public static Point centroid(Collection<Point> points) {
        double x = 0.0;
        double y = 0.0;
        double z = 0.0;
        for (Point point : points) {
            x += point.x;
            y += point.y;
            z += point.z;
        }
        return new Point(x / points.size(), y / points.size(), z / points.size());
    }

    public static double rmsd(List<Point> x, List<Point> y) {
        double s = 0.0;
        for (int i = 0; i < x.size(); i++) {
            double d = distance(x.get(i), y.get(i));
            s += d * d;
        }
        return FastMath.sqrt(s / x.size());
    }

    /**
     * Finds translation matrix U such that rmsd of two sets Ux, y is minimal.
     * <p>
     * Algorithm from http://cnx.org/content/m11608/latest/
     * <p>
     * See also http://www.math.unm.edu/~vageli/papers/rmsd17.pdf
     */
    public static RealMatrix findRmsdOptimalTransformationMatrix(List<Point> x, List<Point> y) {
        Point xc = centroid(x);
        Point yc = centroid(y);

        RealMatrix xm = constructCenteredPointsMatrix(x, xc);
        RealMatrix ym = constructCenteredPointsMatrix(y, yc);
        RealMatrix c = xm.transpose().multiply(ym);
        SingularValueDecomposition svd = new SingularValueDecomposition(c);
        double d = determinant(svd.getU()) * determinant(svd.getV()) < 0 ? -1.0 : 1.0;

        RealMatrix s = MatrixUtils.createRealDiagonalMatrix(new double[]{1.0, 1.0, d});
        RealMatrix u = svd.getU().multiply(s).multiply(svd.getVT());


        RealMatrix rotationMatrix = MatrixUtils.createRealMatrix(new double[][]{
                {u.getEntry(0, 0), u.getEntry(0, 1), u.getEntry(0, 2), 0.0},
                {u.getEntry(1, 0), u.getEntry(1, 1), u.getEntry(1, 2), 0.0},
                {u.getEntry(2, 0), u.getEntry(2, 1), u.getEntry(2, 2), 0.0},
                {0.0, 0.0, 0.0, 1.0}
        });

        Point origin = new Point(0, 0, 0);
        RealMatrix firstTransitionMatrix = getTransitionMatrix(xc, origin);
        RealMatrix secondTransitionMatrix = getTransitionMatrix(origin, yc);

        return firstTransitionMatrix.multiply(rotationMatrix).multiply(secondTransitionMatrix);
    }

    /**
     * Finds transition matrix from point 'a' to point 'b'
     */

    public static RealMatrix getTransitionMatrix(Point a, Point b) {
        return MatrixUtils.createRealMatrix(new double[][]{
                {1.0, 0.0, 0.0, 0.0},
                {0.0, 1.0, 0.0, 0.0},
                {0.0, 0.0, 1.0, 0.0},
                {b.x - a.x, b.y - a.y, b.z - a.z, 1.0}
        });
    }

    private static double determinant(RealMatrix matrix) {
        return new LUDecomposition(matrix).getDeterminant();
    }


    private static RealMatrix constructCenteredPointsMatrix(List<Point> points, Point centroid) {
        double[][] m = new double[points.size()][3];
        for (int i = 0; i < points.size(); i++) {
            Point point = points.get(i);
            m[i][0] = point.x - centroid.x;
            m[i][1] = point.y - centroid.y;
            m[i][2] = point.z - centroid.z;
        }
        return MatrixUtils.createRealMatrix(m);
    }

    public static Point transformPoint(Point p, RealMatrix m) {
        double w = p.x * m.getEntry(0, 3) + p.y * m.getEntry(1, 3) + p.z * m.getEntry(2, 3) + m.getEntry(3, 3);
        return new Point(
                (p.x * m.getEntry(0, 0) + p.y * m.getEntry(1, 0) + p.z * m.getEntry(2, 0) + m.getEntry(3, 0)) / w,
                (p.x * m.getEntry(0, 1) + p.y * m.getEntry(1, 1) + p.z * m.getEntry(2, 1) + m.getEntry(3, 1)) / w,
                (p.x * m.getEntry(0, 2) + p.y * m.getEntry(1, 2) + p.z * m.getEntry(2, 2) + m.getEntry(3, 2)) / w
        );
    }

    public static Vector transformVector(Vector v, RealMatrix m) {
        double w = v.x * m.getEntry(0, 3) + v.y * m.getEntry(1, 3) + v.z * m.getEntry(2, 3) + m.getEntry(3, 3);
        return new Vector(
                (v.x * m.getEntry(0, 0) + v.y * m.getEntry(1, 0) + v.z * m.getEntry(2, 0) + m.getEntry(3, 0)) / w,
                (v.x * m.getEntry(0, 1) + v.y * m.getEntry(1, 1) + v.z * m.getEntry(2, 1) + m.getEntry(3, 1)) / w,
                (v.x * m.getEntry(0, 2) + v.y * m.getEntry(1, 2) + v.z * m.getEntry(2, 2) + m.getEntry(3, 2)) / w
        );
    }

    public static Surface transformSurface(Surface surface, RealMatrix m) {
        List<Point> points = surface.points.stream().map(p -> transformPoint(p, m)).collect(Collectors.toList());
        List<Vector> normals = surface.normals.stream().map(n -> transformVector(n, m)).collect(Collectors.toList());

        return new Surface(surface.name, points, normals, surface.faces, surface.lipophilicity, surface.electricity);
    }

}
