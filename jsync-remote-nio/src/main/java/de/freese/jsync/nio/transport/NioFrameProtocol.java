// Created: 22.08.2021
package de.freese.jsync.nio.transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;
import reactor.core.publisher.Flux;

/**
 * Abstraktion der NIO-Kommunikation mittels Frames.<br>
 * Siehe <a href="https://github.com/rsocket/rsocket/blob/master/Protocol.md">rSocket-Protocol</a>
 *
 * @author Thomas Freese
 */
public class NioFrameProtocol
{
    /**
     * Default: 4 MB
     */
    protected static final int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 4;

    private final ByteBufferPool bufferPool;

    public NioFrameProtocol()
    {
        this(ByteBufferPool.DEFAULT);
    }

    public NioFrameProtocol(final ByteBufferPool bufferPool)
    {
        super();

        this.bufferPool = Objects.requireNonNull(bufferPool, "bufferPool required");
    }

    public ByteBufferPool getBufferPool()
    {
        return this.bufferPool;
    }

    /**
     * Lesen aller Frames bis zum FINISH-Frame.<br>
     * Diese können nach der Verarbeitung wieder in den {@link ByteBufferPool}.
     */
    public Flux<ByteBuffer> readAll(final ReadableByteChannel channel)
    {
        return Flux.create(sink ->
        {
            try
            {
                readAll(channel, sink::next);
            }
            catch (Exception ex)
            {
                sink.error(ex);
            }
            finally
            {
                sink.complete();
            }
        });
    }

    /**
     * Lesen aller Frames bis zum FINISH-Frame.<br>
     * Diese können nach der Verarbeitung wieder in den {@link ByteBufferPool}.
     */
    public void readAll(final ReadableByteChannel channel, final Consumer<ByteBuffer> consumer) throws Exception
    {
        while (true)
        {
            ByteBuffer buffer = readFrame(channel);

            if (buffer == null)
            {
                // FINISH-Frame
                break;
            }

            consumer.accept(buffer);
        }
    }

    /**
     * DATA-Frame liefert den {@link ByteBuffer} des Contents, dieser kann nach der Verarbeitung wieder in den {@link ByteBufferPool}.<br>
     * ERROR-Frame wirft eine Exception.<br>
     * FINISH-Frame liefert null.
     */
    public ByteBuffer readFrame(final ReadableByteChannel channel) throws Exception
    {
        // Header lesen
        ByteBuffer buffer = readFrameHeader(channel);

        FrameType frameType = FrameType.fromEncodedType(buffer.getInt());
        int contentLength = buffer.getInt();

        getBufferPool().free(buffer);

        // Content lesen
        buffer = getBufferPool().get();

        if (FrameType.DATA.equals(frameType))
        {
            read(channel, buffer, contentLength);

            return buffer.flip();
        }
        else if (FrameType.ERROR.equals(frameType))
        {
            read(channel, buffer, contentLength);
            buffer.flip();

            byte[] bytes = new byte[contentLength];
            buffer.get(bytes);
            String message = new String(bytes, StandardCharsets.UTF_8);

            getBufferPool().free(buffer);

            throw new Exception(message);
        }

        // FINISH-Frame
        getBufferPool().free(buffer);

        return null;
    }

    /**
     * DATA-Frame schreiben.
     */
    public void writeData(final WritableByteChannel channel, final ByteBuffer buffer) throws IOException
    {
        int contentLength = 0;

        if (buffer.position() == 0)
        {
            contentLength = buffer.limit();
        }
        else
        {
            contentLength = buffer.position();
        }

        writeFrameHeader(channel, FrameType.DATA, contentLength);
        write(channel, buffer);
    }

    /**
     * DATA-Frame schreiben.
     */
    public void writeData(final WritableByteChannel channel, final Consumer<ByteBuffer> consumer) throws IOException
    {
        ByteBuffer buffer = getBufferPool().get();

        try
        {
            consumer.accept(buffer);

            writeData(channel, buffer);
        }
        finally
        {
            getBufferPool().free(buffer);
        }
    }

    /**
     * ERROR-Frame schreiben.
     */
    public void writeError(final WritableByteChannel channel, final Consumer<ByteBuffer> consumer) throws IOException
    {
        ByteBuffer buffer = getBufferPool().get();

        try
        {
            consumer.accept(buffer);

            int contentLength = 0;

            if (buffer.position() == 0)
            {
                contentLength = buffer.limit();
            }
            else
            {
                contentLength = buffer.position();
            }

            writeFrameHeader(channel, FrameType.ERROR, contentLength);
            write(channel, buffer);
        }
        finally
        {
            getBufferPool().free(buffer);
        }
    }

    /**
     * ERROR-Frame schreiben.
     */
    public void writeError(final WritableByteChannel channel, final Throwable th) throws IOException
    {
        writeError(channel, buffer ->
        {
            String message = th.getMessage() == null ? "" : th.getMessage();
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            buffer.put(messageBytes);
        });
    }

    /**
     * FINISH-Frame schreiben.
     */
    public void writeFinish(final WritableByteChannel channel) throws IOException
    {
        writeFrameHeader(channel, FrameType.FINISH, 0);
    }

    /**
     * Garantiert das alle Daten aus dem Channel gelesen werden wie angefordert.
     */
    protected void read(final ReadableByteChannel channel, final ByteBuffer buffer, final int contentLength) throws IOException
    {
        // Der übergebene Buffer kann größer sein als benötigt.
        ByteBuffer bb = buffer.slice(0, contentLength);

        int totalRead = channel.read(bb);

        while (totalRead < contentLength)
        {
            int bytesRead = channel.read(bb);
            totalRead += bytesRead;
        }

        buffer.position(bb.position());
        buffer.limit(bb.limit());
    }

    protected ByteBuffer readFrameHeader(final ReadableByteChannel channel) throws IOException
    {
        ByteBuffer buffer = getBufferPool().get();

        read(channel, buffer, 8);

        return buffer.flip();
    }

    protected long write(final WritableByteChannel channel, final ByteBuffer buffer) throws IOException
    {
        // for (ByteBuffer buffer : buffers)
        // {
        // if (buffer.position() > 0)
        // // if (buffer.remaining() != buffer.limit())
        // {
        // buffer.flip();
        // }
        // }
        //
        // return channel.write(buffers);

        if (buffer.position() > 0)
        // if (buffer.remaining() != buffer.limit())
        {
            buffer.flip();
        }

        long totalWritten = 0;

        while (buffer.hasRemaining())
        {
            long bytesWritten = channel.write(buffer);

            totalWritten += bytesWritten;
        }

        return totalWritten;
    }

    protected void writeFrameHeader(final WritableByteChannel channel, final FrameType frameType, final int contentLength) throws IOException
    {
        ByteBuffer buffer = getBufferPool().get();

        try
        {
            buffer.putInt(frameType.getEncodedType());
            buffer.putInt(contentLength);

            write(channel, buffer.flip());
        }
        finally
        {
            getBufferPool().free(buffer);
        }
    }
}
