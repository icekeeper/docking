package ru.ifmo.docking.client;

import org.apache.commons.math3.linear.RealMatrix;
import ru.ifmo.docking.calculations.dockers.Docker;
import ru.ifmo.docking.calculations.dockers.GeometryDocker;
import ru.ifmo.docking.geometry.Geometry;
import ru.ifmo.docking.model.Atom;
import ru.ifmo.docking.model.Protein;
import ru.ifmo.docking.model.Surface;
import ru.ifmo.docking.util.PdbUtil;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

public class DockingRunner {

    public static void run(RunConfig config) {
        Surface receptorSurface = Surface.read("receptor", new File(config.getReceptorObjFile()));
        Surface ligandSurface = Surface.read("ligand", new File(config.getLigandObjFile()));

        Docker docker = new GeometryDocker(receptorSurface, ligandSurface, config);
        List<RealMatrix> results = docker.run();

        Protein ligand = PdbUtil.readPdbFile(new File(config.getLigandPdbFile()));

        for (int i = 0; i < 10; i++) {
            writeLigand(new File("result_" + (i + 1) + ".pdb"), ligand, results.get(i));
        }

    }

    public static void writeLigand(File file, Protein ligand, RealMatrix transition) {
        PdbUtil.writePdb(file, transformProtein(ligand, transition));
    }

    public static Protein transformProtein(Protein protein, RealMatrix transition) {
        List<Atom> transformedAtoms = protein.getAtoms()
                .stream()
                .map(atom -> transformAtom(atom, transition))
                .collect(Collectors.toList());

        return new Protein(protein.getName(), transformedAtoms);
    }

    public static Atom transformAtom(Atom atom, RealMatrix matrix) {
        return new Atom(
                atom.recordName,
                atom.serial,
                atom.name,
                atom.altLock,
                atom.resName,
                atom.chainId,
                atom.resSeq,
                atom.iCode,
                Geometry.transformPoint(atom.p, matrix),
                atom.occupancy,
                atom.tempFactor,
                atom.element,
                atom.pdbCharge,
                atom.charge,
                atom.r
        );
    }


}
