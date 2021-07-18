// Created: 05.04.2018
package de.freese.jsync.filesystem.local;

import java.util.function.LongConsumer;

import de.freese.jsync.filesystem.AbstractFileSystem;
import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.model.SyncItem;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

/**
 * Basis-Implementierung des {@link FileSystem}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractLocalFileSystem extends AbstractFileSystem
{
    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, boolean, java.util.function.LongConsumer)
     */
    @Override
    public Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final boolean withChecksum,
                                            final LongConsumer consumerBytesRead)
    {
        getLogger().debug("generate SyncItems: {}, followSymLinks={}, withChecksum={}", baseDir, followSymLinks, withChecksum);

        Flux<SyncItem> syncItems = getGenerator().generateItems(baseDir, followSymLinks);

        if (withChecksum)
        {
            syncItems = syncItems.doOnNext(syncItem -> {

                if (syncItem.isDirectory())
                {
                    return;
                }

                getLogger().debug("generate checksum: {}/{}", baseDir, syncItem.getRelativePath());

                Mono<String> checksum = getGenerator().generateChecksum(baseDir, syncItem.getRelativePath(), consumerBytesRead);

                syncItem.setChecksum(checksum.block());
            });
        }

        return syncItems;
    }
}
