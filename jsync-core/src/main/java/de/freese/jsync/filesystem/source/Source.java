/**
 * Created on 22.10.2016 10:42:26
 */
package de.freese.jsync.filesystem.source;

import java.nio.channels.ReadableByteChannel;
import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.model.FileSyncItem;

/**
 * Source-Filesystem.
 *
 * @author Thomas Freese
 */
public interface Source extends FileSystem
{
    /**
     * @see de.freese.jsync.filesystem.FileSystem#getChannel(de.freese.jsync.model.FileSyncItem)
     */
    @Override
    public ReadableByteChannel getChannel(final FileSyncItem syncItem) throws Exception;
}
