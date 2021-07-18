// Created: 16.07.2021
package de.freese.jsync.utils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.util.Objects;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import org.reactivestreams.Publisher;

import de.freese.jsync.utils.io.ReadableByteChannelGenerator;
import de.freese.jsync.utils.io.WritableByteChannelSubscriber;
import de.freese.jsync.utils.pool.ByteBufferPool;
import reactor.core.publisher.Flux;

/**
 * Geklaut von org.springframework.core.io.buffer.DataBufferUtils.
 *
 * @author Thomas Freese
 */
public final class ReactiveUtils
{
    /**
    *
    */
    private static final Consumer<ByteBuffer> RELEASE_CONSUMER = ReactiveUtils::release;

    /**
     * SocketChannels werden NICHT geschlossen !
     *
     * @param channel {@link Channel}
     */
    public static void close(final Channel channel)
    {
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
     * List den {@link Channel} als Flux von {@link ByteBuffer}.<br>
     * Der {@link Channel} wird im Anschluss geschlossen.<br>
     * Sollen die {@link ByteBuffer} freigegeben werden, muss der return-Flux mit {@link #releaseConsumer()} subscribed werden.
     *
     * @param channelSupplier {@link Callable}
     *
     * @return {@link Flux}
     */
    public static Flux<ByteBuffer> readByteChannel(final Callable<ReadableByteChannel> channelSupplier)
    {
        return Flux.using(channelSupplier, channel -> Flux.generate(new ReadableByteChannelGenerator(channel)), ReactiveUtils::close);
    }

    /**
     * @param byteBuffer {@link ByteBuffer}
     */
    public static void release(final ByteBuffer byteBuffer)
    {
        Objects.requireNonNull(byteBuffer, "byteBuffer required");

        ByteBufferPool.getInstance().free(byteBuffer);
    }

    /**
     * Führt auf jedem {@link ByteBuffer} die Methode {@link #release(ByteBuffer)} aus.
     *
     * @return {@link Consumer}
     */
    public static Consumer<ByteBuffer> releaseConsumer()
    {
        return RELEASE_CONSUMER;
    }

    /**
     * Schreibt den source-Publisher in den Channel, dieser wird danach <strong>nicht</strong> geschlossen.<br>
     * Sollen die {@link ByteBuffer} freigegeben werden, muss der return-Flux mit {@link #releaseConsumer()} subscribed werden.
     *
     * @param source {@link Publisher}
     * @param channel {@link WritableByteChannel}
     *
     * @return {@link Flux}
     */
    public static Flux<ByteBuffer> write(final Publisher<ByteBuffer> source, final WritableByteChannel channel)
    {
        Flux<ByteBuffer> flux = Flux.from(source);

        return Flux.create(sink -> {
            WritableByteChannelSubscriber subscriber = new WritableByteChannelSubscriber(sink, channel);
            sink.onDispose(subscriber);
            flux.subscribe(subscriber);
        });
    }

    /**
     * Erstellt ein neues {@link ReactiveUtils} Object.
     */
    private ReactiveUtils()
    {
        super();
    }
}
