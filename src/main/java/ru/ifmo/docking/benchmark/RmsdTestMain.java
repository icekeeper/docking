package ru.ifmo.docking.benchmark;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import ru.ifmo.docking.calculations.dockers.ElDocker;
import ru.ifmo.docking.calculations.dockers.GeometryDocker;
import ru.ifmo.docking.calculations.dockers.LipDocker;
import ru.ifmo.docking.calculations.dockers.LipElDocker;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.model.Atom;
import ru.ifmo.docking.model.Protein;
import ru.ifmo.docking.util.Pair;
import ru.ifmo.docking.util.PdbUtil;
import ru.ifmo.docking.util.ProteinUtils;
import ru.ifmo.docking.util.Timer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class RmsdTestMain {

    private static final Set<String> RIGID_CASES = Sets.newHashSet((
            "1A2K\n1AHW\n1AK4\n1AKJ\n1AVX\n1AY7\n1AZS\n1B6C\n1BJ1\n1BUH\n1BVK\n1BVN\n1CGI\n1CLV\n1D6R\n1DFJ\n1DQJ\n1E6E\n1E6J\n1E96\n1EAW\n1EFN\n" +
                    "1EWY\n1EZU\n1F34\n1F51\n1F51\n1FCC\n1FFW\n1FLE\n1FQJ\n1FSK\n1GCQ\n1GHQ\n1GL1\n1GLA\n1GPW\n1GXD\n1H9D\n1HCF\n1HE1\n1HIA\n1I4D\n" +
                    "1I9R\n1IQD\n1J2J\n1JPS\n1JTG\n1JWH\n1K4C\n1K74\n1KAC\n1KLU\n1KTZ\n1KXP\n1KXQ\n1MAH\n1ML0\n1MLC\n1N8O\n1NCA\n1NSN\n1OC0\n1OFU\n" +
                    "1OPH\n1OYV\n1PPE\n1PVH\n1QA9\n1R0R\n1RLB\n1RV6\n1S1Q\n1SBB\n1TMQ\n1UDI\n1VFB\n1WDW\n1WEJ\n1XD3\n1XU1\n1YVB\n1Z0K\n1Z5Y\n1ZHH\n" +
                    "1ZHI\n2A5T\n2A9K\n2ABZ\n2AJF\n2AYO\n2B42\n2B4J\n2BTF\n2FD6\n2FJU\n2G77\n2HLE\n2HQS\n2I25\n2J0T\n2JEL\n2MTA\n2O8V\n2OOB\n2OOR\n" +
                    "2OUL\n2PCC\n2SIC\n2SNI\n2UUY\n2VDB\n2VIS\n3BP8\n3D5S\n3SGQ\n4CPA\n7CEI\nBOYV").split("\\n"));

    private static final Set<String> MEDIUM_CASES = Sets.newHashSet(("1BGX\n1ACB\n1IJK\n1JIW\n1KKL\n1M10\n1NW9\n1GP2\n1GRN\n1HE8\n1I2M\n1IB1\n1K5D" +
            "\n1LFD\n1MQ8\n1N2C\n1R6Q\n1SYX\n1WQ1\n1XQS\n1ZM4\n2H7V\n2HRK\n2J7P\n2NZ8\n2OZA\n2Z0E\n3CPH").split("\\n"));

    private static final Set<String> HARD_CASES = Sets.newHashSet(("1E4K\n2HMI\n1F6M\n1FQ1\n1PXV\n1ZLI\n2O3B\n1ATN\n1BKD\n1DE4\n1EER\n1FAK\n1H1V\n" +
            "1IBR\n1IRA\n1JK9\n1JMO\n1JZD\n1R8S\n1Y64\n2C0L\n2I9B\n2IDO\n2OT3").split("\\n"));

    private static int getComplexity(String name) {
        if (RIGID_CASES.contains(name)) {
            return 0;
        } else if (MEDIUM_CASES.contains(name)) {
            return 1;
        } else if (HARD_CASES.contains(name)) {
            return 2;
        } else {
            return 3;
        }
    }

    public static List<String> parseParams(String[] args) {
        return Arrays.stream(args)
                .filter(s -> s.startsWith("-"))
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws FileNotFoundException {
        File dir = new File(args[0]);
        File[] candidates = dir.listFiles(pathname -> pathname.isDirectory() && pathname.getName().matches(".*_data"));

        List<String> proteinNames = Lists.newArrayList(
                Arrays.stream(candidates)
                        .map(file -> file.getName().substring(0, file.getName().indexOf('_')))
                        .collect(Collectors.toSet())
        );

        proteinNames.sort((o1, o2) -> getComplexity(o1) == getComplexity(o2) ? o1.compareTo(o2) : Integer.compare(getComplexity(o1), getComplexity(o2)));

        List<String> params = parseParams(args);
        if (params.contains("-renumerate")) {
            System.out.println("Renumerating pdbs");
            renumerateAtoms(dir, proteinNames);
        }

        if (params.contains("-benchmark")) {
            runBenchmark(new ZdockRunner(), dir, proteinNames);
            runBenchmark(new SpinImageDockerRunner("geometry", GeometryDocker::new), dir, proteinNames);
            runBenchmark(new SpinImageDockerRunner("combined", LipElDocker::new), dir, proteinNames);
            runBenchmark(new SpinImageDockerRunner("electric", ElDocker::new), dir, proteinNames);
            runBenchmark(new SpinImageDockerRunner("lipophilic", LipDocker::new), dir, proteinNames);
            runBenchmark(new PatchdockRunner(), dir, proteinNames);
        }

        if (params.contains("-eval")) {
            runDataEvaluation(dir, proteinNames);
        }
    }

    private static void runBenchmark(DockerRunner runner, File dir, List<String> proteinNames) throws FileNotFoundException {
        try (PrintWriter pw = new PrintWriter("report_" + runner.getName() + ".tsv")) {
            pw.println(String.format("complex\tbest iRMSD\tcomputation time\ttotal results\ttotal hits"));

            System.out.println("Runner: " + runner.getName() + ". Processing proteins: " + proteinNames);
            for (String proteinName : proteinNames) {
                try {
                    compareFiles(pw, dir, proteinName, runner);
                } catch (Exception e) {
                    System.out.println("Error processing: " + proteinName);
                    e.printStackTrace();
                }
            }
        }
    }

    private static void runDataEvaluation(File dir, List<String> proteinNames) throws FileNotFoundException {
        try (PrintWriter pw = new PrintWriter("evaluaton.tsv")) {
            pw.println(String.format("complex\tiRMSD\tCA atoms"));
            for (String proteinName : proteinNames) {
                try {
                    evaluateComplex(pw, dir, proteinName);
                } catch (Exception e) {
                    System.out.println("Error processing: " + proteinName);
                    e.printStackTrace();
                }
            }
        }
    }

    private static void renumerateAtoms(File dir, List<String> proteinNames) {
        for (String proteinName : proteinNames) {
            System.out.println("Renumerating " + proteinName);
            File proteinDir = new File(dir, proteinName + "_data");
            PdbUtil.renumerate(new File(proteinDir, proteinName + "_r_b.pdb"), 1);
            PdbUtil.renumerate(new File(proteinDir, proteinName + "_r_u.pdb"), 1);

            Protein boundReceptor = readProtein(proteinDir, proteinName + "_r_b");
            Protein unboundReceptor = readProtein(proteinDir, proteinName + "_r_u");

            PdbUtil.renumerate(new File(proteinDir, proteinName + "_l_b.pdb"), boundReceptor.getAtoms().size() + 1);
            PdbUtil.renumerate(new File(proteinDir, proteinName + "_l_u.pdb"), unboundReceptor.getAtoms().size() + 1);
        }
    }

    private static void evaluateComplex(PrintWriter pw, File dir, String complex) {
        System.out.println("Processing " + complex);
        File proteinDir = new File(dir, complex + "_data");

        Protein unboundReceptorProtein = readProtein(proteinDir, complex + "_r_u");
        Protein unboundLigandProtein = readProtein(proteinDir, complex + "_l_u");

        Protein boundReceptorProtein = readProtein(proteinDir, complex + "_r_b");
        Protein boundLigandProtein = readProtein(proteinDir, complex + "_l_b");

        List<Pair<Atom, Atom>> alphaCarbonsMapping = ProteinUtils.matchAlphaCarbons(
                boundReceptorProtein,
                boundLigandProtein,
                unboundReceptorProtein,
                unboundLigandProtein
        );

        double irmsd = computeInterfaceRmsd(alphaCarbonsMapping);
        pw.println(String.format(Locale.US, "%s\t%f\t%d", complex, irmsd, alphaCarbonsMapping.size()));
    }

    private static void compareFiles(PrintWriter pw, File dir, String complex, DockerRunner dockerRunner) {
        Timer timer = new Timer();
        List<Supplier<Protein>> results = dockerRunner.run(complex, dir, timer);
        System.out.println("Found " + results.size() + " solutions. Running time total: " + timer.getTime());

        File complexDir = new File(dir, complex + "_data");

        Protein unboundReceptorProtein = readProtein(complexDir, complex + "_r_u");
        Protein unboundLigandProtein = readProtein(complexDir, complex + "_l_u");

        Protein boundReceptorProtein = readProtein(complexDir, complex + "_r_b");
        Protein boundLigandProtein = readProtein(complexDir, complex + "_l_b");

        List<Pair<Atom, Atom>> alphaCarbonsMapping = ProteinUtils.matchAlphaCarbons(
                boundReceptorProtein,
                boundLigandProtein,
                unboundReceptorProtein,
                unboundLigandProtein
        );

        List<Point> boundAlphaCarbonPoints = getBoundCarbonPoints(alphaCarbonsMapping);

        List<Pair<Double, Integer>> solutions = IntStream.range(0, results.size())
                .parallel()
                .mapToObj(i -> {
                    Protein dockedComplex = results.get(i).get();
                    List<Point> dockedAlphaCarbonPoints = getUnboundCarbonPoints(dockedComplex, alphaCarbonsMapping);
                    return Pair.of(Geometry.rmsd(boundAlphaCarbonPoints, dockedAlphaCarbonPoints), i);
                })
                .collect(Collectors.toList());


        Pair<Double, Integer> minRmsd = solutions.stream().min((o1, o2) -> Double.compare(o1.first, o2.first)).get();
        long hits = solutions.stream().filter(p -> p.first < 2.5).count();

        System.out.println("For complex " + complex + " minimum rmsd is: " + minRmsd.first + " hits: " + hits);

        pw.println(String.format(Locale.US, "%s\t%f\t%d\t%d\t%d", complex, minRmsd.first, timer.getTime(), results.size(), hits));
        pw.flush();

        dockerRunner.cleanup(complex);
    }

    public static void printAtomsThatDiffer(Protein dockedComplex, Protein unboundReceptorProtein, Protein unboundLigandProtein) {
        for (int i = 0; i < unboundReceptorProtein.getAtoms().size(); i++) {
            Atom dockedAtom = dockedComplex.getAtoms().get(i);
            Atom receptorAtom = unboundReceptorProtein.getAtoms().get(i);
            if (!dockedAtom.equals(receptorAtom)) {
                System.out.println("Receptor atoms missmatch. Docked atom: " + dockedAtom + "\nReceptor atom: " + receptorAtom);
            }
        }

        for (int i = 0; i < unboundLigandProtein.getAtoms().size(); i++) {
            Atom dockedAtom = dockedComplex.getAtoms().get(i + unboundReceptorProtein.getAtoms().size());
            Atom ligandAtom = unboundLigandProtein.getAtoms().get(i);
            if (!dockedAtom.equals(ligandAtom)) {
                System.out.println("Ligand atoms missmatch. Docked atom: " + dockedAtom + "\nReceptor atom: " + ligandAtom);
            }
        }

    }

    private static Protein readProtein(File dir, String name) {
        File pdb = new File(dir, name + ".pdb");
        return PdbUtil.readPdbFile(pdb);
    }

    private static List<Point> getBoundCarbonPoints(List<Pair<Atom, Atom>> alphaCarbonsMapping) {
        return alphaCarbonsMapping
                .stream()
                .map(pair -> pair.first.getPoint())
                .collect(Collectors.toList());
    }

    private static List<Point> getUnboundCarbonPoints(Protein complex, List<Pair<Atom, Atom>> alphaCarbonsMapping) {
        return alphaCarbonsMapping
                .stream()
                .map(Pair::getSecond)
                .map(atom -> complex.getAtoms().get(complex.getAtoms().indexOf(atom)))
                .map(Atom::getPoint)
                .collect(Collectors.toList());
    }

    private static double computeInterfaceRmsd(List<Pair<Atom, Atom>> alphaCarbonMatching) {
        List<Point> bound = alphaCarbonMatching.stream().map(pair -> pair.first.getPoint()).collect(Collectors.toList());
        List<Point> unbound = alphaCarbonMatching.stream().map(pair -> pair.second.getPoint()).collect(Collectors.toList());
        return Geometry.rmsd(bound, unbound);
    }

}
