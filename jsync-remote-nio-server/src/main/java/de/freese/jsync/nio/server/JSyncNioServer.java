// Created: 31.10.2016
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
import reactor.core.scheduler.Schedulers;

import de.freese.jsync.nio.server.dispatcher.Dispatcher;
import de.freese.jsync.nio.server.dispatcher.DispatcherPool;
import de.freese.jsync.nio.server.handler.IoHandler;
import de.freese.jsync.nio.server.handler.JSyncIoHandler;
import de.freese.jsync.utils.JSyncThreadFactory;
import de.freese.jsync.utils.pool.bytebuffer.ByteBufferPool;

/**
 * These Server is working by the Acceptor-Reactor Pattern.<br>
 * The {@link Acceptor} handles the Client-Connections and delegate them to the {@link Dispatcher}.<br>
 * The {@link Dispatcher} handles the Client Connections after the 'accept'.<br>
 * The {@link IoHandler} handles the Request and Response in a separate Thread.<br>
 *
 * @author Thomas Freese
 */
public final class JSyncNioServer implements Runnable {
    private static final Logger LOGGER = LoggerFactory.getLogger(JSyncNioServer.class);

    public static void main(final String[] args) {
        final int port = Integer.parseInt(args[0]);

        final JSyncNioServer server = new JSyncNioServer(port, 2, 4);
        server.setIoHandler(new JSyncIoHandler());
        server.start();

        Runtime.getRuntime().addShutdownHook(new Thread(server::stop, "server-stop"));

        // server.stop();

        // Thread thread = new Thread(() -> server.start(8888), "nio-server");
        // thread.setDaemon(false);
        // thread.start();

        // System.in.read();
    }

    private static Logger getLogger() {
        return LOGGER;
    }

    private final DispatcherPool dispatcherPool;
    private final int port;
    private final SelectorProvider selectorProvider;
    /**
     * ReentrantLock is not possible, Locks are handles on Thread-Level.
     */
    private final Semaphore startLock = new Semaphore(1, true);

    private Acceptor acceptor;
    private IoHandler<SelectionKey> ioHandler;
    private String name = getClass().getSimpleName();
    private ServerSocketChannel serverSocketChannel;

    public JSyncNioServer(final int port, final int numOfDispatcher, final int numOfWorker) {
        this(port, numOfDispatcher, numOfWorker, SelectorProvider.provider());
    }

    public JSyncNioServer(final int port, final int numOfDispatcher, final int numOfWorker, final SelectorProvider selectorProvider) {
        super();

        if (port <= 0) {
            throw new IllegalArgumentException("port <= 0: " + port);
        }

        this.port = port;
        this.dispatcherPool = new DispatcherPool(numOfDispatcher, numOfWorker);
        this.selectorProvider = Objects.requireNonNull(selectorProvider, "selectorProvider required");

        this.startLock.acquireUninterruptibly();
    }

    public boolean isStarted() {
        return this.startLock.availablePermits() > 0;
    }

    @Override
    public void run() {
        getLogger().info("starting '{}' on port: {}", this.name, this.port);

        Objects.requireNonNull(this.ioHandler, "ioHandler required");

        try {
            // this.serverSocketChannel = ServerSocketChannel.open();
            this.serverSocketChannel = this.selectorProvider.openServerSocketChannel();
            this.serverSocketChannel.configureBlocking(false);

            if (this.serverSocketChannel.supportedOptions().contains(StandardSocketOptions.TCP_NODELAY)) {
                // this.serverSocketChannel.getOption(StandardSocketOptions.TCP_NODELAY);
                this.serverSocketChannel.setOption(StandardSocketOptions.TCP_NODELAY, true);
            }

            if (this.serverSocketChannel.supportedOptions().contains(StandardSocketOptions.SO_REUSEADDR)) {
                // this.serverSocketChannel.getOption(StandardSocketOptions.SO_REUSEADDR);
                this.serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEADDR, true);
            }

            if (this.serverSocketChannel.supportedOptions().contains(StandardSocketOptions.SO_REUSEPORT)) {
                // this.serverSocketChannel.getOption(StandardSocketOptions.SO_REUSEPORT);
                this.serverSocketChannel.setOption(StandardSocketOptions.SO_REUSEPORT, true);
            }

            if (this.serverSocketChannel.supportedOptions().contains(StandardSocketOptions.SO_RCVBUF)) {
                // this.serverSocketChannel.getOption(StandardSocketOptions.SO_RCVBUF);
                this.serverSocketChannel.setOption(StandardSocketOptions.SO_RCVBUF, 64 * 1024);
            }

            if (this.serverSocketChannel.supportedOptions().contains(StandardSocketOptions.SO_SNDBUF)) {
                // this.serverSocketChannel.getOption(StandardSocketOptions.SO_SNDBUF);
                this.serverSocketChannel.setOption(StandardSocketOptions.SO_SNDBUF, 64 * 1024);
            }

            this.serverSocketChannel.bind(new InetSocketAddress(this.port), 50);

            // ServerSocket socket = this.serverSocketChannel.socket();
            // socket.setReuseAddress(true);
            // socket.bind(new InetSocketAddress(this.port), 50);

            // Create Dispatcher.
            this.dispatcherPool.start(this.ioHandler, this.selectorProvider, this.name + "-" + this.port);

            // Create Acceptor.
            this.acceptor = new Acceptor(this.selectorProvider.openSelector(), this.serverSocketChannel, this.dispatcherPool);

            final Thread thread = new JSyncThreadFactory(this.name + "-" + this.port + "-acceptor-").newThread(this.acceptor);
            getLogger().debug("start {}", thread.getName());
            thread.start();

            getLogger().info("'{}' listening on port: {}", this.name, this.port);
            this.startLock.release();
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
        }
    }

    public void setIoHandler(final IoHandler<SelectionKey> ioHandler) {
        this.ioHandler = Objects.requireNonNull(ioHandler, "ioHandler required");
    }

    public void setName(final String name) {
        this.name = Objects.requireNonNull(name, "name required");
    }

    public void start() {
        run();

        // Wait if ready.
        // this.startLock.acquireUninterruptibly();
        // this.startLock.release();
    }

    public void stop() {
        getLogger().info("stopping '{}' on port: {}", this.name, this.port);

        this.acceptor.stop();
        this.dispatcherPool.stop();

        try {
            // SelectionKey selectionKey = this.serverSocketChannel.keyFor(this.selector);
            //
            // if (selectionKey != null) {
            // selectionKey.cancel();
            // }

            this.serverSocketChannel.close();
        }
        catch (IOException ex) {
            getLogger().error(ex.getMessage(), ex);
        }

        Schedulers.shutdownNow();

        getLogger().info("'{}' stopped on port: {}", this.name, this.port);

        ByteBufferPool.DEFAULT.clear();
    }
}
