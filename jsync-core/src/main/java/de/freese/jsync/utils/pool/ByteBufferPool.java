// Created: 27.08.20
package de.freese.jsync.utils.pool;

import java.nio.ByteBuffer;
import de.freese.jsync.Options;

/**
 * @author Thomas Freese
 */
public class ByteBufferPool extends AbstractPool<ByteBuffer>
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
        super.clear(obj -> {
        });
    }

    /**
     * @see de.freese.jsync.utils.pool.AbstractPool#createObject()
     */
    @Override
    protected ByteBuffer createObject()
    {
        return ByteBuffer.allocateDirect(Options.BUFFER_SIZE);
    }

    /**
     * @see de.freese.jsync.utils.pool.AbstractPool#destroyObject(java.lang.Object)
     */
    @Override
    protected void destroyObject(final ByteBuffer object)
    {
        // ByteBuffer haben nichts zum aufräumen.
        object.clear();
    }
}
