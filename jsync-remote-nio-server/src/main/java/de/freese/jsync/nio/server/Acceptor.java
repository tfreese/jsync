// Created: 08.09.2020
package de.freese.jsync.nio.server;

import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.Objects;

import de.freese.jsync.nio.server.dispatcher.Dispatcher;

/**
 * Der {@link Acceptor} nimmt die neuen Client-Verbindungen entgegen und übergibt sie einem {@link Dispatcher}.<br>
 *
 * @author Thomas Freese
 */
class Acceptor extends AbstractNioProcessor
{
    /**
     *
     */
    private final Dispatcher dispatcher;
    /**
     *
     */
    private final ServerSocketChannel serverSocketChannel;

    /**
     * Erstellt ein neues {@link Acceptor} Object.
     *
     * @param selector {@link Selector}
     * @param serverSocketChannel {@link ServerSocketChannel}
     * @param dispatcher {@link Dispatcher}
     */
    public Acceptor(final Selector selector, final ServerSocketChannel serverSocketChannel, final Dispatcher dispatcher)
    {
        super(selector);

        this.serverSocketChannel = Objects.requireNonNull(serverSocketChannel, "serverSocketChannel required");
        this.dispatcher = Objects.requireNonNull(dispatcher, "dispatcher required");
    }

    /**
     * @see de.freese.jsync.nio.server.AbstractNioProcessor#beforeSelectorWhile()
     */
    @Override
    protected void beforeSelectorWhile() throws Exception
    {
        this.serverSocketChannel.register(getSelector(), SelectionKey.OP_ACCEPT);
    }

    /**
     * @see de.freese.jsync.nio.server.AbstractNioProcessor#onAcceptable(java.nio.channels.SelectionKey)
     */
    @Override
    protected void onAcceptable(final SelectionKey selectionKey)
    {
        try
        {
            // Verbindung mit Client herstellen.
            SocketChannel socketChannel = this.serverSocketChannel.accept();

            if (socketChannel == null)
            {
                // Falls sich schon ein anderer Acceptor den Channel geschnappt hat.
                // Deswegen ist es auch Blödsinn mehrere zu registrieren, da immer alle reagieren, aber nur einer den Channel hat.
                return;
            }

            getLogger().debug("{}: connection accepted", socketChannel.getRemoteAddress());

            // Socket dem Dispatcher übergeben.
            this.dispatcher.register(socketChannel);
        }
        catch (Exception ex)
        {
            getLogger().error(null, ex);
        }
    }
}
