// Created: 28.04.2020
package de.freese.jsync.filesystem;

import java.net.URI;
import java.util.function.LongConsumer;

import reactor.core.publisher.Flux;

import de.freese.jsync.filter.PathFilter;
import de.freese.jsync.model.SyncItem;

/**
 * @author Thomas Freese
 */
public interface FileSystem {
    void connect(URI uri);

    void disconnect();

    String generateChecksum(String baseDir, String relativeFile, LongConsumer consumerChecksumBytesRead);

    Flux<SyncItem> generateSyncItems(String baseDir, boolean followSymLinks, PathFilter pathFilter);
}
