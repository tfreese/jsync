// Created: 06.09.2020
package de.freese.jsync.nio.filesystem;

import java.nio.ByteBuffer;

/**
 * @author Thomas Freese
 */
@FunctionalInterface
public interface ChannelReader
{
    /**
     * @param buffer {@link ByteBuffer}
     * @return int; Bytes read
     * @throws Exception Falls was schief geht.
     */
    public int read(ByteBuffer buffer) throws Exception;
}
