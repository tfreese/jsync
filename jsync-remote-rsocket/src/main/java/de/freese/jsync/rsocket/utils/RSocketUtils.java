// Created: 21.10.2020
package de.freese.jsync.rsocket.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.PooledDataBuffer;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

import io.netty.buffer.ByteBufAllocator;
import io.rsocket.Payload;
import reactor.core.publisher.Flux;

/**
 * @author Thomas Freese
 */
public final class RSocketUtils
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketUtils.class);

    /**
     *
     */
    private static final Consumer<DataBuffer> RELEASE_CONSUMER = RSocketUtils::release;

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
     * Obtain a {@link ReadableByteChannel} from the given supplier, and read it into a {@code Flux} of {@code DataBuffer}s.<br>
     * Closes the channel when the Flux is terminated.
     *
     * @param channelSupplier the supplier for the channel to read from
     * @param bufferFactory the factory to create data buffers with
     * @param bufferSize the maximum size of the data buffers
     *
     * @return a Flux of data buffers read from the given channel
     *
     * @see org.springframework.core.io.buffer.DataBufferUtils.readByteChannel(Callable<ReadableByteChannel>, DataBufferFactory, int)
     */
    @SuppressWarnings("javadoc")
    public static Flux<ByteBuffer> readByteChannel(final Callable<ReadableByteChannel> channelSupplier, final ByteBufAllocator byteBufAllocator,
                                                   final int bufferSize)
    {
        Assert.notNull(channelSupplier, "'channelSupplier' must not be null");
        Assert.notNull(byteBufAllocator, "'byteBufAllocator' must not be null");
        Assert.isTrue(bufferSize > 0, "'bufferSize' must be > 0");

        return Flux.using(channelSupplier, channel -> Flux.generate(new ReadableByteChannelGenerator(channel, byteBufAllocator, bufferSize)),
                RSocketUtils::close);

        // No doOnDiscard as operators used do not cache
    }

    /**
     * Release the given data buffer, if it is a {@link PooledDataBuffer} and has been {@linkplain PooledDataBuffer#isAllocated() allocated}.
     *
     * @param dataBuffer the data buffer to release
     *
     * @return {@code true} if the buffer was released; {@code false} otherwise.
     */
    public static boolean release(@Nullable final DataBuffer dataBuffer)
    {
        if (dataBuffer instanceof PooledDataBuffer pooledDataBuffer)
        {
            if (pooledDataBuffer.isAllocated())
            {
                try
                {
                    return pooledDataBuffer.release();
                }
                catch (IllegalStateException ex)
                {
                    // Avoid dependency on Netty: IllegalReferenceCountException
                    if (LOGGER.isDebugEnabled())
                    {
                        LOGGER.debug("Failed to release PooledDataBuffer: " + dataBuffer, ex);
                    }
                }
            }
        }

        return false;
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
     * Return a consumer that calls {@link #release(DataBuffer)} on all passed data buffers.
     *
     * @return {@link Consumer}
     */
    public static Consumer<DataBuffer> releaseConsumer()
    {
        return RELEASE_CONSUMER;
    }

    /**
     * @param payload {@link Payload}
     * @param channel {@link WritableByteChannel}
     *
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
     * Write the given stream of {@link DataBuffer DataBuffers} to the given {@code WritableByteChannel}.<br>
     * Does <strong>not</strong> close the channel when the flux is terminated, and does <strong>not</strong> {@linkplain #release(DataBuffer) release} the data
     * buffers in the source.<br>
     * If releasing is required, then subscribe to the returned {@code Flux} with a {@link #releaseConsumer()}.
     * <p>
     * Note that the writing process does not start until the returned {@code Flux} is subscribed to.
     *
     * @param source the stream of data buffers to be written
     * @param channel the channel to write to
     *
     * @return a Flux containing the same buffers as in {@code source}, that starts the writing process when subscribed to, and that publishes any writing
     *         errors and the completion signal
     *
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
