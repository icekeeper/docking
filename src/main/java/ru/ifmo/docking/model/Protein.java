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

    public Map<String, List<Atom>> getResiduesById() {
        Map<String, List<Atom>> result = Maps.newHashMap();
        for (Atom atom : atoms) {
            if (!result.containsKey(atom.resId)) {
                result.put(atom.resId, Lists.newArrayList());
            }
            result.get(atom.resId).add(atom);
        }
        return result;
    }
}
