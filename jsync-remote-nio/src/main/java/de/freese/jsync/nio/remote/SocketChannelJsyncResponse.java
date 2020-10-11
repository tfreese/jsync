// Created: 29.09.2020
package de.freese.jsync.nio.remote;

import java.io.IOException;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.nio.filesystem.RemoteSupport;
import de.freese.jsync.remote.api.JsyncResponse;

/**
 * @author Thomas Freese
 */
public class SocketChannelJsyncResponse implements JsyncResponse, RemoteSupport
{
    /**
    *
    */
    private final SocketChannel channel;

    /**
    *
    */
    private final DataBuffer dataBuffer;

    /**
    *
    */
    private final Serializer<DataBuffer> serializer;

    /**
     * Erstellt ein neues {@link SocketChannelJsyncResponse} Object.
     *
     * @param channel {@link SocketChannel}
     * @param serializer {@link Serializer}
     * @param dataBuffer {@link DataBuffer}
     */
    public SocketChannelJsyncResponse(final SocketChannel channel, final Serializer<DataBuffer> serializer, final DataBuffer dataBuffer)
    {
        super();

        this.channel = Objects.requireNonNull(channel, "channel required");
        this.serializer = Objects.requireNonNull(serializer, "serializer required");
        this.dataBuffer = Objects.requireNonNull(dataBuffer, "dataBuffer required");
    }

    /**
     * @see java.io.Closeable#close()
     */
    @Override
    public void close() throws IOException
    {
        DataBufferUtils.release(this.dataBuffer);
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncResponse#getContentLength()
     */
    @Override
    public int getContentLength()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncResponse#getException()
     */
    @Override
    public Throwable getException()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncResponse#getReadableByteChannel()
     */
    @Override
    public ReadableByteChannel getReadableByteChannel()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see de.freese.jsync.nio.filesystem.RemoteSupport#getSerializer()
     */
    @Override
    public Serializer<DataBuffer> getSerializer()
    {
        return this.serializer;
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncResponse#getStatus()
     */
    @Override
    public int getStatus()
    {
        // TODO Auto-generated method stub
        return 0;
    }
}
