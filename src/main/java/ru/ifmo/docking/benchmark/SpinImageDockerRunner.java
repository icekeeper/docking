package ru.ifmo.docking.benchmark;

import com.google.common.collect.Lists;
import org.apache.commons.math3.linear.MatrixUtils;
import org.apache.commons.math3.linear.RealMatrix;
import ru.ifmo.docking.calculations.dockers.Docker;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.model.Atom;
import ru.ifmo.docking.model.Protein;
import ru.ifmo.docking.model.Surface;
import ru.ifmo.docking.util.IOUtils;
import ru.ifmo.docking.util.PdbUtil;
import ru.ifmo.docking.util.Timer;

import java.io.*;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class SpinImageDockerRunner implements DockerRunner {

    private final BiFunction<Surface, Surface, Docker> dockerSupplier;
    private final String name;

    public SpinImageDockerRunner(String name, BiFunction<Surface, Surface, Docker> dockerSupplier) {
        this.name = name;
        this.dockerSupplier = dockerSupplier;
    }


    @Override
    public List<Supplier<Protein>> run(String complex, File benchmarkDir, Timer timer) {
        File resultsFileDir = new File(getName() + "_results");
        if (!resultsFileDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            resultsFileDir.mkdirs();
        }
        File complextDir = new File(benchmarkDir, complex + "_data");
        String unboundReceptorFile = complex + "_r_" + "u";
        String unboundLigandFile = complex + "_l_" + "u";

        File resultTs = new File(resultsFileDir, complex + ".ts");
        File resultFile = new File(resultsFileDir, complex + ".out");
        List<RealMatrix> results;

        if (resultTs.exists()) {
            System.out.println("Result exists. Read from file");
            timer.setTime(readLong(new File(resultsFileDir, complex + ".ts")));
            results = readResultsFile(resultFile);
        } else {
            System.out.println("Run spin docker on: " + complex);

            Surface firstSurface = Surface.read(unboundReceptorFile,
                    new File(complextDir, unboundReceptorFile + ".obj"),
                    new File(complextDir, unboundReceptorFile + ".pdb"),
                    new File(complextDir, unboundReceptorFile + ".pqr"),
                    new File("fi_potentials.txt")
            );

            Surface secondSurface = Surface.read(unboundLigandFile,
                    new File(complextDir, unboundLigandFile + ".obj"),
                    new File(complextDir, unboundLigandFile + ".pdb"),
                    new File(complextDir, unboundLigandFile + ".pqr"),
                    new File("fi_potentials.txt")
            );

            Docker docker = dockerSupplier.apply(firstSurface, secondSurface);

            timer.start();
            results = docker.run();
            timer.stop();

            writeLong(resultTs, timer.getTime());
            writeResultsFile(resultFile, results);
        }


        Protein unboundReceptorProtein = PdbUtil.readPdbFile(new File(complextDir, unboundReceptorFile + ".pdb"));
        Protein unboundLigandProtein = PdbUtil.readPdbFile(new File(complextDir, unboundLigandFile + ".pdb"));

        //noinspection RedundantCast
        return results.stream()
                .map(matrix -> (Supplier<Protein>) () -> {
                    List<Atom> result = Lists.newArrayListWithCapacity(unboundReceptorProtein.getAtoms().size() + unboundLigandProtein.getAtoms().size());
                    result.addAll(unboundReceptorProtein.getAtoms());
                    result.addAll(unboundLigandProtein.getAtoms()
                            .stream()
                            .map(atom -> transformAtom(atom, matrix))
                            .collect(Collectors.toList()));
                    return new Protein(name, result);
                })
                .collect(Collectors.toList());
    }

    @Override
    public void cleanup(String complex) {

    }

    private RealMatrix deserializeMatrix(String string) {
        double[][] data = new double[4][4];
        String[] tokens = string.split("\\s+");
        int i = 0;
        for (int row = 0; row < 4; row++) {
            for (int column = 0; column < 4; column++) {
                data[row][column] = Double.parseDouble(tokens[i++]);
            }
        }
        return MatrixUtils.createRealMatrix(data);
    }

    private String serializeMatrix(RealMatrix matrix) {
        double[][] data = matrix.getData();
        List<String> values = Lists.newArrayList();

        for (double[] row : data) {
            for (double value : row) {
                values.add(Double.toString(value));
            }
        }
        return String.join(" ", values);
    }

    private List<RealMatrix> readResultsFile(File file) {
        return IOUtils.linesStream(file)
                .map(this::deserializeMatrix)
                .collect(Collectors.toList());
    }

    private void writeResultsFile(File file, List<RealMatrix> results) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (RealMatrix result : results) {
                writer.write(serializeMatrix(result));
                writer.write("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String getName() {
        return name;
    }

    public static Atom transformAtom(Atom atom, RealMatrix matrix) {
        return new Atom(
                atom.recordName,
                atom.serial,
                atom.name,
                atom.altLock,
                atom.resName,
                atom.chainId,
                atom.resSeq,
                atom.iCode,
                Geometry.transformPoint(atom.p, matrix),
                atom.occupancy,
                atom.tempFactor,
                atom.element,
                atom.pdbCharge,
                atom.charge,
                atom.r
        );
    }

    private Long readLong(File file) {
        return IOUtils.linesStream(file).map(Long::parseLong).findFirst().get();
    }

    private void writeLong(File file, long value) {
        try (PrintWriter pw = new PrintWriter(file)) {
            pw.write(value + "\n");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
