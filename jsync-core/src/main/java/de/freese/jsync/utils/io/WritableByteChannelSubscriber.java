// Created: 20.10.2020
package de.freese.jsync.utils.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.FluxSink;

/**
 * Geklaut von org.springframework.core.io.buffer.DataBufferUtils#WritableByteChannelSubscriber
 *
 * @author Thomas Freese
 */
public class WritableByteChannelSubscriber extends BaseSubscriber<ByteBuffer> // implements Subscriber<ByteBuffer>
{
    private final WritableByteChannel channel;

    private final FluxSink<Long> sink;

    // private Subscription subscription;

    /**
     * @param sink {@link FluxSink} Geschriebene Bytes pro ByteBuffer/Chunk
     */
    public WritableByteChannelSubscriber(final FluxSink<Long> sink, final Path path) throws IOException
    {
        this(sink, Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE));
    }

    /**
     * @param sink {@link FluxSink} Geschriebene Bytes pro ByteBuffer/Chunk
     */
    public WritableByteChannelSubscriber(final FluxSink<Long> sink, final WritableByteChannel channel)
    {
        super();

        this.sink = sink;
        this.channel = channel;
    }

    // /**
    // * @see org.reactivestreams.Subscriber#onComplete()
    // */
    // @Override
    // public void onComplete()
    // {
    // this.sink.complete();
    // }
    //
    // /**
    // * @see org.reactivestreams.Subscriber#onError(java.lang.Throwable)
    // */
    // @Override
    // public void onError(final Throwable throwable)
    // {
    // this.sink.error(throwable);
    // }
    //
    // /**
    // * @see org.reactivestreams.Subscriber#onNext(java.lang.Object)
    // */
    // @Override
    // public void onNext(final ByteBuffer byteBuffer)
    // {
    // try
    // {
    // while (byteBuffer.hasRemaining())
    // {
    // this.channel.write(byteBuffer);
    // }
    //
    // this.sink.next(byteBuffer);
    // this.subscription.request(1);
    // }
    // catch (IOException ex)
    // {
    // this.sink.next(byteBuffer);
    // this.sink.error(ex);
    // }
    // }
    //
    // /**
    // * @see org.reactivestreams.Subscriber#onSubscribe(org.reactivestreams.Subscription)
    // */
    // @Override
    // public void onSubscribe(final Subscription subscription)
    // {
    // this.subscription = subscription;
    //
    // this.subscription.request(1);
    // }

    /**
     * @see reactor.core.publisher.BaseSubscriber#hookOnComplete()
     */
    @Override
    protected void hookOnComplete()
    {
        this.sink.complete();
    }

    /**
     * @see reactor.core.publisher.BaseSubscriber#hookOnError(java.lang.Throwable)
     */
    @Override
    protected void hookOnError(final Throwable throwable)
    {
        this.sink.error(throwable);
    }

    @Override
    protected void hookOnNext(final ByteBuffer buffer)
    {
        try
        {
            long limit = buffer.limit();

            while (buffer.hasRemaining())
            {
                this.channel.write(buffer);
            }

            ByteBufferPool.DEFAULT.free(buffer);

            this.sink.next(limit);
            request(1);
        }
        catch (IOException ex)
        {
            this.sink.next(-1L);
            this.sink.error(ex);
        }
    }

    /**
     * @see reactor.core.publisher.BaseSubscriber#hookOnSubscribe(org.reactivestreams.Subscription)
     */
    @Override
    protected void hookOnSubscribe(final Subscription subscription)
    {
        request(1);
    }
}
