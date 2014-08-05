package ru.ifmo.docking.util;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

public class Logger {
    public static enum Level {
        NONE, NORMAL, VERBOSE
    }

    public static Level level = Level.NORMAL;

    public static void log(String message, Object... args) {
        if (level != Level.NONE) {
            System.out.print(String.format("[%s] ", LocalTime.now().format(DateTimeFormatter.ofLocalizedTime(FormatStyle.MEDIUM))));
            if (args.length == 0) {
                System.out.println(message);
            } else {
                System.out.println(String.format(message, args));
            }
        }
    }

    public static void debug(String message, Object... args) {
        if (level == Level.VERBOSE) {
            if (args.length == 0) {
                System.out.println(message);
            } else {
                System.out.println(String.format(message, args));
            }
        }
    }

}
