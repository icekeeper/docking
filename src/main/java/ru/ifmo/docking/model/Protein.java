package ru.ifmo.docking.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Protein {
    private final List<Atom> atoms;

    public Protein(List<Atom> atoms) {
        this.atoms = Collections.unmodifiableList(atoms);
    }

    public List<Atom> getAtoms() {
        return atoms;
    }

    public Map<Integer, List<Atom>> getResiduesByNum() {
        Map<Integer, List<Atom>> result = Maps.newHashMap();
        for (Atom atom : atoms) {
            if (!result.containsKey(atom.resSeq)) {
                result.put(atom.resSeq, Lists.newArrayList());
            }
            result.get(atom.resSeq).add(atom);
        }
        return result;
    }
}
