// Created: 29.09.2020
package de.freese.jsync.nio.remote;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import de.freese.jsync.model.serializer.DefaultSerializer;
import de.freese.jsync.model.serializer.Serializer;
import de.freese.jsync.model.serializer.adapter.ByteBufferAdapter;
import de.freese.jsync.remote.api.JsyncConnection;
import de.freese.jsync.remote.api.JsyncConnectionFactory;

/**
 * @author Thomas Freese
 */
public class SocketChannelJsyncConnectionFactory implements JsyncConnectionFactory
{
    /**
    *
    */
    private final Serializer<ByteBuffer> serializer = DefaultSerializer.of(new ByteBufferAdapter());

    /**
    *
    */
    private URI uri;

    /**
     * Erstellt ein neues {@link SocketChannelJsyncConnectionFactory} Object.
     */
    public SocketChannelJsyncConnectionFactory()
    {
        super();
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncConnectionFactory#close()
     */
    @Override
    public void close()
    {
        // Empty
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncConnectionFactory#connect(java.net.URI)
     */
    @Override
    public void connect(final URI uri)
    {
        this.uri = Objects.requireNonNull(uri, "uri required");
    }

    /**
     * @see de.freese.jsync.remote.api.JsyncConnectionFactory#getConnection()
     */
    @Override
    public JsyncConnection getConnection()
    {
        try
        {
            InetSocketAddress serverAddress = new InetSocketAddress(this.uri.getHost(), this.uri.getPort());

            SocketChannel channel = SocketChannel.open(serverAddress);

            while (!channel.finishConnect())
            {
                // can do something here...
            }

            channel.configureBlocking(true);

            return new SocketChannelJsyncConnection(channel, this.serializer);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }
}
