package ru.ifmo.docking.util;

import ru.ifmo.docking.model.Atom;
import ru.ifmo.docking.model.Protein;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;
import java.util.stream.Collectors;

public class PdbUtil {

    public static Protein readPqrFile(File pdbFile) {
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
                    double charge = Double.parseDouble(line.substring(55, 62).trim());
                    double r = Double.parseDouble(line.substring(62, 69).trim());

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
                            0.0,
                            0.0,
                            "  ",
                            "  ",
                            charge,
                            r);
                })
                .collect(Collectors.toList()));
    }

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

                    String element = line.length() >= 78 ? line.substring(76, 78) : "  ";
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
                            charge,
                            0.0,
                            0.0);
                })
                .collect(Collectors.toList()));
    }

    public static Protein readSimplifiedPdbFile(File pdbFile) {
        return new Protein(IOUtils.linesStream(pdbFile)
                .filter(line -> line.startsWith("ATOM  ") || line.startsWith("HETATM"))
                .map(line -> line.replaceAll("\\*", ""))
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
                            0.0,
                            0.0,
                            "  ",
                            "  ",
                            0.0,
                            0.0);
                })
                .collect(Collectors.toList()));
    }

    public static void renumerate(File file, int startNum) {
        Protein protein = readPdbFile(file);
        int atomNum = startNum;
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            for (Atom atom : protein.getAtoms()) {
                writer.write(String.format("%-6s", atom.recordName));
                writer.write(String.format("%5d ", atomNum++));
                if (atom.name.length() > 3) {
                    writer.write(String.format("%-4s", atom.name));
                } else {
                    writer.write(String.format(" %-3s", atom.name));
                }
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
                writer.write(String.format("%2s", atom.pdbCharge));
                writer.write('\n');
            }
        } catch (IOException ignored) {
        }
    }

    public static void writePdb(File output, Protein protein) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(output))) {
            for (Atom atom : protein.getAtoms()) {
                writer.write(String.format("%-6s", atom.recordName));
                writer.write(String.format("%5d ", atom.serial));
                if (atom.name.length() > 3) {
                    writer.write(String.format("%-4s", atom.name));
                } else {
                    writer.write(String.format(" %-3s", atom.name));
                }
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
                writer.write(String.format("%2s", atom.pdbCharge));
                writer.write('\n');
            }
        } catch (IOException ignored) {
        }
    }

}
