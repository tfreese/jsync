/**
 * Created: 23.11.2018
 */

package de.freese.jsync.client.listener;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import de.freese.jsync.Options;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;

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
     * @see de.freese.jsync.client.listener.ClientListener#copyFileProgress(de.freese.jsync.model.SyncItem, long, long)
     */
    @Override
    public void copyFileProgress(final SyncItem syncItem, final long size, final long bytesTransferred)
    {
        String message = copyFileProgressMessage(syncItem, size, bytesTransferred);

        message = "\r\t" + message;

        getPrintStream().print(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#createDirectory(de.freese.jsync.Options, java.lang.String)
     */
    @Override
    public void createDirectory(final Options options, final String directory)
    {
        String message = createDirectoryMessage(options, directory);

        getPrintStream().println(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#debugSyncPair(de.freese.jsync.model.SyncPair)
     */
    @Override
    public void debugSyncPair(final SyncPair syncPair)
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#deleteDirectory(de.freese.jsync.Options, java.lang.String)
     */
    @Override
    public void deleteDirectory(final Options options, final String directory)
    {
        String message = deleteDirectoryMessage(options, directory);

        getPrintStream().println(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#deleteFile(de.freese.jsync.Options, java.lang.String)
     */
    @Override
    public void deleteFile(final Options options, final String file)
    {
        String message = deleteFileMessage(options, file);

        getPrintStream().println(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#dryRunInfo(de.freese.jsync.Options)
     */
    @Override
    public void dryRunInfo(final Options options)
    {
        List<String> messagesList = dryRunInfoMessage(options);

        messagesList.forEach(getPrintStream()::println);
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
     * @see de.freese.jsync.client.listener.ClientListener#generatingFileListInfo()
     */
    @Override
    public void generatingFileListInfo()
    {
        String message = generatingFileListInfoMessage();

        getPrintStream().println(message);
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
     * @see de.freese.jsync.client.listener.ClientListener#syncFinishedInfo()
     */
    @Override
    public void syncFinishedInfo()
    {
        String message = syncFinishedInfoMessage();

        getPrintStream().println();
        getPrintStream().println(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#syncStartInfo()
     */
    @Override
    public void syncStartInfo()
    {
        String message = syncStartInfoMessage();

        getPrintStream().println();
        getPrintStream().println(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#updateDirectory(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void updateDirectory(final Options options, final SyncItem syncItem)
    {
        String message = updateDirectoryMessage(options, syncItem);

        getPrintStream().println(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#updateFile(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void updateFile(final Options options, final SyncItem syncItem)
    {
        String message = updateFileMessage(options, syncItem);

        message = "\t" + message;

        getPrintStream().println(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#validateFile(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void validateFile(final Options options, final SyncItem syncItem)
    {
        String message = validateFileMessage(options, syncItem);

        message = "\t" + message;

        getPrintStream().println(message);
    }
}
