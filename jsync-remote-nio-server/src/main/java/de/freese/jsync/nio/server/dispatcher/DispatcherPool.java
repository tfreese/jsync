// Created: 10.09.2020
package de.freese.jsync.nio.server.dispatcher;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.nio.server.handler.IoHandler;
import de.freese.jsync.utils.JSyncThreadFactory;

/**
 * The {@link Dispatcher} handles the Client Connections after the 'accept'.<br>
 *
 * @author Thomas Freese
 */
public class DispatcherPool implements Dispatcher {
    private static final Logger LOGGER = LoggerFactory.getLogger(DispatcherPool.class);

    private final LinkedList<DefaultDispatcher> dispatchers = new LinkedList<>();

    private final int numOfDispatcher;

    private final int numOfWorker;

    private ExecutorService executorServiceWorker;

    public DispatcherPool(final int numOfDispatcher, final int numOfWorker) {
        super();

        if (numOfDispatcher < 1) {
            throw new IllegalArgumentException("numOfDispatcher < 1: " + numOfDispatcher);
        }

        if (numOfWorker < 1) {
            throw new IllegalArgumentException("numOfWorker < 1: " + numOfWorker);
        }

        if (numOfDispatcher > numOfWorker) {
            String message = String.format("numOfDispatcher > numOfWorker: %d < %d", numOfDispatcher, numOfWorker);
            throw new IllegalArgumentException(message);
        }

        this.numOfDispatcher = numOfDispatcher;
        this.numOfWorker = numOfWorker;
    }

    /**
     * @see de.freese.jsync.nio.server.dispatcher.Dispatcher#register(java.nio.channels.SocketChannel)
     */
    @Override
    public synchronized void register(final SocketChannel socketChannel) {
        nextDispatcher().register(socketChannel);
    }

    public void start(final IoHandler<SelectionKey> ioHandler, final SelectorProvider selectorProvider, final String serverName) throws Exception {
        ThreadFactory threadFactoryDispatcher = new JSyncThreadFactory(serverName + "-dispatcher-");
        ThreadFactory threadFactoryWorker = new JSyncThreadFactory(serverName + "-worker-");

        // this.executorServiceWorker = new ThreadPoolExecutor(1, this.numOfWorker, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactoryWorker);
        this.executorServiceWorker = Executors.newFixedThreadPool(this.numOfWorker, threadFactoryWorker);

        while (this.dispatchers.size() < this.numOfDispatcher) {
            DefaultDispatcher dispatcher = new DefaultDispatcher(selectorProvider.openSelector(), ioHandler, this.executorServiceWorker);
            this.dispatchers.add(dispatcher);

            Thread thread = threadFactoryDispatcher.newThread(dispatcher);

            getLogger().debug("start dispatcher: {}", thread.getName());
            thread.start();
        }
    }

    public void stop() {
        this.dispatchers.forEach(DefaultDispatcher::stop);
        this.executorServiceWorker.shutdown();
    }

    protected Logger getLogger() {
        return LOGGER;
    }

    /**
     * Returns the next {@link Dispatcher} in a Round-Robin procedure.<br>
     */
    private synchronized Dispatcher nextDispatcher() {
        // Ersten Dispatcher entnehmen.
        DefaultDispatcher dispatcher = this.dispatchers.poll();

        // Dispatcher wieder hinten dran hängen.
        this.dispatchers.add(dispatcher);

        return dispatcher;
    }
}
