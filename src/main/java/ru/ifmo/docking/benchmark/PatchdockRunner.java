package ru.ifmo.docking.benchmark;

import com.google.common.collect.Lists;
import ru.ifmo.docking.model.Protein;
import ru.ifmo.docking.util.IOUtils;
import ru.ifmo.docking.util.PdbUtil;
import ru.ifmo.docking.util.Timer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.function.Supplier;

public class PatchdockRunner implements DockerRunner {

    @Override
    public List<Supplier<Protein>> run(String complex, File benchmarkDir, Timer timer) {
        File resultsFileDir = new File(getName() + "_results");
        if (!resultsFileDir.exists()) {
            //noinspection ResultOfMethodCallIgnored
            resultsFileDir.mkdirs();
        }


        String unboundReceptorFile = complex + "_r_u";
        String unboundLigandFile = complex + "_l_u";

        File complexDir = new File(benchmarkDir, complex + "_data");

        File unboundReceptorPdb = new File(complexDir, unboundReceptorFile + ".pdb");
        File unboundLigandPdb = new File(complexDir, unboundLigandFile + ".pdb");

        preprocess(unboundReceptorPdb, unboundLigandPdb);

        File resultTs = new File(resultsFileDir, complex + ".ts");

        if (resultTs.exists()) {
            System.out.println("Result exists. Only creating results.");
            timer.setTime(readLong(new File(resultsFileDir, complex + ".ts")));
            runProcess("cp", resultsFileDir.getName() + "/" + complex + ".out", complex + ".out");
        } else {
            System.out.println("Start docking");
            timer.start();
            runProcess("./patch_dock.Linux", "params.txt", complex + ".out");
            timer.stop();
            writeLong(resultTs, timer.getTime());
            runProcess("cp", complex + ".out", resultsFileDir.getName() + "/" + complex + ".out");
        }

        System.out.println("Start writing results");
        runProcess("./transOutput.pl", complex + ".out", "0", "500000");

        List<Supplier<Protein>> results = Lists.newArrayList();
        File currentDir = new File(".");
        File[] files = currentDir.listFiles((dir, name) -> name.startsWith(complex + ".") && name.endsWith(".pdb"));
        for (File file : files) {
            results.add(() -> PdbUtil.readSimplifiedPdbFile(file));
        }
        return results;
    }

    @Override
    public void cleanup(String complex) {
        deleteFile(new File("params.txt"));
        deleteFile(new File(complex + ".out"));
        File currentDir = new File(".");
        File[] files = currentDir.listFiles((dir, name) -> name.startsWith(complex + ".") && name.endsWith(".pdb"));
        for (File file : files) {
            deleteFile(file);
        }
    }

    @Override
    public String getName() {
        return "patchdock";
    }

    private void deleteFile(File file) {
        if (file.exists()) {
            if (!file.delete()) {
                runProcess("rm", file.getAbsolutePath());
            }
        }
    }

    private void preprocess(File receptorPdb, File ligandPdb) {
        try {
            runProcess("./buildParams.pl", receptorPdb.getCanonicalPath(), ligandPdb.getCanonicalPath(), "4.0");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void runProcess(String... args) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            System.out.println("Running " + String.join(" ", processBuilder.command()));
            Process process = processBuilder.start();
            while (process.isAlive()) {
                try {
                    process.waitFor();
                } catch (InterruptedException ignored) {
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
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
