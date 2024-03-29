// Created: 28.04.2020
package de.freese.jsync.filesystem;

import java.nio.ByteBuffer;

import reactor.core.publisher.Flux;

/**
 * Datenquelle.
 *
 * @author Thomas Freese
 */
public interface Sender extends FileSystem {
    Flux<ByteBuffer> readFile(String baseDir, String relativeFile, long sizeOfFile);
}
