// Created: 27.08.20
package de.freese.jsync.utils.pool;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
@SuppressWarnings("resource")
public class SocketChannelPool
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(SocketChannelPool.class);
    /**
     *
     */
    private final List<SocketChannel> channelPool = new ArrayList<>();
    /**
     *
     */
    private final ReentrantLock lock = new ReentrantLock(true);
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
     * @param disconnector {@link Consumer}
     */
    public void clear(final Consumer<SocketChannel> disconnector)
    {
        getLock().lock();

        try
        {
            for (Iterator<SocketChannel> iterator = this.channelPool.iterator(); iterator.hasNext();)
            {
                SocketChannel channel = iterator.next();

                disconnector.accept(channel);

                try
                {
                    channel.shutdownInput();
                    channel.shutdownOutput();
                    channel.close();
                }
                catch (IOException ex)
                {
                    getLogger().error(null, ex);
                }

                iterator.remove();
            }
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * @return {@link SocketChannel}
     */
    public SocketChannel getChannel()
    {
        getLock().lock();

        try
        {
            SocketChannel channel = null;

            if (this.channelPool.isEmpty())
            {
                try
                {
                    InetSocketAddress serverAddress = new InetSocketAddress(this.uri.getHost(), this.uri.getPort());

                    channel = SocketChannel.open(serverAddress);
                    channel.configureBlocking(true);
                }
                catch (IOException ex)
                {
                    throw new UncheckedIOException(ex);
                }
            }
            else
            {
                channel = this.channelPool.remove(0);
            }

            return channel;
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * @param channel {@link SocketChannel}
     */
    public void releaseChannel(final SocketChannel channel)
    {
        getLock().lock();

        try
        {
            this.channelPool.add(channel);
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * @return {@link ReentrantLock}
     */
    private ReentrantLock getLock()
    {
        return this.lock;
    }

    /**
     * @return {@link Logger}
     */
    private Logger getLogger()
    {
        return LOGGER;
    }
}
