package ru.ifmo.docking.calculations;

import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.model.Atom;
import ru.ifmo.docking.util.PdbUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple lipophilicity calculator.
 * Based on pyMLP script written by Julien Lefeuvre <lefeuvrejulien@yahoo.fr>
 * https://code.google.com/p/pymlp
 */

public class LipophilicityCalculator {
    private final Collection<Atom> atomsData;
    private final Map<String, Double> fiData;

    public static LipophilicityCalculator construct(File pdbFile, File fiFile) {
        Map<String, Double> fiData = readFiFile(fiFile);
        Collection<Atom> atomsData = readPdbFile(pdbFile, fiData);
        return new LipophilicityCalculator(atomsData, fiData);
    }

    private static Collection<Atom> readPdbFile(File pdbFile, Map<String, Double> fiData) {
        return PdbUtil.readPdbFile(pdbFile).getAtoms();
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

    private LipophilicityCalculator(Collection<Atom> atomsData, Map<String, Double> fiData) {
        this.atomsData = atomsData;
        this.fiData = fiData;
    }

    public double compute(Point p) {
        return atomsData
                .stream()
                .filter(atom -> fiData.containsKey(getFiKey(atom)))
                .mapToDouble(atom -> 100.0 * fiData.get(getFiKey(atom)) * Math.exp(-Geometry.distance(p, atom.p)))
                .sum();
    }

    private String getFiKey(Atom atom) {
        return atom.resName + "_" + atom.name;
    }


}
