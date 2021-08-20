// Created: 20.08.2021
package de.freese.jsync.nio.transport;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.Objects;
import java.util.function.Consumer;

import de.freese.jsync.nio.transport.pool.ByteBufferPool;
import de.freese.jsync.nio.transport.pool.DefaultByteBufferPool;

/**
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
    private ByteBufferPool bufferPool = new DefaultByteBufferPool();

    /**
     * MetaData Error-Frame schreiben.
     *
     * @param channel {@link SocketChannel}
     * @param th {@link Throwable}
     *
     * @throws IOException Falls was schief geht
     */
    public void error(final SocketChannel channel, final Throwable th) throws IOException
    {
        ByteBuffer buffer = getBufferPool().get(DEFAULT_BUFFER_SIZE);

        try
        {
            String message = th.getMessage() == null ? "" : th.getMessage();
            byte[] messageBytes = message.getBytes(StandardCharsets.UTF_8);

            writeFrameHeader(buffer, FrameType.ERROR, messageBytes.length);

            buffer.put(messageBytes);

            buffer.flip();

            channel.write(buffer);
        }
        finally
        {
            getBufferPool().free(buffer);
        }
    }

    /**
     * Finish-Frame schreiben.
     *
     * @param channel {@link SocketChannel}
     *
     * @throws IOException Falls was schief geht
     */
    public void finish(final SocketChannel channel) throws IOException
    {
        ByteBuffer buffer = getBufferPool().get(8);

        try
        {
            buffer.putInt(FrameType.FINISH.getEncodedType());

            buffer.flip();

            write(channel, buffer);
        }
        finally
        {
            getBufferPool().free(buffer);
        }
    }

    /**
     * @return {@link ByteBufferPool}
     */
    protected ByteBufferPool getBufferPool()
    {
        return this.bufferPool;
    }

    /**
     * @param channel {@link SocketChannel}
     * @param consumer {@link Consumer}
     *
     * @throws IOException Falls was schief geht
     */
    public void read(final SocketChannel channel, final Consumer<ByteBuffer> consumer) throws IOException
    {
        ByteBuffer buffer = getBufferPool().get(DEFAULT_BUFFER_SIZE);

        try
        {
            // Auf keinen Fall mehr lesen als den Type und Status: 8 Bytes
            ByteBuffer bufferHeader = buffer.slice(0, 8);

            channel.read(bufferHeader);
            bufferHeader.flip();

            FrameType frameType = FrameType.fromEncodedType(bufferHeader.getInt());

            buffer.clear();

            if (FrameType.ERROR.equals(frameType))
            {
                // // Exception einlesen.
                // int totalRead = channel.read(byteBuffer);
                //
                // while (totalRead < contentLength)
                // {
                // int bytesRead = channel.read(byteBuffer);
                // totalRead += bytesRead;
                // }
                //
                // byteBuffer.flip();
                //
                // Exception exception = getSerializer().readFrom(byteBuffer, Exception.class);
                //
                // throw exception;
            }
        }
        finally
        {
            getBufferPool().free(buffer);
        }
    }

    /**
     * @param bufferPool {@link ByteBufferPool}
     */
    public void setBufferPool(final ByteBufferPool bufferPool)
    {
        this.bufferPool = Objects.requireNonNull(bufferPool, "bufferPool required");
    }

    /**
     * @param selectionKey {@link SelectionKey}
     */
    public void write(final SelectionKey selectionKey)
    {
        write((SocketChannel) selectionKey.channel());
    }

    /**
     * @param channel {@link SocketChannel}
     */
    public void write(final SocketChannel channel)
    {

    }

    /**
     * @param channel {@link SocketChannel}
     * @param buffer {@link ByteBuffer}
     *
     * @throws IOException Falls was schief geht
     */
    protected void write(final SocketChannel channel, final ByteBuffer buffer) throws IOException
    {
        while (buffer.hasRemaining())
        {
            channel.write(buffer);
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
