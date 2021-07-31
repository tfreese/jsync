// Created: 31.07.2021
package de.freese.jsync.rsocket.factory;

import java.net.InetSocketAddress;

import reactor.core.Disposable;

/**
 * Siehe<br>
 * org.springframework.boot.rsocket.netty.NettyRSocketServerFactory<br>
 * org.springframework.boot.rsocket.netty.NettyRSocketServer<br>
 *
 * @author Thomas Freese
 */
public interface MyRsocketServer extends Disposable
{
    /**
     *
     */
    enum Transport
    {
        /**
         *
         */
        LOCAL,

        /**
         *
         */
        TCP,

        /**
         *
         */
        WEBSOCKET
    }

    /**
     * @see reactor.core.Disposable#dispose()
     */
    @Override
    default void dispose()
    {
        stop();
    }

    /**
     * @return {@link InetSocketAddress}
     */
    InetSocketAddress getAddress();

    /**
     * @throws Exception Falls was schief geht.
     */
    void start() throws Exception;

    /**
     *
     */
    void stop();
}
