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
     *
     * @return boolean
     */
    boolean checksum();

    /**
     * Option: --delete
     *
     * @return boolean
     */
    boolean delete();

    /**
     * Option: -n; --dry-run
     *
     * @return boolean
     */
    boolean dryRun();

    /**
     * Option: -f; --follow-symlinks
     *
     * @return boolean
     */
    boolean followSymlinks();

    /**
     * Sind Argumente vorhanden ?
     *
     * @return boolean
     */
    boolean hasArgs();

    /**
     * @param printStream {@link PrintStream}
     */
    void printHelp(PrintStream printStream);

    /**
     * Option: -r; --receiver
     *
     * @return String
     */
    String receiver();

    /**
     * Option: -s; --sender
     *
     * @return String
     */
    String sender();
}
