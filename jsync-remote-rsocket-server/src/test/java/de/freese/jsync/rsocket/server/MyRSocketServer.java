// Created: 11.07.2021
package de.freese.jsync.rsocket.server;

import io.rsocket.Closeable;

/**
 * Siehe org.springframework.boot.rsocket.netty.NettyRSocketServer
 *
 * @author Thomas Freese
 */
public interface MyRSocketServer
{
    /**
     * @return {@link Closeable}
     */
    Closeable getServer();

    /**
     * @throws Exception Falls was schief geht.
     */
    void start() throws Exception;

    /**
     *
     */
    void stop();
}
