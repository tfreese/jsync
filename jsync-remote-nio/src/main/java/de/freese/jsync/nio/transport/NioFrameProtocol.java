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
 * Siehe <a href="https://github.com/rsocket/rsocket/blob/master/Protocol.md">rsocket-Protocol</a>
 *
 * @author Thomas Freese
 */
public class NioFrameProtocol
{
    /**
     * Default: 4 MB
     */
    protected static final int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 4;
    /**
     *
     */
    private final ByteBufferPool bufferPool;

    /**
     * Erstellt ein neues {@link NioFrameProtocol} Object.
     */
    public NioFrameProtocol()
    {
        this(ByteBufferPool.DEFAULT);
    }

    /**
     * Erstellt ein neues {@link NioFrameProtocol} Object.
     *
     * @param bufferPool {@link ByteBufferPool}
     */
    public NioFrameProtocol(final ByteBufferPool bufferPool)
    {
        super();

        this.bufferPool = Objects.requireNonNull(bufferPool, "bufferPool required");
    }

    /**
     * @return {@link ByteBufferPool}
     */
    public ByteBufferPool getBufferPool()
    {
        return this.bufferPool;
    }

    /**
     * Lesen aller Frames bis zum FINISH-Frame.<br>
     * Diese können nach der Verarbeitung wieder in den {@link ByteBufferPool}.
     *
     * @param channel {@link ReadableByteChannel}
     *
     * @return {@link Flux}
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
     *
     * @param channel {@link ReadableByteChannel}
     * @param consumer {@link Consumer}
     *
     * @throws Exception Falls was schiefgeht
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
     *
     * @param channel {@link ReadableByteChannel}
     *
     * @return {@link ByteBuffer}
     *
     * @throws Exception Falls was schiefgeht
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
     *
     * @param channel {@link WritableByteChannel}
     * @param buffer {@link ByteBuffer}
     *
     * @throws IOException Falls was schiefgeht.
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
     *
     * @param channel {@link WritableByteChannel}
     * @param consumer {@link Consumer}
     *
     * @throws IOException Falls was schiefgeht.
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
     *
     * @param channel {@link WritableByteChannel}
     * @param consumer {@link Consumer}
     *
     * @throws IOException Falls was schiefgeht.
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
     *
     * @param channel {@link WritableByteChannel}
     * @param th {@link Throwable}
     *
     * @throws IOException Falls was schiefgeht.
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
     *
     * @param channel {@link WritableByteChannel}
     *
     * @throws IOException Falls was schiefgeht.
     */
    public void writeFinish(final WritableByteChannel channel) throws IOException
    {
        writeFrameHeader(channel, FrameType.FINISH, 0);
    }

    /**
     * Garantiert das alle Daten aus dem Channel gelesen werden wie angefordert.
     *
     * @param channel {@link ReadableByteChannel}
     * @param buffer {@link ByteBuffer}
     * @param contentLength int
     *
     * @throws IOException Falls was schiefgeht.
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

    /**
     * @param channel {@link ReadableByteChannel}
     *
     * @return {@link ByteBuffer}
     *
     * @throws IOException Falls was schiefgeht.
     */
    protected ByteBuffer readFrameHeader(final ReadableByteChannel channel) throws IOException
    {
        ByteBuffer buffer = getBufferPool().get();

        read(channel, buffer, 8);

        return buffer.flip();
    }

    /**
     * @param channel {@link WritableByteChannel}
     * @param buffer {@link ByteBuffer}
     *
     * @return long
     *
     * @throws IOException Falls was schiefgeht
     */
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

    /**
     * @param channel {@link WritableByteChannel}
     * @param frameType {@link FrameType}
     * @param contentLength int
     *
     * @throws IOException Falls was schiefgeht.
     */
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
