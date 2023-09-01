// Created: 05.04.2018
package de.freese.jsync.filesystem.local;

import java.net.URI;
import java.util.function.LongConsumer;

import reactor.core.publisher.Flux;

import de.freese.jsync.filesystem.AbstractFileSystem;
import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.SyncItem;

/**
 * Basis-Implementierung des {@link FileSystem}.
 *
 * @author Thomas Freese
 */
public abstract class AbstractLocalFileSystem extends AbstractFileSystem {
    @Override
    public void connect(final URI uri) {
        // Empty
    }

    @Override
    public void disconnect() {
        // Empty
    }

    @Override
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead) {
        return getGenerator().generateChecksum(baseDir, relativeFile, consumerChecksumBytesRead);
    }

    @Override
    public Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final PathFilter pathFilter) {
        return getGenerator().generateItems(baseDir, followSymLinks, pathFilter);
    }
}
