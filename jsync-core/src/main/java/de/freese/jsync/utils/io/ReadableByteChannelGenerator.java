// Created: 15.07.2021
package de.freese.jsync.utils.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;
import java.util.function.Consumer;

import reactor.core.publisher.SynchronousSink;

import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;

/**
 * @author Thomas Freese
 * @see org.springframework.core.io.buffer.DataBufferUtils.ReadableByteChannelGenerator
 */
public class ReadableByteChannelGenerator implements Consumer<SynchronousSink<ByteBuffer>> {
    private final ReadableByteChannel channel;

    public ReadableByteChannelGenerator(final ReadableByteChannel channel) {
        super();

        this.channel = Objects.requireNonNull(channel, "channel required");
    }

    @Override
    public void accept(final SynchronousSink<ByteBuffer> sink) {
        boolean release = true;
        final ByteBuffer buffer = ByteBufferPool.DEFAULT.get();

        try {
            if (channel.read(buffer) >= 0) {
                release = false;
                buffer.flip();
                sink.next(buffer);
            }
            else {
                sink.complete();
            }
        }
        catch (IOException ex) {
            sink.error(ex);
        }
        finally {
            if (release) {
                ByteBufferPool.DEFAULT.free(buffer);
            }
        }
    }
}
