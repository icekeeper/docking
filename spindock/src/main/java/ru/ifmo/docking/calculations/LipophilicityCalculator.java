package ru.ifmo.docking.calculations;

import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.model.Atom;
import ru.ifmo.docking.model.Protein;
import ru.ifmo.docking.util.PdbUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Simple lipophilicity calculator.
 * Based on pyMLP script written by Julien Lefeuvre <lefeuvrejulien@yahoo.fr>
 * https://code.google.com/p/pymlp
 */

public class LipophilicityCalculator {
    private final Map<String, Double> fiData;
    private final Protein protein;

    public static LipophilicityCalculator fromPdb(File pdbFile, File fiFile) {
        Map<String, Double> fiData = readFiFile(fiFile);
        return new LipophilicityCalculator(PdbUtil.readPdbFile(pdbFile), fiData);
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

    private LipophilicityCalculator(Protein protein, Map<String, Double> fiData) {
        this.protein = protein;
        this.fiData = fiData;
    }

    public double calculate(Point p) {
        return protein.getAtoms()
                .stream()
                .filter(atom -> fiData.containsKey(getFiKey(atom)))
                .mapToDouble(atom -> 100.0 * fiData.get(getFiKey(atom)) * Math.exp(-Geometry.distance(p, atom.p)))
                .sum();
    }

    private String getFiKey(Atom atom) {
        return atom.resName + "_" + atom.name;
    }


}
