// Created: 28.04.2020
package de.freese.jsync.filesystem.sender;

import de.freese.jsync.filesystem.FileSystem;
import de.freese.jsync.filesystem.fileHandle.FileHandle;

/**
 * Datenquelle.
 *
 * @author Thomas Freese
 */
public interface Sender extends FileSystem
{
    // /**
    // * Liest nur einen bestimmten Bereich aus einer Datei.
    // *
    // * @param baseDir String
    // * @param relativeFile String
    // * @param position long
    // * @param sizeOfChunk long
    // * @param byteBuffer {@link ByteBuffer}
    // */
    // public void readChunk(String baseDir, String relativeFile, long position, long sizeOfChunk, ByteBuffer byteBuffer);

    /**
     * Liefert den {@link FileHandle} zur Datei.
     *
     * @param baseDir String
     * @param relativeFile String
     * @param sizeOfFile long
     * @return {@link FileHandle}
     */
    FileHandle readFileHandle(String baseDir, final String relativeFile, long sizeOfFile);
}
