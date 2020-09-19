// Created: 27.08.20
package de.freese.jsync.nio.utils.pool;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.channels.AsynchronousChannelGroup;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.function.Consumer;
import de.freese.jsync.utils.JSyncUtils;
import de.freese.jsync.utils.pool.AbstractPool;

/**
 * @author Thomas Freese
 */
public class AsynchronousSocketChannelPool extends AbstractPool<AsynchronousSocketChannel>
{
    /**
     *
     */
    private AsynchronousChannelGroup channelGroup;

    /**
     *
     */
    private final URI uri;

    /**
     * Erzeugt eine neue Instanz von {@link AsynchronousSocketChannelPool}
     *
     * @param uri {@link URI}
     * @param executorService {@link ExecutorService}
     */
    public AsynchronousSocketChannelPool(final URI uri, final ExecutorService executorService)
    {
        super();

        this.uri = Objects.requireNonNull(uri, "uri required");

        try
        {
            this.channelGroup = AsynchronousChannelGroup.withCachedThreadPool(Objects.requireNonNull(executorService, "executorService required"), 2);
        }
        catch (IOException ex)
        {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * @see de.freese.jsync.utils.pool.AbstractPool#createObject()
     */
    @Override
    protected AsynchronousSocketChannel createObject()
    {
        try
        {
            InetSocketAddress serverAddress = new InetSocketAddress(this.uri.getHost(), this.uri.getPort());

            AsynchronousSocketChannel channel = AsynchronousSocketChannel.open(this.channelGroup);

            Future<Void> futureConnect = channel.connect(serverAddress);

            futureConnect.get();

            return channel;
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

    /**
     * @see de.freese.jsync.utils.pool.AbstractPool#destroy(java.util.function.Consumer)
     */
    @Override
    public void destroy(final Consumer<AsynchronousSocketChannel> cleaner)
    {
        super.destroy(cleaner);

        JSyncUtils.shutdown(this.channelGroup, getLogger());
    }

    /**
     * @see de.freese.jsync.utils.pool.AbstractPool#destroyObject(java.lang.Object)
     */
    @Override
    protected void destroyObject(final AsynchronousSocketChannel object)
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
