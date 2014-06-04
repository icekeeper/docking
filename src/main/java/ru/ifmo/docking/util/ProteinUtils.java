package ru.ifmo.docking.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.model.Atom;
import ru.ifmo.docking.model.Protein;

import java.io.File;
import java.util.*;
import java.util.stream.Collectors;

public class ProteinUtils {
    public static final int CONTACT_DISTANCE = 10;

    public static List<Pair<Atom, Atom>> matchAlphaCarbons(Protein boundReceptor,
                                                           Protein boundLigand,
                                                           Protein unboundReceptor,
                                                           Protein unboundLigand) {
        Set<String> receptorResidues = findContactResidues(boundReceptor, boundLigand);
        Set<String> ligandResidues = findContactResidues(boundLigand, boundReceptor);

//        Map<String, String> receptorAlignment = readAlignmentFile(receptorAlignmentFile);
//        Map<String, String> ligandAlignment = readAlignmentFile(ligandAlignmentFile);
//
//        Map<String, String> matchedReceptorResidues = matchResidues(boundReceptor, unboundReceptor, receptorAlignment);
//        Map<String, String> matchedLigandResidues = matchResidues(boundLigand, unboundLigand, ligandAlignment);

        Set<String> unboundReceptorResidues = unboundReceptor.getResiduesById().keySet();
        Set<String> unboundLigandResidues = unboundLigand.getResiduesById().keySet();

        List<Pair<String, String>> receptorContactResiduesMatch = receptorResidues.stream()
//                .filter(matchedReceptorResidues::containsKey)
//                .map(residue -> Pair.of(residue, matchedReceptorResidues.get(residue)))
                .filter(unboundReceptorResidues::contains)
                .map(residue -> Pair.of(residue, residue))
                .collect(Collectors.toList());

        List<Pair<String, String>> ligandContactResiduesMatch = ligandResidues.stream()
//                .filter(matchedLigandResidues::containsKey)
//                .map(residue -> Pair.of(residue, matchedLigandResidues.get(residue)))
                .filter(unboundLigandResidues::contains)
                .map(residue -> Pair.of(residue, residue))
                .collect(Collectors.toList());


        List<Pair<Atom, Atom>> alphaCarbonsMatch = Lists.newArrayList();

        alphaCarbonsMatch.addAll(matchAlphaCarbons(boundReceptor, unboundReceptor, receptorContactResiduesMatch));
        alphaCarbonsMatch.addAll(matchAlphaCarbons(boundLigand, unboundLigand, ligandContactResiduesMatch));

        return alphaCarbonsMatch;
    }

    private static Map<String, String> readAlignmentFile(File alignmentFile) {
        List<String> lines = IOUtils.linesStream(alignmentFile).collect(Collectors.toList());
        if (!lines.get(0).startsWith("CLUSTAL")) {
            throw new RuntimeException("Unknown alignment format");
        }

        StringBuilder first = new StringBuilder();
        StringBuilder second = new StringBuilder();
        StringBuilder third = new StringBuilder();

        for (int i = 2; i < lines.size(); i += 4) {
            first.append(lines.get(i).split("\\s+")[1].trim());
        }

        for (int i = 3; i < lines.size(); i += 4) {
            second.append(lines.get(i).split("\\s+")[1].trim());
        }

        for (int i = 4; i < lines.size(); i += 4) {
            String line = lines.get(i);
            third.append(line.substring(line.length() - 63));
        }

        String firstName = lines.get(2).split("\\s+")[0].trim();
        String secondName = lines.get(3).split("\\s+")[0].trim();

        Map<String, String> results = Maps.newHashMap();
        results.put(firstName, first.toString());
        results.put(secondName, second.toString());
        results.put("match", third.toString());
        return results;
    }

    private static Set<String> findContactResidues(Protein first, Protein second) {
        return first.getAtoms()
                .stream()
                .filter(atom -> second.getAtoms().stream().anyMatch(secondAtom -> Geometry.distance(atom.p, secondAtom.p) <= CONTACT_DISTANCE))
                .map(Atom::getResidueUid)
                .collect(Collectors.toCollection(Sets::newLinkedHashSet));
    }

    private static List<String> getResidues(Protein protein) {
        Set<String> found = Sets.newHashSet();
        List<String> residues = Lists.newArrayList();
        for (Atom atom : protein.getAtoms()) {
            String residue = atom.getResidueUid();
            if (found.add(residue)) {
                residues.add(residue);
            }
        }
        return residues;
    }

    private static Map<String, String> matchResidues(Protein bound, Protein unbound, Map<String, String> alignment) {
        String boundAlignment = alignment.get(bound.getName());
        String unboundAlignment = alignment.get(unbound.getName());

        List<String> boundResidues = getResidues(bound);
        List<String> unboundResidues = getResidues(unbound);

        Map<String, String> results = Maps.newHashMap();

        int boundIndex = 0;
        int unboundIndex = 0;

        for (int i = 0; i < boundAlignment.length(); i++) {
            char boundChar = boundAlignment.charAt(i);
            char unboundChar = unboundAlignment.charAt(i);
            char match = alignment.get("match").charAt(i);

            if (boundChar == unboundChar && match == '*') {
                results.put(boundResidues.get(boundIndex), unboundResidues.get(unboundIndex));
            }
            if (boundChar != '-') {
                boundIndex++;
            }
            if (unboundChar != '-') {
                unboundIndex++;
            }
        }
        return results;
    }

    private static List<Pair<Atom, Atom>> matchAlphaCarbons(Protein boundProtein, Protein unboundProtein, List<Pair<String, String>> residuesMatch) {
        Map<String, List<Atom>> boundResidues = boundProtein.getResiduesById();
        Map<String, List<Atom>> unboundResidues = unboundProtein.getResiduesById();

        List<Pair<Atom, Atom>> result = Lists.newArrayList();

        for (Pair<String, String> residuesPair : residuesMatch) {
            Optional<Atom> firstCarbon = findAlphaCarbon(boundResidues.get(residuesPair.first));
            Optional<Atom> secondCarbon = findAlphaCarbon(unboundResidues.get(residuesPair.second));
            if (firstCarbon.isPresent() && secondCarbon.isPresent()) {
                result.add(Pair.of(firstCarbon.get(), secondCarbon.get()));
            }
        }

        return result;
    }

    private static Optional<Atom> findAlphaCarbon(Collection<Atom> atoms) {
        return atoms.stream().filter(atom -> atom.name.equals("CA")).findFirst();
    }


}
