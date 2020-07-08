/**
 * Created: 14.03.2020
 */

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
    public boolean checksum();

    /**
     * Option: --delete
     *
     * @return boolean
     */
    public boolean delete();

    /**
     * Option: -n; --dry-run
     *
     * @return boolean
     */
    public boolean dryRun();

    /**
     * Option: -f; --follow-symlinks
     *
     * @return boolean
     */
    public boolean followSymlinks();

    /**
     * Sind Argumente vorhanden ?
     *
     * @return boolean
     */
    public boolean hasArgs();

    /**
     * @param printStream {@link PrintStream}
     */
    public void printHelp(PrintStream printStream);

    /**
     * Option: -sk; --sink
     *
     * @return String
     */
    public String sink();

    /**
     * Option: -s; --source
     *
     * @return String
     */
    public String source();
}