package ru.ifmo.docking.calculations;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.math3.linear.RealMatrix;
import ru.ifmo.docking.model.Surface;

import java.util.*;
import java.util.concurrent.RecursiveTask;
import java.util.stream.Collectors;

public class TomitaTask extends RecursiveTask<List<List<Docker.PointMatch>>> {
    private final Set<Docker.PointMatch> r;
    private final Set<Docker.PointMatch> p;
    private final Set<Docker.PointMatch> x;
    private final Map<Docker.PointMatch, Set<Docker.PointMatch>> graph;
    private final Set<Docker.PointMatch> startSet;
    private final Surface firstSurface;
    private final Surface secondSurface;

    public TomitaTask(Set<Docker.PointMatch> r,
                      Set<Docker.PointMatch> p,
                      Set<Docker.PointMatch> x,
                      Map<Docker.PointMatch, Set<Docker.PointMatch>> graph,
                      Set<Docker.PointMatch> startSet,
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
    protected List<List<Docker.PointMatch>> compute() {
        if (p.size() < 100) {
            return computeRecurrent(r, p, x);
        }

        ImmutableSet<Docker.PointMatch> candidates;

        Set<Docker.PointMatch> pivotCandidates = Objects.firstNonNull(startSet, p);
        Docker.PointMatch pivot = findPivot(pivotCandidates);
        candidates = Sets.difference(pivotCandidates, graph.get(pivot)).immutableCopy();

        List<List<Docker.PointMatch>> result = Lists.newArrayList();
        Queue<TomitaTask> subtasks = new ArrayDeque<>(1000);

        for (Docker.PointMatch v : candidates) {
            r.add(v);
            if (isValidClique(r)) {

                Set<Docker.PointMatch> n = graph.get(v);

                Set<Docker.PointMatch> pn = intersectionSet(p, n);
                Set<Docker.PointMatch> xn = intersectionSet(x, n);

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

    private List<List<Docker.PointMatch>> computeRecurrent(Set<Docker.PointMatch> r, Set<Docker.PointMatch> p, Set<Docker.PointMatch> x) {
        Docker.PointMatch pivot = findPivot(p);

        List<List<Docker.PointMatch>> result = Lists.newArrayList();
        for (Docker.PointMatch v : Sets.difference(p, graph.get(pivot)).immutableCopy()) {
            r.add(v);
            if (isValidClique(r)) {
                Set<Docker.PointMatch> n = graph.get(v);
                Set<Docker.PointMatch> pn = intersectionSet(p, n);
                Set<Docker.PointMatch> xn = intersectionSet(x, n);

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

    private Docker.PointMatch findPivot(Set<Docker.PointMatch> candidates) {
        Docker.PointMatch max = null;
        long maxIntersection = -1;

        for (Docker.PointMatch pointMatch : candidates) {
            long intersectionSize = intersectionSize(p, graph.get(pointMatch));
            if (intersectionSize > maxIntersection) {
                maxIntersection = intersectionSize;
                max = pointMatch;
            }
        }

        for (Docker.PointMatch pointMatch : x) {
            long intersectionSize = intersectionSize(p, graph.get(pointMatch));
            if (intersectionSize > maxIntersection) {
                maxIntersection = intersectionSize;
                max = pointMatch;
            }
        }

        return max;
    }

    private long intersectionSize(Set<Docker.PointMatch> first, Set<Docker.PointMatch> second) {
        Set<Docker.PointMatch> big = first.size() > second.size() ? first : second;
        Set<Docker.PointMatch> small = first.size() > second.size() ? second : first;
        return small.stream().filter(big::contains).count();
    }

    private Set<Docker.PointMatch> intersectionSet(Set<Docker.PointMatch> first, Set<Docker.PointMatch> second) {
        Set<Docker.PointMatch> big = first.size() > second.size() ? first : second;
        Set<Docker.PointMatch> small = first.size() > second.size() ? second : first;
        return small.stream().filter(big::contains).collect(Collectors.toCollection(HashSet::new));
    }

    private boolean isValidClique(Set<Docker.PointMatch> r) {
        if (r.size() > 2) {
            ArrayList<Docker.PointMatch> rl = Lists.newArrayList(r);
            RealMatrix transition = Docker.findTransition(rl);
            return Docker.isNormalsConsistent(rl, transition);
        }
        return true;
    }

}
