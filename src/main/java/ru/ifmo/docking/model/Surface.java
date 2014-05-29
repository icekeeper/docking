package ru.ifmo.docking.model;

import ru.ifmo.docking.calculations.ElectricityCalculator;
import ru.ifmo.docking.calculations.LipophilicityCalculator;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.geometry.Vector;
import ru.ifmo.docking.util.CollectionUtils;
import ru.ifmo.docking.util.IOUtils;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

public class Surface {
    public final String name;
    public final List<Point> points;
    public final List<Vector> normals;
    public final List<Face> faces;
    public final double[] lipophilicity;
    public final double[] electricity;

    public Surface(String name,
                   List<Point> points,
                   List<Vector> normals,
                   List<Face> faces,
                   double[] lipophilicity,
                   double[] electricity) {
        this.name = name;
        this.points = Collections.unmodifiableList(points);
        this.normals = Collections.unmodifiableList(normals);
        this.faces = Collections.unmodifiableList(faces);
        this.lipophilicity = lipophilicity;
        this.electricity = electricity;
    }

    public static Surface read(String name, File surfaceFile, File pdbFile, File pqrFile, File fiPotentials) {
        List<Point> points = new ArrayList<>();
        List<Vector> normals = new ArrayList<>();
        List<Face> faces = new ArrayList<>();

        IOUtils.linesStream(surfaceFile)
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


        LipophilicityCalculator calculator = LipophilicityCalculator.fromPdb(pdbFile, fiPotentials);
        List<Double> lipophilicity = points.parallelStream()
                .map(calculator::calculate)
                .collect(Collectors.toList());

        ElectricityCalculator electricityCalculator = ElectricityCalculator.fromPqr(pqrFile);
        List<Double> electricity = points.parallelStream()
                .map(electricityCalculator::calculate)
                .collect(Collectors.toList());

        return new Surface(name, points, normals, faces, CollectionUtils.listToDoubleArray(lipophilicity), CollectionUtils.listToDoubleArray(electricity));
    }

    public static Surface read(String name, File surfaceFile) {
        List<Point> points = new ArrayList<>();
        List<Vector> normals = new ArrayList<>();
        List<Face> faces = new ArrayList<>();

        IOUtils.linesStream(surfaceFile)
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

        return new Surface(name, points, normals, faces, null, null);
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

    public double getAverageEdgeLength() {
        double sum = 0.0;
        for (Face face : faces) {
            sum += Geometry.distance(points.get(face.p1 - 1), points.get(face.p2 - 1));
            sum += Geometry.distance(points.get(face.p2 - 1), points.get(face.p3 - 1));
            sum += Geometry.distance(points.get(face.p3 - 1), points.get(face.p1 - 1));
        }
        return sum / (faces.size() * 3);
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
