package ru.ifmo.docking.client;

import org.apache.commons.cli.CommandLine;

public class RunConfig {

    private final CommandLine cmd;

    public RunConfig(CommandLine cmd) {
        this.cmd = cmd;
    }


    public String getReceptorPdbFile() {
        return cmd.getOptionValue("r");
    }

    public String getReceptorObjFile() {
        return cmd.getOptionValue("rs");
    }

    public String getLigandPdbFile() {
        return cmd.getOptionValue("l");
    }

    public String getLigandObjFile() {
        return cmd.getOptionValue("ls");
    }

}