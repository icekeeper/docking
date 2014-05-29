package ru.ifmo.docking.util;

import com.google.common.collect.BiMap;
import com.google.common.collect.HashBiMap;
import com.google.common.collect.Maps;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.model.Atom;
import ru.ifmo.docking.model.Protein;

import java.util.*;
import java.util.stream.Collectors;

public class ProteinUtils {
    public static final int CONTACT_DISTANCE = 10;

    public static BiMap<Atom, Atom> matchAlphaCarbons(Protein boundReceptor, Protein boundLigand, Protein unboundReceptor, Protein unboundLigand) {
        Set<String> ligandResidues = findContactResidues(boundLigand, boundReceptor);
        Set<String> receptorResidues = findContactResidues(boundReceptor, boundLigand);

        Map<String, String> matchedLigandResidues = findMatchedResidues(boundLigand, unboundLigand, ligandResidues);
        Map<String, String> matchedReceptorResidues = findMatchedResidues(boundReceptor, unboundReceptor, receptorResidues);

        BiMap<Atom, Atom> alphaCarbonsMatch = HashBiMap.create();
        alphaCarbonsMatch.putAll(matchAlphaCarbons(boundLigand, unboundLigand, matchedLigandResidues));
        alphaCarbonsMatch.putAll(matchAlphaCarbons(boundReceptor, unboundReceptor, matchedReceptorResidues));

        return alphaCarbonsMatch;
    }

    private static Set<String> findContactResidues(Protein first, Protein second) {
        return first.getAtoms()
                .stream()
                .filter(atom -> second.getAtoms().stream().anyMatch(secondAtom -> Geometry.distance(atom.p, secondAtom.p) <= CONTACT_DISTANCE))
                .map(atom -> atom.iCode + "_" + atom.resSeq)
                .collect(Collectors.toSet());
    }

    private static Map<String, String> findMatchedResidues(Protein boundProtein, Protein unboundProtein, Set<String> boundResidueIds) {
        Map<String, List<Atom>> boundResidues = boundProtein.getResiduesById();
        Map<String, List<Atom>> unboundResidues = unboundProtein.getResiduesById();

        BiMap<String, String> result = HashBiMap.create();
        Map<String, Double> unboundMinRmsd = Maps.newHashMap();

        for (String contactBoundResidueNum : boundResidueIds) {
            List<Atom> boundResidue = boundResidues.get(contactBoundResidueNum);
            String resName = boundResidues.get(contactBoundResidueNum).get(0).resName;

            double minRmsd = Double.MAX_VALUE;
            String unboundMinRmsdId = "";
            for (String unboundResNum : unboundResidues.keySet()) {
                List<Atom> unboundResidue = unboundResidues.get(unboundResNum);
                if (boundResidue.size() == unboundResidue.size()) {
                    double rmsd = Geometry.atomsRmsd(boundResidue, unboundResidue);
                    if (rmsd < minRmsd) {
                        minRmsd = rmsd;
                        unboundMinRmsdId = unboundResNum;
                    }
                }
            }
            if (minRmsd != Double.MAX_VALUE && unboundResidues.get(unboundMinRmsdId).get(0).resName.equals(resName)) {
                if (!unboundMinRmsd.containsKey(unboundMinRmsdId) || unboundMinRmsd.get(unboundMinRmsdId) > minRmsd) {
                    result.forcePut(contactBoundResidueNum, unboundMinRmsdId);
                    unboundMinRmsd.put(unboundMinRmsdId, minRmsd);
                }
            }
        }

        return result;
    }

    private static Map<Atom, Atom> matchAlphaCarbons(Protein boundProtein, Protein unboundProtein, Map<String, String> residuesMatch) {
        Map<String, List<Atom>> boundResidues = boundProtein.getResiduesById();
        Map<String, List<Atom>> unboundResidues = unboundProtein.getResiduesById();

        Map<Atom, Atom> result = Maps.newHashMap();

        for (String boundResidueNum : residuesMatch.keySet()) {
            Optional<Atom> boundAlphaCarbon = findAlphaCarbon(boundResidues.get(boundResidueNum));
            Optional<Atom> unboundAlphaCarbon = findAlphaCarbon(unboundResidues.get(residuesMatch.get(boundResidueNum)));
            if (boundAlphaCarbon.isPresent() && unboundAlphaCarbon.isPresent()) {
                result.put(boundAlphaCarbon.get(), unboundAlphaCarbon.get());
            }
        }

        return result;
    }

    private static Optional<Atom> findAlphaCarbon(Collection<Atom> atoms) {
        return atoms.stream().filter(atom -> atom.name.equals("CA")).findFirst();
    }


}
