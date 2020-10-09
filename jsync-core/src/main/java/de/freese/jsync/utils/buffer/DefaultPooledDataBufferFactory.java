// Created: 06.10.2020
package de.freese.jsync.utils.buffer;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.util.Assert;

/**
 * @author Thomas Freese
 * @see org.springframework.core.io.buffer.DefaultDataBufferFactory
 */
public class DefaultPooledDataBufferFactory implements DataBufferFactory
{
    /**
     * ThreadSafe Singleton-Pattern.
     *
     * @author Thomas Freese
     */
    private static final class DefaultPooledDataBufferFactoryHolder
    {
        /**
         *
         */
        private static final DefaultPooledDataBufferFactory INSTANCE = new DefaultPooledDataBufferFactory(true);

        /**
         * Erstellt ein neues {@link DefaultPooledDataBufferFactoryHolder} Object.
         */
        private DefaultPooledDataBufferFactoryHolder()
        {
            super();
        }
    }

    /**
     *
     */
    private static final int DEFAULT_INITIAL_CAPACITY = 1024;

    /**
    *
    */
    private static final AtomicInteger ID_GENERATOR = new AtomicInteger(0);

    /**
     * @return {@link DefaultPooledDataBufferFactory}
     */
    public static DefaultPooledDataBufferFactory getInstance()
    {
        return DefaultPooledDataBufferFactoryHolder.INSTANCE;
    }

    /**
     *
     */
    private final int defaultInitialCapacity;

    /**
    *
    */
    private final String id;

    /**
     *
     */
    private final ReentrantLock lock = new ReentrantLock(true);

    /**
     *
     */
    private final Queue<DefaultPooledDataBuffer> pool = new ConcurrentLinkedQueue<>();

    /**
     *
     */
    private final boolean preferDirect;

    /**
     * Erstellt ein neues {@link DefaultPooledDataBufferFactory} Object.
     */
    public DefaultPooledDataBufferFactory()
    {
        this(false);
    }

    /**
     * Erstellt ein neues {@link DefaultPooledDataBufferFactory} Object.
     *
     * @param preferDirect boolean
     */
    public DefaultPooledDataBufferFactory(final boolean preferDirect)
    {
        this(preferDirect, DEFAULT_INITIAL_CAPACITY);
    }

    /**
     * Erstellt ein neues {@link DefaultPooledDataBufferFactory} Object.
     *
     * @param preferDirect boolean
     * @param defaultInitialCapacity int
     */
    public DefaultPooledDataBufferFactory(final boolean preferDirect, final int defaultInitialCapacity)
    {
        super();

        Assert.isTrue(defaultInitialCapacity > 0, "'defaultInitialCapacity' should be larger than 0");

        this.preferDirect = preferDirect;
        this.defaultInitialCapacity = defaultInitialCapacity;

        this.id = "factory-" + ID_GENERATOR.incrementAndGet();
    }

    /**
     * @see org.springframework.core.io.buffer.DataBufferFactory#allocateBuffer()
     */
    @Override
    public DataBuffer allocateBuffer()
    {
        return allocateBuffer(this.defaultInitialCapacity);
    }

    /**
     * @see org.springframework.core.io.buffer.DataBufferFactory#allocateBuffer(int)
     */
    @Override
    public DataBuffer allocateBuffer(final int initialCapacity)
    {
        return get(initialCapacity);
    }

    /**
     * @param initialCapacity int
     * @return {@link DefaultPooledDataBuffer}
     */
    DefaultPooledDataBuffer get(final int initialCapacity)
    {
        getLock().lock();

        try
        {
            DefaultPooledDataBuffer dataBuffer = this.pool.poll();

            if (dataBuffer == null)
            {
                ByteBuffer byteBuffer = this.preferDirect ? ByteBuffer.allocateDirect(initialCapacity) : ByteBuffer.allocate(initialCapacity);

                dataBuffer = new DefaultPooledDataBuffer(this, byteBuffer);
            }

            dataBuffer.readPosition(0);
            dataBuffer.writePosition(0);

            if (dataBuffer.capacity() < initialCapacity)
            {
                dataBuffer.ensureCapacity(initialCapacity);
            }

            return dataBuffer;
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * @return String
     */
    String getId()
    {
        return this.id;
    }

    /**
     * @return {@link ReentrantLock}
     */
    protected ReentrantLock getLock()
    {
        return this.lock;
    }

    /**
     * @see org.springframework.core.io.buffer.DataBufferFactory#join(java.util.List)
     */
    @Override
    public DataBuffer join(final List<? extends DataBuffer> dataBuffers)
    {
        Assert.notEmpty(dataBuffers, "DataBuffer List must not be empty");

        int capacity = dataBuffers.stream().mapToInt(DataBuffer::readableByteCount).sum();
        DataBuffer result = allocateBuffer(capacity);
        dataBuffers.forEach(result::write);
        dataBuffers.forEach(DataBufferUtils::release);

        return result;
    }

    /**
     * @param dataBuffer {@link DefaultPooledDataBuffer}
     */
    void release(final DefaultPooledDataBuffer dataBuffer)
    {
        Objects.requireNonNull(dataBuffer, "dataBuffer required");

        getLock().lock();

        try
        {
            this.pool.add(dataBuffer);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * @see org.springframework.core.io.buffer.DataBufferFactory#wrap(byte[])
     */
    @Override
    public DataBuffer wrap(final byte[] bytes)
    {
        return wrap(ByteBuffer.wrap(bytes));
    }

    /**
     * @see org.springframework.core.io.buffer.DataBufferFactory#wrap(java.nio.ByteBuffer)
     */
    @Override
    public DataBuffer wrap(final ByteBuffer byteBuffer)
    {
        // DataBuffer dataBuffer = new DefaultPooledDataBuffer(this, byteBuffer.slice());
        // dataBuffer.writePosition(byteBuffer.remaining());

        ByteBuffer slice = byteBuffer.slice();

        DataBuffer dataBuffer = get(slice.limit());

        dataBuffer.write(slice);
        dataBuffer.writePosition(byteBuffer.remaining());

        return dataBuffer;
    }
}
