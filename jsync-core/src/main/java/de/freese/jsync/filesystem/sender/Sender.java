/**
 * Created on 22.10.2016 10:42:26
 */
package de.freese.jsync.filesystem.sender;

import java.nio.channels.ReadableByteChannel;
import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.model.FileSyncItem;

/**
 * Datenquelle.
 *
 * @author Thomas Freese
 */
public interface Sender extends FileSystem
{
    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChannel(de.freese.jsync.model.FileSyncItem)
     */
    @Override
    public ReadableByteChannel getChannel(final FileSyncItem syncItem) throws Exception;
}