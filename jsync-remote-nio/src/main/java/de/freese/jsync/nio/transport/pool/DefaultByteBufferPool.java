// Created: 20.08.2021
package de.freese.jsync.nio.transport.pool;

import java.nio.ByteBuffer;

/**
 * @author Thomas Freese
 */
public class DefaultByteBufferPool implements ByteBufferPool
{
    /**
     * @see de.freese.jsync.nio.transport.pool.ByteBufferPool#free(java.nio.ByteBuffer)
     */
    @Override
    public void free(final ByteBuffer buffer)
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.nio.transport.pool.ByteBufferPool#get(int)
     */
    @Override
    public ByteBuffer get(final int size)
    {
        return ByteBuffer.allocate(size);
    }
}
