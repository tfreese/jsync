// Created: 23.11.2018

package de.freese.jsync.client.listener;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;

import de.freese.jsync.Options;
import de.freese.jsync.model.SyncItem;

/**
 * Console-Implementierung des {@link ClientListener}.
 *
 * @author Thomas Freese
 */
@SuppressWarnings("resource")
public class ConsoleClientListener extends AbstractClientListener
{
    /**
     *
     */
    private final PrintStream printStream;

    /**
     *
     */
    private final PrintStream printStreamError;

    /**
     * Erstellt ein neues {@link ConsoleClientListener} Object.
     */
    public ConsoleClientListener()
    {
        super();

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
     * @see de.freese.jsync.client.listener.ClientListener#copyProgress(de.freese.jsync.Options, de.freese.jsync.model.SyncItem, long)
     */
    @Override
    public void copyProgress(final Options options, final SyncItem syncItem, final long bytesTransferred)
    {
        String message = copyProgressMessage(options, syncItem, bytesTransferred);

        if (message == null)
        {
            return;
        }

        message = "\r\t" + message;

        getPrintStream().print(message);

        if (syncItem.getSize() == bytesTransferred)
        {
            getPrintStream().println();
        }
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#delete(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void delete(final Options options, final SyncItem syncItem)
    {
        String message = deleteMessage(options, syncItem);

        getPrintStream().println(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#error(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void error(final String message, final Throwable th)
    {
        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        th.printStackTrace(printWriter);
        String stackTrace = stringWriter.toString();

        getPrintStreamError().println();
        getPrintStreamError().println("ERROR - " + (message == null ? "" : message));
        getPrintStreamError().println(stackTrace);
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
     * @see de.freese.jsync.client.listener.ClientListener#update(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void update(final Options options, final SyncItem syncItem)
    {
        String message = updateMessage(options, syncItem);

        message = "\t" + message;

        getPrintStream().println(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#validate(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void validate(final Options options, final SyncItem syncItem)
    {
        String message = validateMessage(options, syncItem);

        message = "\t" + message;

        getPrintStream().println(message);
    }
}
