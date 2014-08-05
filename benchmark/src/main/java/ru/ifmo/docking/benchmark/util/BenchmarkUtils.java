package ru.ifmo.docking.benchmark.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.model.Atom;
import ru.ifmo.docking.model.Protein;
import ru.ifmo.docking.util.Pair;

import java.io.*;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class BenchmarkUtils {
    public static final int CONTACT_DISTANCE = 10;

    public static Map<String, ChainMatch> CHAINS_MAP = Maps.newHashMap();

    static {
        CHAINS_MAP.put("1AHW", new ChainMatch("AB:LH", "C:A"));
        CHAINS_MAP.put("1BVK", new ChainMatch("DE:BA", "F: "));
        CHAINS_MAP.put("1DQJ", new ChainMatch("AB:CD", "C: "));
        CHAINS_MAP.put("1E6J", new ChainMatch("HL:HL", "P: "));
        CHAINS_MAP.put("1JPS", new ChainMatch("HL:HL", "T:B"));
        CHAINS_MAP.put("1MLC", new ChainMatch("AB:AB", "E: "));
        CHAINS_MAP.put("1VFB", new ChainMatch("AB:AB", "C: "));
        CHAINS_MAP.put("1WEJ", new ChainMatch("HL:HL", "F: "));
        CHAINS_MAP.put("2FD6", new ChainMatch("HL:HL", "U:A"));
        CHAINS_MAP.put("2VIS", new ChainMatch("AB:LH", "C:C"));
        CHAINS_MAP.put("1BJ1", new ChainMatch("HL:HL", "VW:GH"));
        CHAINS_MAP.put("1FSK", new ChainMatch("BC:BC", "A: "));
        CHAINS_MAP.put("1I9R", new ChainMatch("ABC:ABC", "HL:HL"));
        CHAINS_MAP.put("1IQD", new ChainMatch("AB:AB", "C:M"));
        CHAINS_MAP.put("1K4C", new ChainMatch("AB:AB", "C:A"));
        CHAINS_MAP.put("1KXQ", new ChainMatch("A: ", "H:H"));
        CHAINS_MAP.put("1NCA", new ChainMatch("HL:HL", "N: "));
        CHAINS_MAP.put("1NSN", new ChainMatch("HL:HL", "S: "));
        CHAINS_MAP.put("1QFW", new ChainMatch("IM:IM", "AB:AB"));
        CHAINS_MAP.put("9QFW", new ChainMatch("HL:HL", "AB:AB"));
        CHAINS_MAP.put("2JEL", new ChainMatch("HL:HL", "P: "));
        CHAINS_MAP.put("1AVX", new ChainMatch("A:A", "B:B"));
        CHAINS_MAP.put("1AY7", new ChainMatch("A:B", "B:B"));
        CHAINS_MAP.put("1BVN", new ChainMatch("P: ", "T: "));
        CHAINS_MAP.put("1CGI", new ChainMatch("E:B", "I: "));
        CHAINS_MAP.put("1CLV", new ChainMatch("A:A", "I:A"));
        CHAINS_MAP.put("1D6R", new ChainMatch("A: ", "I:A"));
        CHAINS_MAP.put("1DFJ", new ChainMatch("E:B", "I: "));
        CHAINS_MAP.put("1E6E", new ChainMatch("A:A", "B:D"));
        CHAINS_MAP.put("1EAW", new ChainMatch("A:A", "B: "));
        CHAINS_MAP.put("1EWY", new ChainMatch("A:A", "C:A"));
        CHAINS_MAP.put("1EZU", new ChainMatch("C:A", "AB:AB"));
        CHAINS_MAP.put("1F34", new ChainMatch("A: ", "B:A"));
        CHAINS_MAP.put("1FLE", new ChainMatch("E:A", "I:A"));
        CHAINS_MAP.put("1GL1", new ChainMatch("A:1", "I:A"));
        CHAINS_MAP.put("1GXD", new ChainMatch("A:A", "C:A"));
        CHAINS_MAP.put("1HIA", new ChainMatch("AB:XY", "I: "));
        CHAINS_MAP.put("1JTG", new ChainMatch("A:A", "B:B"));
        CHAINS_MAP.put("1MAH", new ChainMatch("A:B", "F: "));
        CHAINS_MAP.put("1N8O", new ChainMatch("A:A", "E:A"));
        CHAINS_MAP.put("1OC0", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1OPH", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1OYV", new ChainMatch("B:A", "I:A"));
        CHAINS_MAP.put("1OYV", new ChainMatch("A:A", "I:A"));
        CHAINS_MAP.put("1PPE", new ChainMatch("E: ", "I:A"));
        CHAINS_MAP.put("1R0R", new ChainMatch("E:E", "I:I"));
        CHAINS_MAP.put("1TMQ", new ChainMatch("A: ", "B:A"));
        CHAINS_MAP.put("1UDI", new ChainMatch("E: ", "I:B"));
        CHAINS_MAP.put("1YVB", new ChainMatch("A:A", "I:I"));
        CHAINS_MAP.put("2ABZ", new ChainMatch("B:A", "E:A"));
        CHAINS_MAP.put("2B42", new ChainMatch("A:X", "B:A"));
        CHAINS_MAP.put("2J0T", new ChainMatch("A:A", "D:A"));
        CHAINS_MAP.put("2MTA", new ChainMatch("HL:JM", "A:A"));
        CHAINS_MAP.put("2O8V", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("2OUL", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("2PCC", new ChainMatch("A: ", "B: "));
        CHAINS_MAP.put("2SIC", new ChainMatch("E: ", "I: "));
        CHAINS_MAP.put("2SNI", new ChainMatch("E:A", "I:I"));
        CHAINS_MAP.put("2UUY", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("3SGQ", new ChainMatch("E:E", "I:A"));
        CHAINS_MAP.put("4CPA", new ChainMatch("A:A", "I:A"));
        CHAINS_MAP.put("7CEI", new ChainMatch("A:D", "B:B"));
        CHAINS_MAP.put("1A2K", new ChainMatch("AB:AB", "C:A"));
        CHAINS_MAP.put("1AK4", new ChainMatch("A: ", "D:P"));
        CHAINS_MAP.put("1AKJ", new ChainMatch("AB:DE", "DE:AB"));
        CHAINS_MAP.put("1AZS", new ChainMatch("AB:AB", "C:A"));
        CHAINS_MAP.put("1B6C", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1BUH", new ChainMatch("A: ", "B:A"));
        CHAINS_MAP.put("1E96", new ChainMatch("A: ", "B:A"));
        CHAINS_MAP.put("1EFN", new ChainMatch("B:A", "A:A"));
        CHAINS_MAP.put("1F51", new ChainMatch("AB:AB", "E:C"));
        CHAINS_MAP.put("1FC2", new ChainMatch("C: ", "D:AB"));
        CHAINS_MAP.put("1FCC", new ChainMatch("AB:AB", "C:A"));
        CHAINS_MAP.put("1FFW", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1FQJ", new ChainMatch("A:C", "B:A"));
        CHAINS_MAP.put("1GCQ", new ChainMatch("B:B", "C:B"));
        CHAINS_MAP.put("1GHQ", new ChainMatch("A: ", "B:A"));
        CHAINS_MAP.put("1GLA", new ChainMatch("G:O", "F:A"));
        CHAINS_MAP.put("1GPW", new ChainMatch("A:D", "B:F"));
        CHAINS_MAP.put("1H9D", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1HCF", new ChainMatch("AB:AM", "X:X"));
        CHAINS_MAP.put("1HE1", new ChainMatch("A:A", "C: "));
        CHAINS_MAP.put("1I4D", new ChainMatch("AB:AB", "D: "));
        CHAINS_MAP.put("1J2J", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1JWH", new ChainMatch("CD:AB", "A:A"));
        CHAINS_MAP.put("1K74", new ChainMatch("AB:AB", "DE:AB"));
        CHAINS_MAP.put("1KAC", new ChainMatch("A:F", "B:B"));
        CHAINS_MAP.put("1KLU", new ChainMatch("AB:AB", "D: "));
        CHAINS_MAP.put("1KTZ", new ChainMatch("A: ", "B:A"));
        CHAINS_MAP.put("1KXP", new ChainMatch("A:B", "D:B"));
        CHAINS_MAP.put("1ML0", new ChainMatch("AB:AB", "D: "));
        CHAINS_MAP.put("1OFU", new ChainMatch("XY:AB", "A:A"));
        CHAINS_MAP.put("1PVH", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1QA9", new ChainMatch("A: ", "B:A"));
        CHAINS_MAP.put("1RLB", new ChainMatch("ABCD:ABCD", "E: "));
        CHAINS_MAP.put("1RV6", new ChainMatch("VW:AB", "X:A"));
        CHAINS_MAP.put("1S1Q", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1SBB", new ChainMatch("A: ", "B: "));
        CHAINS_MAP.put("1T6B", new ChainMatch("X:A", "Y:X"));
        CHAINS_MAP.put("1US7", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1WDW", new ChainMatch("BD:AB", "A:A"));
        CHAINS_MAP.put("1XD3", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1XU1", new ChainMatch("ABD:ABD", "T:A"));
        CHAINS_MAP.put("1Z0K", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1Z5Y", new ChainMatch("E:A", "D:A"));
        CHAINS_MAP.put("1ZHH", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1ZHI", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("2A5T", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("2A9K", new ChainMatch("A:A", "B:X"));
        CHAINS_MAP.put("2AJF", new ChainMatch("A:A", "E:E"));
        CHAINS_MAP.put("2AYO", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("2B4J", new ChainMatch("AB:AB", "C:A"));
        CHAINS_MAP.put("2BTF", new ChainMatch("A:B", "P: "));
        CHAINS_MAP.put("2FJU", new ChainMatch("B:X", "A:A"));
        CHAINS_MAP.put("2G77", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("2HLE", new ChainMatch("A:A", "B:P"));
        CHAINS_MAP.put("2HQS", new ChainMatch("A:A", "H:A"));
        CHAINS_MAP.put("2I25", new ChainMatch("N:N", "L:A"));
        CHAINS_MAP.put("2OOB", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("2OOR", new ChainMatch("AB:AB", "C:A"));
        CHAINS_MAP.put("2VDB", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("3BP8", new ChainMatch("AB:AB", "C:A"));
        CHAINS_MAP.put("3D5S", new ChainMatch("A:A", "C:A"));
        CHAINS_MAP.put("1BGX", new ChainMatch("HL:HL", "T:A"));
        CHAINS_MAP.put("1ACB", new ChainMatch("E:B", "I: "));
        CHAINS_MAP.put("1IJK", new ChainMatch("BC:AB", "A: "));
        CHAINS_MAP.put("1JIW", new ChainMatch("P:A", "I:A"));
        CHAINS_MAP.put("1KKL", new ChainMatch("ABC:ABC", "H: "));
        CHAINS_MAP.put("1M10", new ChainMatch("A: ", "B:B"));
        CHAINS_MAP.put("1NW9", new ChainMatch("B:A", "A:A"));
        CHAINS_MAP.put("1GP2", new ChainMatch("A: ", "BG:DH"));
        CHAINS_MAP.put("1GRN", new ChainMatch("A:A", "B: "));
        CHAINS_MAP.put("1HE8", new ChainMatch("A:A", "B: "));
        CHAINS_MAP.put("1I2M", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1IB1", new ChainMatch("AB:AB", "E:A"));
        CHAINS_MAP.put("1K5D", new ChainMatch("AB:AB", "C:B"));
        CHAINS_MAP.put("1LFD", new ChainMatch("B:A", "A:A"));
        CHAINS_MAP.put("1MQ8", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1N2C", new ChainMatch("ABCD:ABCD", "EF:AB"));
        CHAINS_MAP.put("1R6Q", new ChainMatch("A:X", "C:A"));
        CHAINS_MAP.put("1SYX", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1WQ1", new ChainMatch("G: ", "R:D"));
        CHAINS_MAP.put("1XQS", new ChainMatch("A:A", "C:A"));
        CHAINS_MAP.put("1ZM4", new ChainMatch("A:C", "B:A"));
        CHAINS_MAP.put("2CFH", new ChainMatch("A:A", "C:A"));
        CHAINS_MAP.put("2H7V", new ChainMatch("C:A", "A:A"));
        CHAINS_MAP.put("2HRK", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("2J7P", new ChainMatch("A:A", "D:D"));
        CHAINS_MAP.put("2NZ8", new ChainMatch("B:A", "A:A"));
        CHAINS_MAP.put("2OZA", new ChainMatch("B:A", "A:X"));
        CHAINS_MAP.put("2Z0E", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("3CPH", new ChainMatch("G:G", "A:C"));
        CHAINS_MAP.put("1E4K", new ChainMatch("AB:AB", "C:A"));
        CHAINS_MAP.put("2HMI", new ChainMatch("AB:AB", "CD:CD"));
        CHAINS_MAP.put("1F6M", new ChainMatch("A:A", "C:A"));
        CHAINS_MAP.put("1FQ1", new ChainMatch("A:F", "B:A"));
        CHAINS_MAP.put("1PXV", new ChainMatch("A:A", "C:A"));
        CHAINS_MAP.put("1ZLI", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("2O3B", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1ATN", new ChainMatch("A:B", "D: "));
        CHAINS_MAP.put("1BKD", new ChainMatch("S:A", "R:A"));
        CHAINS_MAP.put("1DE4", new ChainMatch("AB:AB", "CF:AB"));
        CHAINS_MAP.put("1EER", new ChainMatch("A:A", "BC:AB"));
        CHAINS_MAP.put("1FAK", new ChainMatch("HL:HL", "T:B"));
        CHAINS_MAP.put("1H1V", new ChainMatch("A:B", "G:B"));
        CHAINS_MAP.put("1IBR", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("1IRA", new ChainMatch("Y:R", "X: "));
        CHAINS_MAP.put("1JK9", new ChainMatch("B:A", "A:A"));
        CHAINS_MAP.put("1JMO", new ChainMatch("A:A", "HL:HL"));
        CHAINS_MAP.put("1JZD", new ChainMatch("AB:AB", "C:A"));
        CHAINS_MAP.put("1R8S", new ChainMatch("A:A", "E:E"));
        CHAINS_MAP.put("1Y64", new ChainMatch("B:A", "A:A"));
        CHAINS_MAP.put("2C0L", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("2I9B", new ChainMatch("E:A", "A:A"));
        CHAINS_MAP.put("2IDO", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("2OT3", new ChainMatch("A:A", "B:A"));
        CHAINS_MAP.put("BOYV", new ChainMatch("B:A", "I:A"));
        CHAINS_MAP.put("9QFM", new ChainMatch("B:A", "I:A"));
    }


    public static List<Pair<Atom, Atom>> matchAlphaCarbons(File complexDir,
                                                           String name,
                                                           Protein boundReceptor,
                                                           Protein boundLigand,
                                                           Protein unboundReceptor,
                                                           Protein unboundLigand) {

        Set<Atom> receptorContactCa = findContactResiduesCaAtoms(boundReceptor, boundLigand);
        Set<Atom> ligandContactCa = findContactResiduesCaAtoms(boundLigand, boundReceptor);

        List<Pair<Atom, Atom>> alphaCarbonsMatch = Lists.newArrayList();

        alphaCarbonsMatch.addAll(match(complexDir, name + "_r", boundReceptor, unboundReceptor, CHAINS_MAP.get(name).receptorMatch));
        alphaCarbonsMatch.addAll(match(complexDir, name + "_l", boundLigand, unboundLigand, CHAINS_MAP.get(name).ligandMatch));

        return alphaCarbonsMatch
                .stream()
                .filter(pair -> receptorContactCa.contains(pair.first) || ligandContactCa.contains(pair.first))
                .collect(Collectors.toList())
                ;
    }


    private static Set<Atom> findContactResiduesCaAtoms(Protein first, Protein second) {
        return first.getResiduesById()
                .values()
                .stream()
                .filter(list -> list
                        .stream()
                        .anyMatch(atom -> atom.name.equals("CA")))
                .filter(list -> list
                                .stream()
                                .anyMatch(firstAtom -> second
                                                .getAtoms()
                                                .stream()
                                                .anyMatch(secondAtom -> Geometry.distance(firstAtom.p, secondAtom.p) <= CONTACT_DISTANCE)
                                )
                )
                .map(list -> list
                                .stream()
                                .filter(atom -> atom.name.equals("CA"))
                                .findFirst()
                                .get()
                )
                .collect(Collectors.toSet());
    }

    private static List<Atom> filterAlphaCarbons(Collection<Atom> atoms) {
        return atoms.stream().filter(atom -> atom.name.equals("CA")).collect(Collectors.toList());
    }

    private static List<Pair<Atom, Atom>> match(File dir,
                                                String name,
                                                Protein bound,
                                                Protein unbound,
                                                String chainsMatch) {
        String[] tokens = chainsMatch.split(":");

        int count = tokens[0].length();
        List<Pair<Atom, Atom>> result = Lists.newArrayList();

        for (int i = 0; i < count; i++) {
            char firstChain = tokens[0].charAt(i);
            char secondChain = tokens[1].charAt(i);

            List<Atom> firstCarbons = filterAlphaCarbons(firstChain == ' ' ? bound.getAtoms() : bound.getChain(firstChain));
            List<Atom> secondCarbons = filterAlphaCarbons(secondChain == ' ' ? unbound.getAtoms() : unbound.getChain(secondChain));

            String firstName = dir.getAbsolutePath() + "/" + name + "_b" + (firstChain == ' ' ? "" : "_" + firstChain) + ".fa";
            String secondName = dir.getAbsolutePath() + "/" + name + "_u" + (secondChain == ' ' ? "" : "_" + secondChain) + ".fa";

            Map<Integer, Integer> carbonMapping = runBlast(firstName, secondName);
            for (Map.Entry<Integer, Integer> entry : carbonMapping.entrySet()) {
                result.add(Pair.of(firstCarbons.get(entry.getKey()), secondCarbons.get(entry.getValue())));
            }

        }
        return result;
    }

    private static Map<Integer, Integer> runBlast(String first, String second) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder("blastp", "-query", first, "-subject", second, "-outfmt", "6 qstart sstart qseq sseq");
            System.out.println("Running " + String.join(" ", processBuilder.command()));
            Process process = processBuilder.start();
            while (process.isAlive()) {
                try {
                    process.waitFor();
                } catch (InterruptedException ignored) {
                }
            }
            InputStream inputStream = process.getInputStream();

            Map<Integer, Integer> result = Maps.newHashMap();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
                String line = reader.readLine();
                while (line != null) {
                    String[] tokens = line.split("\t");
                    int qpos = Integer.parseInt(tokens[0]) - 1;
                    int spos = Integer.parseInt(tokens[1]) - 1;

                    String firstLine = tokens[2];
                    String secondLine = tokens[3];

                    if (firstLine.length() != secondLine.length()) {
                        throw new RuntimeException("blastp alignment strings of different length found");
                    }

                    for (int i = 0; i < firstLine.length(); i++) {
                        if (firstLine.charAt(i) == secondLine.charAt(i)) {
                            result.put(qpos, spos);
                        }
                        if (firstLine.charAt(i) != '-') {
                            qpos++;
                        }
                        if (secondLine.charAt(i) != '-') {
                            spos++;
                        }
                    }

                    line = reader.readLine();
                }
            }
            return result;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class ChainMatch {
        final String receptorMatch;
        final String ligandMatch;

        private ChainMatch(String receptorMatch, String ligandMatch) {
            this.receptorMatch = receptorMatch;
            this.ligandMatch = ligandMatch;
        }

    }

}
