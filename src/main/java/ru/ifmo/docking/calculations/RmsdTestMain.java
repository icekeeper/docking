package ru.ifmo.docking.calculations;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import org.apache.commons.math3.linear.RealMatrix;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.model.Atom;
import ru.ifmo.docking.model.Protein;
import ru.ifmo.docking.model.Surface;
import ru.ifmo.docking.util.Pair;
import ru.ifmo.docking.util.PdbUtil;
import ru.ifmo.docking.util.RmsdCalculator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
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

    public static void main(String[] args) throws FileNotFoundException {
        try (PrintWriter pw = new PrintWriter("report.tsv")) {
            pw.println(String.format("complex\trmsd\tcomputation time\ttotal results"));

            File dir = new File(args[0]);
            File[] candidates = dir.listFiles(pathname -> pathname.isDirectory() && pathname.getName().matches(".*_[lr]_[bu]_data"));

            List<String> proteinNames = Lists.newArrayList(
                    Arrays.stream(candidates)
                            .map(file -> file.getName().substring(0, file.getName().indexOf('_')))
                            .collect(Collectors.toSet())
            );

            proteinNames.sort((o1, o2) -> getComplexity(o1) == getComplexity(o2) ? o1.compareTo(o2) : Integer.compare(getComplexity(o1), getComplexity(o2)));

            System.out.println("Processing proteins: " + proteinNames);

            for (String proteinName : proteinNames) {
                try {
                    compareFiles(pw, dir, proteinName);
                } catch (Exception e) {
                    System.out.println("Error processing: " + proteinName);
                    e.printStackTrace();
                }
            }
        }
    }

    private static Protein readProtein(File parentDir, String name) {
        File dir = new File(parentDir, name + "_data");
        File pdb = new File(dir, name + ".pdb");
        return PdbUtil.readPdbFile(pdb);
    }

    private static void compareFiles(PrintWriter pw, File dir, String complex) {
        String unboundReceptorFile = complex + "_r_" + "u";
        String unboundLigandFile = complex + "_l_" + "u";


        File unboundReceptorDir = new File(dir, unboundReceptorFile + "_data");
        Surface firstSurface = Surface.read(unboundReceptorFile, new File(unboundReceptorDir, unboundReceptorFile + ".obj"));

        File unboundLigandDir = new File(dir, unboundLigandFile + "_data");
        Surface secondSurface = Surface.read(unboundLigandFile, new File(unboundLigandDir, unboundLigandFile + ".obj"));

        Docker docker = new Docker(firstSurface, secondSurface);

        long startTs = System.currentTimeMillis();
        List<Pair<List<Docker.PointMatch>, RealMatrix>> results = docker.run();
        long endTs = System.currentTimeMillis();

        System.out.println("Found " + results.size() + " solutions. Running time total: " + (endTs - startTs));

        Protein unboundReceptorProtein = readProtein(dir, unboundReceptorFile);
        Protein unboundLigandProtein = readProtein(dir, unboundLigandFile);

        Protein boundReceptorProtein = readProtein(dir, complex + "_r_" + "b");
        Protein boundLigandProtein = readProtein(dir, complex + "_l_" + "b");

        RealMatrix pdbTransition = getProteinsCenteringTransition(unboundReceptorProtein, unboundLigandProtein);

        Map<Atom, Atom> alphaCarbonsMapping = RmsdCalculator.matchAlphaCarbons(boundReceptorProtein, boundLigandProtein, unboundReceptorProtein, unboundLigandProtein);
        System.out.println("Matched " + alphaCarbonsMapping.size() + " CA atoms.");

        List<Point> boundAlphaCarbonPoints = Lists.newArrayList();
        boundAlphaCarbonPoints.addAll(boundReceptorProtein.getAtoms()
                        .stream()
                        .filter(alphaCarbonsMapping::containsKey)
                        .map(Atom::getPoint)
                        .collect(Collectors.toList())
        );

        boundAlphaCarbonPoints.addAll(boundLigandProtein.getAtoms()
                        .stream()
                        .filter(alphaCarbonsMapping::containsKey)
                        .map(Atom::getPoint)
                        .collect(Collectors.toList())
        );

        List<Point> unboundReceptorAlphaCarbonPoints = boundReceptorProtein.getAtoms()
                .stream()
                .filter(alphaCarbonsMapping::containsKey)
                .map(alphaCarbonsMapping::get)
                .map(Atom::getPoint)
                .map(p -> Geometry.transformPoint(p, pdbTransition))
                .collect(Collectors.toList());

        List<Point> unboundLigandAlphaCarbonPoints = boundLigandProtein.getAtoms()
                .stream()
                .filter(alphaCarbonsMapping::containsKey)
                .map(alphaCarbonsMapping::get)
                .map(Atom::getPoint)
                .map(p -> Geometry.transformPoint(p, pdbTransition))
                .collect(Collectors.toList());

        Pair<Double, Integer> minRmsd = IntStream.range(0, results.size())
                .parallel()
                .mapToObj(i -> {
                    RealMatrix transition = results.get(i).second;

                    List<Point> dockedLigandAlphaCarbonPoints = unboundLigandAlphaCarbonPoints
                            .stream()
                            .map(point -> Geometry.transformPoint(point, transition))
                            .collect(Collectors.toList());

                    List<Point> dockedAlphaCarbonPoints = Lists.newArrayList();
                    dockedAlphaCarbonPoints.addAll(unboundReceptorAlphaCarbonPoints);
                    dockedAlphaCarbonPoints.addAll(dockedLigandAlphaCarbonPoints);

                    RealMatrix optimalRmsdTransition = Geometry.findRmsdOptimalTransformationMatrix(dockedAlphaCarbonPoints, boundAlphaCarbonPoints);
                    List<Point> alignedDockedAlphaCarbonPoints = dockedAlphaCarbonPoints
                            .stream()
                            .map(p -> Geometry.transformPoint(p, optimalRmsdTransition))
                            .collect(Collectors.toList());

                    return Pair.of(Geometry.rmsd(boundAlphaCarbonPoints, alignedDockedAlphaCarbonPoints), i);
                })
                .min((o1, o2) -> Double.compare(o1.first, o2.first)).get();


        System.out.println("For complex " + complex + " minimum rmsd is: " + minRmsd.first);

        pw.println(String.format("%s\t%f\t%d\t%d", complex, minRmsd.first, (endTs - startTs), results.size()));
        pw.flush();

        PdbUtil.rewritePdb(new File(dir, unboundReceptorFile + "_best.pdb"), unboundReceptorProtein, pdbTransition);
        PdbUtil.rewritePdb(new File(dir, unboundLigandFile + "_best.pdb"), unboundLigandProtein, pdbTransition.multiply(results.get(minRmsd.second).second));
    }

    public static RealMatrix getProteinsCenteringTransition(Protein firstProtein, Protein secondProtein) {
        List<Point> firstPoints = firstProtein.getAtoms().stream().map(Atom::getPoint).collect(Collectors.toList());
        List<Point> secondPoints = secondProtein.getAtoms().stream().map(Atom::getPoint).collect(Collectors.toList());

        List<Point> points = Lists.newArrayList(firstPoints);
        points.addAll(secondPoints);

        Point pdbCentroid = Geometry.centroid(points);
        return Geometry.getTransitionMatrix(pdbCentroid, new Point(0, 0, 0));
    }


}
