package ru.ifmo.docking.benchmark;

import com.google.common.collect.Lists;
import org.apache.commons.math3.linear.RealMatrix;
import ru.ifmo.docking.calculations.Docker;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.model.Atom;
import ru.ifmo.docking.model.Protein;
import ru.ifmo.docking.model.Surface;
import ru.ifmo.docking.util.Pair;
import ru.ifmo.docking.util.PdbUtil;
import ru.ifmo.docking.util.Timer;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SpinImageDockerRunner implements DockerRunner {


    @Override
    public List<Supplier<Protein>> run(String complex, File benchmarkDir, Timer timer) {
        String unboundReceptorFile = complex + "_r_" + "u";
        String unboundLigandFile = complex + "_l_" + "u";

        File unboundReceptorDir = new File(benchmarkDir, unboundReceptorFile + "_data");
        Surface firstSurface = Surface.read(unboundReceptorFile, new File(unboundReceptorDir, unboundReceptorFile + ".obj"));

        File unboundLigandDir = new File(benchmarkDir, unboundLigandFile + "_data");
        Surface secondSurface = Surface.read(unboundLigandFile, new File(unboundLigandDir, unboundLigandFile + ".obj"));

        Docker docker = new Docker(firstSurface, secondSurface);

        timer.start();
        List<Pair<List<Docker.PointMatch>, RealMatrix>> results = docker.run();
        timer.stop();

        Protein unboundReceptorProtein = readProtein(benchmarkDir, unboundReceptorFile);
        Protein unboundLigandProtein = readProtein(benchmarkDir, unboundLigandFile);

        RealMatrix pdbTransition = getProteinsCenteringTransition(unboundReceptorProtein, unboundLigandProtein);

        List<Atom> unboundReceptorCenteredAtoms = unboundReceptorProtein.getAtoms()
                .stream()
                .map(atom -> transformAtom(atom, pdbTransition))
                .collect(Collectors.toList());

        List<Atom> unboundLigandCenteredAtoms = unboundLigandProtein.getAtoms()
                .stream()
                .map(atom -> transformAtom(atom, pdbTransition))
                .collect(Collectors.toList());

        //noinspection RedundantCast
        return results.stream()
                .map(pair -> (Supplier<Protein>) () -> {
                    RealMatrix matrix = pair.second;
                    List<Atom> result = Lists.newArrayListWithCapacity(unboundLigandCenteredAtoms.size() + unboundLigandCenteredAtoms.size());
                    result.addAll(unboundReceptorCenteredAtoms);
                    result.addAll(unboundLigandCenteredAtoms
                            .stream()
                            .map(atom -> transformAtom(atom, matrix))
                            .collect(Collectors.toList()));
                    return new Protein(result);
                })
                .collect(Collectors.toList());
    }

    @Override
    public void cleanup(String complex) {

    }

    @Override
    public String getName() {
        return "spin";
    }

    private static Protein readProtein(File parentDir, String name) {
        File dir = new File(parentDir, name + "_data");
        File pdb = new File(dir, name + ".pdb");
        return PdbUtil.readPdbFile(pdb);
    }

    public static RealMatrix getProteinsCenteringTransition(Protein firstProtein, Protein secondProtein) {
        List<Point> firstPoints = firstProtein.getAtoms().stream().map(Atom::getPoint).collect(Collectors.toList());
        List<Point> secondPoints = secondProtein.getAtoms().stream().map(Atom::getPoint).collect(Collectors.toList());

        List<Point> points = Lists.newArrayList(firstPoints);
        points.addAll(secondPoints);

        Point pdbCentroid = Geometry.centroid(points);
        return Geometry.getTransitionMatrix(pdbCentroid, new Point(0, 0, 0));
    }

    public static Atom transformAtom(Atom atom, RealMatrix matrix) {
        return new Atom(
                atom.recordName,
                atom.serial,
                atom.name,
                atom.altLock,
                atom.resName,
                atom.chainId,
                atom.resSeq,
                atom.iCode,
                Geometry.transformPoint(atom.p, matrix),
                atom.occupancy,
                atom.tempFactor,
                atom.element,
                atom.pdbCharge,
                atom.charge,
                atom.r
        );
    }
}
