// Created: 15.07.2021
package de.freese.jsync.utils.io;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.util.Objects;
import java.util.function.Consumer;

import de.freese.jsync.utils.pool.ByteBufferPool;
import reactor.core.publisher.SynchronousSink;

/**
 * @author Thomas Freese
 *
 * @see de.freese.jsync.utils.io.springframework.core.io.buffer.DataBufferUtils.ReadableByteChannelGenerator
 */
@SuppressWarnings("javadoc")
public class ReadableByteChannelGenerator implements Consumer<SynchronousSink<ByteBuffer>>
{
    /**
     *
     */
    private final ReadableByteChannel channel;

    /**
     * Erstellt ein neues {@link ReadableByteChannelGenerator} Object.
     *
     * @param channel {@link ReadableByteChannel}
     */
    public ReadableByteChannelGenerator(final ReadableByteChannel channel)
    {
        this.channel = Objects.requireNonNull(channel, "channel requried");
    }

    /**
     * @see java.util.function.Consumer#accept(java.lang.Object)
     */
    @Override
    public void accept(final SynchronousSink<ByteBuffer> sink)
    {
        boolean release = true;
        ByteBuffer byteBuffer = ByteBufferPool.getInstance().obtain();

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
                ByteBufferPool.getInstance().free(byteBuffer);
            }
        }
    }
}
