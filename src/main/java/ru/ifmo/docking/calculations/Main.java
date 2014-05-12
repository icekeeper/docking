package ru.ifmo.docking.calculations;

import ru.ifmo.docking.model.Surface;

import java.io.File;

public class Main {

    public static void main(String[] args) {
        String firstFile = args[0];
        String secondFile = args[1];

        File fiPotentials = new File("data", "fi_potentials.txt");

        String firstDir = "data/" + firstFile + "_data";
        Surface firstSurface = Surface.read(
                new File(firstDir, firstFile + ".pdb"),
                new File(firstDir, firstFile + ".obj"),
                new File(firstDir, firstFile + "_pot.csv"),
                fiPotentials
        );

        String secondDir = "data/" + secondFile + "_data";
        Surface secondSurface = Surface.read(
                new File(secondDir, secondFile + ".pdb"),
                new File(secondDir, secondFile + ".obj"),
                new File(secondDir, secondFile + "_pot.csv"),
                fiPotentials
        );

        Docker docker = new Docker(firstSurface, secondSurface);
        docker.run();

    }


}
