// Created: 30.09.2020
package de.freese.jsync.nio.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.function.Consumer;
import de.freese.jsync.nio.utils.RemoteUtils;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * @author Thomas Freese
 */
public class JsyncServerResponse
{
    /**
     * @param bufferBody {@link ByteBuffer}
     * @return {@link JsyncServerResponse}
     */
    public static JsyncServerResponse error(final ByteBuffer bufferBody)
    {
        return new JsyncServerResponse(RemoteUtils.STATUS_ERROR, bufferBody);
    }

    /**
     * @param bufferBody {@link ByteBuffer}
     * @return {@link JsyncServerResponse}
     */
    public static JsyncServerResponse ok(final ByteBuffer bufferBody)
    {
        return new JsyncServerResponse(RemoteUtils.STATUS_OK, bufferBody);
    }

    /**
     *
     */
    private final ByteBuffer buffer;

    /**
     *
     */
    private final ByteBuffer bufferBody;

    /**
     *
     */
    private final int status;

    /**
     * Erstellt ein neues {@link JsyncServerResponse} Object.
     *
     * @param status int
     * @param bufferBody {@link ByteBuffer}
     */
    private JsyncServerResponse(final int status, final ByteBuffer bufferBody)
    {
        super();

        this.status = status;
        this.buffer = ByteBufferPool.getInstance().get();
        // this.buffer = ByteBuffer.allocate(Options.BYTEBUFFER_SIZE);
        this.bufferBody = Objects.requireNonNull(bufferBody, "bufferBody required");
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param buffer {@link ByteBuffer}
     * @throws IOException @throws Exception Falls was schief geht
     */
    private void write(final SelectionKey selectionKey, final ByteBuffer buffer) throws IOException
    {
        SocketChannel channel = (SocketChannel) selectionKey.channel();

        write(channel, buffer);
    }

    /**
     * @param selectionKey {@link SelectionKey}
     * @param consumer {@link Consumer}
     * @throws IOException Falls was schief geht.
     */
    public void write(final SelectionKey selectionKey, final Consumer<ByteBuffer> consumer) throws IOException
    {
        this.buffer.clear();
        this.bufferBody.clear();

        try
        {
            consumer.accept(this.bufferBody);

            this.buffer.putInt(this.status); // Status
            this.buffer.putInt(this.bufferBody.position()); // Content-Length

            this.bufferBody.flip();
            this.buffer.put(this.bufferBody);

            this.buffer.flip();
            write(selectionKey, this.buffer);
        }
        finally
        {
            ByteBufferPool.getInstance().release(this.buffer);
        }
    }

    /**
     * @param channel {@link SocketChannel}
     * @param buffer {@link ByteBuffer}
     * @throws IOException @throws Exception Falls was schief geht
     */
    private void write(final SocketChannel channel, final ByteBuffer buffer) throws IOException
    {
        while (buffer.hasRemaining())
        {
            channel.write(buffer);
        }
    }
}
