package ru.ifmo.docking.util;

import java.util.List;

public class CollectionUtils {

    public static double[] listToDoubleArray(List<Double> list) {
        double[] result = new double[list.size()];
        for (int i = 0; i < list.size(); i++) {
            result[i] = list.get(i);
        }
        return result;
    }
}
