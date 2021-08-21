// Created: 20.08.2021
package de.freese.jsync.nio.transport.pool;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Thomas Freese
 */
public class DefaultByteBufferPool implements ByteBufferPool
{
    /**
     *
     */
    private final Map<Integer, ByteBuffer> cache = new HashMap<>();

    /**
     *
     */
    private final ReentrantLock lock = new ReentrantLock(true);

    /**
     * @see de.freese.jsync.nio.transport.pool.ByteBufferPool#clear()
     */
    @Override
    public void clear()
    {
        this.cache.clear();
    }

    /**
     * @see de.freese.jsync.nio.transport.pool.ByteBufferPool#free(java.nio.ByteBuffer)
     */
    @Override
    public void free(final ByteBuffer buffer)
    {
        this.lock.lock();

        try
        {
            this.cache.put(buffer.capacity(), buffer);
        }
        finally
        {
            this.lock.unlock();
        }
    }

    /**
     * @see de.freese.jsync.nio.transport.pool.ByteBufferPool#get(int)
     */
    @Override
    public ByteBuffer get(final int size)
    {
        this.lock.lock();

        try
        {
            ByteBuffer buffer = this.cache.remove(size);

            if (buffer == null)
            {
                // System.err.println("ByteBufferPool cache miss");
                buffer = ByteBuffer.allocate(size);
            }
            else
            {
                // System.err.println("ByteBufferPool cache hit");
                buffer.clear();
            }

            return buffer;
        }
        finally
        {
            this.lock.unlock();
        }
    }
}
