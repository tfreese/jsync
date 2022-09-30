// Created: 07.08.2021
package de.freese.jsync.rsocket.payload;

import java.nio.ByteBuffer;

import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.impl.ByteBufferAdapter;
import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.util.DefaultPayload;
import reactor.util.annotation.Nullable;

/**
 * {@link DefaultPayload} mit Verwendung des {@link ByteBufferPool}.
 *
 * @author Thomas Freese
 * @deprecated Funktioniert nicht
 */
@Deprecated
class JSyncPayload implements Payload
{
    /**
     *
     */
    private static final ByteBufferPool BYTE_BUFFER_POOL = ByteBufferPool.DEFAULT;
    /**
     *
     */
    private static final Serializer<ByteBuffer, ByteBuffer> SERIALIZER = DefaultSerializer.of(new ByteBufferAdapter());

    /**
     * @param data {@link ByteBuffer}
     *
     * @return {@link Payload}
     */
    public static Payload create(final ByteBuffer data)
    {
        return create(data, null);
    }

    /**
     * @param data {@link ByteBuffer}
     * @param metadata {@link ByteBuffer}
     *
     * @return {@link Payload}
     */
    public static Payload create(final ByteBuffer data, @Nullable final ByteBuffer metadata)
    {
        return new JSyncPayload(data, metadata);
    }

    /**
     * @param data {@link CharSequence}
     *
     * @return {@link Payload}
     */
    public static Payload create(final CharSequence data)
    {
        ByteBuffer buffer = BYTE_BUFFER_POOL.get();
        SERIALIZER.writeTo(buffer, data.toString());
        buffer.flip();

        return create(buffer);
    }

    /**
     *
     */
    private final ByteBuffer data;

    /**
     *
     */
    private final ByteBuffer metadata;

    /**
     * Erstellt ein neues {@link JSyncPayload} Object.
     *
     * @param data {@link ByteBuffer}
     * @param metadata {@link ByteBuffer}
     */
    private JSyncPayload(final ByteBuffer data, final ByteBuffer metadata)
    {
        super();

        this.data = data;
        this.metadata = metadata;
    }

    /**
     * @see io.rsocket.Payload#data()
     */
    @Override
    public ByteBuf data()
    {
        return sliceData();
    }

    /**
     * @see io.rsocket.Payload#getData()
     */
    @Override
    public ByteBuffer getData()
    {
        return this.data.duplicate();
    }

    /**
     * @see io.rsocket.Payload#getMetadata()
     */
    @Override
    public ByteBuffer getMetadata()
    {
        return this.metadata == null ? DefaultPayload.EMPTY_BUFFER : this.metadata.duplicate();
    }

    /**
     * @see io.rsocket.Payload#hasMetadata()
     */
    @Override
    public boolean hasMetadata()
    {
        return this.metadata != null;
    }

    /**
     * @see io.rsocket.Payload#metadata()
     */
    @Override
    public ByteBuf metadata()
    {
        return sliceMetadata();
    }

    /**
     * @see io.netty.util.ReferenceCounted#refCnt()
     */
    @Override
    public int refCnt()
    {
        return 1;
    }

    /**
     * @see io.netty.util.ReferenceCounted#release()
     */
    @Override
    public boolean release()
    {
        if (this.data != DefaultPayload.EMPTY_BUFFER)
        {
            BYTE_BUFFER_POOL.free(this.data);
        }

        if ((this.metadata != null) && (this.metadata != DefaultPayload.EMPTY_BUFFER))
        {
            BYTE_BUFFER_POOL.free(this.metadata);
        }

        return false;
    }

    /**
     * @see io.netty.util.ReferenceCounted#release(int)
     */
    @Override
    public boolean release(final int decrement)
    {
        return false;
    }

    /**
     * @see io.rsocket.Payload#retain()
     */
    @Override
    public Payload retain()
    {
        return this;
    }

    /**
     * @see io.rsocket.Payload#retain(int)
     */
    @Override
    public Payload retain(final int increment)
    {
        return this;
    }

    /**
     * @see io.rsocket.Payload#sliceData()
     */
    @Override
    public ByteBuf sliceData()
    {
        return Unpooled.wrappedBuffer(this.data);
    }

    /**
     * @see io.rsocket.Payload#sliceMetadata()
     */
    @Override
    public ByteBuf sliceMetadata()
    {
        return this.metadata == null ? Unpooled.EMPTY_BUFFER : Unpooled.wrappedBuffer(this.metadata);
    }

    /**
     * @see io.rsocket.Payload#touch()
     */
    @Override
    public Payload touch()
    {
        return this;
    }

    /**
     * @see io.rsocket.Payload#touch(java.lang.Object)
     */
    @Override
    public Payload touch(final Object hint)
    {
        return this;
    }
}
