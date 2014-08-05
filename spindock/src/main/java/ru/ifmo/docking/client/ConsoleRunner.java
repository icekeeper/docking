package ru.ifmo.docking.client;

import org.apache.commons.cli.*;

public class ConsoleRunner {
    public static void main(String[] args) {
        CommandLineParser commandLineParser = new DefaultParser();
        Options options = makeOptions();

        CommandLine cmd;
        try {
            cmd = commandLineParser.parse(options, args);
        } catch (MissingOptionException e) {
            System.err.print("Required options missing:");
            for (Object optionObject : e.getMissingOptions()) {
                if (optionObject instanceof String) {
                    System.err.print(" -" + optionObject);
                } else if (optionObject instanceof OptionGroup) {
                    OptionGroup optionGroup = (OptionGroup) optionObject;
                    System.err.print(" -" + optionGroup.toString());
                }
            }
            System.err.println();
            printUsage(options);
            return;
        } catch (ParseException e) {
            printUsage(options);
            return;
        }

        if (cmd.hasOption("h")) {
            printUsage(options);
            return;
        }

        RunConfig runConfig = new RunConfig(cmd);
        DockingRunner.run(runConfig);
    }

    private static Options makeOptions() {
        Options options = new Options();
        options.addOption(
                Option.builder("h")
                        .longOpt("help")
                        .desc("Prints this help message")
                        .build()
        );

        options.addOption(
                Option.builder("r")
                        .hasArg()
                        .argName("file")
                        .longOpt("receptor")
                        .required()
                        .desc("Receptor pdb file")
                        .build()
        );

        options.addOption(
                Option.builder("rs")
                        .longOpt("receptor-surface")
                        .hasArg()
                        .argName("file")
                        .required()
                        .desc("Receptor surface obj file")
                        .build()
        );

        options.addOption(
                Option.builder("l")
                        .longOpt("ligand")
                        .hasArg()
                        .argName("file")
                        .required()
                        .desc("Ligand pdb file")
                        .build()
        );

        options.addOption(
                Option.builder("ls")
                        .longOpt("ligand-surface")
                        .hasArg()
                        .argName("file")
                        .required()
                        .desc("Ligand surface obj file")
                        .build()
        );

        return options;
    }

    private static void printUsage(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("java -jar spin_dock.jar", options);
    }
}
