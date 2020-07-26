/**
 * Created: 26.07.2020
 */

package de.freese.jsync.client.listener;

import de.freese.jsync.Options;
import de.freese.jsync.model.SyncItem;
import de.freese.jsync.model.SyncPair;

/**
 * @author Thomas Freese
 */
public class EmptyClientListener implements ClientListener
{
    /**
     * Erstellt ein neues {@link EmptyClientListener} Object.
     */
    public EmptyClientListener()
    {
        super();
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#copyFileProgress(de.freese.jsync.model.SyncItem, long, long)
     */
    @Override
    public void copyFileProgress(final SyncItem syncItem, final long size, final long bytesTransferred)
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#createDirectory(de.freese.jsync.Options, java.lang.String)
     */
    @Override
    public void createDirectory(final Options options, final String directory)
    {
        // Empty
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
        // Empty
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#deleteFile(de.freese.jsync.Options, java.lang.String)
     */
    @Override
    public void deleteFile(final Options options, final String file)
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#dryRunInfo(de.freese.jsync.Options)
     */
    @Override
    public void dryRunInfo(final Options options)
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#error(java.lang.String, java.lang.Throwable)
     */
    @Override
    public void error(final String message, final Throwable th)
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#generatingFileListInfo()
     */
    @Override
    public void generatingFileListInfo()
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#syncFinishedInfo()
     */
    @Override
    public void syncFinishedInfo()
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#syncStartInfo()
     */
    @Override
    public void syncStartInfo()
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#updateDirectory(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void updateDirectory(final Options options, final SyncItem syncItem)
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#updateFile(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void updateFile(final Options options, final SyncItem syncItem)
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.client.listener.ClientListener#validateFile(de.freese.jsync.Options, de.freese.jsync.model.SyncItem)
     */
    @Override
    public void validateFile(final Options options, final SyncItem syncItem)
    {
        // Empty
    }
}
