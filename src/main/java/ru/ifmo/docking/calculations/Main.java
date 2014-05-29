package ru.ifmo.docking.calculations;

import com.google.common.collect.Lists;
import org.apache.commons.math3.linear.RealMatrix;
import ru.ifmo.docking.calculations.dockers.GeometryDocker;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.model.Atom;
import ru.ifmo.docking.model.Surface;
import ru.ifmo.docking.util.Pair;
import ru.ifmo.docking.util.PdbUtil;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) {
        String firstFile = args[0];
        String secondFile = args[1];

        File fiPotentials = new File("data", "fi_potentials.txt");

        String firstDir = "data/" + firstFile + "_data";
        File firstPdb = new File(firstDir, firstFile + ".pdb");
        Surface firstSurface = Surface.read(
                firstFile,
                new File(firstDir, firstFile + ".obj"),
                firstPdb,
                new File(firstDir, firstFile + ".pqr"),
                fiPotentials
        );

        String secondDir = "data/" + secondFile + "_data";
        File secondPdb = new File(secondDir, secondFile + ".pdb");
        Surface secondSurface = Surface.read(
                secondFile,
                new File(secondDir, secondFile + ".obj"),
                secondPdb,
                new File(secondDir, secondFile + ".pqr"),
                fiPotentials
        );

        GeometryDocker docker = new GeometryDocker(firstSurface, secondSurface);
        List<Pair<List<GeometryDocker.PointMatch>, RealMatrix>> transitions = docker.run();
        System.out.println("Total transitions count: " + transitions.size());

//        ArrayList<List<Docker.PointMatch>> cliques = Lists.newArrayList(transitions.keySet());
//
//        RealMatrix pdbTransition = getProteinsCenteringTransition(firstPdb, secondPdb);
//
//        for (int i = 0; i < cliques.size(); i++) {
//            List<Docker.PointMatch> clique = cliques.get(i);
//            docker.writeSurfacesWithCliques(clique, (i + 1), "surface");
//            RealMatrix transition = transitions.get(clique);
//
//            PdbUtil.renumerate(firstPdb, new File(firstFile + "_solution_" + (i + 1) + ".pdb"), pdbTransition);
//            PdbUtil.renumerate(secondPdb, new File(secondFile + "_solution_" + (i + 1) + ".pdb"), pdbTransition.multiply(transition));
//        }

//
//        PdbUtil.renumerate(firstPdb, new File(firstFile + "_centered.pdb"), transition);
//        PdbUtil.renumerate(secondPdb, new File(secondFile + "_centered.pdb"), transition);

    }

    private static RealMatrix getPdbTransition(File firstPdb, File secondPdb) {
        List<Atom> firstAtoms = PdbUtil.readPdbFile(firstPdb).getAtoms();
        List<Atom> secondAtoms = PdbUtil.readPdbFile(secondPdb).getAtoms();

        List<Point> firstPoints = firstAtoms.stream().map(Atom::getPoint).collect(Collectors.toList());
        List<Point> secondPoints = secondAtoms.stream().map(Atom::getPoint).collect(Collectors.toList());

        List<Point> points = Lists.newArrayList(firstPoints);
        points.addAll(secondPoints);

        Point pdbCentroid = Geometry.centroid(points);
        return Geometry.getTransitionMatrix(pdbCentroid, new Point(0, 0, 0));
    }


}
