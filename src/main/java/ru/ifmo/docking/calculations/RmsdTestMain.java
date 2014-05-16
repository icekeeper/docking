package ru.ifmo.docking.calculations;

import com.google.common.collect.Lists;
import org.apache.commons.math3.linear.RealMatrix;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.model.Atom;
import ru.ifmo.docking.model.Surface;
import ru.ifmo.docking.util.IOUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

public class RmsdTestMain {

    public static void main(String[] args) throws FileNotFoundException {
        try (PrintWriter pw = new PrintWriter("report.txt")) {
            for (String arg : args) {
                compareFiles(pw, arg, true);
                compareFiles(pw, arg, false);
            }
        }


    }

    private static void compareFiles(PrintWriter pw, String complex, boolean bound) {
        File fiPotentials = new File("data", "fi_potentials.txt");

        String firstFile = complex + "_r_" + (bound ? "b" : "u");
        String secondFile = complex + "_l_" + (bound ? "b" : "u");

        String firstDir = "data/" + firstFile + "_data";
        File firstPdb = new File(firstDir, firstFile + ".pdb");
        Surface firstSurface = Surface.read(
                firstFile,
                firstPdb,
                new File(firstDir, firstFile + ".obj"),
                new File(firstDir, firstFile + "_pot.csv"),
                fiPotentials
        );

        String secondDir = "data/" + secondFile + "_data";
        File secondPdb = new File(secondDir, secondFile + ".pdb");
        Surface secondSurface = Surface.read(
                secondFile,
                secondPdb,
                new File(secondDir, secondFile + ".obj"),
                new File(secondDir, secondFile + "_pot.csv"),
                fiPotentials
        );

        Docker docker = new Docker(firstSurface, secondSurface);
//                List<RealMatrix> transitions = Collections.emptyList();
        List<RealMatrix> transitions = docker.run();
        System.out.println("Found " + transitions.size() + " solutions");

        Collection<Atom> firstAtoms = readPdbFile(firstPdb);
        Collection<Atom> secondAtoms = readPdbFile(secondPdb);

        List<Point> firstPoints = firstAtoms.stream().map(atom -> atom.p).collect(Collectors.toList());
        List<Point> secondPoints = secondAtoms.stream().map(atom -> atom.p).collect(Collectors.toList());

        double minRmsd = Double.MAX_VALUE;

        List<Point> startComplex = Lists.newArrayList();
        startComplex.addAll(firstPoints);
        startComplex.addAll(secondPoints);

        for (int i = 0; i < transitions.size(); i++) {
            RealMatrix transition = transitions.get(i);
            List<Point> translatedSecond = secondPoints
                    .stream()
                    .map(point -> Geometry.transformPoint(point, transition))
                    .collect(Collectors.toList());

            List<Point> resultComplex = Lists.newArrayList();
            resultComplex.addAll(firstPoints);
            resultComplex.addAll(translatedSecond);

            RealMatrix t = Geometry.findRmsdOptimalTransformationMatrix(resultComplex, startComplex);
            List<Point> alignedComplex = resultComplex.stream().map(p -> Geometry.transformPoint(p, t)).collect(Collectors.toList());

            minRmsd = Math.min(minRmsd, Geometry.rmsd(startComplex, alignedComplex));
            rewritePdb(secondPdb, new File(complex + "_" + i + (bound ? "_b" : "_u") + ".pdb"), transition);
        }
        pw.println("For " + (bound ? "bound" : "unbound") + " complex " + complex + " minimum rmsd is: " + minRmsd);
        pw.flush();
    }

    private static void rewritePdb(File source, File output, RealMatrix matrix) {
        Collection<Atom> atoms = readPdbFile(source);
        try (PrintWriter pw = new PrintWriter(output)) {
            for (Atom atom : atoms) {
                Point transformed = Geometry.transformPoint(atom.p, matrix);
                StringBuilder s = new StringBuilder(atom.s);
                s.replace(30, 38, String.format("%8f.3", transformed.x));
                s.replace(38, 46, String.format("%8f.3", transformed.y));
                s.replace(46, 54, String.format("%8f.3", transformed.z));
                pw.println(s.toString());
            }
        } catch (FileNotFoundException ignored) {
        }
    }

    private static Collection<Atom> readPdbFile(File pdbFile) {
        return IOUtils.linesStream(pdbFile)
                .filter(line -> line.startsWith("ATOM  ") || line.startsWith("HETATM"))
                .map(line -> {
                    double atomX = Double.parseDouble(line.substring(30, 38).trim());
                    double atomY = Double.parseDouble(line.substring(38, 46).trim());
                    double atomZ = Double.parseDouble(line.substring(46, 54).trim());

                    return new Atom(atomX, atomY, atomZ, line);
                })
                .collect(Collectors.toList());
    }

}
