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
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, boolean, java.util.function.Consumer,
     *      java.util.function.LongConsumer)
     */
    @Override
    public void generateSyncItems(final String baseDir, final boolean followSymLinks, final boolean withChecksum, final Consumer<SyncItem> consumerSyncItem,
                                  final LongConsumer consumerBytesRead)
    {
        getLogger().debug("generate SyncItems: {}, followSymLinks={}, withChecksum={}", baseDir, followSymLinks, withChecksum);

        getGenerator().generateItems(baseDir, followSymLinks, syncItem -> {
            if (withChecksum && syncItem.isFile())
            {
                getLogger().debug("generate checksum: {}/{}", baseDir, syncItem.getRelativePath());

                String checksum = getGenerator().generateChecksum(baseDir, syncItem.getRelativePath(), consumerBytesRead);
                syncItem.setChecksum(checksum);
            }

            consumerSyncItem.accept(syncItem);
        });
    }
}
