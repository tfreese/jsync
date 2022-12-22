// Created: 20.08.2021
package de.freese.jsync.utils.pool.bytebuffer;

import java.nio.ByteBuffer;

/**
 * @author Thomas Freese
 */
public interface ByteBufferPool
{
    ByteBufferPool DEFAULT = new SimpleByteBufferPool();

    void clear();

    void free(ByteBuffer buffer);

    /**
     * {@link ByteBuffer} with DEFAULT Size.
     */
    ByteBuffer get();
}
