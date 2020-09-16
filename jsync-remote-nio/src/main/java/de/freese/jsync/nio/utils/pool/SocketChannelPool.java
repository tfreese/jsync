// Created: 27.08.20
package de.freese.jsync.nio.utils.pool;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.SocketChannel;
import de.freese.jsync.utils.pool.AbstractPool;

/**
 * @author Thomas Freese
 */
public class SocketChannelPool extends AbstractPool<SocketChannel>
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

        this.uri = uri;
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
            object.shutdownInput();
            object.shutdownOutput();
            object.close();
        }
        catch (IOException ex)
        {
            getLogger().error(null, ex);
        }
    }
}
