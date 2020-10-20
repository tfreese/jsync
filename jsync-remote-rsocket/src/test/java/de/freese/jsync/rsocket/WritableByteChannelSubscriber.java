// Created: 20.10.2020
package de.freese.jsync.rsocket;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;

/**
 * @author Thomas Freese
 */
public class WritableByteChannelSubscriber implements Subscriber<ByteBuffer>
{
    /**
     *
     */
    private final WritableByteChannel channel;

    /**
     *
     */
    private Subscription subscription;

    /**
     * Erstellt ein neues {@link WritableByteChannelSubscriber} Object.
     *
     * @param path {@link Path}
     * @throws IOException Falls was schief geht.
     */
    public WritableByteChannelSubscriber(final Path path) throws IOException
    {
        this(Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE));
    }

    /**
     * Erstellt ein neues {@link WritableByteChannelSubscriber} Object.
     *
     * @param channel {@link WritableByteChannel}
     */
    public WritableByteChannelSubscriber(final WritableByteChannel channel)
    {
        super();

        this.channel = channel;
    }

    /**
     * @see org.reactivestreams.Subscriber#onComplete()
     */
    @Override
    public void onComplete()
    {
        try
        {
            this.channel.close();
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @see org.reactivestreams.Subscriber#onError(java.lang.Throwable)
     */
    @Override
    public void onError(final Throwable throwable)
    {
        if (throwable instanceof RuntimeException)
        {
            throw (RuntimeException) throwable;
        }

        throw new RuntimeException(throwable);
    }

    /**
     * @see org.reactivestreams.Subscriber#onNext(java.lang.Object)
     */
    @Override
    public void onNext(final ByteBuffer byteBuffer)
    {
        try
        {
            while (byteBuffer.hasRemaining())
            {
                this.channel.write(byteBuffer);
            }

            this.subscription.request(1);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @see org.reactivestreams.Subscriber#onSubscribe(org.reactivestreams.Subscription)
     */
    @Override
    public void onSubscribe(final Subscription subscription)
    {
        this.subscription = subscription;

        this.subscription.request(1);
    }
}
