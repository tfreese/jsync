// Created: 28.04.2020
package de.freese.jsync.filesystem.sender;

import java.nio.ByteBuffer;
import de.freese.jsync.filesystem.FileSystem;

/**
 * Datenquelle.
 *
 * @author Thomas Freese
 */
public interface Sender extends FileSystem
{
    /**
     * Liest nur einen bestimmten Bereich aus einer Datei.
     *
     * @param baseDir String
     * @param relativeFile String
     * @param position long
     * @param sizeOfChunk long
     * @param buffer {@link ByteBuffer}
     */
    public void readChunk(String baseDir, String relativeFile, long position, long sizeOfChunk, ByteBuffer buffer);
}
