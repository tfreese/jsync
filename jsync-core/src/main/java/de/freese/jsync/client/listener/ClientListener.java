/**
 * Created: 23.11.2018
 */

package de.freese.jsync.client.listener;

import java.util.EventListener;
import de.freese.jsync.Options;
import de.freese.jsync.client.Client;
import de.freese.jsync.model.DirectorySyncItem;
import de.freese.jsync.model.FileSyncItem;
import de.freese.jsync.model.SyncPair;

/**
 * Listener für den {@link Client}.
 *
 * @author Thomas Freese
 */
public interface ClientListener extends EventListener
{
    /**
     * Wird nur aufgerufen, wenn DRY-RUN = false ist.
     *
     * @param syncItem {@link FileSyncItem}
     * @param size long
     * @param bytesTransferred long
     */
    public void copyFileProgress(final FileSyncItem syncItem, long size, long bytesTransferred);

    /**
     * @param options {@link Options}
     * @param directory String
     */
    public void createDirectory(Options options, final String directory);

    /**
     * @param syncPair {@link SyncPair}
     */
    public void debugSyncPair(SyncPair syncPair);

    /**
     * @param options {@link Options}
     * @param directory String
     */
    public void deleteDirectory(Options options, final String directory);

    /**
     * @param options {@link Options}
     * @param file String
     */
    public void deleteFile(Options options, final String file);

    /**
     * @param options {@link Options}
     */
    public void dryRunInfo(Options options);

    /**
     * @param message String
     * @param th {@link Throwable}
     */
    public void error(String message, Throwable th);

    /**
     *
     */
    public void generatingFileListInfo();

    /**
     *
     */
    public void syncFinishedInfo();

    /**
    *
    */
    public void syncStartInfo();

    /**
     * @param options {@link Options}
     * @param syncItem {@link DirectorySyncItem}
     */
    public void updateDirectory(Options options, final DirectorySyncItem syncItem);

    /**
     * @param options {@link Options}
     * @param syncItem {@link FileSyncItem}
     */
    public void updateFile(Options options, final FileSyncItem syncItem);

    /**
     * @param options {@link Options}
     * @param syncItem {@link FileSyncItem}
     */
    public void validateFile(Options options, final FileSyncItem syncItem);
}
