package ru.ifmo.docking.calculations.dockers;

import org.apache.commons.math3.linear.RealMatrix;
import ru.ifmo.docking.util.Pair;

import java.util.List;

public interface Docker {
    List<Pair<List<GeometryDocker.PointMatch>, RealMatrix>> run();
}
