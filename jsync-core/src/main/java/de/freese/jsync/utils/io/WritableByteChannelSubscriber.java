// Created: 20.10.2020
package de.freese.jsync.utils.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

import org.reactivestreams.Subscription;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.FluxSink;

import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;

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
     * @param sink {@link FluxSink} Number of written Bytes for each ByteBuffer/Chunk
     */
    public WritableByteChannelSubscriber(final FluxSink<Long> sink, final Path path) throws IOException {
        this(sink, Files.newByteChannel(path, StandardOpenOption.CREATE, StandardOpenOption.WRITE));
    }

    /**
     * @param sink {@link FluxSink} Number of written Bytes for each ByteBuffer/Chunk
     */
    public WritableByteChannelSubscriber(final FluxSink<Long> sink, final WritableByteChannel channel) {
        super();

        this.sink = sink;
        this.channel = channel;
    }

    // @Override
    // public void onComplete() {
    // sink.complete();
    // }
    //
    // @Override
    // public void onError(final Throwable throwable) {
    // sink.error(throwable);
    // }
    //
    // @Override
    // public void onNext(final ByteBuffer byteBuffer) {
    // try {
    // while (byteBuffer.hasRemaining()) {
    // channel.write(byteBuffer);
    // }
    //
    // sink.next(byteBuffer);
    // subscription.request(1);
    // }
    // catch (IOException ex) {
    // sink.next(byteBuffer);
    // sink.error(ex);
    // }
    // }
    //
    // @Override
    // public void onSubscribe(final Subscription subscription) {
    // this.subscription = subscription;
    //
    // this.subscription.request(1);
    // }

    @Override
    protected void hookOnComplete() {
        sink.complete();
    }

    @Override
    protected void hookOnError(final Throwable throwable) {
        sink.error(throwable);
    }

    @Override
    protected void hookOnNext(final ByteBuffer buffer) {
        try {
            final long limit = buffer.limit();

            while (buffer.hasRemaining()) {
                channel.write(buffer);
            }

            ByteBufferPool.DEFAULT.free(buffer);

            sink.next(limit);
            request(1);
        }
        catch (IOException ex) {
            sink.next(-1L);
            sink.error(ex);
        }
    }

    @Override
    protected void hookOnSubscribe(final Subscription subscription) {
        request(1);
    }
}
