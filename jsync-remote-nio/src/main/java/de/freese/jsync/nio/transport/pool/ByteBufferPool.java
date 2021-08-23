// Created: 20.08.2021
package de.freese.jsync.nio.transport.pool;

import java.nio.ByteBuffer;

/**
 * @author Thomas Freese
 */
public interface ByteBufferPool
{
    // /**
    // *
    // */
    // ByteBufferPool DEFAULT = new DefaultByteBufferPool();

    /**
     *
     */
    void clear();

    /**
     * @param buffer {@link ByteBuffer}
     */
    void free(ByteBuffer buffer);

    /**
     * @param size int
     *
     * @return {@link ByteBuffer}
     */
    ByteBuffer get(int size);
}
