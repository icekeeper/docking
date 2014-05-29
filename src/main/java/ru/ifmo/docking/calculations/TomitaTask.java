package ru.ifmo.docking.calculations;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.math3.linear.RealMatrix;
import ru.ifmo.docking.calculations.dockers.GeometryDocker;
import ru.ifmo.docking.model.Surface;

import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

public class TomitaTask extends RecursiveTask<List<List<GeometryDocker.PointMatch>>> {
    private final Set<GeometryDocker.PointMatch> r;
    private final Set<GeometryDocker.PointMatch> p;
    private final Set<GeometryDocker.PointMatch> x;
    private final Map<GeometryDocker.PointMatch, Set<GeometryDocker.PointMatch>> graph;
    private final Set<GeometryDocker.PointMatch> startSet;
    private final Surface firstSurface;
    private final Surface secondSurface;

    public TomitaTask(Set<GeometryDocker.PointMatch> r,
                      Set<GeometryDocker.PointMatch> p,
                      Set<GeometryDocker.PointMatch> x,
                      Map<GeometryDocker.PointMatch, Set<GeometryDocker.PointMatch>> graph,
                      Set<GeometryDocker.PointMatch> startSet,
                      Surface firstSurface,
                      Surface secondSurface) {

        this.graph = graph;
        this.startSet = startSet;
        this.firstSurface = firstSurface;
        this.secondSurface = secondSurface;
        this.r = Sets.newHashSet(r);
        this.p = p;
        this.x = x;
    }

    @Override
    protected List<List<GeometryDocker.PointMatch>> compute() {
        if (p.size() < 100) {
            return computeRecurrent(r, p, x);
        }

        ImmutableSet<GeometryDocker.PointMatch> candidates;

        Set<GeometryDocker.PointMatch> pivotCandidates = Objects.firstNonNull(startSet, p);
        GeometryDocker.PointMatch pivot = findPivot(pivotCandidates);
        candidates = Sets.difference(pivotCandidates, graph.get(pivot)).immutableCopy();

        List<List<GeometryDocker.PointMatch>> result = Lists.newArrayList();
        Queue<TomitaTask> subtasks = new ArrayDeque<>(1000);

        for (GeometryDocker.PointMatch v : candidates) {
            r.add(v);
            if (isValidClique(r)) {

                Set<GeometryDocker.PointMatch> n = graph.get(v);

                Set<GeometryDocker.PointMatch> pn = intersectionSet(p, n);
                Set<GeometryDocker.PointMatch> xn = intersectionSet(x, n);

                if (pn.isEmpty()) {
                    if (xn.isEmpty() && r.size() > 2) {
                        result.add(Lists.newArrayList(r));
                    }
                } else {
                    TomitaTask tomitaTask = new TomitaTask(r, pn, xn, graph, null, firstSurface, secondSurface);
                    tomitaTask.fork();
                    subtasks.add(tomitaTask);

                    if (subtasks.size() == 1000) {
                        for (int i = 0; i < 800; i++) {
                            result.addAll(subtasks.poll().join());
                        }
                    }
                }
            }
            r.remove(v);
            p.remove(v);
            x.add(v);
        }
        for (TomitaTask subtask : subtasks) {
            result.addAll(subtask.join());
        }
        return result;
    }

    private List<List<GeometryDocker.PointMatch>> computeRecurrent(Set<GeometryDocker.PointMatch> r, Set<GeometryDocker.PointMatch> p, Set<GeometryDocker.PointMatch> x) {
        GeometryDocker.PointMatch pivot = findPivot(p);

        List<List<GeometryDocker.PointMatch>> result = Lists.newArrayList();
        for (GeometryDocker.PointMatch v : Sets.difference(p, graph.get(pivot)).immutableCopy()) {
            r.add(v);
            if (isValidClique(r)) {
                Set<GeometryDocker.PointMatch> n = graph.get(v);
                Set<GeometryDocker.PointMatch> pn = intersectionSet(p, n);
                Set<GeometryDocker.PointMatch> xn = intersectionSet(x, n);

                if (pn.isEmpty()) {
                    if (xn.isEmpty() && r.size() > 2) {
                        result.add(Lists.newArrayList(r));
                    }
                } else {
                    result.addAll(computeRecurrent(r, pn, xn));
                }


            }
            r.remove(v);
            p.remove(v);
            x.add(v);
        }
        return result;
    }

    private GeometryDocker.PointMatch findPivot(Set<GeometryDocker.PointMatch> candidates) {
        GeometryDocker.PointMatch max = null;
        long maxIntersection = -1;

        for (GeometryDocker.PointMatch pointMatch : candidates) {
            long intersectionSize = intersectionSize(p, graph.get(pointMatch));
            if (intersectionSize > maxIntersection) {
                maxIntersection = intersectionSize;
                max = pointMatch;
            }
        }

        for (GeometryDocker.PointMatch pointMatch : x) {
            long intersectionSize = intersectionSize(p, graph.get(pointMatch));
            if (intersectionSize > maxIntersection) {
                maxIntersection = intersectionSize;
                max = pointMatch;
            }
        }

        return max;
    }

    private long intersectionSize(Set<GeometryDocker.PointMatch> first, Set<GeometryDocker.PointMatch> second) {
        Set<GeometryDocker.PointMatch> big = first.size() > second.size() ? first : second;
        Set<GeometryDocker.PointMatch> small = first.size() > second.size() ? second : first;
        return small.stream().filter(big::contains).count();
    }

    private Set<GeometryDocker.PointMatch> intersectionSet(Set<GeometryDocker.PointMatch> first, Set<GeometryDocker.PointMatch> second) {
        Set<GeometryDocker.PointMatch> big = first.size() > second.size() ? first : second;
        Set<GeometryDocker.PointMatch> small = first.size() > second.size() ? second : first;
        return small.stream().filter(big::contains).collect(Collectors.toCollection(HashSet::new));
    }

    private boolean isValidClique(Set<GeometryDocker.PointMatch> r) {
        if (r.size() > 2) {
            ArrayList<GeometryDocker.PointMatch> rl = Lists.newArrayList(r);
            RealMatrix transition = GeometryDocker.findTransition(rl);
            return GeometryDocker.isNormalsConsistent(rl, transition);
        }
        return true;
    }

}
