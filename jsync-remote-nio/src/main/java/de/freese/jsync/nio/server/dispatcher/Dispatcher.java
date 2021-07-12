// Created: 10.09.2020
package de.freese.jsync.nio.server.dispatcher;

import java.nio.channels.SocketChannel;

/**
 * Der {@link Dispatcher} kümmert sich um das Connection-Handling der Clients nach dem 'accept'.<br>
 *
 * @author Thomas Freese
 */
@FunctionalInterface
public interface Dispatcher
{
    /**
     * @param socketChannel {@link SocketChannel}
     */
    void register(final SocketChannel socketChannel);
}
