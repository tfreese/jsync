// Created: 27.08.20
package de.freese.jsync.utils.pool;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;

import de.freese.jsync.utils.JSyncUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public class AsynchronousSocketChannelPool
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AsynchronousSocketChannelPool.class);
    /**
     *
     */
    private final List<AsynchronousSocketChannel> channelPool = new ArrayList<>();
    /**
     *
     */
    private final ReentrantLock lock = new ReentrantLock(true);
    /**
     *
     */
    private final URI uri;
    /**
     *
     */
    private AsynchronousChannelGroup channelGroup;

    /**
     * Erzeugt eine neue Instanz von {@link AsynchronousSocketChannelPool}
     *
     * @param uri             {@link URI}
     * @param executorService {@link ExecutorService}
     */
    public AsynchronousSocketChannelPool(final URI uri, final ExecutorService executorService)
    {
        super();

        this.uri = uri;

        try
        {
            this.channelGroup = AsynchronousChannelGroup.withCachedThreadPool(executorService, 2);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @param disconnector {@link Consumer}
     */
    public void clear(final Consumer<AsynchronousSocketChannel> disconnector)
    {
        getLock().lock();

        try
        {
            for (Iterator<AsynchronousSocketChannel> iterator = this.channelPool.iterator(); iterator.hasNext(); )
            {
                AsynchronousSocketChannel channel = iterator.next();

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

            JSyncUtils.shutdown(this.channelGroup, getLogger());
        }
        finally
        {
            getLock().unlock();
        }
    }

    /**
     * @return {@link AsynchronousSocketChannel}
     */
    public AsynchronousSocketChannel getChannel()
    {
        getLock().lock();

        try
        {
            AsynchronousSocketChannel channel = null;

            if (this.channelPool.isEmpty())
            {
                try
                {
                    InetSocketAddress serverAddress = new InetSocketAddress(this.uri.getHost(), this.uri.getPort());

                    channel = AsynchronousSocketChannel.open(this.channelGroup);

                    Future<Void> futureConnect = channel.connect(serverAddress);
                    futureConnect.get();
                }
                catch (RuntimeException ex)
                {
                    throw ex;
                }
                catch (IOException ex)
                {
                    throw new UncheckedIOException(ex);
                }
                catch (InterruptedException | ExecutionException ex)
                {
                    throw new RuntimeException(ex);
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

    /**
     * @param channel {@link AsynchronousSocketChannel}
     */
    public void releaseChannel(final AsynchronousSocketChannel channel)
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
}
