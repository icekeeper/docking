package ru.ifmo.docking.geometry;

import com.google.common.collect.Lists;
import org.apache.commons.math3.util.FastMath;
import ru.ifmo.docking.model.Surface;
import ru.ifmo.docking.util.Pair;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class DistanceGrid {

    final double[] distances;
    final Point minBound;
    final Point maxBound;

    final int xRes;
    final int yRes;
    final int zRes;
    final double step;


    public DistanceGrid(Surface surface, double step, double margin) {
        int surfacePointsCount = surface.points.size();
        System.out.println("Grid construction for surface " + surface.name + " started");
        System.out.println("Surface points count: " + surfacePointsCount);

        this.step = step;
        this.minBound = findMinBound(surface, margin);
        this.maxBound = findMaxBound(surface, margin);

        System.out.println("Min bound: " + this.minBound);
        System.out.println("Max bound: " + this.maxBound);

        xRes = (int) FastMath.round((maxBound.x - minBound.x) / step);
        yRes = (int) FastMath.round((maxBound.y - minBound.y) / step);
        zRes = (int) FastMath.round((maxBound.z - minBound.z) / step);

        System.out.println("XRes: " + xRes + " YRes: " + yRes + " ZRes: " + zRes);

        distances = new double[(xRes + 1) * (yRes + 1) * (zRes + 1)];
        System.out.println("Total grid points count: " + distances.length);

        Arrays.fill(distances, Double.MAX_VALUE);

        int groupSize = surfacePointsCount / Runtime.getRuntime().availableProcessors();
        List<Pair<Integer, Integer>> groups = Lists.newArrayList();
        int k = 0;
        while (k < surfacePointsCount) {
            groups.add(Pair.of(k, Math.min(k + groupSize, surfacePointsCount)));
            k += groupSize;
        }

        List<double[]> groupsResults = groups.parallelStream()
                .map(groupPoints -> {
                    double[] groupDistances = new double[distances.length];
                    Arrays.fill(groupDistances, Double.MAX_VALUE);
                    IntStream.range(groupPoints.first, groupPoints.second)
                            .forEach(i -> {
                                Point p = surface.points.get(i);
                                Vector n = surface.normals.get(i);
                                processSurfacePoint(p, n, groupDistances);
                            });
                    return groupDistances;
                })
                .collect(Collectors.toList());

        for (double[] groupsResult : groupsResults) {
            for (int i = 0; i < groupsResult.length; i++) {
                if (Math.abs(distances[i]) > Math.abs(groupsResult[i])) {
                    distances[i] = groupsResult[i];
                }
            }
        }

        System.out.println("Finished processing surface points");

//        IntStream.rangeClosed(0, zRes)
//                .parallel()
//                .forEach(iz -> {
//                    for (int iy = 0; iy <= yRes; iy++) {
//                        for (int ix = 0; ix <= xRes; ix++) {
//                            int index = assembleIndex(ix, iy, iz);
//                            if (Double.MAX_VALUE == distances[index]) {
//                                processGridPoint(ix, iy, iz, surface);
//                            }
//                        }
//                    }
//                });
//
//        System.out.println("Finished processing grid points");
    }

    public double getDistanceForPoint(Point p) {
        if (isWithinBounds(p)) {
            return distances[getIndexForPoint(p)];
        } else {
            return Double.POSITIVE_INFINITY;
        }
    }

    private void processSurfacePoint(Point p, Vector n, double[] distances) {
        int kx = (int) FastMath.round((p.x - minBound.x) / step);
        int ky = (int) FastMath.round((p.y - minBound.y) / step);
        int kz = (int) FastMath.round((p.z - minBound.z) / step);

        int k = (int) FastMath.round(12.0 / step);

        for (int iz = Math.max(0, kz - k); iz <= Math.min(zRes, kz + k); iz++) {
            for (int iy = Math.max(0, ky - k); iy <= Math.min(yRes, ky + k); iy++) {
                for (int ix = Math.max(0, kx - k); ix <= Math.min(xRes, kx + k); ix++) {
                    int index = assembleIndex(ix, iy, iz);
                    double distance = distanceFromPointToGrid(p, n, ix, iy, iz);
                    if (Math.abs(distances[index]) > Math.abs(distance)) {
                        distances[index] = distance;
                    }
                }
            }
        }
    }

    private void processGridPoint(int x, int y, int z, Surface surface) {
        int index = assembleIndex(x, y, z);
        for (int i = 0; i < surface.points.size(); i++) {
            Point p = surface.points.get(i);
            Vector n = surface.normals.get(i);
            double distance = distanceFromPointToGrid(p, n, x, y, z);
            if (Math.abs(distances[index]) > Math.abs(distance)) {
                distances[index] = distance;
            }
        }
    }

    private double distanceFromPointToGrid(Point p, Vector n, int x, int y, int z) {
        double px = minBound.x + x * step;
        double py = minBound.y + y * step;
        double pz = minBound.z + z * step;
        double distance = FastMath.sqrt((px - p.x) * (px - p.x) + (py - p.y) * (py - p.y) + (pz - p.z) * (pz - p.z));

        Vector k = new Vector(px - p.x, py - p.y, pz - p.z);
        if (n.dot(k) >= 0) {
            return distance;
        } else {
            return -distance;
        }
    }

    private int getIndexForPoint(Point p) {
        int ix = (int) FastMath.round((p.x - minBound.x) / step);
        int iy = (int) FastMath.round((p.y - minBound.y) / step);
        int iz = (int) FastMath.round((p.z - minBound.z) / step);
        return assembleIndex(ix, iy, iz);
    }

    private int assembleIndex(int ix, int iy, int iz) {
        return ix + (iy * (xRes + 1)) + (iz * (yRes + 1) * (xRes + 1));
    }

    private Point findMinBound(Surface surface, double margin) {
        double minX = Double.MAX_VALUE;
        double minY = Double.MAX_VALUE;
        double minZ = Double.MAX_VALUE;

        for (Point point : surface.points) {
            minX = Math.min(point.x, minX);
            minY = Math.min(point.y, minY);
            minZ = Math.min(point.z, minZ);

        }

        return new Point(minX - margin, minY - margin, minZ - margin);
    }

    private Point findMaxBound(Surface surface, double margin) {
        double maxX = Double.MIN_VALUE;
        double maxY = Double.MIN_VALUE;
        double maxZ = Double.MIN_VALUE;

        for (Point point : surface.points) {
            maxX = Math.max(point.x, maxX);
            maxY = Math.max(point.y, maxY);
            maxZ = Math.max(point.z, maxZ);
        }

        return new Point(maxX + margin, maxY + margin, maxZ + margin);
    }

    private boolean isWithinBounds(Point p) {
        return p.x >= minBound.x
                && p.y >= minBound.y
                && p.z >= minBound.z
                && p.x <= maxBound.x
                && p.y <= maxBound.y
                && p.z <= maxBound.z;
    }


}
