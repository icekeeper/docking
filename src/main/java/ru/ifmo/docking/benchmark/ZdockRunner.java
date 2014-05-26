package ru.ifmo.docking.benchmark;

import com.google.common.collect.Lists;
import ru.ifmo.docking.model.Protein;
import ru.ifmo.docking.util.PdbUtil;
import ru.ifmo.docking.util.Timer;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

public class ZdockRunner implements DockerRunner {

    @Override
    public List<Supplier<Protein>> run(String complex, File benchmarkDir, Timer timer) {
        String unboundReceptorFile = complex + "_r_" + "u";
        String unboundLigandFile = complex + "_l_" + "u";

        File unboundReceptorDir = new File(benchmarkDir, unboundReceptorFile + "_data");
        File unboundLigandDir = new File(benchmarkDir, unboundLigandFile + "_data");

        File unboundReceptorPdb = new File(unboundReceptorDir, unboundReceptorFile + ".pdb");
        File unboundLigandPdb = new File(unboundLigandDir, unboundLigandFile + ".pdb");

        System.out.println("Preprocessing receptor");
        preprocess(unboundReceptorPdb);
        System.out.println("Preprocessing ligand");
        preprocess(unboundLigandPdb);

        System.out.println("Start docking");
        timer.start();
        runProcess(
////                "mpirun",
////                "-np",
////                "" + Runtime.getRuntime().availableProcessors(),
                "./zdock",
                "-N", "4000",
                "-o", complex + ".out",
                "-R", unboundReceptorFile + "_m.pdb",
                "-L", unboundLigandFile + "_m.pdb");
        timer.stop();

        System.out.println("Start writing results");
        runProcess("./create.pl", complex + ".out", "50000");

        List<Supplier<Protein>> results = Lists.newArrayList();
        File currentDir = new File(".");
        File[] files = currentDir.listFiles((dir, name) -> name.startsWith("complex."));
        for (File file : files) {
            results.add(() -> PdbUtil.readSimplifiedPdbFile(file));
        }
        return results;
    }

    @Override
    public void cleanup(String complex) {
        deleteFile(new File(complex + "_r_u_m.pdb"));
        deleteFile(new File(complex + "_l_u_m.pdb"));
        File currentDir = new File(".");
        File[] files = currentDir.listFiles((dir, name) -> name.startsWith("complex."));
        for (File file : files) {
            deleteFile(file);
        }
    }

    @Override
    public String getName() {
        return "zdock";
    }

    private void deleteFile(File file) {
        if (!file.delete()) {
            runProcess("rm", file.getAbsolutePath());
        }
    }

    private void preprocess(File pdb) {
        try {
            runProcess("./mark_sur", pdb.getCanonicalPath(), pdb.getName().replace(".pdb", "_m.pdb"));
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
}
