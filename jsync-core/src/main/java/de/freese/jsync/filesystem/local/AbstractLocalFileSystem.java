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
    /**
     * @see de.freese.jsync.filesystem.FileSystem#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri) {
        // Empty
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#disconnect()
     */
    @Override
    public void disconnect() {
        // Empty
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateChecksum(java.lang.String, java.lang.String, java.util.function.LongConsumer)
     */
    @Override
    public String generateChecksum(final String baseDir, final String relativeFile, final LongConsumer consumerChecksumBytesRead) {
        return getGenerator().generateChecksum(baseDir, relativeFile, consumerChecksumBytesRead);
    }

    /**
     * @see de.freese.jsync.filesystem.FileSystem#generateSyncItems(java.lang.String, boolean, de.freese.jsync.filter.PathFilter)
     */
    @Override
    public Flux<SyncItem> generateSyncItems(final String baseDir, final boolean followSymLinks, final PathFilter pathFilter) {
        return getGenerator().generateItems(baseDir, followSymLinks, pathFilter);
    }
}
