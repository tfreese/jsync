// Created: 20.08.2021
package de.freese.jsync.nio.transport;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

import de.freese.jsync.nio.transport.pool.ByteBufferPool;
import de.freese.jsync.nio.transport.pool.DefaultByteBufferPool;

/**
 * Abstraktion der NIO-Kommunikation mittles Frames.<br>
 * Siehe <a href="https://github.com/rsocket/rsocket/blob/master/Protocol.md">rsocket-Protocol</a>
 *
 * @author Thomas Freese
 */
public class NioTransport
{
    /**
     * Default: 4 MB
     */
    private static final int DEFAULT_BUFFER_SIZE = 1024 * 1024 * 4;

    /**
     *
     */
    private final ByteBufferPool bufferPool;

    /**
     * Erstellt ein neues {@link NioTransport} Object.
     */
    public NioTransport()
    {
        this(new DefaultByteBufferPool());
    }

    /**
     * Erstellt ein neues {@link NioTransport} Object.
     *
     * @param bufferPool {@link ByteBufferPool}
     */
    public NioTransport(final ByteBufferPool bufferPool)
    {
        super();

        this.bufferPool = Objects.requireNonNull(bufferPool, "bufferPool required");
    }

    /**
     * @return {@link ByteBufferPool}
     */
    protected ByteBufferPool getBufferPool()
    {
        return this.bufferPool;
    }

    /**
     * Garantiert das alle Daten aus dem Channel gelesen werden wie angefordert.
     *
     * @param channel {@link SocketChannel}
     * @param buffer {@link ByteBuffer}
     * @param contentLength int
     *
     * @throws IOException Falls was schief geht.
     */
    protected void read(final SocketChannel channel, final ByteBuffer buffer, final int contentLength) throws IOException
    {
        int totalRead = channel.read(buffer);

        while (totalRead < contentLength)
        {
            int bytesRead = channel.read(buffer);
            totalRead += bytesRead;
        }
    }

    /**
     * Lesen aller Frames bis zum FINISH-Frame.
     *
     * @param channel {@link SocketChannel}
     * @param consumer {@link Consumer}
     *
     * @throws Exception Falls was schief geht
     */
    public void readAll(final SocketChannel channel, final Consumer<ByteBuffer> consumer) throws Exception
    {
        ByteBuffer buffer = getBufferPool().get(DEFAULT_BUFFER_SIZE);

        try
        {
            while (true)
            {
                buffer.clear();

                // Lesen bis FINISH-Frame.
                if (!readFrame(channel, buffer, consumer))
                {
                    break;
                }
            }
        }
        finally
        {
            getBufferPool().free(buffer);
        }
    }

    /**
     * @param channel {@link SocketChannel}
     * @param buffer {@link ByteBuffer}
     * @param consumer {@link Consumer}
     *
     * @return boolean
     *
     * @throws Exception Falls was schief geht
     */
    protected boolean readFrame(final SocketChannel channel, final ByteBuffer buffer, final Consumer<ByteBuffer> consumer) throws Exception
    {
        // Header lesen
        ByteBuffer bufferHeader = buffer.slice(0, 8);
        read(channel, bufferHeader, 8);
        bufferHeader.flip();

        FrameType frameType = FrameType.fromEncodedType(bufferHeader.getInt());
        int contentLength = bufferHeader.getInt();

        buffer.clear();

        // Buffer mit passender Capacity für ContentLength erzeugen.
        ByteBuffer bufferData = buffer.slice(0, contentLength);

        if (FrameType.DATA.equals(frameType))
        {
            read(channel, bufferData, contentLength);
            bufferData.flip();

            consumer.accept(bufferData);
        }
        else if (FrameType.ERROR.equals(frameType))
        {
            read(channel, bufferData, contentLength);
            bufferData.flip();

            byte[] bytes = new byte[contentLength];
            bufferData.get(bytes);
            String message = new String(bytes, StandardCharsets.UTF_8);

            throw new Exception(message);

            // Exception exception = serializer.readFrom(buffer, Exception.class);
            //
            // throw exception;
        }
        else if (FrameType.FINISH.equals(frameType))
        {
            // ContentLength = 0
            return false;
        }

        // Nächsten Frame lesen.
        return true;
    }

