package ru.ifmo.docking.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Spliterator;
import java.util.function.Consumer;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class IOUtils {

    public static Stream<String> linesStream(File file) {
        try {
            BufferedReader reader = new BufferedReader(new FileReader(file));
            return StreamSupport.stream(new LinesReaderSpliterator(reader), false);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private static class LinesReaderSpliterator implements Spliterator<String> {
        private final BufferedReader reader;

        public LinesReaderSpliterator(BufferedReader reader) {
            this.reader = reader;
        }

        @Override
        public boolean tryAdvance(Consumer<? super String> action) {
            try {
                String line = reader.readLine();
                if (line != null) {
                    action.accept(line);
                    return true;
                } else {
                    reader.close();
                    return false;
                }
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        @Override
        public Spliterator<String> trySplit() {
            return null;
        }

        @Override
        public long estimateSize() {
            return Long.MAX_VALUE;
        }

        @Override
        public int characteristics() {
            return ORDERED | IMMUTABLE | NONNULL;
        }
    }

    private void runProcess(String... args) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(args);
            System.out.println("Running " + String.join(" ", processBuilder.command()));
            Process process = processBuilder.start();
            while (process.isAlive()) {
                try {
                    process.waitFor();
                } catch (InterruptedException ignored) {
                }
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
