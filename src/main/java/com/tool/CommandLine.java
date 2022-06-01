package com.tool;

import java.io.IOError;
import java.io.IOException;

public class CommandLine {
    static void executeCommand(String... cmd) throws IOException, InterruptedException {
        Process pr = Runtime.getRuntime().exec(cmd);
        pr.waitFor();
        if (pr.exitValue() != 0 ) {
            throw new RuntimeException(String.join("", cmd) + " Exited with non 0 exit code");
        }
    }
}
