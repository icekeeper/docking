package ru.ifmo.docking.calculations.dockers;

import com.google.common.collect.Lists;
import ru.ifmo.docking.client.RunConfig;
import ru.ifmo.docking.model.SpinImage;
import ru.ifmo.docking.model.Surface;
import ru.ifmo.docking.util.Logger;
import ru.ifmo.docking.util.Pair;

import java.util.*;
import java.util.stream.Collectors;

public class CombinedDocker extends GeometryDocker {

    final double minLipDelta;
    final double maxLipDelta;
    final double minElDelta;
    final double maxElDelta;

    public CombinedDocker(Surface receptorSurface, Surface ligandSurface, RunConfig config) {
        super(receptorSurface, ligandSurface, config);
        Pair<Double, Double> lipDelta = getLipDelta();
        Pair<Double, Double> elDelta = getElDelta();
        this.minLipDelta = -Math.log(lipDelta.first);
        this.maxLipDelta = -Math.log(lipDelta.second);
        this.minElDelta = -Math.log(elDelta.first);
        this.maxElDelta = -Math.log(elDelta.second);
    }

    @Override
    protected List<List<PointMatch>> searchForSolutions(int pairsCount) {
        List<List<PointMatch>> results = Lists.newArrayListWithCapacity(150000);
        results.addAll(getPointsMatch(super.findTopCorrelatedPairs(pairsCount)));
        results.addAll(getPointsMatch(findTopCorrelatedPairsEl(pairsCount)));
        results.addAll(getPointsMatch(findTopCorrelatedPairsLip(pairsCount)));

        Logger.log("Cliques computation completed. Cliques count: %d", results.size());

        return results;
    }

    private List<List<PointMatch>> getPointsMatch(List<PointMatch> pointMatches) {
        Map<PointMatch, Set<PointMatch>> graph = constructGraph(pointMatches);
        Logger.log("Graph constructed");
        return findCliques(graph, null);
    }

    protected List<PointMatch> findTopCorrelatedPairsLip(int count) {
        List<SpinImage> firstStack = computeSpinImageStack(receptorSurface);
        Logger.log("First stack computed");
        List<SpinImage> secondStack = computeSpinImageStack(ligandSurface);
        Logger.log("Second stack computed");

        List<List<SpinImage>> partition = Lists.partition(secondStack, secondStack.size() / Runtime.getRuntime().availableProcessors());

        List<PointMatch> result = partition.parallelStream()
                .flatMap(part -> {
                    Queue<PointMatch> queue = new PriorityQueue<>((o1, o2) -> Double.compare(o1.correlation, o2.correlation));
                    for (SpinImage firstImage : firstStack) {
                        for (SpinImage secondImage : part) {
                            double correlation = firstImage.correlation(secondImage) + getLipCorrelation(firstImage, secondImage);
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


    protected List<PointMatch> findTopCorrelatedPairsEl(int count) {
        List<SpinImage> firstStack = computeSpinImageStack(receptorSurface);
        Logger.log("First stack computed");
        List<SpinImage> secondStack = computeSpinImageStack(ligandSurface);
        Logger.log("Second stack computed");

        List<List<SpinImage>> partition = Lists.partition(secondStack, secondStack.size() / Runtime.getRuntime().availableProcessors());

        List<PointMatch> result = partition.parallelStream()
                .flatMap(part -> {
                    Queue<PointMatch> queue = new PriorityQueue<>((o1, o2) -> Double.compare(o1.correlation, o2.correlation));
                    for (SpinImage firstImage : firstStack) {
                        for (SpinImage secondImage : part) {
                            double correlation = firstImage.correlation(secondImage) + getElCorrelation(firstImage, secondImage);
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

    private double getLipCorrelation(SpinImage first, SpinImage second) {
        double delta = -Math.log(Math.abs(receptorSurface.lipophilicity[first.getPointIndex()] - ligandSurface.lipophilicity[second.getPointIndex()]));
        return (delta - minLipDelta) / (maxLipDelta - minLipDelta);
    }

    private double getElCorrelation(SpinImage first, SpinImage second) {
        double delta = -Math.log(Math.abs(receptorSurface.electricity[first.getPointIndex()] + ligandSurface.electricity[second.getPointIndex()]));
        return (delta - minElDelta) / (maxElDelta - minElDelta);
    }


    private Pair<Double, Double> getLipDelta() {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double firstLip : receptorSurface.lipophilicity) {
            for (double secondLip : ligandSurface.lipophilicity) {
                double delta = Math.abs(firstLip - secondLip);
                if (min > delta) {
                    min = delta;
                }
                if (max < delta) {
                    max = delta;
                }
            }
        }
        return Pair.of(min, max);
    }

    private Pair<Double, Double> getElDelta() {
        double min = Double.MAX_VALUE;
        double max = Double.MIN_VALUE;
        for (double firstEl : receptorSurface.electricity) {
            for (double secondEl : ligandSurface.electricity) {
                double delta = Math.abs(firstEl + secondEl);
                if (min > delta) {
                    min = delta;
                }
                if (max < delta) {
                    max = delta;
                }
            }
        }
        return Pair.of(min, max);
    }
}
