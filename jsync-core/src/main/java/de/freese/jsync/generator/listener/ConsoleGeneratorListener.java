/**
 * Created: 23.11.2018
 */

package de.freese.jsync.generator.listener;

import java.io.PrintStream;
import java.nio.file.Path;
import java.util.Objects;
import de.freese.jsync.client.listener.ClientListener;

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
     * @see de.freese.jsync.generator.listener.GeneratorListener#currentMeta(java.lang.String)
     */
    @Override
    public void currentMeta(final String relativePath)
    {
        // String message = currentMetaMessage(relativePath, this.prefix);
        //
        // getPrintStream().println(message);
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
     * @see de.freese.jsync.generator.listener.GeneratorListener#itemCount(java.nio.file.Path, int)
     */
    @Override
    public void itemCount(final Path path, final int itemCount)
    {
        String message = itemCountMessage(path, itemCount, this.prefix);

        getPrintStream().println(message);
    }
}
