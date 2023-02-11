// Created: 28.04.2020
package de.freese.jsync.filesystem;

import java.nio.ByteBuffer;
import java.util.function.LongConsumer;

import reactor.core.publisher.Flux;

import de.freese.jsync.model.SyncItem;

/**
 * Datensenke.
 *
 * @author Thomas Freese
 */
public interface Receiver extends FileSystem {
    void createDirectory(String baseDir, String relativePath);

    void delete(String baseDir, String relativePath, boolean followSymLinks);

    void update(String baseDir, SyncItem syncItem);

    void validateFile(String baseDir, SyncItem syncItem, boolean withChecksum, LongConsumer consumerChecksumBytesRead);

    /**
     * Writes the {@link Flux} into the File.<br>
     * Returns a {@link Flux} with the written Bytes for each ByteBuffer/Chunk.
     */
    Flux<Long> writeFile(String baseDir, String relativeFile, long sizeOfFile, Flux<ByteBuffer> fileFlux);
}
