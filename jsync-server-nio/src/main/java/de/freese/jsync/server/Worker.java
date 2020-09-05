// Created: 04.11.2018
package de.freese.jsync.server;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Semaphore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import de.freese.jsync.server.handler.IoHandler;

/**
 * Übernimmt das Connection-Handling.<br>
 * Ein Worker kann für mehrere Connections zuständig sein.
 *
 * @author Thomas Freese
 */
@SuppressWarnings("resource")
class Worker implements Runnable
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(Worker.class);

    /**
    *
    */
    private final boolean directRegistration = true;
    /**
     *
     */
    private final IoHandler ioHandler;
    /**
     *
     */
    private boolean isShutdown;
    /**
     *
     */
    private final Queue<SocketChannel> newSessions = new ConcurrentLinkedQueue<>();
    /**
     *
     */
    private final Selector selector;

    /**
     *
     */
    private final Semaphore stopLock = new Semaphore(1, true);

    /**
     * Erstellt ein neues {@link Worker} Object.
     *
     * @param ioHandler {@link IoHandler}
     * @throws IOException Falls was schief geht.
     */
    public Worker(final IoHandler ioHandler) throws IOException
    {
        super();

        this.ioHandler = Objects.requireNonNull(ioHandler, "ioHandler required");
        this.selector = Selector.open();
    }

    /**
     * Neue Session zum Processor hinzufügen.
     *
     * @param socketChannel {@link SocketChannel}
     * @throws IOException Falls was schief geht.
     */
    void addSession(final SocketChannel socketChannel) throws IOException
    {
        Objects.requireNonNull(socketChannel, "socketChannel required");

        if (this.directRegistration)
        {
            socketChannel.configureBlocking(false);
            getLogger().debug("{}: attach new session", socketChannel.getRemoteAddress());
            socketChannel.register(this.selector, SelectionKey.OP_READ);
        }
        else
        {
            this.newSessions.add(socketChannel);
        }

        this.selector.wakeup();
    }

    /**
     * @return {@link Logger}
     */
    private Logger getLogger()
    {
        return LOGGER;
    }

    /**
     * Die neuen Sessions zum Selector hinzufügen.
     *
     * @throws IOException Falls was schief geht.
     */
    void processNewSessions() throws IOException
    {
        // for (SocketChannel socketChannel = this.newSessions.poll(); socketChannel != null; socketChannel =
        // this.newSessions.poll())
        while (!this.newSessions.isEmpty())
        {
            SocketChannel socketChannel = this.newSessions.poll();

            if (socketChannel == null)
            {
                continue;
            }

            socketChannel.configureBlocking(false);

            getLogger().debug("{}: attach new session", socketChannel.getRemoteAddress());

            @SuppressWarnings("unused")
            SelectionKey selectionKey = socketChannel.register(this.selector, SelectionKey.OP_READ);
            // selectionKey.attach(obj)
        }
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
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

                        if (selectionKey.isReadable())
                        {
                            // getLogger().debug("{}: Read Request", ((SocketChannel) selectionKey.channel()).getRemoteAddress());

                            // Request lesen.
                            this.ioHandler.read(selectionKey);
                        }
                        else if (selectionKey.isWritable())
                        {
                            // getLogger().debug("{}: Write Response", ((SocketChannel) selectionKey.channel()).getRemoteAddress());

                            // Response schreiben.
                            this.ioHandler.write(selectionKey);
                        }
                    }

                    selected.clear();
                }

                // Die neuen Sessions zum Selector hinzufügen.
                processNewSessions();
            }
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);
        }
        finally
        {
            this.stopLock.release();
        }

        getLogger().debug("worker stopped");
    }

    /**
     * Stoppen des Processors.
     */
    void stop()
    {
        getLogger().debug("stopping worker");

        this.isShutdown = true;
        this.selector.wakeup();

        this.stopLock.acquireUninterruptibly();

        try
        {
            Set<SelectionKey> selected = this.selector.selectedKeys();
            Iterator<SelectionKey> iterator = selected.iterator();

            while (iterator.hasNext())
            {
                SelectionKey selectionKey = iterator.next();
                iterator.remove();

                if (selectionKey != null)
                {
                    selectionKey.cancel();
                }
            }

            selected.clear();

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
