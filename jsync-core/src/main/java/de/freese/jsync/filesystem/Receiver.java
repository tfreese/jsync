// Created: 28.04.2020
package de.freese.jsync.filesystem;

import java.nio.ByteBuffer;
import java.util.function.LongConsumer;

import de.freese.jsync.model.SyncItem;
import reactor.core.publisher.Flux;

/**
 * Datensenke.
 *
 * @author Thomas Freese
 */
public interface Receiver extends FileSystem
{
    void createDirectory(String baseDir, String relativePath);

    void delete(String baseDir, String relativePath, boolean followSymLinks);

    void update(String baseDir, SyncItem syncItem);

    void validateFile(String baseDir, final SyncItem syncItem, boolean withChecksum, final LongConsumer consumerChecksumBytesRead);

    /**
     * Writes the {@link Flux} into the File.<br>
     * Returns a {@link Flux} with the written Bytes for each ByteBuffer/Chunk.
     */
    Flux<Long> writeFile(String baseDir, final String relativeFile, long sizeOfFile, Flux<ByteBuffer> fileFlux);
}
