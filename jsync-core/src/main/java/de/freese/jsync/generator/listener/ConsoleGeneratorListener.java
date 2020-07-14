/**
 * Created: 23.11.2018
 */

package de.freese.jsync.generator.listener;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Objects;
import de.freese.jsync.client.listener.ClientListener;
import de.freese.jsync.model.SyncItem;

/**
 * Console-Implementierung des {@link ClientListener}.
 *
 * @author Thomas Freese
 */
@SuppressWarnings("resource")
public class ConsoleGeneratorListener extends AbstractGeneratorListener
{
    /**
     *
     */
    private final String prefix;

    /**
     *
     */
    private final PrintStream printStream;

    /**
     *
     */
    private final PrintStream printStreamError;

    /**
     * Erstellt ein neues {@link ConsoleGeneratorListener} Object.
     *
     * @param prefix String
     */
    public ConsoleGeneratorListener(final String prefix)
    {
        super();

        this.prefix = Objects.requireNonNull(prefix, "prefix required");

        // Console console = System.console();
        //
        // if (console != null)
        // {
        // printStream = console.writer();
        // }
        // else
        // {
        // printStream = System.out;
        // }

        this.printStream = System.out;

        this.printStreamError = System.err;
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#checksum(long, long)
     */
    @Override
    public void checksum(final long size, final long bytesRead)
    {
        // Empty
    }

    /**
     * @return {@link PrintStream}
     */
    protected PrintStream getPrintStream()
    {
        return this.printStream;
    }

    /**
     * @return {@link PrintStream}
     */
    protected PrintStream getPrintStreamError()
    {
        return this.printStreamError;
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#pathCount(java.nio.file.Path, int)
     */
    @Override
    public void pathCount(final Path path, final int pathCount)
    {
        String message = pathCountMessage(path, pathCount, this.prefix);

        getPrintStream().println(message);
    }

    /**
     * @see de.freese.jsync.generator.listener.GeneratorListener#syncItem(de.freese.jsync.model.SyncItem)
     */
    @Override
    public void syncItem(final SyncItem syncItem)
    {
        // Empty
        // String message = processingSyncItemMessage(syncItem, this.prefix);
        //
        // getPrintStream().println(message);
    }
}
