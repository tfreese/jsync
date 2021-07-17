// Created: 16.07.2021
package de.freese.jsync.utils.pool;

import java.nio.ByteBuffer;

import de.freese.jsync.Options;

/**
 * @author Thomas Freese
 */
public class ByteBufferPool extends Pool<ByteBuffer>
{
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
     * @return {@link ByteBufferPool}
     */
    public static ByteBufferPool getInstance()
    {
        return ByteBufferPoolHolder.INSTANCE;
    }

    /**
     * Erstellt ein neues {@link ByteBufferPool} Object.
     */
    private ByteBufferPool()
    {
        super(true, true);
    }

    /**
     * @see de.freese.jsync.utils.pool.Pool#create()
     */
    @Override
    protected ByteBuffer create()
    {
        return ByteBuffer.allocateDirect(Options.BUFFER_SIZE);
    }

    /**
     * @see de.freese.jsync.utils.pool.Pool#obtain()
     */
    @Override
    public ByteBuffer obtain()
    {
        ByteBuffer byteBuffer = super.obtain();

        byteBuffer.clear();

        return byteBuffer;
    }
}
