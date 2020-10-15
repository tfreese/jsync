// Created: 06.10.2020
package de.freese.jsync.utils.buffer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.Buffer;
import java.nio.ByteBuffer;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.function.IntPredicate;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.util.Assert;
import org.springframework.util.ObjectUtils;

/**
 * @author Thomas Freese
 * @see org.springframework.core.io.buffer.DefaultDataBuffer
 */
public class DefaultPooledDataBuffer implements PooledDataBuffer
{
    /**
     * @author Thomas Freese
     */
    private class DefaultPooledDataBufferInputStream extends InputStream
    {
        /**
         *
         */
        private final boolean releaseOnClose;

        /**
         * Erstellt ein neues {@link DefaultPooledDataBufferInputStream} Object.
         *
         * @param releaseOnClose boolean
         */
        private DefaultPooledDataBufferInputStream(final boolean releaseOnClose)
        {
            super();

            this.releaseOnClose = releaseOnClose;
        }

        /**
         * @see java.io.InputStream#available()
         */
        @Override
        public int available()
        {
            return readableByteCount();
        }

        /**
         * @see java.io.InputStream#close()
         */
        @Override
        public void close() throws IOException
        {
            if (this.releaseOnClose)
            {
                release();
            }

            super.close();
        }

        /**
         * @see java.io.InputStream#read()
         */
        @Override
        public int read()
        {
            return available() > 0 ? DefaultPooledDataBuffer.this.read() & 0xFF : -1;
        }

        /**
         * @see java.io.InputStream#read(byte[], int, int)
         */
        @Override
        public int read(final byte[] bytes, final int off, int len) throws IOException
        {
            int available = available();

            if (available > 0)
            {
                len = Math.min(len, available);
                DefaultPooledDataBuffer.this.read(bytes, off, len);

                return len;
            }

            return -1;
        }
    }

    /**
     * @author Thomas Freese
     */
    private class DefaultPooledDataBufferOutputStream extends OutputStream
    {
        /**
         * @see java.io.OutputStream#write(byte[], int, int)
         */
        @Override
        public void write(final byte[] bytes, final int off, final int len) throws IOException
        {
            DefaultPooledDataBuffer.this.write(bytes, off, len);
        }

        /**
         * @see java.io.OutputStream#write(int)
         */
        @Override
        public void write(final int b) throws IOException
        {
            DefaultPooledDataBuffer.this.write((byte) b);
        }
    }

    /**
     * @author Thomas Freese
     */
    private static class SlicedDefaultPooledDataBuffer extends DefaultPooledDataBuffer
    {
        /**
         * Erstellt ein neues {@link SlicedDefaultPooledDataBuffer} Object.
         *
         * @param byteBuffer {@link ByteBuffer}
         * @param dataBufferFactory {@link DefaultPooledDataBufferFactory}
         * @param id String
         * @param length int
         */
        SlicedDefaultPooledDataBuffer(final ByteBuffer byteBuffer, final DefaultPooledDataBufferFactory dataBufferFactory, final String id, final int length)
        {
            super(dataBufferFactory, byteBuffer, id);

            writePosition(length);
        }

        /**
         * @see org.springframework.core.io.buffer.DefaultDataBuffer#capacity(int)
         */
        @Override
        public DefaultPooledDataBuffer capacity(final int newCapacity)
        {
            throw new UnsupportedOperationException("Changing the capacity of a sliced buffer is not supported");
        }
    }

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
    private ByteBuffer byteBuffer;

    /**
     *
     */
    private int capacity;

    /**
     *
     */
    private final DefaultPooledDataBufferFactory dataBufferFactory;

    /**
     *
     */
    private final String id;

    /**
     *
     */
    private int readPosition;

    /**
     *
     */
    private int writePosition;

    /**
     * Erstellt ein neues {@link DefaultPooledDataBuffer} Object.
     *
     * @param dataBufferFactory {@link DefaultPooledDataBufferFactory}
     * @param byteBuffer {@link ByteBuffer}
     * @param id String
     */
    DefaultPooledDataBuffer(final DefaultPooledDataBufferFactory dataBufferFactory, final ByteBuffer byteBuffer, final String id)
    {
        super();

        Assert.notNull(dataBufferFactory, "DefaultDataBufferFactory must not be null");
        Assert.notNull(byteBuffer, "ByteBuffer must not be null");

        this.dataBufferFactory = dataBufferFactory;

        ByteBuffer slice = byteBuffer.slice();
        this.byteBuffer = slice;
        this.capacity = slice.remaining();

        this.id = id;
    }

