// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.client;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;

import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.loadbalance.LoadbalanceRSocketClient;
import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import org.reactivestreams.Publisher;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.TcpClient;

/**
 * @author Thomas Freese
 */
public class RSocketClientRemoteLoadBalancedWithServiceDiscoveryBuilder
        extends AbstractRSocketClientRemoteBuilder<RSocketClientRemoteLoadBalancedWithServiceDiscoveryBuilder>
{
    private Supplier<List<SocketAddress>> serviceDiscovery;

    /**
     * @see de.freese.jsync.rsocket.builder.AbstractRSocketBuilder#build()
     */
    @Override
    public RSocketClient build()
    {
        // @formatter:off
        Publisher<List<LoadbalanceTarget>> serverProducer = Mono.fromSupplier(this.serviceDiscovery)
                .map(servers -> {
                    getLogger().info("Update Server Instances: {}", servers);

                    return servers.stream()
                            .map(serverAddress -> {
                                TcpClient tcpClient = configure(TcpClient.create()).remoteAddress(() -> serverAddress);
                                ClientTransport clientTransport = TcpClientTransport.create(tcpClient);

                                return LoadbalanceTarget.from(serverAddress.toString(), clientTransport);
                            })
                            .toList()
                            ;
                })
                .repeatWhen(flux -> flux.delayElements(Duration.ofMillis(600))) // Flux regelmäßig aktualisieren.
                ;
        // @formatter:on

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

    /**
     * Simulate Service-Discovery.<br>
     * org.springframework.cloud.client.discovery.DiscoveryClient - org.springframework.cloud:spring-cloud-commons
     */
    public RSocketClientRemoteLoadBalancedWithServiceDiscoveryBuilder serviceDiscovery(final Supplier<List<SocketAddress>> serviceDiscovery)
    {
        this.serviceDiscovery = Objects.requireNonNull(serviceDiscovery, "serviceDiscovery required");

        return this;
    }
}
