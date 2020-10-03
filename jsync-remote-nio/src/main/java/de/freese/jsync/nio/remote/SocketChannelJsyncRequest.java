// Created: 29.09.2020
package de.freese.jsync.nio.remote;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.channels.WritableByteChannel;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.nio.filesystem.RemoteSupport;
import de.freese.jsync.remote.api.JsyncRequest;
import de.freese.jsync.remote.api.JsyncResponse;
import de.freese.jsync.utils.pool.ByteBufferPool;

/**
 * @author Thomas Freese
 */
public class SocketChannelJsyncRequest implements JsyncRequest, RemoteSupport
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SocketChannelJsyncRequest.class);

    /**
     *
     */
    private final ByteBuffer buffer;

    /**
     *
     */
    private final SocketChannel channel;

    /**
     *
     */
    private JSyncCommand command;

    /**
     *
     */
    private final Map<Class<?>, Object> parameter = new LinkedHashMap<>();

    /**
     *
     */
    private final Serializer<ByteBuffer> serializer;

    /**
     * Erstellt ein neues {@link SocketChannelJsyncRequest} Object.
     *
     * @param channel {@link SocketChannel}
     * @param serializer {@link Serializer}
     */
    public SocketChannelJsyncRequest(final SocketChannel channel, final Serializer<ByteBuffer> serializer)
    {
        super();

        this.channel = Objects.requireNonNull(channel, "channel required");
        this.serializer = Objects.requireNonNull(serializer, "serializer required");
        this.buffer = ByteBufferPool.getInstance().get();
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncRequest#execute()
     */
    @SuppressWarnings(
    {
            "unchecked", "rawtypes"
    })
    @Override
    public JsyncResponse execute()
    {
        this.buffer.clear();
        getSerializer().writeTo(this.buffer, this.command);

        this.parameter.forEach((type, value) -> getSerializer().writeTo(this.buffer, value, (Class) type));

        this.buffer.flip();

        try
        {
            write(this.channel, this.buffer);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
        catch (Exception ex)
        {
            throw new RuntimeException(ex);
        }

        return new SocketChannelJsyncResponse(this.channel, this.serializer, this.buffer);
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * @see de.freese.jsync.nio.filesystem.RemoteSupport#getSerializer()
     */
    @Override
    public Serializer<ByteBuffer> getSerializer()
    {
        return this.serializer;
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncRequest#getWritableByteChannel()
     */
    @Override
    public WritableByteChannel getWritableByteChannel()
    {
        // TODO Auto-generated method stub
        return null;
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncRequest#param(java.lang.Object, java.lang.Class)
     */
    @Override
    public SocketChannelJsyncRequest param(final Object value, final Class<?> type)
    {
        this.parameter.put(type, value);

        return this;
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncRequest#setCommand(de.freese.jsync.model.JSyncCommand)
     */
    @Override
    public void setCommand(final JSyncCommand command)
    {
        this.command = command;
    }
}