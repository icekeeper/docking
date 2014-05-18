package ru.ifmo.docking.calculations;

import ru.ifmo.docking.model.Atom;
import ru.ifmo.docking.util.PdbUtil;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.List;

public class PdbFixer {

    public static void main(String[] args) throws FileNotFoundException {
        File firstPdb = new File(args[0]);
        File secondPdb = new File(args[1]);
//        RealMatrix transition = RmsdTestMain.getPdbTransition(firstPdb, secondPdb);
//        PdbUtil.rewritePdb(firstPdb, new File("fixed_" + args[0]), transition);
//        PdbUtil.rewritePdb(secondPdb, new File("fixed_" + args[1]), transition);

        List<Atom> atoms1 = PdbUtil.readPdbFile(firstPdb);
        List<Atom> atoms2 = PdbUtil.readPdbFile(secondPdb);

        try (PrintWriter pw = new PrintWriter(args[2])) {
            pw.println(atoms1.size() + atoms2.size());
            atoms1.forEach(atom -> pw.println(atom.s.charAt(77) + " " + atom.p.x + " " + atom.p.y + " " + atom.p.z));
            atoms2.forEach(atom -> pw.println(atom.s.charAt(77) + " " + atom.p.x + " " + atom.p.y + " " + atom.p.z));
        }
    }

}
