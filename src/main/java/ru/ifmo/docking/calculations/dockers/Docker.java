package ru.ifmo.docking.calculations.dockers;

import org.apache.commons.math3.linear.RealMatrix;

import java.util.List;

public interface Docker {
    List<RealMatrix> run();

    List<RealMatrix> rescore(List<RealMatrix> solutions);
}
