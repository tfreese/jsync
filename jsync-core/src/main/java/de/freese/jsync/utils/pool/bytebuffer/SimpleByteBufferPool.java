// Created: 20.08.2021
package de.freese.jsync.utils.pool.bytebuffer;

import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import de.freese.jsync.Options;

/**
 * SimpleByteBufferPool: created=9, free=951: 2192x1: 8176x1: 18235x1: 25538x1: 1242454x1: 1990756x1: 4194304x3
 *
 * @author Thomas Freese
 */
class SimpleByteBufferPool implements ByteBufferPool
{
    /**
     *
     */
    private final Deque<ByteBuffer> cache = new LinkedList<>();

    /**
    *
    */
    private int created;

    /**
    *
    */
    private int free;

    /**
     *
     */
    private final ReentrantLock lock = new ReentrantLock(true);

    /**
     * Erstellt ein neues {@link SimpleByteBufferPool} Object.
     */
    SimpleByteBufferPool()
    {
        super();
    }

    /**
     * @see de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool#clear()
     */
    @Override
    public void clear()
    {
        this.lock.lock();

        try
        {
            this.cache.clear();
        }
        finally
        {
            this.lock.unlock();
        }
    }

    /**
     * @see de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool#free(java.nio.ByteBuffer)
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
            this.free++;

            this.cache.addLast(buffer);
        }
        finally
        {
            this.lock.unlock();
        }
    }

    /**
     * @see de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool#get(int)
     */
    @Override
    public ByteBuffer get(final int size)
    {
        this.lock.lock();

        try
        {

            // int capacity = SoftReferenceByteBufferPool.calculateCapacity(size);
            int capacity = Options.BUFFER_SIZE;

            ByteBuffer buffer = this.cache.pollFirst();

            if (buffer == null)
            {
                this.created++;

                buffer = ByteBuffer.allocate(capacity);
            }
            else
            {
                buffer.clear();
            }

            return buffer;
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
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(":");
        sb.append(" created=").append(this.created);
        sb.append(", free=").append(this.free);
        sb.append(", size=").append(this.cache.size());

        return sb.toString();
    }
}
