// Created: 06.09.2020
package de.freese.jsync.filesystem;

import java.nio.ByteBuffer;

/**
 * @author Thomas Freese
 */
@FunctionalInterface
public interface ChannelWriter
{
    /**
     * @param buffer {@link ByteBuffer}
     * @return int; Bytes written
     * @throws Exception Falls was schief geht.
     */
    public int write(ByteBuffer buffer) throws Exception;
}
