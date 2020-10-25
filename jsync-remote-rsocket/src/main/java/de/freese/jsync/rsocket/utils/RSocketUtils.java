// Created: 21.10.2020
package de.freese.jsync.rsocket.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.WritableByteChannel;
import org.reactivestreams.Publisher;
import org.springframework.util.Assert;
import io.rsocket.Payload;
import reactor.core.publisher.Flux;

/**
 * @author Thomas Freese
 */
public final class RSocketUtils
{
    /**
     * @param channel {@link Channel}
     */
    public static void close(final Channel channel)
    {
        // System.out.println("RSocketUtils.close(): " + Thread.currentThread().getName());

        if ((channel != null) && channel.isOpen())
        {
            try
            {
                if (channel instanceof FileChannel)
                {
                    ((FileChannel) channel).force(false);
                }

                channel.close();
            }
            catch (IOException ex)
            {
                throw new UncheckedIOException(ex);
            }
        }
    }

    /**
     * @param payload {@link Payload}
     */
    public static void release(final Payload payload)
    {
        if (payload == null)
        {
            return;
        }

        if (payload.refCnt() > 2)
        {
            payload.release();
        }
        else
        {
            payload.retain();
        }
    }

    /**
     * @param payload {@link Payload}
     * @param channel {@link WritableByteChannel}
     * @return int
     */
    public static int write(final Payload payload, final WritableByteChannel channel)
    {
        ByteBuffer byteBuffer = payload.getData();

        int bytesWritten = 0;

        try
        {
            while (byteBuffer.hasRemaining())
            {
                bytesWritten += channel.write(byteBuffer);
            }
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }

        return bytesWritten;
    }

    /**
     * @param source {@link Publisher}
     * @param channel {@link WritableByteChannel}
     * @return {@link Flux}
     * @see org.springframework.core.io.buffer.DataBufferUtils#write(Publisher, WritableByteChannel)
     */
    public static Flux<ByteBuffer> write(final Publisher<ByteBuffer> source, final WritableByteChannel channel)
    {
        Assert.notNull(source, "'source' must not be null");
        Assert.notNull(channel, "'channel' must not be null");

        Flux<ByteBuffer> flux = Flux.from(source);

        return Flux.create(sink -> {
            WritableByteChannelSubscriber subscriber = new WritableByteChannelSubscriber(sink, channel);
            sink.onDispose(subscriber);
            flux.subscribe(subscriber);
        });
    }

    /**
     * Erstellt ein neues {@link RSocketUtils} Object.
     */
    private RSocketUtils()
    {
        super();
    }
}
