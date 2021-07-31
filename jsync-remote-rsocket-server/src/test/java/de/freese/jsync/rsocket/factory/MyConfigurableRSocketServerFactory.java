// Created: 31.07.2021
package de.freese.jsync.rsocket.factory;

import java.net.InetAddress;

import de.freese.jsync.rsocket.factory.MyRsocketServer.Transport;

/**
 * Siehe<br>
 * org.springframework.boot.rsocket.netty.NettyRSocketServerFactory<br>
 * org.springframework.boot.rsocket.netty.NettyRSocketServer<br>
 *
 * @author Thomas Freese
 */
public interface MyConfigurableRSocketServerFactory
{
    /**
     * @param address {@link InetAddress}
     */
    void setAddress(InetAddress address);

    /**
     * @param fragmentSize int [Bytes]
     */
    void setFragmentSize(int fragmentSize);

    /**
     * @param port int
     */
    void setPort(int port);

    /**
     * @param transport {@link Transport}
     */
    void setTransport(MyRsocketServer.Transport transport);

    // /**
    // * Sets the SSL configuration that will be applied to the server's default connector.
    // * @param ssl the SSL configuration
    // */
    // void setSsl(Ssl ssl);
    //
    // /**
    // * Sets a provider that will be used to obtain SSL stores.
    // * @param sslStoreProvider the SSL store provider
    // */
    // void setSslStoreProvider(SslStoreProvider sslStoreProvider);
}
