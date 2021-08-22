// Created: 20.08.2021
package de.freese.jsync.nio.transport.pool;

import java.nio.ByteBuffer;
import java.util.Deque;
import java.util.LinkedList;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.locks.ReentrantLock;

/**
 * @author Thomas Freese
 */
public class DefaultByteBufferPool implements ByteBufferPool
{
    /**
       *
       */
    private static final int CAPACITY_THRESHOLD = 1024 * 1024 * 4;

    /**
    *
    */
    private static final int MAX_CAPACITY = Integer.MAX_VALUE;

    /**
     *
     */
    private final Map<Integer, Deque<ByteBuffer>> cache = new TreeMap<>();

    /**
    *
    */
    private final ReentrantLock lock = new ReentrantLock(true);

    /**
     * Die unterschiedlichen Größen der Buffer gruppieren.
     *
     * @param neededCapacity int
     *
     * @return int
     */
    int calculateCapacity(final int neededCapacity)
    {
        if (neededCapacity < 0)
        {
            throw new IllegalArgumentException("'neededCapacity' must >= 0");
        }

        if (neededCapacity == CAPACITY_THRESHOLD)
        {
            return CAPACITY_THRESHOLD;
        }

        // Über dem Schwellenwert: die neue Größe nicht einfach verdoppeln, sondern um Schwellenwert vergrößern.
        if (neededCapacity > CAPACITY_THRESHOLD)
        {
            int newCapacity = (neededCapacity / CAPACITY_THRESHOLD) * CAPACITY_THRESHOLD;

            if (newCapacity > (MAX_CAPACITY - CAPACITY_THRESHOLD))
            {
                newCapacity = MAX_CAPACITY;
            }
            else
            {
                newCapacity += CAPACITY_THRESHOLD;
            }

            return newCapacity;
        }

        // Nicht über dem Schwellenwert: bis auf Schwellenwert vergrößern in "power of 2" Schritten, angefangen bei 64.
        // << 1: Bit-Shift nach links, vergrößert um power of 2; 1,2,4,8,16,32,...
        // >> 1: Bit-Shift nach rechts, verkleinert um power of 2; ...,32,16,8,4,2,
        int newCapacity = 1024;

        while (newCapacity < neededCapacity)
        {
            newCapacity <<= 1;
        }

        return Math.min(newCapacity, MAX_CAPACITY);
    }

    /**
     * @see de.freese.jsync.nio.transport.pool.ByteBufferPool#clear()
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
            this.cache.computeIfAbsent(buffer.capacity(), key -> new LinkedList<>()).addLast(buffer);
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

            int capacity = calculateCapacity(size);

            ByteBuffer buffer = this.cache.computeIfAbsent(capacity, key -> new LinkedList<>()).pollFirst();

            if (buffer == null)
            {
                // System.err.println("ByteBufferPool cache miss");
                buffer = ByteBuffer.allocate(capacity);
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

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());

        this.cache.forEach((key, value) -> {
            sb.append(": ").append(key).append("x").append(value.size());
        });

        return sb.toString();
    }
}
