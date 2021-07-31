// Created: 31.07.2021
package de.freese.jsync.rsocket.factory;

import io.rsocket.SocketAcceptor;

/**
 * Siehe<br>
 * org.springframework.boot.rsocket.netty.NettyRSocketServerFactory<br>
 * org.springframework.boot.rsocket.netty.NettyRSocketServer<br>
 *
 * @author Thomas Freese
 */
public interface MyRsocketServerFactory
{
    /**
     * Erzeugt einen Server, aber startet ihn nicht.<br>
     *
     * @param socketAcceptor {@link SocketAcceptor}
     *
     * @return {@link MyRsocketServer}
     *
     * @see MyRsocketServer#start()
     * @see MyRsocketServer#stop()
     */
    MyRsocketServer create(SocketAcceptor socketAcceptor);
}
