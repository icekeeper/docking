package ru.ifmo.docking.benchmark;

import com.google.common.collect.Lists;
import org.apache.commons.math3.linear.RealMatrix;
import ru.ifmo.docking.calculations.dockers.Docker;
import ru.ifmo.docking.model.Protein;
import ru.ifmo.docking.util.Timer;

import java.io.*;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Supplier;

public class FakeSpinRunner implements DockerRunner {

    private final BiFunction<File, String, Docker> dockerSupplier;
    private final String name;

    public FakeSpinRunner(String name, BiFunction<File, String, Docker> dockerSupplier) {
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

        File resultTs = new File(resultsFileDir, complex + ".ts");
        File resultFile = new File(resultsFileDir, complex + ".out");
        List<RealMatrix> results;

        if (!resultTs.exists()) {
            System.out.println("Run " + name + " docker on: " + complex);

            Docker docker = dockerSupplier.apply(complextDir, complex);
            timer.start();
            results = docker.run();
            timer.stop();

            writeLong(resultTs, timer.getTime());
            writeResultsFile(resultFile, results);
        }

        return Collections.emptyList();
    }

    @Override
    public void cleanup(String complex) {

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

    private void writeLong(File file, long value) {
        try (PrintWriter pw = new PrintWriter(file)) {
            pw.write(value + "\n");
        } catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

}
