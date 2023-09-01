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
public abstract class AbstractNioProcessor implements Runnable {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final Selector selector;
    private final Semaphore stopLock = new Semaphore(1, true);
    private boolean isShutdown;

    protected AbstractNioProcessor(final Selector selector) {
        super();

        this.selector = Objects.requireNonNull(selector, "selector required");
    }

    @Override
    public void run() {
        getStopLock().acquireUninterruptibly();

        try {
            beforeSelectorWhile();

            while (!Thread.interrupted()) {
                int readyChannels = getSelector().select();

                getLogger().debug("readyChannels = {}", readyChannels);

                if (isExitCondition(readyChannels)) {
                    break;
                }

                if (readyChannels > 0) {
                    Set<SelectionKey> selected = getSelector().selectedKeys();
                    Iterator<SelectionKey> iterator = selected.iterator();

                    try {
                        while (iterator.hasNext()) {
                            SelectionKey selectionKey = iterator.next();
                            iterator.remove();

                            if (!selectionKey.isValid()) {
                                getLogger().debug("{}: selectionKey not valid", ((SocketChannel) selectionKey.channel()).getRemoteAddress());

                                onInValid(selectionKey);
                            }
                            else if (selectionKey.isAcceptable()) {
                                getLogger().debug("new client accepted");

                                onAcceptable(selectionKey);
                            }
                            else if (selectionKey.isReadable()) {
                                getLogger().debug("{}: read request", ((SocketChannel) selectionKey.channel()).getRemoteAddress());

                                onReadable(selectionKey);
                            }
                            else if (selectionKey.isWritable()) {
                                getLogger().debug("{}: write response", ((SocketChannel) selectionKey.channel()).getRemoteAddress());

                                onWritable(selectionKey);
                            }
                            else if (selectionKey.isConnectable()) {
                                getLogger().debug("{}: client connected", ((SocketChannel) selectionKey.channel()).getRemoteAddress());

                                onConnectable(selectionKey);
                            }
                        }
                    }
                    finally {
                        selected.clear();
                    }
                }

                afterSelectorLoop();
            }

            afterSelectorWhile();
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
        }
        finally {
            getStopLock().release();
        }

        if (getLogger().isDebugEnabled()) {
            getLogger().debug("{} stopped", getClass().getSimpleName().toLowerCase());
        }
    }

    /**
     * Stop the Processor.
     */
    public void stop() {
        if (getLogger().isDebugEnabled()) {
            getLogger().debug("stopping {}", getClass().getSimpleName().toLowerCase());
        }

        setShutdown();
        getSelector().wakeup();

        getStopLock().acquireUninterruptibly();
        getStopLock().release();
    }

    /**
     * Method after {@link Selector#select()}.
     */
    protected void afterSelectorLoop() {
        // Empty
    }

    /**
     * Methode after the while-Loop.
     */
    protected void afterSelectorWhile() {
        cancelKeys();
        closeSelector();
    }

    /**
     * Methode bevore the while-Loop.
     */
    protected void beforeSelectorWhile() throws Exception {
        // Empty
    }

    protected void cancelKeys() {
        Set<SelectionKey> selected = getSelector().selectedKeys();
        Iterator<SelectionKey> iterator = selected.iterator();

        while (iterator.hasNext()) {
            SelectionKey selectionKey = iterator.next();
            iterator.remove();

            if (selectionKey == null) {
                continue;
            }

            try {
                selectionKey.cancel();
            }
            catch (Exception ex) {
                getLogger().error(ex.getMessage(), ex);
            }
        }
    }

    protected void closeSelector() {
        if (getSelector().isOpen()) {
            try {
                getSelector().close();
            }
            catch (Exception ex) {
                getLogger().error(ex.getMessage(), ex);
            }
        }
    }

    protected Logger getLogger() {
        return this.logger;
    }

    protected Selector getSelector() {
        return this.selector;
    }

    protected Semaphore getStopLock() {
        return this.stopLock;
    }

    protected boolean isExitCondition(final int readyChannels) {
        return isShutdown() || !getSelector().isOpen();
    }

    protected boolean isShutdown() {
        return this.isShutdown;
    }

    protected void onAcceptable(final SelectionKey selectionKey) {
        // Empty
    }

    protected void onConnectable(final SelectionKey selectionKey) {
        // Empty
    }

    protected void onInValid(final SelectionKey selectionKey) {
        // Empty
    }

    protected void onReadable(final SelectionKey selectionKey) {
        // Empty
    }

    protected void onWritable(final SelectionKey selectionKey) {
        // Empty
    }

    protected void setShutdown() {
        this.isShutdown = true;
    }
}
