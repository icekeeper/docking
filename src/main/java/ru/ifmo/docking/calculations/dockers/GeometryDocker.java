package ru.ifmo.docking.calculations.dockers;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.math3.linear.RealMatrix;
import ru.ifmo.docking.calculations.TomitaTask;
import ru.ifmo.docking.geometry.DistanceGrid;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.geometry.Vector;
import ru.ifmo.docking.model.SpinImage;
import ru.ifmo.docking.model.Surface;
import ru.ifmo.docking.util.Pair;
import ru.ifmo.docking.util.PlyWriter;

import java.io.File;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GeometryDocker implements Docker {

    public static final double MAX_ANGLE_DELTA = Math.PI / 8;
    protected final Surface firstSurface;
    protected final Surface secondSurface;
    protected long start;

    private Map<String, List<SpinImage>> cache = new HashMap<>();

    @Override
    public List<RealMatrix> run() {
        start = System.currentTimeMillis();

        DistanceGrid firstSurfaceGrid = new DistanceGrid(firstSurface, 0.25, 1.5);
        System.out.println((System.currentTimeMillis() - start) + " First surface distance grid constructed");

//        int pairsCount = Math.min(firstSurface.points.size() * secondSurface.points.size() / 1000, 100000);
        int pairsCount = 50000;

        System.out.println("Pairs count will be used: " + pairsCount);

        List<List<PointMatch>> cliques = searchForSolutions(pairsCount);

        return cliques
                .parallelStream()
                .map(GeometryDocker::findTransition)
                .map(transition -> Pair.of(transition, Geometry.transformSurface(secondSurface, transition)))
                .map(pair -> Pair.of(pair.first, score(firstSurfaceGrid, pair.second)))
                .filter(pair -> !Double.isInfinite(pair.second))
                .sorted((o1, o2) -> -Double.compare(o1.second, o2.second))
                .map(Pair::getFirst)
                .collect(Collectors.toList());

    }

    @Override
    public List<RealMatrix> rescore(List<RealMatrix> solutions) {
        DistanceGrid firstSurfaceGrid = new DistanceGrid(firstSurface, 0.25, 1.5);

        return solutions.parallelStream()
                .map(transition -> Pair.of(transition, Geometry.transformSurface(secondSurface, transition)))
                .map(pair -> Pair.of(pair.first, score(firstSurfaceGrid, pair.second)))
                .filter(pair -> !Double.isInfinite(pair.second))
                .sorted((o1, o2) -> -Double.compare(o1.second, o2.second))
                .map(Pair::getFirst)
                .collect(Collectors.toList());
    }

    protected List<List<PointMatch>> searchForSolutions(int pairsCount) {
        List<PointMatch> pointMatches = findTopCorrelatedPairs(pairsCount);
        System.out.println((System.currentTimeMillis() - start) + " Point matched. Smallest correlation is " + pointMatches.get(pointMatches.size() - 1).correlation);

        Map<PointMatch, Set<PointMatch>> graph = constructGraph(pointMatches);
        System.out.println((System.currentTimeMillis() - start) + " Graph constructed");

        List<List<PointMatch>> cliques = findCliques(graph, null);
        System.out.println((System.currentTimeMillis() - start) + " Cliques computation completed. Cliques count: " + cliques.size());
        return cliques;
    }

    public double score(DistanceGrid grid, Surface transformedSecond) {
        int bins[] = new int[4];
        for (Point point : transformedSecond.points) {
            double d = grid.getDistanceForPoint(point);
            if (d < -5.0) {
                return Double.NEGATIVE_INFINITY;
            } else if (d < -3.5) {
                bins[0] += 1;
            } else if (d < -2.0) {
                bins[1] += 1;
            } else if (d < -1.0) {
                bins[2] += 1;
            } else if (d < 1.0) {
                bins[3] += 1;
            }
        }
        if ((bins[3] + bins[2] + bins[1] + bins[0]) > transformedSecond.points.size() * 0.4) {
            return Double.NEGATIVE_INFINITY;
        }
        return (bins[3] - bins[2] - bins[1] * 2.5 - bins[0] * 5);
    }

    public static boolean isNormalsConsistent(List<PointMatch> clique, RealMatrix transitionMatrix) {
        for (PointMatch match : clique) {
            Vector firstNormal = match.getFirstNormal();
            Vector secondNormal = match.getSecondNormal();
            Vector transformedSecond = Geometry.transformVector(secondNormal, transitionMatrix);
            if (firstNormal.dot(transformedSecond) > 0) {
                return false;
            }
        }
        return true;
    }

    public void writeSurfacesWithCliques(List<PointMatch> clique, int num, String prefix) {
        List<String> comments = Lists.newArrayList();
        comments.add("clique №" + num);
        comments.addAll(
                clique.stream().map(pointMatch -> String.format("%s distance=%f", pointMatch, Geometry.distance(
                        pointMatch.getFirstPoint(),
                        pointMatch.getSecondPoint()
                ))).collect(Collectors.toList())
        );

        Set<Integer> firstPoints = clique.stream().map(match -> match.firstIndex).collect(Collectors.toSet());
        Set<Integer> secondPoints = clique.stream().map(match -> match.secondIndex).collect(Collectors.toSet());

        String firstName = prefix + "_clique_" + num + "_" + firstSurface.name + ".ply";
        String secondName = prefix + "_clique_" + num + "_" + secondSurface.name + ".ply";

        writeSurface(firstSurface, firstPoints, new int[]{255, 0, 0}, comments, firstName);
        RealMatrix transitionMatrix = findTransition(clique);
        Surface transformedSecondSurface = Geometry.transformSurface(secondSurface, transitionMatrix);
        writeSurface(transformedSecondSurface, secondPoints, new int[]{0, 0, 255}, comments, secondName);
    }

    public static RealMatrix findTransition(List<PointMatch> clique) {
        List<Point> firstPoints = Lists.newArrayListWithCapacity(clique.size());
        List<Point> secondPoints = Lists.newArrayListWithCapacity(clique.size());

        for (PointMatch match : clique) {
            firstPoints.add(match.getFirstPoint());
            secondPoints.add(match.getSecondPoint());
        }

        return Geometry.findRmsdOptimalTransformationMatrix(secondPoints, firstPoints);
    }

    private void writeSurface(Surface surface, Set<Integer> pointIndices, int[] otherColor, Collection<String> comments, String fileName) {
        File file = new File(fileName);
        int[] blackColor = {124, 124, 124};
        List<int[]> colors = IntStream.range(0, surface.points.size()).mapToObj(i -> {
            if (pointIndices.contains(i)) {
                return otherColor;
            } else {
                return blackColor;
            }
        }).collect(Collectors.toList());
        PlyWriter.writeAsPlyFile(surface, colors, file, comments);
    }

    protected List<List<PointMatch>> findCliques(Map<PointMatch, Set<PointMatch>> graph, Set<PointMatch> startSet) {
        Set<PointMatch> r = Sets.newHashSet();
        Set<PointMatch> p = Sets.newHashSet(graph.keySet());
        Set<PointMatch> x = Sets.newHashSet();

        return ForkJoinPool.commonPool().invoke(new TomitaTask(r, p, x, graph, startSet, firstSurface, secondSurface));
    }


    public GeometryDocker(File complexDir, String complex) {
        String unboundReceptorFile = complex + "_r_" + "u";
        String unboundLigandFile = complex + "_l_" + "u";

        this.firstSurface = Surface.read(unboundReceptorFile,
                new File(complexDir, unboundReceptorFile + ".obj"),
                new File(complexDir, unboundReceptorFile + ".pdb"),
                new File(complexDir, unboundReceptorFile + ".pqr"),
                new File("fi_potentials.txt")
        );

        this.secondSurface = Surface.read(unboundLigandFile,
                new File(complexDir, unboundLigandFile + ".obj"),
                new File(complexDir, unboundLigandFile + ".pdb"),
                new File(complexDir, unboundLigandFile + ".pqr"),
                new File("fi_potentials.txt")
        );

    }

    protected Map<PointMatch, Set<PointMatch>> constructGraph(List<PointMatch> matches) {
        Map<PointMatch, Set<PointMatch>> result = IntStream.range(0, matches.size())
                .parallel()
                .boxed()
                .collect(Collectors.toConcurrentMap(
                        matches::get,
                        i -> IntStream.range(i + 1, matches.size())
                                .filter(j -> isGoodPair(matches.get(i), matches.get(j)))
                                .mapToObj(matches::get)
                                .collect(Collectors.toCollection(HashSet::new))
                ));

        for (PointMatch a : result.keySet()) {
            for (PointMatch b : result.get(a)) {
                result.get(b).add(a);
            }
        }


        return result;
    }


    protected List<PointMatch> findTopCorrelatedPairs(int count) {
        List<SpinImage> firstStack = computeSpinImageStack(firstSurface);
        System.out.println((System.currentTimeMillis() - start) + " First stack computed");
        List<SpinImage> secondStack = computeSpinImageStack(secondSurface);
        System.out.println((System.currentTimeMillis() - start) + " Second stack computed");

        List<List<SpinImage>> partition = Lists.partition(secondStack, secondStack.size() / Runtime.getRuntime().availableProcessors());

        List<PointMatch> result = partition.parallelStream()
                .flatMap(part -> {
                    Queue<PointMatch> queue = new PriorityQueue<>((o1, o2) -> Double.compare(o1.correlation, o2.correlation));
                    for (SpinImage firstImage : firstStack) {
                        for (SpinImage secondImage : part) {
                            double correlation = firstImage.correlation(secondImage);
                            if (queue.size() < count || queue.peek().correlation < correlation) {
                                queue.add(new PointMatch(firstImage.getPointIndex(), secondImage.getPointIndex(), correlation));
                            }
                            if (queue.size() > count) {
                                queue.poll();
                            }
                        }
                    }
                    return queue.stream();
                })
                .collect(Collectors.toList());

        result.sort((o1, o2) -> -Double.compare(o1.correlation, o2.correlation));

        return Lists.newArrayList(result.subList(0, count));
    }

    private boolean isGoodPair(PointMatch firstMatch, PointMatch secondMatch) {
        if (firstMatch.firstIndex == secondMatch.firstIndex || firstMatch.secondIndex == secondMatch.secondIndex) {
            return false;
        }

        double firstDistance = Geometry.distance(firstMatch.getFirstPoint(), secondMatch.getFirstPoint());
        double secondDistance = Geometry.distance(firstMatch.getSecondPoint(), secondMatch.getSecondPoint());
        double distDelta = Math.abs(firstDistance - secondDistance);

        if (distDelta > 1.0) {
            return false;
        }

        //for angle between each surface point normals
        double firstAngle = firstMatch.getFirstNormal().angle(secondMatch.getFirstNormal());
        double secondAngle = firstMatch.getSecondNormal().angle(secondMatch.getSecondNormal());
        double normalsAngleDelta = Math.abs(firstAngle - secondAngle);

        if (normalsAngleDelta > MAX_ANGLE_DELTA) {
            return false;
        }

        Vector firstLineVector = Geometry.vectorFromPoints(secondMatch.getFirstPoint(), firstMatch.getFirstPoint()).unite();
        Vector secondLineVector = Geometry.vectorFromPoints(secondMatch.getSecondPoint(), firstMatch.getSecondPoint()).unite();

        //for angle between line and first normal of pair
        double firstStartLineAngle = firstMatch.getFirstNormal().angle(firstLineVector);
        double secondStartLineAngle = firstMatch.getSecondNormal().angle(secondLineVector);
        double startLineAngleDelta = Math.abs(Math.PI - firstStartLineAngle - secondStartLineAngle);

        if (startLineAngleDelta > MAX_ANGLE_DELTA) {
            return false;
        }

        //for angle between line and second normal of pair
        double firstEndLineAngle = secondMatch.getFirstNormal().angle(firstLineVector);
        double secondEndLineAngle = secondMatch.getSecondNormal().angle(secondLineVector);
        double endLineAngleDelta = Math.abs(Math.PI - firstEndLineAngle - secondEndLineAngle);

        return endLineAngleDelta < MAX_ANGLE_DELTA;
    }

    protected List<SpinImage> computeSpinImageStack(Surface surface) {
        if (!cache.containsKey(surface.name)) {
            cache.put(surface.name, IntStream.range(0, surface.points.size())
                    .parallel()
                    .mapToObj(i -> SpinImage.compute(i, surface, 6.0, 1.0))
                    .collect(Collectors.toList()));
        }
        return cache.get(surface.name);
    }

    private static double getMaxPenetration(DistanceGrid grid, Surface surface) {
        return surface.points.stream().mapToDouble(grid::getDistanceForPoint).min().getAsDouble();
    }

    public class PointMatch {
        final int firstIndex;
        final int secondIndex;
        final double correlation;

        public Point getFirstPoint() {
            return firstSurface.points.get(firstIndex);
        }

        public Point getSecondPoint() {
            return secondSurface.points.get(secondIndex);
        }

        public Vector getFirstNormal() {
            return firstSurface.normals.get(firstIndex);
        }

        public Vector getSecondNormal() {
            return secondSurface.normals.get(secondIndex);
        }

        public double getCorrelation() {
            return correlation;
        }

        PointMatch(int firstPointIndex, int secondPointIndex, double correlation) {
            this.firstIndex = firstPointIndex;
            this.secondIndex = secondPointIndex;
            this.correlation = correlation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PointMatch that = (PointMatch) o;

            return firstIndex == that.firstIndex && secondIndex == that.secondIndex;

        }

        @Override
        public int hashCode() {
            int result = firstIndex;
            result = 31 * result + secondIndex;
            return result;
        }

        @Override
        public String toString() {
            return "PointMatch{" +
                    "firstIndex=" + firstIndex +
                    ", secondIndex=" + secondIndex +
                    ", correlation=" + correlation +
                    '}';
        }
    }
}
