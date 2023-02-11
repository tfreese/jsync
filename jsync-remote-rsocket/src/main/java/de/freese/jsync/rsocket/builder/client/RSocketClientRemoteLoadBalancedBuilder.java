// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.client;

import java.net.SocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.loadbalance.LoadbalanceRSocketClient;
import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Flux;
import reactor.netty.tcp.TcpClient;

/**
 * @author Thomas Freese
 */
public class RSocketClientRemoteLoadBalancedBuilder extends AbstractRSocketClientRemoteBuilder<RSocketClientRemoteLoadBalancedBuilder> {
    private final List<SocketAddress> remoteAddresses = new ArrayList<>();

    /**
     * @see de.freese.jsync.rsocket.builder.AbstractRSocketBuilder#build()
     */
    @Override
    public RSocketClient build() {
        // @formatter:off
        Publisher<List<LoadbalanceTarget>> serverProducer = Flux.fromIterable(this.remoteAddresses)
            .map(serverAddress ->  {
                TcpClient tcpClient = configure(TcpClient.create()).remoteAddress(() -> serverAddress);
                ClientTransport clientTransport = TcpClientTransport.create(tcpClient);

                return LoadbalanceTarget.from(serverAddress.toString(), clientTransport);
            })
            .collectList()
            ;
        // @formatter:on

        // Publisher<List<LoadbalanceTarget>> serverProducer2 = Flux.interval(Duration.ofSeconds(1)).log().map(i -> {
        // int val = i.intValue();
        //
        // return switch (val)
        // {
        // case 0 -> Collections.emptyList();
        // case 1 -> List.of(targets.get(0));
        // case 2 -> List.of(targets.get(0), targets.get(1));
        // case 3 -> List.of(targets.get(0), targets.get(2));
        // case 4 -> List.of(targets.get(1), targets.get(2));
        // case 5 -> List.of(targets.get(0), targets.get(1), targets.get(2));
        // case 6 -> Collections.emptyList();
        // case 7 -> Collections.emptyList();
        // default -> List.of(targets.get(0), targets.get(1), targets.get(2));
        // };
        // });

        RSocketConnector rSocketConnector = configure(RSocketConnector.create());

        // @formatter:off
        return LoadbalanceRSocketClient.builder(serverProducer)
                .connector(rSocketConnector)
                .roundRobinLoadbalanceStrategy()
                // .weightedLoadbalanceStrategy()
                .build()
                ;
        // @formatter:on
    }

    public RSocketClientRemoteLoadBalancedBuilder remoteAddresses(final List<? extends SocketAddress> remoteAddresses) {
        Objects.requireNonNull(remoteAddresses, "remoteAddresses required");

        this.remoteAddresses.addAll(remoteAddresses);

        return this;
    }
}
