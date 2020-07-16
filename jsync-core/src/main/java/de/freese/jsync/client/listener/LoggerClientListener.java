/**
 * Created: 23.11.2018
 */

package de.freese.jsync.client.listener;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.Options;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;

/**
 * {@link Logger}-Implementierung des {@link ClientListener}.
 *
 * @author Thomas Freese
 */
public class LoggerClientListener extends AbstractClientListener
{
    /**
    *
    */
    private final Logger logger = LoggerFactory.getLogger("Client");

    /**
     * Erstellt ein neues {@link LoggerClientListener} Object.
     */
    public LoggerClientListener()
    {
        super();
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#copyFileProgress(de.freese.jsync.model.SyncItem, long, long)
     */
    @Override
    public void copyFileProgress(final SyncItem syncItem, final long size, final long bytesTransferred)
    {
        String message = copyFileProgressMessage(syncItem, size, bytesTransferred);

        getLogger().info(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#createDirectory(de.freese.jsync.Options, java.lang.String)
     */
    @Override
    public void createDirectory(final Options options, final String directory)
    {
        String message = createDirectoryMessage(options, directory);

        getLogger().info(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#debugSyncPair(de.freese.jsync.model.SyncPair)
     */
    @Override
    public void debugSyncPair(final SyncPair syncPair)
    {
        if (getLogger().isDebugEnabled())
        {
            getLogger().debug(syncPair.toString());
        }
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#deleteDirectory(de.freese.jsync.Options, java.lang.String)
     */
    @Override
    public void deleteDirectory(final Options options, final String directory)
    {
        String message = deleteDirectoryMessage(options, directory);

        getLogger().info(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#deleteFile(de.freese.jsync.Options, java.lang.String)
     */
    @Override
    public void deleteFile(final Options options, final String file)
    {
        String message = deleteFileMessage(options, file);

        getLogger().info(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#dryRunInfo(de.freese.jsync.Options)
     */
    @Override
    public void dryRunInfo(final Options options)
    {
        List<String> messagesList = dryRunInfoMessage(options);

        messagesList.forEach(getLogger()::info);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#error(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void error(final String message, final Throwable th)
    {
        getLogger().error(message, th);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#generatingFileListInfo()
     */
    @Override
    public void generatingFileListInfo()
    {
        String message = generatingFileListInfoMessage();

        getLogger().info(message);
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#syncFinishedInfo()
     */
    @Override
    public void syncFinishedInfo()
    {
        String message = syncFinishedInfoMessage();

        getLogger().info(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#syncStartInfo()
     */
    @Override
    public void syncStartInfo()
    {
        String message = syncStartInfoMessage();

        getLogger().info(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#updateDirectory(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void updateDirectory(final Options options, final SyncItem syncItem)
    {
        String message = updateDirectoryMessage(options, syncItem);

        getLogger().info(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#updateFile(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void updateFile(final Options options, final SyncItem syncItem)
    {
        String message = updateFileMessage(options, syncItem);

        getLogger().info(message);
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#validateFile(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void validateFile(final Options options, final SyncItem syncItem)
    {
        String message = validateFileMessage(options, syncItem);

        getLogger().info(message);
    }
}
