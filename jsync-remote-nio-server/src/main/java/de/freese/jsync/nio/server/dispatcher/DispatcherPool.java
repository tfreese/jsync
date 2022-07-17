// Created: 10.09.2020
package de.freese.jsync.nio.server.dispatcher;

import java.nio.channels.SelectionKey;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.LinkedList;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;

import de.freese.jsync.nio.server.handler.IoHandler;
import de.freese.jsync.utils.JsyncThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Der {@link Dispatcher} kümmert sich um das Connection-Handling der Clients nach dem 'accept'.<br>
 *
 * @author Thomas Freese
 */
public class DispatcherPool implements Dispatcher
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(DispatcherPool.class);
    /**
     *
     */
    private final LinkedList<DefaultDispatcher> dispatchers = new LinkedList<>();
    /**
     *
     */
    private ExecutorService executorServiceWorker;
    /**
     *
     */
    private final int numOfDispatcher;
    /**
     *
     */
    private final int numOfWorker;

    /**
     * Erstellt ein neues {@link DispatcherPool} Object.
     *
     * @param numOfDispatcher int
     * @param numOfWorker int
     */
    public DispatcherPool(final int numOfDispatcher, final int numOfWorker)
    {
        super();

        if (numOfDispatcher < 1)
        {
            throw new IllegalArgumentException("numOfDispatcher < 1: " + numOfDispatcher);
        }

        if (numOfWorker < 1)
        {
            throw new IllegalArgumentException("numOfWorker < 1: " + numOfWorker);
        }

        if (numOfDispatcher > numOfWorker)
        {
            String message = String.format("numOfDispatcher > numOfWorker: %d < %d", numOfDispatcher, numOfWorker);
            throw new IllegalArgumentException(message);
        }

        this.numOfDispatcher = numOfDispatcher;
        this.numOfWorker = numOfWorker;
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * Liefert den nächsten {@link Dispatcher} im Round-Robin Verfahren.<br>
     *
     * @return {@link Dispatcher}
     */
    private synchronized Dispatcher nextDispatcher()
    {
        // Ersten Dispatcher entnehmen.
        DefaultDispatcher dispatcher = this.dispatchers.poll();

        // Dispatcher wieder hinten dran hängen.
        this.dispatchers.add(dispatcher);

        return dispatcher;
    }

    /**
     * @see de.freese.jsync.nio.server.dispatcher.Dispatcher#register(java.nio.channels.SocketChannel)
     */
    @Override
    public synchronized void register(final SocketChannel socketChannel)
    {
        nextDispatcher().register(socketChannel);
    }

    /**
     * @param ioHandler {@link IoHandler}
     * @param selectorProvider {@link SelectorProvider}
     * @param serverName String
     *
     * @throws Exception Falls was schief geht.
     */
    public void start(final IoHandler<SelectionKey> ioHandler, final SelectorProvider selectorProvider, final String serverName) throws Exception
    {
        ThreadFactory threadFactoryDispatcher = new JsyncThreadFactory(serverName + "-dispatcher-");
        ThreadFactory threadFactoryWorker = new JsyncThreadFactory(serverName + "-worker-");

        // this.executorServiceWorker = new ThreadPoolExecutor(1, this.numOfWorker, 60L, TimeUnit.SECONDS, new SynchronousQueue<>(), threadFactoryWorker);
        this.executorServiceWorker = Executors.newFixedThreadPool(this.numOfWorker, threadFactoryWorker);

        while (this.dispatchers.size() < this.numOfDispatcher)
        {
            DefaultDispatcher dispatcher = new DefaultDispatcher(selectorProvider.openSelector(), ioHandler, this.executorServiceWorker);
            this.dispatchers.add(dispatcher);

            Thread thread = threadFactoryDispatcher.newThread(dispatcher);

            getLogger().debug("start dispatcher: {}", thread.getName());
            thread.start();
        }
    }

    /**
     *
     */
    public void stop()
    {
        this.dispatchers.forEach(DefaultDispatcher::stop);
        this.executorServiceWorker.shutdown();
    }
}
