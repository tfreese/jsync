// Created: 27.08.20
package de.freese.jsync.utils.pool;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import de.freese.jsync.Options;

/**
 * @author Thomas Freese
 */
public class ByteBufferPool
{
    /**
     *
     */
    private static final boolean ENABLED = false;

    /**
     * @return {@link ByteBufferPool}
     */
    public static ByteBufferPool getInstance()
    {
        return ByteBufferPoolHolder.INSTANCE;
    }

    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class ByteBufferPoolHolder
    {
        /**
         *
         */
        private static final ByteBufferPool INSTANCE = new ByteBufferPool();

        /**
         * Erstellt ein neues {@link ByteBufferPoolHolder} Object.
         */
        private ByteBufferPoolHolder()
        {
            super();
        }
    }

    /**
     *
     */
    private final List<ByteBuffer> bufferPool = new ArrayList<>();

    /**
     *
     */
    private final ReentrantLock lock = new ReentrantLock(true);

    /**
     * Erzeugt eine neue Instanz von {@link ByteBufferPool}
     */
    private ByteBufferPool()
    {
        super();
    }

    /**
     *
     */
    public void clear()
    {
        // getLock().lock();
        //
        // try
        // {
        synchronized (this)
        {
            for (Iterator<ByteBuffer> iterator = this.bufferPool.iterator(); iterator.hasNext();)
            {
                ByteBuffer buffer = iterator.next();

                // ByteBuffer haben nichts zum aufräumen.
                buffer.clear();

                iterator.remove();
            }
        }
        // finally
        // {
        // getLock().unlock();
        // }
    }

    /**
     * @return {@link ByteBuffer}
     */
    public ByteBuffer getBuffer()
    {
        // getLock().lock();

        if (!ENABLED)
        {
            return ByteBuffer.allocateDirect(Options.BUFFER_SIZE);
        }

        // try
        synchronized (this)
        {
            ByteBuffer buffer = null;

            if (this.bufferPool.isEmpty())
            {
                buffer = ByteBuffer.allocateDirect(Options.BUFFER_SIZE);
            }
            else
            {
                buffer = this.bufferPool.remove(0);
            }

            return buffer;
        }
        // finally
        // {
        // getLock().unlock();
        // }
    }

    /**
     * @param buffer {@link ByteBuffer}
     */
    public void releaseBuffer(final ByteBuffer buffer)
    {
        // getLock().lock();

        if (!ENABLED)
        {
            return;
        }

        // try
        synchronized (this)
        {
            this.bufferPool.add(buffer);
        }
        // finally
        // {
        // getLock().unlock();
        // }
    }

    /**
     * @return {@link ReentrantLock}
     */
    private ReentrantLock getLock()
    {
        return this.lock;
    }
}
