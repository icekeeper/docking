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
        Set<Integer> ligandResidues = findContactResidues(boundLigand, boundReceptor);
        Set<Integer> receptorResidues = findContactResidues(boundReceptor, boundLigand);

        Map<Integer, Integer> matchedLigandResidues = findMatchedResidues(boundLigand, unboundLigand, ligandResidues);
        Map<Integer, Integer> matchedReceptorResidues = findMatchedResidues(boundReceptor, unboundReceptor, receptorResidues);

        BiMap<Atom, Atom> alphaCarbonsMatch = HashBiMap.create();
        alphaCarbonsMatch.putAll(matchAlphaCarbons(boundLigand, unboundLigand, matchedLigandResidues));
        alphaCarbonsMatch.putAll(matchAlphaCarbons(boundReceptor, unboundReceptor, matchedReceptorResidues));

        return alphaCarbonsMatch;
    }

    private static Set<Integer> findContactResidues(Protein first, Protein second) {
        return first.getAtoms()
                .stream()
                .filter(atom -> second.getAtoms().stream().anyMatch(secondAtom -> Geometry.distance(atom.p, secondAtom.p) <= CONTACT_DISTANCE))
                .map(atom -> atom.resSeq)
                .collect(Collectors.toSet());
    }

    private static Map<Integer, Integer> findMatchedResidues(Protein boundProtein, Protein unboundProtein, Set<Integer> boundResidueNums) {
        Map<Integer, List<Atom>> boundResidues = boundProtein.getResiduesByNum();
        Map<Integer, List<Atom>> unboundResidues = unboundProtein.getResiduesByNum();

        BiMap<Integer, Integer> result = HashBiMap.create();
        Map<Integer, Double> unboundMinRmsd = Maps.newHashMap();

        for (Integer contactBoundResidueNum : boundResidueNums) {
            List<Atom> boundResidue = boundResidues.get(contactBoundResidueNum);
            String resName = boundResidues.get(contactBoundResidueNum).get(0).resName;

            double minRmsd = Double.MAX_VALUE;
            int unboundMinRmsdNum = 0;
            for (Integer unboundResNum : unboundResidues.keySet()) {
                List<Atom> unboundResidue = unboundResidues.get(unboundResNum);
                if (boundResidue.size() == unboundResidue.size()) {
                    double rmsd = Geometry.atomsRmsd(boundResidue, unboundResidue);
                    if (rmsd < minRmsd) {
                        minRmsd = rmsd;
                        unboundMinRmsdNum = unboundResNum;
                    }
                }
            }
            if (minRmsd != Double.MAX_VALUE && unboundResidues.get(unboundMinRmsdNum).get(0).resName.equals(resName)) {
                if (!unboundMinRmsd.containsKey(unboundMinRmsdNum) || unboundMinRmsd.get(unboundMinRmsdNum) > minRmsd) {
                    result.forcePut(contactBoundResidueNum, unboundMinRmsdNum);
                    unboundMinRmsd.put(unboundMinRmsdNum, minRmsd);
                }
            }
        }

        return result;
    }

    private static Map<Atom, Atom> matchAlphaCarbons(Protein boundProtein, Protein unboundProtein, Map<Integer, Integer> residuesMatch) {
        Map<Integer, List<Atom>> boundResidues = boundProtein.getResiduesByNum();
        Map<Integer, List<Atom>> unboundResidues = unboundProtein.getResiduesByNum();

        Map<Atom, Atom> result = Maps.newHashMap();

        for (Integer boundResidueNum : residuesMatch.keySet()) {
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
