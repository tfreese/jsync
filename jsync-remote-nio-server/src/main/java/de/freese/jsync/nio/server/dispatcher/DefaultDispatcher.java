// Created: 08.09.2020
package de.freese.jsync.nio.server.dispatcher;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;
import java.util.Iterator;
import java.util.Objects;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executor;

import de.freese.jsync.nio.server.AbstractNioProcessor;
import de.freese.jsync.nio.server.handler.IoHandler;

/**
 * The {@link Dispatcher} handles the Client Connections after the 'accept'.<br>
 * The {@link IoHandler} handles the Request and Response in a separate Thread.<br>
 *
 * @author Thomas Freese
 */
class DefaultDispatcher extends AbstractNioProcessor implements Dispatcher {
    private final Executor executor;

    private final IoHandler<SelectionKey> ioHandler;

    private final Queue<SocketChannel> newSessions = new ConcurrentLinkedQueue<>();

    DefaultDispatcher(final Selector selector, final IoHandler<SelectionKey> ioHandler, final Executor executor) {
        super(selector);

        this.ioHandler = Objects.requireNonNull(ioHandler, "ioHandler required");
        this.executor = Objects.requireNonNull(executor, "executor required");
    }

    @Override
    public void register(final SocketChannel socketChannel) {
        if (isShutdown()) {
            return;
        }

        Objects.requireNonNull(socketChannel, "socketChannel required");

        try {
            getLogger().debug("{}: register new channel", socketChannel.getRemoteAddress());

            getNewSessions().add(socketChannel);

            getSelector().wakeup();
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
        }
    }

    @Override
    protected void afterSelectorLoop() {
        // Add the new Channels to the Selector.
        processNewChannels();
    }

    @Override
    protected void afterSelectorWhile() {
        // Close new Channels.
        for (Iterator<SocketChannel> iterator = getNewSessions().iterator(); iterator.hasNext(); ) {
            SocketChannel socketChannel = iterator.next();
            iterator.remove();

            try {

                socketChannel.close();
            }
            catch (Exception ex) {
                getLogger().error(ex.getMessage(), ex);
            }
        }

        super.afterSelectorWhile();
    }

    @Override
    protected void onReadable(final SelectionKey selectionKey) {
        // Request.
        // this.ioHandler.read(selectionKey);

        selectionKey.interestOps(0); // Deactivate Selector-Selektion.

        this.executor.execute(() -> {
            this.ioHandler.read(selectionKey);
            selectionKey.selector().wakeup();
        });
    }

    @Override
    protected void onWritable(final SelectionKey selectionKey) {
        // Response.
        // this.ioHandler.write(selectionKey);

        selectionKey.interestOps(0); // Deactivate Selector-Selektion.

        this.executor.execute(() -> {
            this.ioHandler.write(selectionKey);
            selectionKey.selector().wakeup();
        });
    }

    private Queue<SocketChannel> getNewSessions() {
        return this.newSessions;
    }

    /**
     * Add the new Channels to the Selector.
     */
    private void processNewChannels() {
        if (isShutdown()) {
            return;
        }

        // for (SocketChannel socketChannel = getNewSessions().poll(); socketChannel != null; socketChannel = this.newSessions.poll())
        while (!getNewSessions().isEmpty()) {
            SocketChannel socketChannel = getNewSessions().poll();

            if (socketChannel == null) {
                continue;
            }

            try {
                socketChannel.configureBlocking(false);

                getLogger().debug("{}: register channel on selector", socketChannel.getRemoteAddress());

                //                SelectionKey selectionKey =
                socketChannel.register(getSelector(), SelectionKey.OP_READ);
                // selectionKey.attach(obj)
            }
            catch (Exception ex) {
                getLogger().error(ex.getMessage(), ex);
            }
        }
    }
}
