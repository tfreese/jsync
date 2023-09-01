// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.client;

import java.net.SocketAddress;
import java.util.Objects;

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
public class RSocketClientRemoteBuilder extends AbstractRSocketClientRemoteBuilder<RSocketClientRemoteBuilder> {
    @Override
    public RSocketClient build() {
        TcpClient tcpClient = configure(TcpClient.create());
        RSocketConnector rSocketConnector = configure(RSocketConnector.create());

        ClientTransport clientTransport = TcpClientTransport.create(tcpClient);

        Mono<RSocket> rSocket = rSocketConnector.connect(clientTransport);

        return RSocketClient.from(rSocket);
    }

    public RSocketClientRemoteBuilder remoteAddress(final SocketAddress remoteAddress) {
        Objects.requireNonNull(remoteAddress, "remoteAddress required");

        addTcpClientCustomizer(tcpClient -> tcpClient.remoteAddress(() -> remoteAddress));

        return this;
    }
}
