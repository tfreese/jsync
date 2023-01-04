// Created: 27.08.20
package de.freese.jsync.nio.utils.pool;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.SocketChannel;
import java.util.Objects;
import java.util.concurrent.TimeUnit;

import de.freese.jsync.utils.pool.Pool;

/**
 * @author Thomas Freese
 */
public final class SocketChannelPool extends Pool<SocketChannel>
{
    private final URI uri;

    public SocketChannelPool(final URI uri)
    {
        super(true, false);

        this.uri = Objects.requireNonNull(uri, "uri required");
    }

    /**
     * @see de.freese.jsync.utils.pool.Pool#clear()
     */
    @Override
    public void clear()
    {
        super.clear(channel ->
        {
            try
            {
                channel.shutdownInput();
                channel.shutdownOutput();
                channel.close();
            }
            catch (IOException ex)
            {
                getLogger().warn(ex.getMessage());
            }
        });
    }

    /**
     * @see de.freese.jsync.utils.pool.Pool#create()
     */
    @Override
    protected SocketChannel create()
    {
        try
        {
            InetSocketAddress serverAddress = new InetSocketAddress(this.uri.getHost(), this.uri.getPort());

            SocketChannel channel = SocketChannel.open(serverAddress);

            while (!channel.finishConnect())
            {
                try
                {
                    TimeUnit.MILLISECONDS.sleep(10);
                }
                catch (InterruptedException ex)
                {
                    // Ignore
                }
            }

            channel.configureBlocking(true);

            return channel;
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }
}
