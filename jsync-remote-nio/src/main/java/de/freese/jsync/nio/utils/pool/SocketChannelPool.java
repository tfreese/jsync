// Created: 27.08.20
package de.freese.jsync.nio.utils.pool;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import de.freese.jsync.utils.pool.AbstractPool;

/**
 * @author Thomas Freese
 */
public final class SocketChannelPool extends AbstractPool<SocketChannel>
{
    /**
     *
     */
    private final URI uri;

    /**
     * Erzeugt eine neue Instanz von {@link SocketChannelPool}
     *
     * @param uri {@link URI}
     */
    public SocketChannelPool(final URI uri)
    {
        super();

        this.uri = Objects.requireNonNull(uri, "uri required");
    }

    /**
     * @see de.freese.jsync.utils.pool.AbstractPool#createObject()
     */
    @Override
    protected SocketChannel createObject()
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

            return channel;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @see de.freese.jsync.utils.pool.AbstractPool#destroyObject(java.lang.Object)
     */
    @Override
    protected void destroyObject(final SocketChannel object)
    {
        try
        {
            // if(object.isOpen())
            // {
            //
            // }
            object.shutdownInput();
            object.shutdownOutput();
            object.close();
        }
        catch (IOException ex)
        {
            getLogger().warn(ex.getMessage());
        }
    }

    /**
     * @see de.freese.jsync.utils.pool.AbstractPool#get()
     */
    @Override
    public SocketChannel get()
    {
        return super.get();
    }
}
