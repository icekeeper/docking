package ru.ifmo.docking.calculations;

import com.google.common.collect.Lists;
import org.apache.commons.math3.linear.RealMatrix;
import ru.ifmo.docking.geometry.DistanceGrid;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.geometry.Vector;
import ru.ifmo.docking.model.SpinImage;
import ru.ifmo.docking.model.Surface;
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

    public List<RealMatrix> run() {
        start = System.currentTimeMillis();

        DistanceGrid firstSurfaceGrid = new DistanceGrid(firstSurface, 0.2, 0.5);
        System.out.println((System.currentTimeMillis() - start) + " First surface distance grid constructed");

        List<PointMatch> pointMatches = findTopCorrelatedPairs(50000);
        System.out.println((System.currentTimeMillis() - start) + " Point matched. Smallest correlation is " + pointMatches.get(pointMatches.size() - 1).correlation);
        System.out.println("Close points in first 50000 correlated pairs:");

        for (int index = 0; index < pointMatches.size(); index++) {
            PointMatch match = pointMatches.get(index);
            if (Geometry.distance(firstSurface.points.get(match.firstPointIndex), secondSurface.points.get(match.secondPointIndex)) < 1.0) {
                System.out.println(index + " " + match);
            }
        }

        Map<PointMatch, Set<PointMatch>> graph = constructGraph(pointMatches);
        System.out.println((System.currentTimeMillis() - start) + " Graph constructed");

        int edgesCount = graph.values().stream().mapToInt(Set::size).sum() / 2;
        System.out.println("Edges count: " + edgesCount);

        List<List<PointMatch>> cliques = findCliques(graph, null);
//        List<List<PointMatch>> cliques = findCliques(graph, Sets.newHashSet(pointMatches.subList(0, 10000)));
        System.out.println((System.currentTimeMillis() - start) + " Cliques computation completed. Cliques count: " + cliques.size());


//        printCliquesInfo(firstSurfaceGrid, cliques);

        Map<List<PointMatch>, RealMatrix> cliqueToTransition = cliques.stream().collect(Collectors.toMap(Function.<List<PointMatch>>identity(), this::findTransition));

        List<RealMatrix> consistentTransitions = cliqueToTransition.keySet()
                .parallelStream()
                .filter(clique -> isNormalsConsistent(clique, cliqueToTransition.get(clique)))
                .map(cliqueToTransition::get)
                .collect(Collectors.toList());

        System.out.println("Consistent transitions count: " + consistentTransitions.size());

        return consistentTransitions.parallelStream()
                .filter(transition -> getMaxPenetration(firstSurfaceGrid, Geometry.transformSurface(secondSurface, transition)) > PENETRATION_THRESHOLD)
                .collect(Collectors.toList());

    }

    private boolean isNormalsConsistent(List<PointMatch> clique, RealMatrix transitionMatrix) {
        for (PointMatch match : clique) {
            Vector firstNormal = firstSurface.normals.get(match.firstPointIndex);
            Vector secondNormal = secondSurface.normals.get(match.secondPointIndex);
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

        List<List<PointMatch>> contactCliques = cliques.stream()
                .filter(clique -> clique.stream()
                                .filter(match ->
                                        Geometry.distance(
                                                firstSurface.points.get(match.firstPointIndex),
                                                secondSurface.points.get(match.secondPointIndex)
                                        ) < 1.0)
                                .count() > 2
                )
                .collect(Collectors.toList());

        System.out.println((System.currentTimeMillis() - start) + " Contact cliques count: " + contactCliques.size());

        List<List<PointMatch>> consistent = contactCliques.stream()
                .filter(clique -> isNormalsConsistent(clique, findTransition(clique)))
                .collect(Collectors.toList());

        System.out.println("Consistent contact cliques count: " + consistent.size());

        for (int i = 0; i < contactCliques.size(); i++) {
            List<PointMatch> contactClique = contactCliques.get(i);
            System.out.println("------start-----");
            System.out.println("Is consistent: " + consistent.contains(contactClique));
            contactClique.forEach(match -> System.out.println(String.format("%s distance=%f", match, Geometry.distance(
                            firstSurface.points.get(match.firstPointIndex),
                            secondSurface.points.get(match.secondPointIndex)
                    )))
            );
            System.out.println("Max penetration: " + getMaxPenetration(firstSurfaceGrid, Geometry.transformSurface(secondSurface, findTransition(contactClique))));
            writeSurfacesWithCliques(firstSurfaceGrid, contactClique, i + 1, "close");
            System.out.println("------end-----");
        }

        Map<Integer, List<List<PointMatch>>> grouped = cliques.stream().collect(Collectors.groupingBy(List::size));
        ArrayList<Integer> keys = Lists.newArrayList(grouped.keySet());
        Collections.sort(keys);
        keys.forEach(key -> System.out.println("Bins of size " + key + ": " + grouped.get(key).size()));

        List<List<PointMatch>> maxCliques = grouped.get(maxClique.size());
        for (int i = 0; i < maxCliques.size(); i++) {
            List<PointMatch> clique = maxCliques.get(i);
            writeSurfacesWithCliques(firstSurfaceGrid, clique, i + 1, "max");
        }
    }

    private void writeSurfacesWithCliques(DistanceGrid firstSurfaceGrid, List<PointMatch> clique, int num, String prefix) {
        List<String> comments = Lists.newArrayList();
        comments.add("clique №" + num);
        comments.addAll(
                clique.stream().map(pointMatch -> String.format("%s distance=%f", pointMatch, Geometry.distance(
                        firstSurface.points.get(pointMatch.firstPointIndex),
                        secondSurface.points.get(pointMatch.secondPointIndex)
                ))).collect(Collectors.toList())
        );

        Set<Integer> firstPoints = clique.stream().map(match -> match.firstPointIndex).collect(Collectors.toSet());
        Set<Integer> secondPoints = clique.stream().map(match -> match.secondPointIndex).collect(Collectors.toSet());

        String firstName = prefix + "_clique_" + num + "_" + firstSurface.name + ".ply";
        String secondName = prefix + "_clique_" + num + "_" + secondSurface.name + ".ply";

        writeSurface(firstSurface, firstPoints, new int[]{255, 0, 0}, comments, firstName);
        RealMatrix transitionMatrix = findTransition(clique);
        Surface transformedSecondSurface = Geometry.transformSurface(secondSurface, transitionMatrix);
        writeSurface(transformedSecondSurface, secondPoints, new int[]{0, 0, 255}, comments, secondName);
    }

    private RealMatrix findTransition(List<PointMatch> clique) {
        List<Point> firstPoints = Lists.newArrayListWithCapacity(clique.size());
        List<Point> secondPoints = Lists.newArrayListWithCapacity(clique.size());

        for (PointMatch match : clique) {
            firstPoints.add(firstSurface.points.get(match.firstPointIndex));
            secondPoints.add(secondSurface.points.get(match.secondPointIndex));
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
        Set<PointMatch> r = Collections.emptySet();
        Set<PointMatch> p = graph.keySet();
        Set<PointMatch> x = Collections.emptySet();

        return ForkJoinPool.commonPool().invoke(new TomitaTask(r, p, x, graph, startSet));
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

    private boolean isGoodPair(PointMatch first, PointMatch second) {
        if (first.firstPointIndex == second.firstPointIndex || first.secondPointIndex == second.secondPointIndex) {
            return false;
        }

        double firstDistance = Geometry.distance(firstSurface.points.get(first.firstPointIndex), firstSurface.points.get(second.firstPointIndex));
        double secondDistance = Geometry.distance(secondSurface.points.get(first.secondPointIndex), secondSurface.points.get(second.secondPointIndex));
        double distDelta = Math.abs(firstDistance - secondDistance);

        if (distDelta > 1.0) {
            return false;
        }

        //for angle between each surface point normals
        double firstAngle = firstSurface.normals.get(first.firstPointIndex).angle(firstSurface.normals.get(second.firstPointIndex));
        double secondAngle = secondSurface.normals.get(first.secondPointIndex).angle(secondSurface.normals.get(second.secondPointIndex));
        double normalsAngleDelta = Math.abs(firstAngle - secondAngle);

        if (firstAngle > Math.PI / 2 || secondAngle > Math.PI / 2 || normalsAngleDelta > MAX_ANGLE_DELTA) {
            return false;
        }

        Vector firstLineVector = Geometry.vectorFromPoints(firstSurface.points.get(first.firstPointIndex), firstSurface.points.get(second.firstPointIndex)).unite();
        Vector secondLineVector = Geometry.vectorFromPoints(secondSurface.points.get(first.secondPointIndex), secondSurface.points.get(second.secondPointIndex)).unite();

        //for angle between line and first normal of pair
        double firstStartLineAngle = firstSurface.normals.get(first.firstPointIndex).angle(firstLineVector);
        double secondStartLineAngle = secondSurface.normals.get(first.secondPointIndex).angle(secondLineVector);
        double startLineAngleDelta = Math.abs(Math.PI - firstStartLineAngle - secondStartLineAngle);

        if (startLineAngleDelta > MAX_ANGLE_DELTA) {
            return false;
        }

        //for angle between line and second normal of pair
        double firstEndLineAngle = firstSurface.normals.get(second.firstPointIndex).angle(firstLineVector);
        double secondEndLineAngle = secondSurface.normals.get(second.secondPointIndex).angle(secondLineVector);
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
        return surface.points.stream().mapToDouble(grid::getDistanceForPoint).min().getAsDouble();
    }

    public static class PointMatch {
        final int firstPointIndex;
        final int secondPointIndex;
        final double correlation;

        private PointMatch(int firstPointIndex, int secondPointIndex, double correlation) {
            this.firstPointIndex = firstPointIndex;
            this.secondPointIndex = secondPointIndex;
            this.correlation = correlation;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            PointMatch that = (PointMatch) o;

            return firstPointIndex == that.firstPointIndex && secondPointIndex == that.secondPointIndex;

        }

        @Override
        public int hashCode() {
            int result = firstPointIndex;
            result = 31 * result + secondPointIndex;
            return result;
        }

        @Override
        public String toString() {
            return "PointMatch{" +
                    "firstPointIndex=" + firstPointIndex +
                    ", secondPointIndex=" + secondPointIndex +
                    ", correlation=" + correlation +
                    '}';
        }
    }
}
