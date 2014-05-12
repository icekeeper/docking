package ru.ifmo.docking.calculations;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.model.SpinImage;
import ru.ifmo.docking.model.Surface;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class Docker {

    private final Surface firstSurface;
    private final Surface secondSurface;
    private long start;

    public void run() {
        start = System.currentTimeMillis();
        List<PointMatch> pointMatches = findTopCorrelatedPairs(50000, 0.8);
        System.out.println((System.currentTimeMillis() - start) + " Point matched. Smallest correlation is " + pointMatches.get(pointMatches.size() - 1).correlation);

        pointMatches.stream().filter(match ->
                Geometry.distance(
                        firstSurface.points.get(match.firstPointIndex),
                        secondSurface.points.get(match.secondPointIndex)
                ) < 1.0).forEach(System.out::println);

        double bound = Math.max(firstSurface.getDiameter(), secondSurface.getDiameter()) * 0.6;
        System.out.println("Bound is " + bound);
        Map<PointMatch, Set<PointMatch>> graph = constructGraph(pointMatches, bound);
        System.out.println((System.currentTimeMillis() - start) + " Graph constructed");

        List<List<PointMatch>> cliques = findCliques(graph, Sets.newHashSet(pointMatches.subList(0, 10000)));
        System.out.println((System.currentTimeMillis() - start) + " Cliques computation completed. Cliques count: " + cliques.size());

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
        for (List<PointMatch> contactClique : contactCliques) {
            System.out.println("------start-----");
            contactClique.forEach(match -> {
                        System.out.println(String.format("%s distance=%f", match, Geometry.distance(
                                firstSurface.points.get(match.firstPointIndex),
                                secondSurface.points.get(match.secondPointIndex)
                        )));
                    }
            );
            System.out.println("------end-----");
        }

        Map<Integer, List<List<PointMatch>>> grouped = cliques.stream().collect(Collectors.groupingBy(List::size));
        ArrayList<Integer> keys = Lists.newArrayList(grouped.keySet());
        Collections.sort(keys);
        keys.forEach(key -> System.out.println("Bins of size " + key + ": " + grouped.get(key).size()));

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

    private Map<PointMatch, Set<PointMatch>> constructGraph(List<PointMatch> matches, double bound) {
        return matches.parallelStream()
                .collect(
                        Collectors.toConcurrentMap(
                                Function.<PointMatch>identity(),
                                match -> matches.stream()
                                        .filter(otherMatch -> !match.equals(otherMatch) && isGoodPair(match, otherMatch, bound))
                                        .collect(Collectors.toSet())
                        )
                );
    }


    private List<PointMatch> findTopCorrelatedPairs(int count, double correlationBound) {
        List<SpinImage> firstStack = computeSpinImageStack(firstSurface);
        System.out.println((System.currentTimeMillis() - start) + " First stack computed");
        List<SpinImage> secondStack = computeSpinImageStack(secondSurface);
        System.out.println((System.currentTimeMillis() - start) + " Second stack computed");

        List<List<SpinImage>> partition = Lists.partition(secondStack, secondStack.size() / Runtime.getRuntime().availableProcessors());

        List<PointMatch> result = partition.parallelStream()
                .flatMap(part -> {
                    List<PointMatch> inner = Lists.newArrayListWithCapacity(part.size() * firstStack.size());
                    for (SpinImage firstImage : firstStack) {
                        for (SpinImage secondImage : part) {
                            double correlation = firstImage.correlation(secondImage);
                            if (correlation > correlationBound) {
                                inner.add(new PointMatch(firstImage.getPointIndex(), secondImage.getPointIndex(), correlation));
                            }
                        }
                    }
                    return inner.stream();
                })
                .collect(Collectors.toList());

        result.sort((o1, o2) -> -Double.compare(o1.correlation, o2.correlation));

        return Lists.newArrayList(result.subList(0, count));
    }

    private boolean isGoodPair(PointMatch first, PointMatch second, double bound) {
        double firstDistance = Geometry.distance(firstSurface.points.get(first.firstPointIndex), firstSurface.points.get(second.firstPointIndex));
        double secondDistance = Geometry.distance(secondSurface.points.get(first.secondPointIndex), secondSurface.points.get(second.secondPointIndex));
        double delta = Math.abs(firstDistance - secondDistance);

        double firstDotProduct = firstSurface.normals.get(first.firstPointIndex).dot(firstSurface.normals.get(second.firstPointIndex));
        double secondDotProduct = secondSurface.normals.get(first.secondPointIndex).dot(secondSurface.normals.get(second.secondPointIndex));

        return delta < 1.0
                && first.firstPointIndex != second.secondPointIndex
                && first.secondPointIndex != second.secondPointIndex
                && firstDotProduct > 0
                && secondDotProduct > 0
                && firstDistance < bound
                && secondDistance < bound;
    }

    private static List<SpinImage> computeSpinImageStack(Surface surface) {
        return IntStream.range(0, surface.points.size())
                .parallel()
                .mapToObj(i -> SpinImage.compute(i, surface, 6.0, 1.0))
                .collect(Collectors.toList());
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
