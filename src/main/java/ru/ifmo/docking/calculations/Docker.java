package ru.ifmo.docking.calculations;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.math3.linear.RealMatrix;
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
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Docker {

    public static final double MAX_ANGLE_DELTA = Math.PI / 8;
    private static final double PENETRATION_THRESHOLD = -5.0;
    private final Surface firstSurface;
    private final Surface secondSurface;
    private long start;

    public List<Pair<List<PointMatch>, RealMatrix>> run() {
        start = System.currentTimeMillis();

        DistanceGrid firstSurfaceGrid = new DistanceGrid(firstSurface, 0.2, 0.2);
        System.out.println((System.currentTimeMillis() - start) + " First surface distance grid constructed");

        List<PointMatch> pointMatches = findTopCorrelatedPairs(50000);
        System.out.println((System.currentTimeMillis() - start) + " Point matched. Smallest correlation is " + pointMatches.get(pointMatches.size() - 1).correlation);
        System.out.println("Close points in first correlated pairs:");

        for (int index = 0; index < pointMatches.size(); index++) {
            PointMatch match = pointMatches.get(index);
            if (Geometry.distance(match.getFirstPoint(), match.getSecondPoint()) < 1.0) {
                System.out.println(index + " " + match);
            }
        }

        Map<PointMatch, Set<PointMatch>> graph = constructGraph(pointMatches);
        System.out.println((System.currentTimeMillis() - start) + " Graph constructed");

        int edgesCount = graph.values().stream().mapToInt(Set::size).sum() / 2;
        System.out.println("Edges count: " + edgesCount);

        List<List<PointMatch>> cliques = findCliques(graph, null);
        System.out.println((System.currentTimeMillis() - start) + " Cliques computation completed. Cliques count: " + cliques.size());


//        printCliquesInfo(firstSurfaceGrid, cliques);

        List<Pair<List<PointMatch>, RealMatrix>> cliquesWithTransitions = cliques
                .parallelStream()
                .map(clique -> Pair.of(clique, findTransition(clique)))
                .collect(Collectors.toList());

        return cliquesWithTransitions
                .parallelStream()
                .filter(clique -> getMaxPenetration(firstSurfaceGrid, Geometry.transformSurface(secondSurface, clique.second)) > PENETRATION_THRESHOLD)
                .collect(Collectors.toList());

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

    private void printCliquesInfo(DistanceGrid firstSurfaceGrid, List<List<PointMatch>> cliques) {
        List<PointMatch> maxClique = cliques.stream().max((s1, s2) -> Integer.compare(s1.size(), s2.size())).get();
        List<PointMatch> minClique = cliques.stream().min((s1, s2) -> Integer.compare(s1.size(), s2.size())).get();

        System.out.println((System.currentTimeMillis() - start) + " Max clique has size: " + maxClique.size());
        System.out.println((System.currentTimeMillis() - start) + " Min clique has size: " + minClique.size());

        List<List<PointMatch>> contactCliques = cliques
                .parallelStream()
                .filter(clique -> clique.stream()
                                .filter(match -> Geometry.distance(match.getFirstPoint(), match.getSecondPoint()) < 1.0)
                                .count() > 2
                )
                .collect(Collectors.toList());

        System.out.println((System.currentTimeMillis() - start) + " Contact cliques count: " + contactCliques.size());

        for (int i = 0; i < contactCliques.size(); i++) {
            List<PointMatch> contactClique = contactCliques.get(i);
            System.out.println("------start-----");
            contactClique.forEach(match -> System.out.println(
                            String.format("%s distance=%f",
                                    match,
                                    Geometry.distance(match.getFirstPoint(), match.getSecondPoint())
                            )
                    )
            );
            System.out.println("Max penetration: " + getMaxPenetration(firstSurfaceGrid, Geometry.transformSurface(secondSurface, findTransition(contactClique))));
            writeSurfacesWithCliques(contactClique, i + 1, "close");
            System.out.println("------end-----");
        }

        List<List<PointMatch>> notPenetrated = contactCliques
                .parallelStream()
                .filter(clique -> getMaxPenetration(firstSurfaceGrid, Geometry.transformSurface(secondSurface, findTransition(clique))) > PENETRATION_THRESHOLD)
                .collect(Collectors.toList());

        System.out.println("Contact cliques without penetration count: " + notPenetrated.size());

        Map<Integer, List<List<PointMatch>>> grouped = cliques.stream().collect(Collectors.groupingBy(List::size));
        ArrayList<Integer> keys = Lists.newArrayList(grouped.keySet());
        Collections.sort(keys);
        keys.forEach(key -> System.out.println("Cliques of size " + key + ": " + grouped.get(key).size()));
//
//        List<List<PointMatch>> maxCliques = grouped.get(maxClique.size());
//        for (int i = 0; i < maxCliques.size(); i++) {
//            List<PointMatch> clique = maxCliques.get(i);
//            writeSurfacesWithCliques(firstSurfaceGrid, clique, i + 1, "max");
//        }
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

    private List<List<PointMatch>> findCliques(Map<PointMatch, Set<PointMatch>> graph, Set<PointMatch> startSet) {
        Set<PointMatch> r = Sets.newHashSet();
        Set<PointMatch> p = Sets.newHashSet(graph.keySet());
        Set<PointMatch> x = Sets.newHashSet();

        return ForkJoinPool.commonPool().invoke(new TomitaTask(r, p, x, graph, startSet, firstSurface, secondSurface));
    }


    public Docker(Surface firstSurface, Surface secondSurface) {
        this.firstSurface = firstSurface;
        this.secondSurface = secondSurface;
    }

    private Map<PointMatch, Set<PointMatch>> constructGraph(List<PointMatch> matches) {
        return matches.parallelStream()
                .collect(
                        Collectors.toConcurrentMap(
                                Function.<PointMatch>identity(),
                                match -> matches.stream()
                                        .filter(otherMatch -> !match.equals(otherMatch) && isGoodPair(match, otherMatch))
                                        .collect(Collectors.toSet())
                        )
                );
    }


    private List<PointMatch> findTopCorrelatedPairs(int count) {
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

    private static List<SpinImage> computeSpinImageStack(Surface surface) {
        return IntStream.range(0, surface.points.size())
                .parallel()
                .mapToObj(i -> SpinImage.compute(i, surface, 6.0, 1.0))
                .collect(Collectors.toList());
    }

    private static double getMaxPenetration(DistanceGrid grid, Surface surface) {
        return surface.points.parallelStream().mapToDouble(grid::getDistanceForPoint).min().getAsDouble();
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

        private PointMatch(int firstPointIndex, int secondPointIndex, double correlation) {
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
