// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.client;

import java.net.SocketAddress;

import io.rsocket.RSocket;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;

/**
 * @author Thomas Freese
 */
public class RSocketClientBuilderRemote extends AbstractClientBuilderRemote<RSocketClientBuilderRemote> {

    @Override
    public RSocketClient build() {
        final TcpClient tcpClient = getBuilderSupport().configure(TcpClient.create());
        final RSocketConnector rSocketConnector = getBuilderSupport().configure(RSocketConnector.create());

        final ClientTransport clientTransport = TcpClientTransport.create(tcpClient);

        final Mono<RSocket> rSocket = rSocketConnector.connect(clientTransport);

        return RSocketClient.from(rSocket);
    }

    public RSocketClientBuilderRemote remoteAddress(final SocketAddress remoteAddress) {
        getBuilderSupport().remoteAddress(remoteAddress);

        return this;
    }

    @Override
    protected RSocketClientBuilderRemote self() {
        return this;
    }
}
