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
    /**
     * Erstellt ein Verzeichnis.
     *
     * @param baseDir String
     * @param relativePath String
     */
    void createDirectory(String baseDir, String relativePath);

    /**
     * Löscht ein Verzeichnis/Datei.
     *
     * @param baseDir String
     * @param relativePath String
     * @param followSymLinks boolean
     */
    void delete(String baseDir, String relativePath, boolean followSymLinks);

    /**
     * Aktualisiert ein {@link SyncItem}.
     *
     * @param baseDir String
     * @param syncItem {@link SyncItem}
     */
    void update(String baseDir, SyncItem syncItem);

    /**
     * Überprüfung der Datei auf Größe und Prüfsumme.
     *
     * @param baseDir String
     * @param syncItem {@link SyncItem}
     * @param withChecksum boolean
     * @param checksumBytesReadConsumer {@link LongConsumer}
     */
    void validateFile(String baseDir, final SyncItem syncItem, boolean withChecksum, final LongConsumer checksumBytesReadConsumer);

    /**
     * Schreibt den {@link Flux} in die Datei.<br>
     * Geliefert wird ein Flux mit den geschriebenen Bytes pro ByteBuffer/Chunk.
     *
     * @param baseDir String
     * @param relativeFile String
     * @param sizeOfFile long
     * @param fileFlux {@link Flux}
     *
     * @return {@link Flux}
     */
    Flux<Long> writeFile(String baseDir, final String relativeFile, long sizeOfFile, Flux<ByteBuffer> fileFlux);
}
