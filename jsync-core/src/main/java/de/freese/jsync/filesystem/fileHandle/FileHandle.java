// Created: 24.10.2020
package de.freese.jsync.filesystem.fileHandle;

import java.nio.channels.WritableByteChannel;

/**
 * Enthält die Daten einer Datei.<br>
 *
 * @author Thomas Freese
 */
public interface FileHandle extends AutoCloseable
{
    /**
     * Liefert das BackEnd hinter diesem Interface.
     *
     * @return Object
     */
    public <T> T getHandle();

    /**
     * @param writableByteChannel {@link WritableByteChannel}
     * @param sizeOfFile long
     * @throws Exception Falls was schief geht.
     */
    public void writeTo(final WritableByteChannel writableByteChannel, final long sizeOfFile) throws Exception;
}