    /**
     * Lesen nur eines Frames.
     *
     * @param channel {@link SocketChannel}
     * @param consumer {@link Consumer}
     *
     * @throws Exception Falls was schief geht
     */
    public void readFrame(final SocketChannel channel, final Consumer<ByteBuffer> consumer) throws Exception
    {
        ByteBuffer buffer = getBufferPool().get(DEFAULT_BUFFER_SIZE);

        try
        {
            readFrame(channel, buffer, consumer);
        }
        finally
        {
            getBufferPool().free(buffer);
        }
    }

    /**
     * @param channel {@link SocketChannel}
     * @param buffers {@link ByteBuffer}[}
     *
     * @return long
     *
     * @throws IOException Falls was schief geht
     */
    protected long write(final SocketChannel channel, final ByteBuffer...buffers) throws IOException
    {
        for (ByteBuffer buffer : buffers)
        {
            if (buffer.position() > 0)
            // if (buffer.remaining() != buffer.limit())
            {
                buffer.flip();
            }
        }

        return channel.write(buffers);

        // long totalWritten = 0;
        //
        // for (ByteBuffer buffer : buffers)
        // {
        // while (buffer.hasRemaining())
        // {
        // long bytesWritten = channel.write(buffer);
        //
        // totalWritten += bytesWritten;
        // }
        // }
        //
        // return totalWritten;
    }

    /**
     * DATA-Frame schreiben.
     *
     * @param channel {@link SocketChannel}
     * @param consumer {@link Consumer}
     */
    public void writeData(final SocketChannel channel, final Consumer<ByteBuffer> consumer)
    {
        ByteBuffer bufferHeader = getBufferPool().get(8);
        ByteBuffer buffer = getBufferPool().get(DEFAULT_BUFFER_SIZE);

        try
        {
            consumer.accept(buffer);

            // Funktioniert nur solange wie der Consumer nicht den flip macht !
            writeFrameHeader(bufferHeader, FrameType.DATA, buffer.position());
            write(channel, bufferHeader, buffer);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        finally
        {
            getBufferPool().free(bufferHeader);
            getBufferPool().free(buffer);
        }
    }

    /**
     * ERROR-Frame schreiben.
     *
     * @param channel {@link SocketChannel}
     * @param th {@link Throwable}
     */
    public void writeError(final SocketChannel channel, final Throwable th)
    {
        ByteBuffer buffer = getBufferPool().get(DEFAULT_BUFFER_SIZE);

        try
        {
            String message = th.getMessage() == null ? "" : th.getMessage();
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);
            buffer.put(messageBytes);

            writeFrameHeader(buffer, FrameType.ERROR, messageBytes.length);
            write(channel, buffer);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        finally
        {
            getBufferPool().free(buffer);
        }
    }

    /**
     * FINISH-Frame schreiben.
     *
     * @param channel {@link SocketChannel}
     */
    public void writeFinish(final SocketChannel channel)
    {
        ByteBuffer buffer = getBufferPool().get(8);

        try
        {
            writeFrameHeader(buffer, FrameType.FINISH, 0);
            write(channel, buffer);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        finally
        {
            getBufferPool().free(buffer);
        }
    }

    /**
     * @param buffer {@link ByteBuffer}
     * @param frameType {@link FrameType}
     * @param contentLength int
     */
    protected void writeFrameHeader(final ByteBuffer buffer, final FrameType frameType, final int contentLength)
    {
        buffer.putInt(frameType.getEncodedType());
        buffer.putInt(contentLength);
    }
}
