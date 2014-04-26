package ru.ifmo.docking.lip;

import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Simple lipophilicity calculator.
 * Based on pyMLP script written by Julien Lefeuvre <lefeuvrejulien@yahoo.fr>
 * https://code.google.com/p/pymlp
 */
public class LipophilicityCalculator {
    private final Collection<Atom> atomsData;

    public static LipophilicityCalculator construct(File pdbFile, File fiFile) {
        Map<String, Double> fiData = readFiFile(fiFile);
        Collection<Atom> atomsData = readPdbFile(pdbFile, fiData);
        return new LipophilicityCalculator(atomsData);
    }

    private static Collection<Atom> readPdbFile(File pdbFile, Map<String, Double> fiData) {
        try (BufferedReader reader = new BufferedReader(new FileReader(pdbFile))) {
            return reader.lines()
                    .filter(line -> line.startsWith("ATOM  ") || line.startsWith("HETATM"))
                    .map(line -> {
                        String atomName = line.substring(12, 16).trim();
                        String resName = line.substring(17, 20).trim();
                        double atomX = Double.parseDouble(line.substring(30, 38).trim());
                        double atomY = Double.parseDouble(line.substring(38, 46).trim());
                        double atomZ = Double.parseDouble(line.substring(46, 54).trim());

                        String fiKey = resName + "_" + atomName;
                        double fi = fiData.containsKey(fiKey) ? fiData.get(fiKey) : Double.NaN;

                        return new Atom(atomX, atomY, atomZ, fi);
                    })
                    .filter(atom -> !Double.isNaN(atom.fi))
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static Map<String, Double> readFiFile(File fiFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fiFile))) {
            Map<String, Double> fiData = new HashMap<>();
            reader.lines().forEach(line -> {
                String[] tokens = line.split("\\s+");
                fiData.put(tokens[0] + "_" + tokens[1], Double.parseDouble(tokens[2]));
            });
            return fiData;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private LipophilicityCalculator(Collection<Atom> atomsData) {
        this.atomsData = atomsData;
    }

    public double compute(Point p) {
        return atomsData
                .stream()
                .mapToDouble(atom -> 100.0 * atom.fi * Math.exp(-Geometry.distance(p, atom.p)))
                .sum();
    }


    private static class Atom {
        Point p;
        double fi;

        private Atom(double x, double y, double z, double fi) {
            this.p = new Point(x, y, z);
            this.fi = fi;
        }
    }
}
