package ru.ifmo.docking.util;

import org.apache.commons.math3.linear.RealMatrix;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.model.Atom;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class PdbUtil {

    public static List<Atom> readPdbFile(File pdbFile) {
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

    public static void rewritePdb(File source, File output, RealMatrix matrix) {
        List<Atom> atoms = PdbUtil.readPdbFile(source);
        try (PrintWriter pw = new PrintWriter(output)) {
            for (Atom atom : atoms) {
                Point transformed = Geometry.transformPoint(atom.p, matrix);
                StringBuilder s = new StringBuilder(atom.s);
                s.replace(30, 38, String.format(Locale.US, "%8.3f", transformed.x));
                s.replace(38, 46, String.format(Locale.US, "%8.3f", transformed.y));
                s.replace(46, 54, String.format(Locale.US, "%8.3f", transformed.z));
                pw.println(s.toString());
            }
        } catch (FileNotFoundException ignored) {
        }
    }

}
