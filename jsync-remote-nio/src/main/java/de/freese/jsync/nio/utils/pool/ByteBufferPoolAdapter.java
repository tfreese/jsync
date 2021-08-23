// Created: 20.08.2021
package de.freese.jsync.nio.utils.pool;

import java.nio.ByteBuffer;
import java.util.concurrent.locks.ReentrantLock;

import de.freese.jsync.nio.transport.pool.ByteBufferPool;

/**
 * @author Thomas Freese
 */
public class ByteBufferPoolAdapter implements ByteBufferPool
{
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
        // Empty
    }

    /**
     * @see de.freese.jsync.nio.transport.pool.ByteBufferPool#free(java.nio.ByteBuffer)
     */
    @Override
    public void free(final ByteBuffer buffer)
    {
        if (buffer == null)
        {
            return;
        }

        this.lock.lock();

        try
        {
            de.freese.jsync.utils.pool.ByteBufferPool.getInstance().free(buffer);
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
            return de.freese.jsync.utils.pool.ByteBufferPool.getInstance().obtain(size);
        }
        finally
        {
            this.lock.unlock();
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return de.freese.jsync.utils.pool.ByteBufferPool.getInstance().toString();
    }
}
