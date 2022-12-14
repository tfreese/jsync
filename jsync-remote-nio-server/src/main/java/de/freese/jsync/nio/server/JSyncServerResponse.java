// Created: 30.09.2020
package de.freese.jsync.nio.server;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.function.Consumer;

import de.freese.jsync.nio.utils.RemoteUtils;

/**
 * @author Thomas Freese
 */
public final class JSyncServerResponse
{
    public static JSyncServerResponse error(final ByteBuffer bufferBody)
    {
        return new JSyncServerResponse(RemoteUtils.STATUS_ERROR, bufferBody);
    }

    public static JSyncServerResponse ok(final ByteBuffer bufferBody)
    {
        return new JSyncServerResponse(RemoteUtils.STATUS_OK, bufferBody);
    }

    private final ByteBuffer bufferBody;

    private final int status;

    private JSyncServerResponse(final int status, final ByteBuffer bufferBody)
    {
        super();

        this.status = status;
        this.bufferBody = Objects.requireNonNull(bufferBody, "bufferBody required");
    }

    public void write(final SelectionKey selectionKey) throws IOException
    {
        this.bufferBody.clear();

        this.bufferBody.putInt(this.status); // Status
        this.bufferBody.putLong(0); // Content-Length

        this.bufferBody.flip();

        write(selectionKey, this.bufferBody);
    }

    public void write(final SelectionKey selectionKey, final Consumer<ByteBuffer> consumer) throws IOException
    {
        this.bufferBody.clear();

        consumer.accept(this.bufferBody);

        ByteBuffer bufferHeader = ByteBuffer.allocate(12);
        bufferHeader.putInt(this.status); // Status
        bufferHeader.putLong(this.bufferBody.position()); // Content-Length

        bufferHeader.flip();
        this.bufferBody.flip();

        SocketChannel channel = (SocketChannel) selectionKey.channel();
        channel.write(new ByteBuffer[]
                {
                        bufferHeader, this.bufferBody
                });
    }

    void write(final SelectionKey selectionKey, final ByteBuffer buffer) throws IOException
    {
        SocketChannel channel = (SocketChannel) selectionKey.channel();

        write(channel, buffer);
    }

    void write(final SocketChannel channel, final ByteBuffer buffer) throws IOException
    {
        while (buffer.hasRemaining())
        {
            channel.write(buffer);
        }
    }
}
