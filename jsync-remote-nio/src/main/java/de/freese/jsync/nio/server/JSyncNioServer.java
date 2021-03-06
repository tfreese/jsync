/**
 * Created: 31.10.2016
 */

package de.freese.jsync.nio.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.StandardSocketOptions;
import java.nio.channels.SelectionKey;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Objects;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.nio.server.dispatcher.Dispatcher;
import de.freese.jsync.nio.server.dispatcher.DispatcherPool;
import de.freese.jsync.nio.server.handler.IoHandler;

/**
 * Dieser Server arbeitet nach dem Acceptor-Reactor Pattern.<br>
 * Der {@link Acceptor} nimmt die neuen Client-Verbindungen entgegen und übergibt sie einem {@link Dispatcher}.<br>
 * Der {@link Dispatcher} kümmert sich um das Connection-Handling der Clients nach dem 'accept'.<br>
 * Der {@link IoHandler} übernimmt das Lesen und Schreiben von Request und Response in einem separatem Thread.<br>
 *
 * @author Thomas Freese
 */
public class JSyncNioServer implements Runnable
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JSyncNioServer.class);
    /**
     *
     */
    private Acceptor acceptor;
    /**
     *
     */
    private final DispatcherPool dispatcherPool;
    /**
     *
     */
    private IoHandler<SelectionKey> ioHandler;
    /**
    *
    */
    private String name = getClass().getSimpleName();
    /**
     *
     */
    private final int port;
    /**
     *
     */
    private final SelectorProvider selectorProvider;
    /**
     *
     */
    private ServerSocketChannel serverSocketChannel;
    /**
     * ReentrantLock nicht möglich, da dort die Locks auf Thread-Ebene verwaltet werden.
     */
    private final Semaphore startLock = new Semaphore(1, true);

    /**
     * Erstellt ein neues {@link JSyncNioServer} Object.
     *
     * @param port int
     * @param numOfDispatcher int
     * @param numOfWorker int
     * @throws IOException Falls was schief geht.
     */
    public JSyncNioServer(final int port, final int numOfDispatcher, final int numOfWorker) throws IOException
    {
        this(port, numOfDispatcher, numOfWorker, SelectorProvider.provider());
    }

    /**
     * Erstellt ein neues {@link JSyncNioServer} Object.
     *
     * @param port int
     * @param numOfDispatcher int
     * @param numOfWorker int
     * @param selectorProvider {@link SelectorProvider}
     * @throws IOException Falls was schief geht.
     */
    public JSyncNioServer(final int port, final int numOfDispatcher, final int numOfWorker, final SelectorProvider selectorProvider) throws IOException
    {
        super();

        if (port <= 0)
        {
            throw new IllegalArgumentException("port <= 0: " + port);
        }

        this.port = port;
        this.dispatcherPool = new DispatcherPool(numOfDispatcher, numOfWorker);
        this.selectorProvider = Objects.requireNonNull(selectorProvider, "selectorProvider required");

        this.startLock.acquireUninterruptibly();
    }

    /**
     * @return {@link Logger}
     */
    private Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * @return boolean
     */
    public boolean isStarted()
    {
        return this.startLock.availablePermits() > 0;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        getLogger().info("starting '{}' on port: {}", this.name, this.port);

        Objects.requireNonNull(this.ioHandler, "ioHandler requried");

        try
        {
            // this.serverSocketChannel = ServerSocketChannel.open();
            this.serverSocketChannel = this.selectorProvider.openServerSocketChannel();
            this.serverSocketChannel.configureBlocking(false);

            if (this.serverSocketChannel.supportedOptions().contains(StandardSocketOptions.TCP_NODELAY))
            {
                // this.serverSocketChannel.getOption(StandardSocketOptions.TCP_NODELAY);
                this.serverSocketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            }

            if (this.serverSocketChannel.supportedOptions().contains(StandardSocketOptions.SO_REUSEADDR))
            {
                // this.serverSocketChannel.getOption(StandardSocketOptions.SO_REUSEADDR);
                this.serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            }

            if (this.serverSocketChannel.supportedOptions().contains(StandardSocketOptions.SO_REUSEPORT))
            {
                // this.serverSocketChannel.getOption(StandardSocketOptions.SO_REUSEPORT);
                this.serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEPORT, true);
            }

            if (this.serverSocketChannel.supportedOptions().contains(StandardSocketOptions.SO_RCVBUF))
            {
                // this.serverSocketChannel.getOption(StandardSocketOptions.SO_RCVBUF);
                this.serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 64 * 1024);
            }

            if (this.serverSocketChannel.supportedOptions().contains(StandardSocketOptions.SO_SNDBUF))
            {
                // this.serverSocketChannel.getOption(StandardSocketOptions.SO_SNDBUF);
                this.serverSocketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 64 * 1024);
            }

            this.serverSocketChannel.bind(new InetSocketAddress(this.port), 50);

            // ServerSocket socket = this.serverSocketChannel.socket();
            // socket.setReuseAddress(true);
            // socket.bind(new InetSocketAddress(this.port), 50);

            // Erzeugen der Dispatcher.
            this.dispatcherPool.start(this.ioHandler, this.selectorProvider, this.name + "-" + this.port);

            // Erzeugen des Acceptors
            this.acceptor = new Acceptor(this.selectorProvider.openSelector(), this.serverSocketChannel, this.dispatcherPool);

            Thread thread = new ServerThreadFactory(this.name + "-" + this.port + "-acceptor-").newThread(this.acceptor);
            getLogger().debug("start {}", thread.getName());
            thread.start();

            getLogger().info("'{}' listening on port: {}", this.name, this.port);
            this.startLock.release();
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);
        }
    }

    /**
     * @param ioHandler {@link IoHandler}
     */
    public void setIoHandler(final IoHandler<SelectionKey> ioHandler)
    {
        this.ioHandler = Objects.requireNonNull(ioHandler, "ioHandler requried");
    }

    /**
     * @param name String
     */
    public void setName(final String name)
    {
        this.name = Objects.requireNonNull(name, "name required");
    }

    /**
     * Starten des Servers.
     */
    public void start()
    {
        run();

        // Warten bis fertich.
        // this.startLock.acquireUninterruptibly();
        // this.startLock.release();
    }

    /**
     * Stoppen des Servers.
     */
    public void stop()
    {
        getLogger().info("stopping '{}' on port: {}", this.name, this.port);

        this.acceptor.stop();
        this.dispatcherPool.stop();

        try
        {
            // SelectionKey selectionKey = this.serverSocketChannel.keyFor(this.selector);
            //
            // if (selectionKey != null)
            // {
            // selectionKey.cancel();
            // }

            this.serverSocketChannel.close();
        }
        catch (IOException ex)
        {
            getLogger().error(null, ex);
        }

        getLogger().info("'{}' stopped on port: {}", this.name, this.port);
    }
}
