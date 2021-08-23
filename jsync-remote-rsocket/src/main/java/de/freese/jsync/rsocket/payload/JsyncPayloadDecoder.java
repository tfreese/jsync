// Created: 07.08.2021
package de.freese.jsync.rsocket.payload;

import java.nio.ByteBuffer;

import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.rsocket.Payload;
import io.rsocket.frame.FrameHeaderCodec;
import io.rsocket.frame.FrameType;
import io.rsocket.frame.MetadataPushFrameCodec;
import io.rsocket.frame.PayloadFrameCodec;
import io.rsocket.frame.RequestChannelFrameCodec;
import io.rsocket.frame.RequestFireAndForgetFrameCodec;
import io.rsocket.frame.RequestResponseFrameCodec;
import io.rsocket.frame.RequestStreamFrameCodec;
import io.rsocket.frame.decoder.PayloadDecoder;

/**
 * DefaultPayloadDecoder mit Verwendung des {@link ByteBufferPool}.
 *
 * @author Thomas Freese
 *
 * @deprecated Funktioniert nicht
 */
@Deprecated
class JsyncPayloadDecoder implements PayloadDecoder
{
    /**
    *
    */
    private final ByteBufferPool byteBufferPool = ByteBufferPool.DEFAULT;

    /**
     * @see java.util.function.Function#apply(java.lang.Object)
     */
    @Override
    public Payload apply(final ByteBuf byteBuf)
    {
        ByteBuf m;
        ByteBuf d;

        FrameType type = FrameHeaderCodec.frameType(byteBuf);

        switch (type)
        {
            case REQUEST_FNF ->
            {
                d = RequestFireAndForgetFrameCodec.data(byteBuf);
                m = RequestFireAndForgetFrameCodec.metadata(byteBuf);
            }
            case REQUEST_RESPONSE ->
            {
                d = RequestResponseFrameCodec.data(byteBuf);
                m = RequestResponseFrameCodec.metadata(byteBuf);
            }
            case REQUEST_STREAM ->
            {
                d = RequestStreamFrameCodec.data(byteBuf);
                m = RequestStreamFrameCodec.metadata(byteBuf);
            }
            case REQUEST_CHANNEL ->
            {
                d = RequestChannelFrameCodec.data(byteBuf);
                m = RequestChannelFrameCodec.metadata(byteBuf);
            }
            case NEXT, NEXT_COMPLETE ->
            {
                d = PayloadFrameCodec.data(byteBuf);
                m = PayloadFrameCodec.metadata(byteBuf);
            }
            case METADATA_PUSH ->
            {
                d = Unpooled.EMPTY_BUFFER;
                m = MetadataPushFrameCodec.metadata(byteBuf);
            }
            default -> throw new IllegalArgumentException("unsupported frame type: " + type);
        }

        ByteBuffer data = this.byteBufferPool.get(d.readableBytes());
        data.put(d.nioBuffer());
        data.flip();

        if (m != null)
        {
            ByteBuffer metadata = this.byteBufferPool.get(m.readableBytes());
            metadata.put(m.nioBuffer());
            metadata.flip();

            return JsyncPayload.create(data, metadata);
        }

        return JsyncPayload.create(data);
    }
}
