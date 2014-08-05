package ru.ifmo.docking.calculations.dockers;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.math3.linear.RealMatrix;
import ru.ifmo.docking.calculations.TomitaTask;
import ru.ifmo.docking.client.RunConfig;
import ru.ifmo.docking.geometry.DistanceGrid;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.geometry.Vector;
import ru.ifmo.docking.model.SpinImage;
import ru.ifmo.docking.model.Surface;
import ru.ifmo.docking.util.Logger;
import ru.ifmo.docking.util.Pair;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GeometryDocker implements Docker {

    protected final RunConfig config;

    public static final double MAX_ANGLE_DELTA = Math.PI / 8;
    protected final Surface receptorSurface;
    protected final Surface ligandSurface;

    private Map<String, List<SpinImage>> cache = new HashMap<>();

    public GeometryDocker(Surface receptorSurface, Surface ligandSurface, RunConfig config) {
        this.config = config;
        this.receptorSurface = receptorSurface;
        this.ligandSurface = ligandSurface;
    }

    @Override
    public List<RealMatrix> run() {
        int pairsCount = 50000;
        Logger.log("Use %d surface vertex pairs", pairsCount);
        List<List<PointMatch>> cliques = searchForSolutions(pairsCount);

        DistanceGrid receptorDistanceGrid = new DistanceGrid(receptorSurface, 0.25, 1.5);
        Logger.log("Distance grid for receptor constructed");

        return filterAndScoreCliques(cliques, receptorDistanceGrid);

    }

    private List<RealMatrix> filterAndScoreCliques(List<List<PointMatch>> cliques, DistanceGrid firstSurfaceGrid) {
        return cliques
                .parallelStream()
                .map(GeometryDocker::findTransition)
                .map(transition -> Pair.of(transition, Geometry.transformSurface(ligandSurface, transition)))
                .map(pair -> Pair.of(pair.first, score(firstSurfaceGrid, pair.second)))
                .filter(pair -> !Double.isInfinite(pair.second))
                .sorted((o1, o2) -> -Double.compare(o1.second, o2.second))
                .map(Pair::getFirst)
                .collect(Collectors.toList());
    }

    protected List<List<PointMatch>> searchForSolutions(int pairsCount) {
        List<PointMatch> pointMatches = findTopCorrelatedPairs(pairsCount);
        Logger.log("Vertex correlations computed");
        Logger.log("Correlation bound is %f", pointMatches.get(pointMatches.size() - 1).correlation);

        Map<PointMatch, Set<PointMatch>> graph = constructGraph(pointMatches);
        Logger.log("Graph constructed");

        List<List<PointMatch>> cliques = findCliques(graph, null);
        Logger.log("Cliques computed");
        Logger.log("Cliques count: %d", cliques.size());
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

    public static RealMatrix findTransition(List<PointMatch> clique) {
        List<Point> firstPoints = Lists.newArrayListWithCapacity(clique.size());
        List<Point> secondPoints = Lists.newArrayListWithCapacity(clique.size());

        for (PointMatch match : clique) {
            firstPoints.add(match.getFirstPoint());
            secondPoints.add(match.getSecondPoint());
        }

        return Geometry.findRmsdOptimalTransformationMatrix(secondPoints, firstPoints);
    }


    protected List<List<PointMatch>> findCliques(Map<PointMatch, Set<PointMatch>> graph, Set<PointMatch> startSet) {
        Set<PointMatch> r = Sets.newHashSet();
        Set<PointMatch> p = Sets.newHashSet(graph.keySet());
        Set<PointMatch> x = Sets.newHashSet();

        return ForkJoinPool.commonPool().invoke(new TomitaTask(r, p, x, graph, startSet, receptorSurface, ligandSurface));
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
        List<SpinImage> firstStack = computeSpinImageStack(receptorSurface);
        Logger.log("Receptor spin image stack computed");
        List<SpinImage> secondStack = computeSpinImageStack(ligandSurface);
        Logger.log("Ligand spin image stack computed");

        List<List<SpinImage>> partition = Lists.partition(secondStack, secondStack.size() / Runtime.getRuntime().availableProcessors());

        List<PointMatch> result = partition.parallelStream()
                .flatMap(part -> {
                    Queue<PointMatch> queue = new PriorityQueue<>((o1, o2) -> Double.compare(o1.getCorrelation(), o2.getCorrelation()));
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

    public class PointMatch {
        final int firstIndex;
        final int secondIndex;
        final double correlation;

        public Point getFirstPoint() {
            return receptorSurface.points.get(firstIndex);
        }

        public Point getSecondPoint() {
            return ligandSurface.points.get(secondIndex);
        }

        public Vector getFirstNormal() {
            return receptorSurface.normals.get(firstIndex);
        }

        public Vector getSecondNormal() {
            return ligandSurface.normals.get(secondIndex);
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
