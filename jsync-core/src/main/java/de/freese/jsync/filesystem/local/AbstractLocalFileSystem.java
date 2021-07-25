// Created: 05.04.2018
package de.freese.jsync.filesystem.local;

import java.util.function.Consumer;
import java.util.function.LongConsumer;

import de.freese.jsync.filesystem.AbstractFileSystem;
import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.model.SyncItem;

/**
 * Basis-Implementierung des {@link FileSystem}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractLocalFileSystem extends AbstractFileSystem
{
    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer checksumBytesReadConsumer)
    {
        getLogger().info("create checksum: {}/{}", baseDir, relativeFile);

        String checksum = getGenerator().generateChecksum(baseDir, relativeFile, checksumBytesReadConsumer);

        return checksum;
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, java.util.function.Consumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final Consumer<SyncItem> consumerSyncItem)
    {
        getLogger().info("generate SyncItems: {}, followSymLinks={}", baseDir, followSymLinks);

        getGenerator().generateItems(baseDir, followSymLinks, consumerSyncItem);
    }
}
