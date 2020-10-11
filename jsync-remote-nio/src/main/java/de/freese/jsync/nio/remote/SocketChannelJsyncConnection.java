// Created: 29.09.2020
package de.freese.jsync.nio.remote;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import de.freese.jsync.model.JSyncCommand;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.remote.api.JsyncConnection;
import de.freese.jsync.remote.api.JsyncRequest;

/**
 * @author Thomas Freese
 */
public class SocketChannelJsyncConnection implements JsyncConnection
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SocketChannelJsyncConnection.class);

    /**
     *
     */
    private final SocketChannel channel;

    /**
     *
     */
    private final Serializer<DataBuffer> serializer;

    /**
     * Erstellt ein neues {@link SocketChannelJsyncConnection} Object.
     *
     * @param channel {@link SocketChannel}
     * @param serializer {@link Serializer}
     */
    public SocketChannelJsyncConnection(final SocketChannel channel, final Serializer<DataBuffer> serializer)
    {
        super();

        this.channel = Objects.requireNonNull(channel, "channel required");
        this.serializer = Objects.requireNonNull(serializer, "serializer required");
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncConnection#close()
     */
    @Override
    public void close()
    {
        try
        {
            // Server-Disconnect senden.
            JsyncRequest request = createRequest();
            request.setCommand(JSyncCommand.DISCONNECT);
            request.execute();

            this.channel.shutdownInput();
            this.channel.shutdownOutput();
            this.channel.close();
        }
        catch (IOException ex)
        {
            getLogger().error(null, ex);
        }
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncConnection#createRequest()
     */
    @Override
    public JsyncRequest createRequest()
    {
        return new SocketChannelJsyncRequest(this.channel, this.serializer);
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return LOGGER;
    }
}
