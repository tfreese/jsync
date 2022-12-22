// Created: 14.03.2020
package de.freese.jsync.arguments;

import java.io.PrintStream;

/**
 * @author Thomas Freese
 */
public interface ArgumentParser
{
    /**
     * Option: -c; --checksum
     */
    boolean checksum();

    /**
     * Option: --delete
     */
    boolean delete();

    /**
     * Option: -n; --dry-run
     */
    boolean dryRun();

    /**
     * Option: -f; --follow-symlinks
     */
    boolean followSymlinks();

    boolean hasArgs();

    void printHelp(PrintStream printStream);

    /**
     * Option: -r; --receiver
     */
    String receiver();

    /**
     * Option: -s; --sender
     */
    String sender();
}
