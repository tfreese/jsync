// Created: 08.09.2020
package de.freese.jsync.nio.server;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Objects;

import de.freese.jsync.nio.server.dispatcher.Dispatcher;

/**
 * The {@link Acceptor} handles new Client-Connections and delegate them to the {@link Dispatcher}.<br>
 *
 * @author Thomas Freese
 */
class Acceptor extends AbstractNioProcessor {
    private final Dispatcher dispatcher;

    private final ServerSocketChannel serverSocketChannel;

    Acceptor(final Selector selector, final ServerSocketChannel serverSocketChannel, final Dispatcher dispatcher) {
        super(selector);

        this.serverSocketChannel = Objects.requireNonNull(serverSocketChannel, "serverSocketChannel required");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher required");
    }

    @Override
    protected void beforeSelectorWhile() throws Exception {
        this.serverSocketChannel.register(getSelector(), SelectionKey.OP_ACCEPT);
    }

    @Override
    protected void onAcceptable(final SelectionKey selectionKey) {
        try {
            // Establish Client Connection.
            SocketChannel socketChannel = this.serverSocketChannel.accept();

            if (socketChannel == null) {
                // In case that another Acceptor has processed the Connection.
                // It is nonsense to register multiple Acceptors, because all are triggered, but only one has the Channel.
                return;
            }

            getLogger().debug("{}: connection accepted", socketChannel.getRemoteAddress());

            // Delegate the Socket to the Dispatcher.
            this.dispatcher.register(socketChannel);
        }
        catch (Exception ex) {
            getLogger().error(ex.getMessage(), ex);
        }
    }
}
