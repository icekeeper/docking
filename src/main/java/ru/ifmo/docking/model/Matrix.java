package ru.ifmo.docking.model;

import java.util.Arrays;

public class Matrix {
    private final double[][] data;

    public int rowsCount() {
        return data.length;
    }

    public int columnsCount() {
        return data[0].length;
    }

    public Matrix(double[][] data) {
        this.data = data;
    }

    public double get(int i, int j) {
        return data[i][j];
    }

    public Matrix mul(Matrix that) {
        double[][] result = new double[this.rowsCount()][that.columnsCount()];
        for (int i = 0; i < this.rowsCount(); i++) {
            for (int j = 0; j < that.columnsCount(); j++) {
                for (int k = 0; k < this.columnsCount(); k++) {
                    result[i][j] += this.get(i, k) * that.get(k, j);
                }
            }
        }
        return new Matrix(result);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder();
        for (double[] row : data) {
            sb.append(Arrays.toString(row)).append('\n');
        }
        return sb.toString();
    }
}
