// Created: 20.10.2020
package de.freese.jsync.rsocket.utils;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.FluxSink;

/**
 * GRklaut von org.springframework.core.io.buffer.DataBufferUtils#WritableByteChannelSubscriber
 *
 * @author Thomas Freese
 */
public class WritableByteChannelSubscriber extends BaseSubscriber<ByteBuffer> // implements Subscriber<ByteBuffer>
{
    /**
     *
     */
    private final WritableByteChannel channel;

    /**
     *
     */
    private final FluxSink<ByteBuffer> sink;

    // /**
    // *
    // */
    // private Subscription subscription;

    /**
     * Erstellt ein neues {@link WritableByteChannelSubscriber} Object.
     *
     * @param sink {@link FluxSink}
     * @param path {@link Path}
     * @throws IOException Falls was schief geht.
     */
    public WritableByteChannelSubscriber(final FluxSink<ByteBuffer> sink, final Path path) throws IOException
    {
        this(sink, Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE));
    }

    /**
     * Erstellt ein neues {@link WritableByteChannelSubscriber} Object.
     *
     * @param sink {@link FluxSink}
     * @param channel {@link WritableByteChannel}
     */
    public WritableByteChannelSubscriber(final FluxSink<ByteBuffer> sink, final WritableByteChannel channel)
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

    /**
     * @param byteBuffer {@link ByteBuffer}
     */
    @Override
    protected void hookOnNext(final ByteBuffer byteBuffer)
    {
        try
        {
            while (byteBuffer.hasRemaining())
            {
                this.channel.write(byteBuffer);
            }

            this.sink.next(byteBuffer);
            request(1);
        }
        catch (IOException ex)
        {
            this.sink.next(byteBuffer);
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
