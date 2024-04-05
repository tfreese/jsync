// Created: 07.08.2021
package de.freese.jsync.rsocket.payload;

import java.nio.ByteBuffer;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.util.DefaultPayload;
import reactor.util.annotation.Nullable;

import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.impl.ByteBufferAdapter;
import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;

/**
 * {@link DefaultPayload} with a {@link ByteBufferPool}.
 *
 * @author Thomas Freese
 * @deprecated Does not work
 */
@Deprecated
final class JSyncPayload implements Payload {
    private static final ByteBufferPool BYTE_BUFFER_POOL = ByteBufferPool.DEFAULT;
    private static final Serializer<ByteBuffer, ByteBuffer> SERIALIZER = DefaultSerializer.of(new ByteBufferAdapter());

    public static Payload create(final ByteBuffer data) {
        return create(data, null);
    }

    public static Payload create(final ByteBuffer data, @Nullable final ByteBuffer metadata) {
        return new JSyncPayload(data, metadata);
    }

    public static Payload create(final CharSequence data) {
        final ByteBuffer buffer = BYTE_BUFFER_POOL.get();
        SERIALIZER.writeTo(buffer, data.toString());
        buffer.flip();

        return create(buffer);
    }

    private final ByteBuffer data;
    private final ByteBuffer metadata;

    private JSyncPayload(final ByteBuffer data, final ByteBuffer metadata) {
        super();

        this.data = data;
        this.metadata = metadata;
    }

    @Override
    public ByteBuf data() {
        return sliceData();
    }

    @Override
    public ByteBuffer getData() {
        return this.data.duplicate();
    }

    @Override
    public ByteBuffer getMetadata() {
        return this.metadata == null ? DefaultPayload.EMPTY_BUFFER : this.metadata.duplicate();
    }

    @Override
    public boolean hasMetadata() {
        return this.metadata != null;
    }

    @Override
    public ByteBuf metadata() {
        return sliceMetadata();
    }

    @Override
    public int refCnt() {
        return 1;
    }

    @Override
    public boolean release() {
        if (this.data != DefaultPayload.EMPTY_BUFFER) {
            BYTE_BUFFER_POOL.free(this.data);
        }

        if (this.metadata != null && this.metadata != DefaultPayload.EMPTY_BUFFER) {
            BYTE_BUFFER_POOL.free(this.metadata);
        }

        return false;
    }

    @Override
    public boolean release(final int decrement) {
        return false;
    }

    @Override
    public Payload retain() {
        return this;
    }

    @Override
    public Payload retain(final int increment) {
        return this;
    }

    @Override
    public ByteBuf sliceData() {
        return Unpooled.wrappedBuffer(this.data);
    }

    @Override
    public ByteBuf sliceMetadata() {
        return this.metadata == null ? Unpooled.EMPTY_BUFFER : Unpooled.wrappedBuffer(this.metadata);
    }

    @Override
    public Payload touch() {
        return this;
    }

    @Override
    public Payload touch(final Object hint) {
        return this;
    }
}
