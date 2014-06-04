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
        return new Vector(second.x - first.x, second.y - first.y, second.z - first.z);
    }

    public static Vector vectorSum(Vector... vectors) {
        double x = 0;
        double y = 0;
        double z = 0;
        for (Vector vector : vectors) {
            x += vector.x;
            y += vector.y;
            z += vector.z;
        }
        return new Vector(x, y, z);
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
        RealMatrix optimalRmsdTransition = Geometry.findRmsdOptimalTransformationMatrix(x, y);
        List<Point> tx = x
                .stream()
                .map(p -> Geometry.transformPoint(p, optimalRmsdTransition))
                .collect(Collectors.toList());

        double s = 0.0;
        for (int i = 0; i < tx.size(); i++) {
            double d = distance(tx.get(i), y.get(i));
            s += d * d;
        }
        return FastMath.sqrt(s / tx.size());
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
                (v.x * m.getEntry(0, 0) + v.y * m.getEntry(1, 0) + v.z * m.getEntry(2, 0)) / w,
                (v.x * m.getEntry(0, 1) + v.y * m.getEntry(1, 1) + v.z * m.getEntry(2, 1)) / w,
                (v.x * m.getEntry(0, 2) + v.y * m.getEntry(1, 2) + v.z * m.getEntry(2, 2)) / w
        );
    }

    public static Surface transformSurface(Surface surface, RealMatrix m) {
        List<Point> points = surface.points.stream().map(p -> transformPoint(p, m)).collect(Collectors.toList());
        List<Vector> normals = surface.normals.stream().map(n -> transformVector(n, m)).collect(Collectors.toList());

        return new Surface(surface.name, points, normals, surface.faces, surface.lipophilicity, surface.electricity);
    }

    /**
     * Find rotation matrix that rotates a to b.
     */

    public static RealMatrix computeRotationMatrix(Vector a, Vector b) {
        double abLength = a.length() * b.length();
        double sin = b.cross(a).length() / abLength;
        double cos = b.dot(a) / abLength;

        if (Math.abs(sin) < 1e-10) {
            if (cos > 0) {
                return MatrixUtils.createRealDiagonalMatrix(new double[]{1.0, 1.0, 1.0, 1.0});
            } else {
                Vector normal = b.unite();
                double x = normal.x;
                double y = normal.y;
                double z = normal.z;

                return MatrixUtils.createRealMatrix(new double[][]{
                        {1 - 2 * x * x, -2 * x * y, -2 * x * z, 0.0},
                        {-2 * x * y, 1 - 2 * y * y, -2 * y * z, 0.0},
                        {-2 * x * z, -2 * y * z, 1 - 2 * z * z, 0.0},
                        {0.0, 0.0, 0.0, 1.0}
                });
            }
        } else {
            Vector rotationAxis = (b.cross(a)).unite();
            double x = rotationAxis.x;
            double y = rotationAxis.y;
            double z = rotationAxis.z;

            return MatrixUtils.createRealMatrix(new double[][]{
                    {cos + (1 - cos) * x * x, (1 - cos) * x * y - sin * z, (1 - cos) * x * z + sin * y, 0.0},
                    {(1 - cos) * y * x + sin * z, cos + (1 - cos) * y * y, (1 - cos) * y * z - sin * x, 0.0},
                    {(1 - cos) * z * x - sin * y, (1 - cos) * z * y + sin * x, cos + (1 - cos) * z * z, 0.0},
                    {0.0, 0.0, 0.0, 1.0}
            });
        }
    }

}
