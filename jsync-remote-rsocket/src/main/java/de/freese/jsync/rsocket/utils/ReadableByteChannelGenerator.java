// Created: 15.07.2021
package de.freese.jsync.rsocket.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.function.Consumer;

import de.freese.jsync.utils.pool.ByteBufferPool;
import io.netty.buffer.ByteBufAllocator;
import reactor.core.publisher.SynchronousSink;

/**
 * @author Thomas Freese
 *
 * @see org.springframework.core.io.buffer.DataBufferUtils.ReadableByteChannelGenerator
 */
@SuppressWarnings("javadoc")
public class ReadableByteChannelGenerator implements Consumer<SynchronousSink<ByteBuffer>>
{
    /**
     *
     */
    private final int bufferSize;

    /**
     *
     */
    private final ByteBufAllocator byteBufAllocator;

    /**
     *
     */
    private final ReadableByteChannel channel;

    /**
     * Erstellt ein neues {@link ReadableByteChannelGenerator} Object.
     *
     * @param channel {@link ReadableByteChannel}
     * @param byteBufAllocator {@link ByteBufAllocator}
     * @param bufferSize int
     */
    public ReadableByteChannelGenerator(final ReadableByteChannel channel, final ByteBufAllocator byteBufAllocator, final int bufferSize)
    {

        this.channel = channel;
        this.byteBufAllocator = byteBufAllocator;
        this.bufferSize = bufferSize;
    }

    /**
     * @see java.util.function.Consumer#accept(java.lang.Object)
     */
    @Override
    public void accept(final SynchronousSink<ByteBuffer> sink)
    {
        boolean release = true;
        ByteBuffer byteBuffer = ByteBufferPool.getInstance().allocate();

        try
        {
            if (this.channel.read(byteBuffer) >= 0)
            {
                release = false;
                byteBuffer.flip();
                sink.next(byteBuffer);
            }
            else
            {
                sink.complete();
            }
        }
        catch (IOException ex)
        {
            sink.error(ex);
        }
        finally
        {
            if (release)
            {
                ByteBufferPool.getInstance().release(byteBuffer);
            }
        }
    }
}
