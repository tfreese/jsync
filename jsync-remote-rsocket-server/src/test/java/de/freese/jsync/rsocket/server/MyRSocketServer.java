// Created: 11.07.2021
package de.freese.jsync.rsocket.server;

import reactor.core.Disposable;

/**
 * Siehe<br>
 * org.springframework.boot.rsocket.netty.NettyRSocketServerFactory<br>
 * org.springframework.boot.rsocket.netty.NettyRSocketServer
 *
 * @author Thomas Freese
 */
public interface MyRSocketServer extends Disposable
{
    /**
     * @see reactor.core.Disposable#dispose()
     */
    @Override
    default void dispose()
    {
        stop();
    }

    /**
     * @throws Exception Falls was schief geht.
     */
    void start() throws Exception;

    /**
     *
     */
    void stop();
}
