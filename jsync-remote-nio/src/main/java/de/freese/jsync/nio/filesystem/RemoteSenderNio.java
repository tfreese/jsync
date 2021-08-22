// Created: 17.08.2021
package de.freese.jsync.nio.filesystem;

import java.nio.ByteBuffer;
import java.util.function.LongConsumer;

import de.freese.jsync.filesystem.Sender;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.SyncItem;
import reactor.core.publisher.Flux;

/**
 * @author Thomas Freese
 */
public class RemoteSenderNio extends AbstractNioFileSystem implements Sender
{
    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead)
    {
        return generateChecksum(baseDir, relativeFile, consumerChecksumBytesRead, JSyncCommand.SOURCE_CHECKSUM);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, de.freese.jsync.filter.PathFilter)
     */
    @Override
    public Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final PathFilter pathFilter)
    {
        return generateSyncItems(baseDir, followSymLinks, pathFilter, JSyncCommand.SOURCE_CREATE_SYNC_ITEMS);
    }

    /**
     * @see de.freese.jsync.filesystem.Sender#readFile(java.lang.String, java.lang.String, long)
     */
    @Override
    public Flux<ByteBuffer> readFile(final String baseDir, final String relativeFile, final long sizeOfFile)
    {
        return Flux.empty();
    }
}
