package ru.ifmo.docking.model;

import ru.ifmo.docking.calculations.LipophilicityCalculator;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.geometry.Vector;
import ru.ifmo.docking.util.IOUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Surface {
    public final List<Point> points;
    public final List<Vector> normals;
    public final List<Face> faces;
    public final List<Double> lipophilicity;
    public final List<Double> electricity;

    public Surface(List<Point> points,
                   List<Vector> normals,
                   List<Face> faces,
                   List<Double> lipophilicity,
                   List<Double> electricity) {
        this.points = Collections.unmodifiableList(points);
        this.normals = Collections.unmodifiableList(normals);
        this.faces = Collections.unmodifiableList(faces);
        this.lipophilicity = Collections.unmodifiableList(lipophilicity);
        this.electricity = Collections.unmodifiableList(electricity);
    }

    public static Surface read(File pdbFile, File surface, File electricityFile, File fiPotentials) {
        List<Point> points = new ArrayList<>();
        List<Vector> normals = new ArrayList<>();
        List<Face> faces = new ArrayList<>();

        IOUtils.readLines(surface)
                .map(line -> line.split("\\s+"))
                .forEach(tokens -> {
                    switch (tokens[0]) {
                        case "v":
                            points.add(new Point(Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2]), Double.parseDouble(tokens[3])));
                            break;
                        case "vn":
                            normals.add(new Vector(Double.parseDouble(tokens[1]), Double.parseDouble(tokens[2]), Double.parseDouble(tokens[3])).unite());
                            break;
                        case "f":
                            faces.add(new Face(Integer.parseInt(tokens[1]), Integer.parseInt(tokens[2]), Integer.parseInt(tokens[3])));
                            break;
                    }
                });


        LipophilicityCalculator calculator = LipophilicityCalculator.construct(pdbFile, fiPotentials);
        List<Double> lipophilicity = points.stream()
                .map(calculator::compute)
                .collect(Collectors.toList());

        List<Double> electricity = IOUtils.readLines(electricityFile)
                .map(line -> Double.valueOf(line.substring(line.lastIndexOf(',') + 1)))
                .collect(Collectors.toList());

        return new Surface(points, normals, faces, lipophilicity, electricity);
    }

    public double getDiameter() {
        double result = 0.0;
        for (Point p1 : points) {
            for (Point p2 : points) {
                result = Math.max(result, Geometry.distance(p1, p2));
            }
        }
        return result;
    }


    public static class Face {
        public final int p1;
        public final int p2;
        public final int p3;

        public Face(int p1, int p2, int p3) {
            this.p1 = p1;
            this.p2 = p2;
            this.p3 = p3;
        }
    }
}