    /**
     * @param capacity int
     * @param direct boolean
     * @return {@link ByteBuffer}
     */
    private ByteBuffer allocate(final int capacity, final boolean direct)
    {
        return (direct ? ByteBuffer.allocateDirect(capacity) : ByteBuffer.allocate(capacity));
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#asByteBuffer()
     */
    @Override
    public ByteBuffer asByteBuffer()
    {
        return asByteBuffer(this.readPosition, readableByteCount());
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#asByteBuffer(int, int)
     */
    @Override
    public ByteBuffer asByteBuffer(final int index, final int length)
    {
        checkIndex(index, length);

        ByteBuffer duplicate = this.byteBuffer.duplicate();

        // Explicit access via Buffer base type for compatibility
        // with covariant return type on JDK 9's ByteBuffer...
        Buffer buffer = duplicate;
        buffer.position(index);
        buffer.limit(index + length);

        return duplicate.slice();
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#asInputStream()
     */
    @Override
    public InputStream asInputStream()
    {
        return asInputStream(false);
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#asInputStream(boolean)
     */
    @Override
    public InputStream asInputStream(final boolean releaseOnClose)
    {
        return new DefaultPooledDataBufferInputStream(releaseOnClose);
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#asOutputStream()
     */
    @Override
    public OutputStream asOutputStream()
    {
        return new DefaultPooledDataBufferOutputStream();
    }

    /**
     * @param expression boolean
     * @param format String
     * @param args Object[]
     */
    private void assertIndex(final boolean expression, final String format, final Object...args)
    {
        if (!expression)
        {
            String message = String.format(format, args);
            throw new IndexOutOfBoundsException(message);
        }
    }

    /**
     * Calculate the capacity of the buffer.
     *
     * @param neededCapacity int
     * @return int
     * @see io.netty.buffer.AbstractByteBufAllocator#calculateNewCapacity(int, int)
     */
    @SuppressWarnings("javadoc")
    private int calculateCapacity(final int neededCapacity)
    {
        Assert.isTrue(neededCapacity >= 0, "'neededCapacity' must >= 0");

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
     * @see org.springframework.core.io.buffer.DataBuffer#capacity()
     */
    @Override
    public int capacity()
    {
        return this.capacity;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#capacity(int)
     */
    @Override
    public DataBuffer capacity(final int newCapacity)
    {
        if (newCapacity <= 0)
        {
            throw new IllegalArgumentException(String.format("'newCapacity' %d must be higher than 0", newCapacity));
        }

        int readPosition = readPosition();
        int writePosition = writePosition();
        int oldCapacity = capacity();

        if (newCapacity > oldCapacity)
        {
            ByteBuffer oldBuffer = this.byteBuffer;
            ByteBuffer newBuffer = allocate(newCapacity, oldBuffer.isDirect());

            oldBuffer.position(0).limit(oldBuffer.capacity());
            newBuffer.position(0).limit(oldBuffer.capacity());
            newBuffer.put(oldBuffer);
            newBuffer.clear();

            setNativeBuffer(newBuffer);
        }
        else if (newCapacity < oldCapacity)
        {
            ByteBuffer oldBuffer = this.byteBuffer;
            ByteBuffer newBuffer = allocate(newCapacity, oldBuffer.isDirect());

            if (readPosition < newCapacity)
            {
                if (writePosition > newCapacity)
                {
                    writePosition = newCapacity;

                    writePosition(writePosition);
                }
                oldBuffer.position(readPosition).limit(writePosition);
                newBuffer.position(readPosition).limit(writePosition);
                newBuffer.put(oldBuffer);
                newBuffer.clear();
            }
            else
            {
                readPosition(newCapacity);
                writePosition(newCapacity);
            }

            setNativeBuffer(newBuffer);
        }

        return this;
    }

    /**
     * @param index int
     * @param length int
     */
    private void checkIndex(final int index, final int length)
    {
        assertIndex(index >= 0, "index %d must be >= 0", index);
        assertIndex(length >= 0, "length %d must be >= 0", index);
        assertIndex(index <= this.capacity, "index %d must be <= %d", index, this.capacity);
        assertIndex(length <= this.capacity, "length %d must be <= %d", index, this.capacity);
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#ensureCapacity(int)
     */
    @Override
    public DataBuffer ensureCapacity(final int length)
    {
        if (length > writableByteCount())
        {
            int newCapacity = calculateCapacity(this.writePosition + length);

            capacity(newCapacity);
        }

        return this;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(final Object other)
    {
        if (this == other)
        {
            return true;
        }

        if (!(other instanceof DefaultPooledDataBuffer))
        {
            return false;
        }

        DefaultPooledDataBuffer otherBuffer = (DefaultPooledDataBuffer) other;

        return ((this.readPosition == otherBuffer.readPosition) && (this.writePosition == otherBuffer.writePosition)
                && this.byteBuffer.equals(otherBuffer.byteBuffer));
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#factory()
     */
    @Override
    public DataBufferFactory factory()
    {
        return this.dataBufferFactory;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#getByte(int)
     */
    @Override
    public byte getByte(final int index)
    {
        assertIndex(index >= 0, "index %d must be >= 0", index);
        assertIndex(index <= (this.writePosition - 1), "index %d must be <= %d", index, this.writePosition - 1);

        return this.byteBuffer.get(index);
    }

    /**
     * Directly exposes the native {@code ByteBuffer} that this buffer is based on also updating the {@code ByteBuffer's} position and limit to match the
     * current {@link #readPosition()} and {@link #readableByteCount()}.
     *
     * @return the wrapped byte buffer
     */
    public ByteBuffer getNativeBuffer()
    {
        this.byteBuffer.position(this.readPosition);
        this.byteBuffer.limit(readableByteCount());

        return this.byteBuffer;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        return this.byteBuffer.hashCode();
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#indexOf(java.util.function.IntPredicate, int)
     */
    @Override
    public int indexOf(final IntPredicate predicate, int fromIndex)
    {
        Assert.notNull(predicate, "IntPredicate must not be null");

        if (fromIndex < 0)
        {
            fromIndex = 0;
        }
        else if (fromIndex >= this.writePosition)
        {
            return -1;
        }

        for (int i = fromIndex; i < this.writePosition; i++)
        {
            byte b = this.byteBuffer.get(i);

            if (predicate.test(b))
            {
                return i;
            }
        }

        return -1;
    }

    /**
     * @see org.springframework.core.io.buffer.PooledDataBuffer#isAllocated()
     */
    @Override
    public boolean isAllocated()
    {
        return true;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#lastIndexOf(java.util.function.IntPredicate, int)
     */
    @Override
    public int lastIndexOf(final IntPredicate predicate, final int fromIndex)
    {
        Assert.notNull(predicate, "IntPredicate must not be null");

        int i = Math.min(fromIndex, this.writePosition - 1);

        for (; i >= 0; i--)
        {
            byte b = this.byteBuffer.get(i);

            if (predicate.test(b))
            {
                return i;
            }
        }

        return -1;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#read()
     */
    @Override
    public byte read()
    {
        assertIndex(this.readPosition <= (this.writePosition - 1), "readPosition %d must be <= %d", this.readPosition, this.writePosition - 1);

        int pos = this.readPosition;
        byte b = this.byteBuffer.get(pos);
        this.readPosition = pos + 1;

        return b;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#read(byte[])
     */
    @Override
    public DataBuffer read(final byte[] destination)
    {
        Assert.notNull(destination, "Byte array must not be null");

        read(destination, 0, destination.length);

        return this;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#read(byte[], int, int)
     */
    @Override
    public DataBuffer read(final byte[] destination, final int offset, final int length)
    {
        Assert.notNull(destination, "Byte array must not be null");
        assertIndex(this.readPosition <= (this.writePosition - length), "readPosition %d and length %d should be smaller than writePosition %d",
                this.readPosition, length, this.writePosition);

        ByteBuffer tmp = this.byteBuffer.duplicate();
        int limit = this.readPosition + length;
        tmp.clear().position(this.readPosition).limit(limit);
        tmp.get(destination, offset, length);

        this.readPosition += length;

        return this;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#readableByteCount()
     */
    @Override
    public int readableByteCount()
    {
        return this.writePosition - this.readPosition;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#readPosition()
     */
    @Override
    public int readPosition()
    {
        return this.readPosition;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#readPosition(int)
     */
    @Override
    public DataBuffer readPosition(final int readPosition)
    {
        assertIndex(readPosition >= 0, "'readPosition' %d must be >= 0", readPosition);
        assertIndex(readPosition <= this.writePosition, "'readPosition' %d must be <= %d", readPosition, this.writePosition);

        this.readPosition = readPosition;

        return this;
    }

    /**
     * @see org.springframework.core.io.buffer.PooledDataBuffer#release()
     */
    @Override
    public boolean release()
    {
        if (this instanceof SlicedDefaultPooledDataBuffer)
        {
            return true;
        }

        this.dataBufferFactory.release(this);

        return false;
    }

    /**
     * @see org.springframework.core.io.buffer.PooledDataBuffer#retain()
     */
    @Override
    public PooledDataBuffer retain()
    {
        release();

        return this;
    }

    /**
     * @param byteBuffer {@link ByteBuffer}
     */
    private void setNativeBuffer(final ByteBuffer byteBuffer)
    {
        this.byteBuffer = byteBuffer;
        this.capacity = byteBuffer.remaining();
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#slice(int, int)
     */
    @Override
    public DataBuffer slice(final int index, final int length)
    {
        checkIndex(index, length);

        int oldPosition = this.byteBuffer.position();
        // Explicit access via Buffer base type for compatibility
        // with covariant return type on JDK 9's ByteBuffer...
        Buffer buffer = this.byteBuffer;

        try
        {
            buffer.position(index);
            ByteBuffer slice = this.byteBuffer.slice();
            // Explicit cast for compatibility with covariant return type on JDK 9's ByteBuffer
            slice.limit(length);

            return new SlicedDefaultPooledDataBuffer(slice, this.dataBufferFactory, this.id + "-sliced", length);
        }
        finally
        {
            buffer.position(oldPosition);
        }
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString()
    {
        return String.format("DefaultPooledDataBuffer (r: %d, w: %d, c: %d, id: %s)", this.readPosition, this.writePosition, this.capacity, this.id);
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#toString(int, int, java.nio.charset.Charset)
     */
    @Override
    public String toString(final int index, final int length, final Charset charset)
    {
        checkIndex(index, length);
        Assert.notNull(charset, "Charset must not be null");

        byte[] bytes;
        int offset;

        if (this.byteBuffer.hasArray())
        {
            bytes = this.byteBuffer.array();
            offset = this.byteBuffer.arrayOffset() + index;
        }
        else
        {
            bytes = new byte[length];
            offset = 0;

            ByteBuffer duplicate = this.byteBuffer.duplicate();
            duplicate.clear().position(index).limit(index + length);
            duplicate.get(bytes, 0, length);
        }

        return new String(bytes, offset, length, charset);
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#writableByteCount()
     */
    @Override
    public int writableByteCount()
    {
        return this.capacity - this.writePosition;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#write(byte)
     */
    @Override
    public DataBuffer write(final byte b)
    {
        ensureCapacity(1);

        int pos = this.writePosition;
        this.byteBuffer.put(pos, b);
        this.writePosition = pos + 1;

        return this;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#write(byte[])
     */
    @Override
    public DataBuffer write(final byte[] source)
    {
        Assert.notNull(source, "Byte array must not be null");

        write(source, 0, source.length);

        return this;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#write(byte[], int, int)
     */
    @Override
    public DataBuffer write(final byte[] source, final int offset, final int length)
    {
        Assert.notNull(source, "Byte array must not be null");

        ensureCapacity(length);

        ByteBuffer tmp = this.byteBuffer.duplicate();
        int limit = this.writePosition + length;
        tmp.clear().position(this.writePosition).limit(limit);
        tmp.put(source, offset, length);

        this.writePosition += length;

        return this;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#write(java.nio.ByteBuffer[])
     */
    @Override
    public DataBuffer write(final ByteBuffer...buffers)
    {
        if (!ObjectUtils.isEmpty(buffers))
        {
            int neededCapacity = Arrays.stream(buffers).mapToInt(ByteBuffer::remaining).sum();

            ensureCapacity(neededCapacity);

            Arrays.stream(buffers).forEach(this::write);
        }

        return this;
    }

    /**
     * @param source {@link ByteBuffer}
     */
    private void write(final ByteBuffer source)
    {
        int length = source.remaining();
        ByteBuffer tmp = this.byteBuffer.duplicate();
        int limit = this.writePosition + source.remaining();
        tmp.clear().position(this.writePosition).limit(limit);
        tmp.put(source);
        this.writePosition += length;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#write(org.springframework.core.io.buffer.DataBuffer[])
     */
    @Override
    public DataBuffer write(final DataBuffer...buffers)
    {
        if (!ObjectUtils.isEmpty(buffers))
        {
            write(Arrays.stream(buffers).map(DataBuffer::asByteBuffer).toArray(ByteBuffer[]::new));
        }

        return this;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#writePosition()
     */
    @Override
    public int writePosition()
    {
        return this.writePosition;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBuffer#writePosition(int)
     */
    @Override
    public DataBuffer writePosition(final int writePosition)
    {
        assertIndex(writePosition >= this.readPosition, "'writePosition' %d must be >= %d", writePosition, this.readPosition);
        assertIndex(writePosition <= this.capacity, "'writePosition' %d must be <= %d", writePosition, this.capacity);

        this.writePosition = writePosition;

        return this;
    }
}
