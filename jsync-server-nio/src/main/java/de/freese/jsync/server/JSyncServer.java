// Created: 31.10.2016
package de.freese.jsync.server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.channels.spi.SelectorProvider;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.server.handler.IoHandler;

/**
 * Der Server nimmt nur die neuen Client-Verbindungen entgegen und übergibt sie einem {@link Worker}.<br>
 * Der {@link Worker} kümmert dann sich um das Connection-Handling.<br>
 * Der {@link IoHandler} übernimmt das Lesen und Schreiben von Request und Response.<br>
 * Zur Performance-Optimierung können mehrere {@link Worker} gestartet werden,<br>
 * die im Wechsel (RoundRobin) mit neuen Verbindungen versorgt werden.
 *
 * @author Thomas Freese
 */
@SuppressWarnings("resource")
public class JSyncServer implements Runnable
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(JSyncServer.class);

    /**
     *
     */
    private static final AtomicInteger SERVER_NUMBER = new AtomicInteger(1);

    /**
     *
     */
    private IoHandler ioHandler;

    /**
     *
     */
    private boolean isShutdown;

    /**
     *
     */
    private String name = getClass().getSimpleName() + "-" + SERVER_NUMBER.getAndIncrement();

    /**
    *
    */
    private final int numOfWorkers;

    /**
     *
     */
    private final int port;

    /**
     *
     */
    private Selector selector;

    /**
     *
     */
    private final SelectorProvider selectorProvider;

    /**
     *
     */
    private ServerSocketChannel serverSocketChannel;

    /**
    *
    */
    private final Semaphore startLock = new Semaphore(1, true);

    /**
     *
     */
    private final Semaphore stopLock = new Semaphore(1, true);

    /**
     *
     */
    private ThreadFactory threadFactory = Executors.defaultThreadFactory();

    /**
     * Queue für die {@link Worker}.
     */
    private final LinkedList<Worker> workers = new LinkedList<>();

    /**
     * Erstellt ein neues {@link JSyncServer} Object.
     *
     * @param port int
     * @param numOfWorkers int
     */
    public JSyncServer(final int port, final int numOfWorkers)
    {
        this(port, numOfWorkers, SelectorProvider.provider());
    }

    /**
     * Erstellt ein neues {@link JSyncServer} Object.
     *
     * @param port int
     * @param numOfWorkers int
     * @param selectorProvider {@link SelectorProvider}
     */
    public JSyncServer(final int port, final int numOfWorkers, final SelectorProvider selectorProvider)
    {
        super();

        this.port = port;
        this.numOfWorkers = numOfWorkers;
        this.selectorProvider = Objects.requireNonNull(selectorProvider, "selectorProvider required");
    }

    /**
     * @return {@link Logger}
     */
    private Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * Reagieren auf Requests.
     */
    private void listen()
    {
        getLogger().info("server listening on port: {}", this.serverSocketChannel.socket().getLocalPort());

        this.startLock.release();
        this.stopLock.acquireUninterruptibly();

        try
        {
            while (!Thread.interrupted())
            {
                int readyChannels = this.selector.select();

                if (this.isShutdown || !this.selector.isOpen())
                {
                    break;
                }

                if (readyChannels > 0)
                {
                    Set<SelectionKey> selected = this.selector.selectedKeys();
                    Iterator<SelectionKey> iterator = selected.iterator();

                    while (iterator.hasNext())
                    {
                        SelectionKey selectionKey = iterator.next();
                        iterator.remove();

                        if (!selectionKey.isValid())
                        {
                            getLogger().debug("{}: SelectionKey not valid", ((SocketChannel) selectionKey.channel()).getRemoteAddress());
                        }

                        if (selectionKey.isAcceptable())
                        {
                            // Verbindung mit Client herstellen.
                            SocketChannel socketChannel = this.serverSocketChannel.accept();

                            getLogger().debug("{}: Connection Accepted", socketChannel.getRemoteAddress());

                            // Socket dem Worker übergeben.
                            nextWorker().addSession(socketChannel);
                        }
                        else if (selectionKey.isConnectable())
                        {
                            getLogger().debug("{}: Client Connected", ((SocketChannel) selectionKey.channel()).getRemoteAddress());
                        }
                    }

                    selected.clear();
                }
            }
        }
        catch (IOException ex)
        {
            getLogger().error(null, ex);
        }
        finally
        {
            this.stopLock.release();
        }

        getLogger().info("server stopped on port: {}", this.port);
    }

    /**
     * Liefert den nächsten {@link Worker} im RoundRobin-Verfahren.<br>
     *
     * @return {@link Worker}
     * @throws IOException Falls was schief geht.
     */
    private synchronized Worker nextWorker() throws IOException
    {
        if (this.isShutdown)
        {
            return null;
        }

        // Ersten Worker entnehmen.
        Worker worker = this.workers.poll();

        // Worker wieder hinten dran hängen.
        this.workers.add(worker);

        return worker;
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        getLogger().info("starting server on port: {}", this.port);

        Objects.requireNonNull(this.ioHandler, "ioHandler requried");

        try
        {
            this.selector = this.selectorProvider.openSelector();

            this.serverSocketChannel = this.selectorProvider.openServerSocketChannel();
            this.serverSocketChannel.configureBlocking(false);

            ServerSocket socket = this.serverSocketChannel.socket();
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(this.port), 50);

            // SelectionKey selectionKey =
            // this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
            // selectionKey.attach(this);

            // Erzeugen der Worker.
            while (this.workers.size() < this.numOfWorkers)
            {
                Worker worker = new Worker(this.ioHandler);

                this.workers.add(worker);

                String threadName = this.name + "-worker-" + this.workers.size();

                getLogger().info("start worker: {}", threadName);

                Thread thread = this.threadFactory.newThread(worker);
                thread.setName(threadName);
                thread.setDaemon(true);
                thread.start();
            }

            @SuppressWarnings("unused")
            SelectionKey selectionKey = this.serverSocketChannel.register(this.selector, SelectionKey.OP_ACCEPT);
            // selectionKey.attach(this);

            listen();
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);
        }
    }

    /**
     * @param ioHandler {@link IoHandler}
     */
    public void setIoHandler(final IoHandler ioHandler)
    {
        this.ioHandler = ioHandler;
    }

    /**
     * @param name String
     */
    public void setName(final String name)
    {
        this.name = Objects.requireNonNull(name, "name required");
    }

    /**
     * @param threadFactory {@link ThreadFactory}
     */
    public void setThreadFactory(final ThreadFactory threadFactory)
    {
        this.threadFactory = Objects.requireNonNull(threadFactory, "threadFactory required");
    }

    /**
     * Starten des Servers.
     *
     * @throws IOException Falls was schief geht.
     */
    public void start() throws IOException
    {
        this.startLock.acquireUninterruptibly();

        Thread thread = this.threadFactory.newThread(this::run);
        thread.setName(this.name);
        thread.setDaemon(false);
        thread.start();

        // Warten bis die Initialisierung fertig ist.
        this.startLock.acquireUninterruptibly();
    }

    /**
     * Stoppen des Servers.
     */
    public void stop()
    {
        getLogger().info("stopping server on port: {}", this.port);

        this.isShutdown = true;

        this.workers.forEach(Worker::stop);

        this.selector.wakeup();

        this.stopLock.acquireUninterruptibly();

        try
        {
            SelectionKey selectionKey = this.serverSocketChannel.keyFor(this.selector);

            if (selectionKey != null)
            {
                selectionKey.cancel();
            }

            this.serverSocketChannel.close();

            if (this.selector.isOpen())
            {
                this.selector.close();
            }
        }
        catch (IOException ex)
        {
            getLogger().error(null, ex);
        }
        finally
        {
            this.stopLock.release();
        }
    }
}
