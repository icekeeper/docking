package ru.ifmo.docking.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IOUtils {
    public static Stream<String> readLines(File file) {
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            return reader.lines().collect(Collectors.toList()).stream();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
