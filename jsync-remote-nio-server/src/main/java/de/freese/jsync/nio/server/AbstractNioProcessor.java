// Created: 09.09.2020
package de.freese.jsync.nio.server;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author Thomas Freese
 */
public abstract class AbstractNioProcessor implements Runnable
{
    /**
     *
     */
    private boolean isShutdown;
    /**
     *
     */
    private final Logger logger = LoggerFactory.getLogger(getClass());
    /**
     *
     */
    private final Selector selector;
    /**
     *
     */
    private final Semaphore stopLock = new Semaphore(1, true);

    /**
     * Erstellt ein neues {@link AbstractNioProcessor} Object.
     *
     * @param selector {@link Selector}
     */
    protected AbstractNioProcessor(final Selector selector)
    {
        super();

        this.selector = Objects.requireNonNull(selector, "selector required");
    }

    /**
     * Methode nach einem {@link Selector#select()} Durchlauf.
     */
    protected void afterSelectorLoop()
    {
        // Empty
    }

    /**
     * Methode nach der while-Schleife.
     */
    protected void afterSelectorWhile()
    {
        cancelKeys();
        closeSelector();
    }

    /**
     * Methode vor der while-Schleife.
     *
     * @throws Exception Falls was schiefgeht.
     */
    protected void beforeSelectorWhile() throws Exception
    {
        // Empty
    }

    /**
     *
     */
    protected void cancelKeys()
    {
        Set<SelectionKey> selected = getSelector().selectedKeys();
        Iterator<SelectionKey> iterator = selected.iterator();

        while (iterator.hasNext())
        {
            SelectionKey selectionKey = iterator.next();
            iterator.remove();

            if (selectionKey == null)
            {
                continue;
            }

            try
            {
                selectionKey.cancel();
            }
            catch (Exception ex)
            {
                getLogger().error(ex.getMessage(), ex);
            }
        }
    }

    /**
     *
     */
    protected void closeSelector()
    {
        if (getSelector().isOpen())
        {
            try
            {
                getSelector().close();
            }
            catch (Exception ex)
            {
                getLogger().error(ex.getMessage(), ex);
            }
        }
    }

    /**
     * @param readyChannels int
     *
     * @return boolean
     */
    protected boolean isExitCondition(final int readyChannels)
    {
        return isShutdown() || !getSelector().isOpen();
    }

    /**
     * @return {@link Logger}
     */
    protected Logger getLogger()
    {
        return this.logger;
    }

    /**
     * @return {@link Selector}
     */
    protected Selector getSelector()
    {
        return this.selector;
    }

    /**
     * @return {@link Semaphore}
     */
    protected Semaphore getStopLock()
    {
        return this.stopLock;
    }

    /**
     * @return boolean
     */
    protected boolean isShutdown()
    {
        return this.isShutdown;
    }

    /**
     * @param selectionKey {@link SelectionKey}
     */
    protected void onAcceptable(final SelectionKey selectionKey)
    {
        // Empty
    }

    /**
     * @param selectionKey {@link SelectionKey}
     */
    protected void onConnectable(final SelectionKey selectionKey)
    {
        // Empty
    }

    /**
     * @param selectionKey {@link SelectionKey}
     */
    protected void onInValid(final SelectionKey selectionKey)
    {
        // Empty
    }

    /**
     * @param selectionKey {@link SelectionKey}
     */
    protected void onReadable(final SelectionKey selectionKey)
    {
        // Empty
    }

    /**
     * @param selectionKey {@link SelectionKey}
     */
    protected void onWritable(final SelectionKey selectionKey)
    {
        // Empty
    }

    /**
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run()
    {
        getStopLock().acquireUninterruptibly();

        try
        {
            beforeSelectorWhile();

            while (!Thread.interrupted())
            {
                int readyChannels = getSelector().select();

                getLogger().debug("readyChannels = {}", readyChannels);

                if (isExitCondition(readyChannels))
                {
                    break;
                }

                if (readyChannels > 0)
                {
                    Set<SelectionKey> selected = getSelector().selectedKeys();
                    Iterator<SelectionKey> iterator = selected.iterator();

                    try
                    {
                        while (iterator.hasNext())
                        {
                            SelectionKey selectionKey = iterator.next();
                            iterator.remove();

                            if (!selectionKey.isValid())
                            {
                                getLogger().debug("{}: selectionKey not valid", ((SocketChannel) selectionKey.channel()).getRemoteAddress());

                                onInValid(selectionKey);
                            }
                            else if (selectionKey.isAcceptable())
                            {
                                getLogger().debug("new client accepted");

                                onAcceptable(selectionKey);
                            }
                            else if (selectionKey.isReadable())
                            {
                                getLogger().debug("{}: read request", ((SocketChannel) selectionKey.channel()).getRemoteAddress());

                                onReadable(selectionKey);
                            }
                            else if (selectionKey.isWritable())
                            {
                                getLogger().debug("{}: write response", ((SocketChannel) selectionKey.channel()).getRemoteAddress());

                                onWritable(selectionKey);
                            }
                            else if (selectionKey.isConnectable())
                            {
                                getLogger().debug("{}: client connected", ((SocketChannel) selectionKey.channel()).getRemoteAddress());

                                onConnectable(selectionKey);
                            }
                        }
                    }
                    finally
                    {
                        selected.clear();
                    }
                }

                afterSelectorLoop();
            }

            afterSelectorWhile();
        }
        catch (Exception ex)
        {
            getLogger().error(ex.getMessage(), ex);
        }
        finally
        {
            getStopLock().release();
        }

        if (getLogger().isDebugEnabled())
        {
            getLogger().debug("{} stopped", getClass().getSimpleName().toLowerCase());
        }
    }

    /**
     *
     */
    protected void setShutdown()
    {
        this.isShutdown = true;
    }

    /**
     * Stoppen des Processors.
     */
    public void stop()
    {
        if (getLogger().isDebugEnabled())
        {
            getLogger().debug("stopping {}", getClass().getSimpleName().toLowerCase());
        }

        setShutdown();
        getSelector().wakeup();

        getStopLock().acquireUninterruptibly();
        getStopLock().release();
    }
}
