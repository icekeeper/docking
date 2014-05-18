package ru.ifmo.docking.calculations;

import com.google.common.collect.Lists;
import org.apache.commons.math3.linear.RealMatrix;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.model.Atom;
import ru.ifmo.docking.model.Surface;
import ru.ifmo.docking.util.Pair;
import ru.ifmo.docking.util.PdbUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RmsdTestMain {

    public static void main(String[] args) throws FileNotFoundException {
        try (PrintWriter pw = new PrintWriter("report.txt")) {
            File dir = new File(args[0]);
            File[] candidates = dir.listFiles(pathname -> pathname.isDirectory() && pathname.getName().matches(".*_[lr]_[bu]_data"));

            List<String> proteinNames = Lists.newArrayList(
                    Arrays.stream(candidates)
                            .map(file -> file.getName().substring(0, file.getName().indexOf('_')))
                            .collect(Collectors.toSet())
            );

            proteinNames.sort((o1, o2) -> o1.compareTo(o2));

            System.out.println("Processing proteins: " + proteinNames);

            for (String proteinName : proteinNames) {
                try {
                    compareFiles(pw, dir, proteinName, true);
                    compareFiles(pw, dir, proteinName, false);
                } catch (Exception e) {
                    System.out.println("Error processing: " + proteinName);
                    e.printStackTrace();
                }
            }
        }
    }

    private static void compareFiles(PrintWriter pw, File dir, String complex, boolean bound) {
        String firstFile = complex + "_r_" + (bound ? "b" : "u");
        String secondFile = complex + "_l_" + (bound ? "b" : "u");

        File firstDir = new File(dir, firstFile + "_data");
        File firstPdb = new File(firstDir, firstFile + ".pdb");
        Surface firstSurface = Surface.read(firstFile, new File(firstDir, firstFile + ".obj"));

        File secondDir = new File(dir, secondFile + "_data");
        File secondPdb = new File(secondDir, secondFile + ".pdb");
        Surface secondSurface = Surface.read(secondFile, new File(secondDir, secondFile + ".obj"));

        Docker docker = new Docker(firstSurface, secondSurface);

        List<Pair<List<Docker.PointMatch>, RealMatrix>> results = docker.run();

        System.out.println("Found " + results.size() + " solutions");

        Map<Pair<List<Docker.PointMatch>, RealMatrix>, Double> diameters = results.stream()
                .collect(Collectors.toMap(Function.identity(), pair -> cliqueDiameter(pair.first)));

        results.sort((o1, o2) -> -Double.compare(diameters.get(o1), diameters.get(o2)));


        Collection<Atom> firstAtoms = PdbUtil.readPdbFile(firstPdb);
        Collection<Atom> secondAtoms = PdbUtil.readPdbFile(secondPdb);

        RealMatrix pdbTransition = getPdbTransition(firstPdb, secondPdb);

        List<Point> firstPoints = firstAtoms.stream().map(atom -> Geometry.transformPoint(atom.p, pdbTransition)).collect(Collectors.toList());
        List<Point> secondPoints = secondAtoms.stream().map(atom -> Geometry.transformPoint(atom.p, pdbTransition)).collect(Collectors.toList());

        List<Point> startComplex = Lists.newArrayList();
        startComplex.addAll(firstPoints);
        startComplex.addAll(secondPoints);

        Pair<Integer, Double> minRmsd = IntStream.range(0, results.size())
                .parallel()
                .mapToObj(i -> {
                    RealMatrix transition = results.get(i).second;

                    List<Point> translatedSecond = secondPoints
                            .stream()
                            .map(point -> Geometry.transformPoint(point, transition))
                            .collect(Collectors.toList());

                    List<Point> resultComplex = Lists.newArrayList();
                    resultComplex.addAll(firstPoints);
                    resultComplex.addAll(translatedSecond);

                    RealMatrix t = Geometry.findRmsdOptimalTransformationMatrix(resultComplex, startComplex);
                    List<Point> alignedComplex = resultComplex.stream().map(p -> Geometry.transformPoint(p, t)).collect(Collectors.toList());

                    double rmsd = Geometry.rmsd(startComplex, alignedComplex);
                    return Pair.of(i, rmsd);
                })
                .min((o1, o2) -> Double.compare(o1.second, o2.second)).get();


        System.out.println("For " + (bound ? "bound" : "unbound") + " complex " + complex + " minimum rmsd is: " + minRmsd.second + " with rank: " + minRmsd.first);

        pw.println("For " + (bound ? "bound" : "unbound") + " complex " + complex + " minimum rmsd is: " + minRmsd.second + " with rank: " + minRmsd.first);
        pw.flush();

        PdbUtil.rewritePdb(firstPdb, new File(dir, firstFile + "_best.pdb"), pdbTransition);
        PdbUtil.rewritePdb(secondPdb, new File(dir, secondFile + "_best.pdb"), pdbTransition.multiply(results.get(minRmsd.first).second));
    }

    private static double cliqueDiameter(List<Docker.PointMatch> clique) {
        double diameter = 0.0;
        for (Docker.PointMatch firstMatch : clique) {
            for (Docker.PointMatch secondMatch : clique) {
                diameter = Math.max(diameter, Geometry.distance(firstMatch.getFirstPoint(), secondMatch.getFirstPoint()));
                diameter = Math.max(diameter, Geometry.distance(firstMatch.getSecondPoint(), secondMatch.getSecondPoint()));
            }
        }
        return diameter;
    }

    public static RealMatrix getPdbTransition(File firstPdb, File secondPdb) {
        List<Atom> firstAtoms = PdbUtil.readPdbFile(firstPdb);
        List<Atom> secondAtoms = PdbUtil.readPdbFile(secondPdb);

        List<Point> firstPoints = firstAtoms.stream().map(Atom::getPoint).collect(Collectors.toList());
        List<Point> secondPoints = secondAtoms.stream().map(Atom::getPoint).collect(Collectors.toList());

        List<Point> points = Lists.newArrayList(firstPoints);
        points.addAll(secondPoints);

        Point pdbCentroid = Geometry.centroid(points);
        return Geometry.getTransitionMatrix(pdbCentroid, new Point(0, 0, 0));
    }


}
