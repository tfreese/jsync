// Created: 28.04.2020
package de.freese.jsync.filesystem;

import java.nio.ByteBuffer;

import reactor.core.publisher.Flux;

/**
 * Datenquelle.
 *
 * @author Thomas Freese
 */
public interface Sender extends FileSystem
{
    /**
     * Liefert den {@link Flux} zur Datei.
     *
     * @param baseDir String
     * @param relativeFile String
     * @param sizeOfFile long
     *
     * @return {@link Flux}
     */
    Flux<ByteBuffer> readFile(String baseDir, final String relativeFile, long sizeOfFile);
}
