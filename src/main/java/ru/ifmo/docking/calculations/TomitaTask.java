package ru.ifmo.docking.calculations;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.RecursiveTask;

public class TomitaTask extends RecursiveTask<List<List<Docker.PointMatch>>> {
    private final Set<Docker.PointMatch> r;
    private final Set<Docker.PointMatch> p;
    private final Set<Docker.PointMatch> x;
    private final Map<Docker.PointMatch, Set<Docker.PointMatch>> graph;
    private final Set<Docker.PointMatch> startSet;

    public TomitaTask(Set<Docker.PointMatch> r,
                      Set<Docker.PointMatch> p,
                      Set<Docker.PointMatch> x,
                      Map<Docker.PointMatch, Set<Docker.PointMatch>> graph,
                      Set<Docker.PointMatch> startSet) {

        this.graph = graph;
        this.startSet = startSet;
        this.r = Sets.newHashSet(r);
        this.p = Sets.newHashSet(p);
        this.x = Sets.newHashSet(x);
    }

    @Override
    protected List<List<Docker.PointMatch>> compute() {
        if (p.size() < 100) {
            return computeRecurrent(r, p, x);
        } else {
            ImmutableSet<Docker.PointMatch> candidates;

            if (startSet != null) {
                Docker.PointMatch pivot = findPivot(startSet);
                candidates = Sets.intersection(graph.get(pivot), startSet).immutableCopy();
            } else {
                Docker.PointMatch pivot = findPivot();
                candidates = Sets.difference(p, graph.get(pivot)).immutableCopy();
            }

            List<List<Docker.PointMatch>> result = Lists.newArrayList();
            List<TomitaTask> subtasks = Lists.newArrayList();

            for (Docker.PointMatch v : candidates) {
                r.add(v);

                Set<Docker.PointMatch> n = graph.get(v);

                if (intersectionSize(p, n) == 0) {
                    if (intersectionSize(x, n) == 0 && r.size() > 2) {
                        result.add(Lists.newArrayList(r));
                    }
                } else {
                    TomitaTask tomitaTask = new TomitaTask(r, Sets.intersection(p, n), Sets.intersection(x, n), graph, null);
                    tomitaTask.fork();
                    subtasks.add(tomitaTask);
                    if (subtasks.size() > 100) {
                        for (TomitaTask subtask : subtasks) {
                            result.addAll(subtask.join());
                        }
                        subtasks.clear();
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
    }

    private Docker.PointMatch findPivot() {
        Docker.PointMatch max = null;
        long maxIntersection = -1;

        for (Docker.PointMatch pointMatch : p) {
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

    private Docker.PointMatch findPivot(Set<Docker.PointMatch> candidates) {
        Docker.PointMatch max = null;
        long maxIntersection = -1;

        for (Docker.PointMatch pointMatch : candidates) {
            long intersectionSize = intersectionSize(candidates, graph.get(pointMatch));
            if (intersectionSize > maxIntersection) {
                maxIntersection = intersectionSize;
                max = pointMatch;
            }
        }

        return max;
    }


    private List<List<Docker.PointMatch>> computeRecurrent(Set<Docker.PointMatch> r, Set<Docker.PointMatch> p, Set<Docker.PointMatch> x) {
        Docker.PointMatch pivot = findPivot();

        List<List<Docker.PointMatch>> result = Lists.newArrayList();
        for (Docker.PointMatch v : Sets.difference(p, graph.get(pivot)).immutableCopy()) {
            r.add(v);

            Set<Docker.PointMatch> n = graph.get(v);

            if (intersectionSize(p, n) == 0) {
                if (intersectionSize(x, n) == 0 && r.size() > 2) {
                    result.add(Lists.newArrayList(r));
                }
            } else {
                Set<Docker.PointMatch> pn = Sets.newHashSet(Sets.intersection(p, n));
                Set<Docker.PointMatch> xn = Sets.newHashSet(Sets.intersection(x, n));
                result.addAll(computeRecurrent(r, pn, xn));
            }

            r.remove(v);
            p.remove(v);
            x.add(v);
        }
        return result;
    }

    private long intersectionSize(Set<Docker.PointMatch> first, Set<Docker.PointMatch> second) {
        Set<Docker.PointMatch> big = first.size() > second.size() ? first : second;
        Set<Docker.PointMatch> small = first.size() > second.size() ? second : first;
        return small.stream().filter(big::contains).count();
    }

}
