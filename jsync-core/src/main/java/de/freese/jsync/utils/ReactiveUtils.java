// Created: 16.07.2021
package de.freese.jsync.utils;

import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import de.freese.jsync.utils.io.ReadableByteChannelGenerator;
import de.freese.jsync.utils.io.WritableByteChannelSubscriber;
import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;

/**
 * Taken from org.springframework.core.io.buffer.DataBufferUtils.
 *
 * @author Thomas Freese
 */
public final class ReactiveUtils
{
    private static final Consumer<ByteBuffer> RELEASE_CONSUMER = ReactiveUtils::release;

    /**
     * A {@link SocketChannel} will NOT be closed !
     */
    public static void close(final Channel channel)
    {
        JSyncUtils.close(channel);
    }

    /**
     * Read the {@link Channel} as Flux from the {@link ByteBuffer}.<br>
     * The {@link Channel} will close after reading.<br>
     * To release the {@link ByteBuffer}, the return-Flux must be subscribed with {@link #releaseConsumer()}.
     */
    public static Flux<ByteBuffer> readByteChannel(final Callable<ReadableByteChannel> channelSupplier)
    {
        return Flux.using(channelSupplier, channel -> Flux.generate(new ReadableByteChannelGenerator(channel)), ReactiveUtils::close);
    }

    public static void release(final ByteBuffer byteBuffer)
    {
        Objects.requireNonNull(byteBuffer, "byteBuffer required");

        ByteBufferPool.DEFAULT.free(byteBuffer);
    }

    /**
     * Execute for each {@link ByteBuffer} the Method {@link #release(ByteBuffer)}.
     */
    public static Consumer<ByteBuffer> releaseConsumer()
    {
        return RELEASE_CONSUMER;
    }

    /**
     * Writes the source-Publisher in the Channel, the Channel will <strong>NOT</strong> closed.<br>
     * Returns a Flux with the written Bytes for each ByteBuffer/Chunk.<br>
     * The {@link ByteBuffer} will be released in the {@link WritableByteChannelSubscriber}.
     */
    public static Flux<Long> write(final Publisher<ByteBuffer> source, final WritableByteChannel channel)
    {
        Flux<ByteBuffer> flux = Flux.from(source);

        return Flux.create(sink ->
        {
            WritableByteChannelSubscriber subscriber = new WritableByteChannelSubscriber(sink, channel);
            sink.onDispose(subscriber);
            flux.subscribe(subscriber);
        });
    }

    private ReactiveUtils()
    {
        super();
    }
}
