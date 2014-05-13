package ru.ifmo.docking.util;

import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.model.Surface;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Locale;

public class PlyWriter {

    public static void writeAsPlyFile(Surface surface, List<int[]> colors, File file, Collection<String> comments) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            writeHeader(writer, surface, comments);
            writeVertices(writer, surface, colors);
            writeFaces(writer, surface);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static void writeHeader(BufferedWriter writer, Surface surface, Collection<String> comments) throws IOException {
        writer.write("ply\n");
        writer.write("format ascii 1.0\n");
        for (String comment : comments) {
            writer.write("comment " + comment + "\n");
        }
        writer.write("element vertex " + surface.points.size() + "\n");
        writer.write("property float x\n");
        writer.write("property float y\n");
        writer.write("property float z\n");
        writer.write("property uchar red\n");
        writer.write("property uchar green\n");
        writer.write("property uchar blue\n");
        writer.write("element face " + surface.faces.size() + "\n");
        writer.write("property list uchar int vertex_index\n");
        writer.write("end_header\n");
    }

    private static void writeVertices(BufferedWriter writer, Surface surface, List<int[]> colors) throws IOException {
        for (int i = 0; i < surface.points.size(); i++) {
            int[] color = colors.get(i);
            Point point = surface.points.get(i);
            writer.write(String.format(Locale.ENGLISH, "%f %f %f %d %d %d\n", point.x, point.y, point.z, color[0], color[1], color[2]));
        }
    }

    private static void writeFaces(BufferedWriter writer, Surface surface) throws IOException {
        for (Surface.Face face : surface.faces) {
            writer.write(String.format("3 %d %d %d\n", face.p1 - 1, face.p2 - 1, face.p3 - 1));
        }
    }

}
