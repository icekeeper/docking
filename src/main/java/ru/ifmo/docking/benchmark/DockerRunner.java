package ru.ifmo.docking.benchmark;

import ru.ifmo.docking.model.Protein;
import ru.ifmo.docking.util.Timer;

import java.io.File;
import java.util.List;
import java.util.function.Supplier;

public interface DockerRunner {

    List<Supplier<Protein>> run(String complex, File benchmarkDir, Timer timer);

    void cleanup(String complex);

    String getName();
}
