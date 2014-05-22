package ru.ifmo.docking.util;

import org.apache.commons.math3.linear.RealMatrix;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.geometry.Point;
import ru.ifmo.docking.model.Atom;
import ru.ifmo.docking.model.Protein;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.stream.Collectors;

public class PdbUtil {

    public static Protein readPdbFile(File pdbFile) {
        return new Protein(IOUtils.linesStream(pdbFile)
                .filter(line -> line.startsWith("ATOM  ") || line.startsWith("HETATM"))
                .map(line -> {

                    String recordName = line.substring(0, 6).trim();
                    int serial = Integer.parseInt(line.substring(6, 11).trim());
                    String name = line.substring(12, 16).trim();
                    char altLoc = line.charAt(16);
                    String resName = line.substring(17, 20);
                    char chainId = line.charAt(21);
                    int resSeq = Integer.parseInt(line.substring(22, 26).trim());
                    char iCode = line.charAt(26);

                    double atomX = Double.parseDouble(line.substring(30, 38).trim());
                    double atomY = Double.parseDouble(line.substring(38, 46).trim());
                    double atomZ = Double.parseDouble(line.substring(46, 54).trim());
                    double occupancy = Double.parseDouble(line.substring(54, 60).trim());
                    double tempFactor = Double.parseDouble(line.substring(60, 66).trim());

                    String element = line.substring(76, 78);
                    String charge = line.length() >= 80 ? line.substring(78, 80) : "  ";

                    return new Atom(
                            recordName,
                            serial,
                            name,
                            altLoc,
                            resName,
                            chainId,
                            resSeq,
                            iCode,
                            atomX,
                            atomY,
                            atomZ,
                            occupancy,
                            tempFactor,
                            element,
                            charge
                    );
                })
                .collect(Collectors.toList()));
    }

    public static void rewritePdb(File output, Protein protein, RealMatrix matrix) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            for (Atom atom : protein.getAtoms()) {
                Point transformed = Geometry.transformPoint(atom.p, matrix);
                writer.write(String.format("%-6s", atom.recordName));
                writer.write(String.format("%5d  ", atom.serial));
                writer.write(String.format("%-3s", atom.name));
                writer.write(atom.altLock);
                writer.write(atom.resName);
                writer.write(" ");
                writer.write(atom.chainId);
                writer.write(String.format("%4d", atom.resSeq));
                writer.write(atom.iCode);
                writer.write("   ");
                writer.write(String.format(Locale.US, "%8.3f", transformed.x));
                writer.write(String.format(Locale.US, "%8.3f", transformed.y));
                writer.write(String.format(Locale.US, "%8.3f", transformed.z));
                writer.write(String.format(Locale.US, "%6.2f", atom.occupancy));
                writer.write(String.format(Locale.US, "%6.2f", atom.tempFactor));
                writer.write("          ");
                writer.write(String.format("%2s", atom.element));
                writer.write(String.format("%2s", atom.charge));
                writer.write('\n');
            }
        } catch (IOException ignored) {
        }
    }

    public static void writePdb(File output, Protein protein) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            for (Atom atom : protein.getAtoms()) {
                writer.write(String.format("%-6s", atom.recordName));
                writer.write(String.format("%5d  ", atom.serial));
                writer.write(String.format("%-3s", atom.name));
                writer.write(atom.altLock);
                writer.write(atom.resName);
                writer.write(" ");
                writer.write(atom.chainId);
                writer.write(String.format("%4d", atom.resSeq));
                writer.write(atom.iCode);
                writer.write("   ");
                writer.write(String.format(Locale.US, "%8.3f", atom.p.x));
                writer.write(String.format(Locale.US, "%8.3f", atom.p.y));
                writer.write(String.format(Locale.US, "%8.3f", atom.p.z));
                writer.write(String.format(Locale.US, "%6.2f", atom.occupancy));
                writer.write(String.format(Locale.US, "%6.2f", atom.tempFactor));
                writer.write("          ");
                writer.write(String.format("%2s", atom.element));
                writer.write(String.format("%2s", atom.charge));
                writer.write('\n');
            }
        } catch (IOException ignored) {
        }
    }

}
