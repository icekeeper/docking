package ru.ifmo.docking.calculations;

import ru.ifmo.docking.model.Protein;
import ru.ifmo.docking.util.PdbUtil;

import java.io.File;
import java.io.FileNotFoundException;

public class PdbFixer {

    public static void main(String[] args) throws FileNotFoundException {
        File firstPdb = new File(args[0]);
        File secondPdb = new File(args[1]);
//        RealMatrix transition = RmsdTestMain.getProteinsCenteringTransition(firstPdb, secondPdb);
//        PdbUtil.renumerate(firstPdb, new File("fixed_" + args[0]), transition);
//        PdbUtil.renumerate(secondPdb, new File("fixed_" + args[1]), transition);

        Protein firstProtein = PdbUtil.readPdbFile(firstPdb);
        Protein secondProtein = PdbUtil.readPdbFile(secondPdb);

        PdbUtil.writePdb(new File("test1.pdb"), firstProtein);
        PdbUtil.writePdb(new File("test2.pdb"), secondProtein);

//        try (PrintWriter pw = new PrintWriter(args[2])) {
//            pw.println(atoms1.size() + atoms2.size());
//            atoms1.forEach(atom -> pw.println(atom.s.charAt(77) + " " + atom.p.x + " " + atom.p.y + " " + atom.p.z));
//            atoms2.forEach(atom -> pw.println(atom.s.charAt(77) + " " + atom.p.x + " " + atom.p.y + " " + atom.p.z));
//        }
    }

}
