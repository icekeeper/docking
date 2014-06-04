package ru.ifmo.docking.model;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class Protein {
    private final String name;
    private final List<Atom> atoms;

    public Protein(String name, List<Atom> atoms) {
        this.name = name;
        this.atoms = Collections.unmodifiableList(atoms);
    }

    public List<Atom> getAtoms() {
        return atoms;
    }

    public Map<String, List<Atom>> getResiduesById() {
        Map<String, List<Atom>> result = Maps.newHashMap();
        for (Atom atom : atoms) {
            if (!result.containsKey(atom.getResidueUid())) {
                result.put(atom.getResidueUid(), Lists.newArrayList());
            }
            result.get(atom.getResidueUid()).add(atom);
        }
        return result;
    }

    public String getName() {
        return name;
    }
}
