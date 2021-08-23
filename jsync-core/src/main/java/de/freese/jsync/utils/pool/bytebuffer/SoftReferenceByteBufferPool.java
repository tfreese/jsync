// Created: 16.07.2021
package de.freese.jsync.utils.pool.bytebuffer;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import de.freese.jsync.Options;
import de.freese.jsync.utils.pool.Pool;

/**
 * @author Thomas Freese
 */
class SoftReferenceByteBufferPool extends Pool<ByteBuffer> implements ByteBufferPool
{
    /**
     * Default: 4 MB
     */
    private static final int CAPACITY_THRESHOLD = 1024 * 1024 * 4;

    /**
     *
     */
    private static final int MAX_CAPACITY = Integer.MAX_VALUE;

    /**
     * Calculate the capacity of the buffer.
     *
     * @param neededCapacity int
     *
     * @return int
     *
     * @see io.netty.buffer.AbstractByteBufAllocator#calculateNewCapacity(int, int)
     */
    @SuppressWarnings("javadoc")
    static int calculateCapacity(final int neededCapacity)
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
        int newCapacity = 64;

        while (newCapacity < neededCapacity)
        {
            newCapacity <<= 1;
        }

        return Math.min(newCapacity, MAX_CAPACITY);
    }

    /**
     * Erstellt ein neues {@link ByteBufferPool} Object.
     */
    SoftReferenceByteBufferPool()
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
     * @param byteBuffer {@link ByteBuffer}
     * @param newCapacity int
     *
     * @return {@link ByteBuffer}
     */
    private ByteBuffer createNewBuffer(final ByteBuffer byteBuffer, final int newCapacity)
    {
        if (newCapacity <= 0)
        {
            throw new IllegalArgumentException(String.format("'newCapacity' %d must be higher than 0", newCapacity));
        }

        ByteOrder bo = byteBuffer.order();

        ByteBuffer newBuffer = byteBuffer.isDirect() ? ByteBuffer.allocateDirect(newCapacity) : ByteBuffer.allocate(newCapacity);

        byteBuffer.flip();
        newBuffer.put(byteBuffer);

        newBuffer.order(bo);

        return newBuffer;
    }

    /**
     * @param byteBuffer {@link ByteBuffer}
     * @param capacity int
     *
     * @return {@link ByteBuffer}
     */
    private ByteBuffer ensureCapacity(final ByteBuffer byteBuffer, final int capacity)
    {
        if (capacity > byteBuffer.capacity())
        {
            int newCapacity = calculateCapacity(capacity);

            return createNewBuffer(byteBuffer, newCapacity);
        }

        return byteBuffer;
    }

    /**
     * @see de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool#get(int)
     */
    @Override
    public ByteBuffer get(final int size)
    {
        ByteBuffer byteBuffer = super.obtain();

        byteBuffer = ensureCapacity(byteBuffer, size);

        byteBuffer.clear();

        return byteBuffer;
    }

    /**
     * @see de.freese.jsync.utils.pool.Pool#obtain()
     */
    @Override
    public ByteBuffer obtain()
    {
        return get(Options.BUFFER_SIZE);
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        StringBuilder sb = new StringBuilder(getClass().getSimpleName());
        sb.append(":");
        sb.append(" created=").append(getCreated());
        sb.append(", free=").append(getFree());
        sb.append(", peak=").append(getPeak());

        return sb.toString();
    }
}
