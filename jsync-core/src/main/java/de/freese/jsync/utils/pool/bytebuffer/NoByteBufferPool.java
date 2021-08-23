// Created: 23.08.2021
package de.freese.jsync.utils.pool.bytebuffer;

import java.nio.ByteBuffer;

/**
 * @author Thomas Freese
 */
class NoByteBufferPool implements ByteBufferPool
{
    /**
    *
    */
    private int created;

    /**
    *
    */
    private int free;

    /**
     * Erstellt ein neues {@link NoByteBufferPool} Object.
     */
    NoByteBufferPool()
    {
        super();
    }

    /**
     * @see de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool#clear()
     */
    @Override
    public void clear()
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool#free(java.nio.ByteBuffer)
     */
    @Override
    public void free(final ByteBuffer buffer)
    {
        this.free++;
    }

    /**
     * @see de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool#get(int)
     */
    @Override
    public ByteBuffer get(final int size)
    {
        this.created++;

        return ByteBuffer.allocate(size);
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

        return sb.toString();
    }
}
