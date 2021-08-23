// Created: 20.08.2021
package de.freese.jsync.utils.pool.bytebuffer;

import java.nio.ByteBuffer;

import de.freese.jsync.Options;

/**
 * @author Thomas Freese
 */
public interface ByteBufferPool
{
    /**
     *
     */
    ByteBufferPool DEFAULT = new SimpleByteBufferPool();

    /**
     *
     */
    void clear();

    /**
     * @param buffer {@link ByteBuffer}
     */
    void free(ByteBuffer buffer);

    /**
     * {@link ByteBuffer} mit DEFAULT Größe.
     *
     * @return {@link ByteBuffer}
     */
    default ByteBuffer get()
    {
        return get(Options.BUFFER_SIZE);
    }

    /**
     * @param size int
     *
     * @return {@link ByteBuffer}
     */
    ByteBuffer get(int size);
}