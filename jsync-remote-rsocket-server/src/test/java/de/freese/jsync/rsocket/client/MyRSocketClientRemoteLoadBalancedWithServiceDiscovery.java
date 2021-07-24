// Created: 11.07.2021
package de.freese.jsync.rsocket.client;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.loadbalance.LoadbalanceRSocketClient;
import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.SslProvider.ProtocolSslContextSpec;
import reactor.netty.tcp.TcpClient;

/**
 * @author Thomas Freese
 */
public class MyRSocketClientRemoteLoadBalancedWithServiceDiscovery implements MyRSocketClient<List<InetSocketAddress>>
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(MyRSocketClientRemoteLoadBalancedWithServiceDiscovery.class);

    /**
    *
    */
    private RSocketClient client;

    /**
     * @see de.freese.jsync.rsocket.client.MyRSocketClient#connect(java.lang.Object)
     */
    @Override
    public void connect(final List<InetSocketAddress> serverInfos) throws Exception
    {
        ProtocolSslContextSpec protocolSslContextSpec = MyRSocketClientRemote.createProtocolSslContextSpec();
        Random random = new Random();

        // Wechselnde Service-Discovery Anfrage simulieren.
        // org.springframework.cloud.client.discovery.DiscoveryClient - org.springframework.cloud:spring-cloud-commons

        // @formatter:off
        Supplier<List<InetSocketAddress>> serviceRegistry = () -> serverInfos.stream()
                .filter(server -> random.nextBoolean()) // Nicht jeden Server verwenden.
                .toList()
                ;
        // @formatter:on

        // @formatter:off
        Publisher<List<LoadbalanceTarget>> serverProducer = Mono.fromSupplier(serviceRegistry)
                .map(servers -> {
                    LOGGER.info("Update Server Instances: {}", servers);

                    return servers.stream()
                            .map(serverAddress -> {
                                TcpClient tcpClient = MyRSocketClientRemote.createTcpClient(serverAddress, protocolSslContextSpec);
                                ClientTransport clientTransport = TcpClientTransport.create(tcpClient);

                                return LoadbalanceTarget.from(serverAddress.toString(), clientTransport);
                            })
                            .toList()
                            ;
                })
                .repeatWhen(flux -> flux.delayElements(Duration.ofMillis(600))) // Flux regelmäßig aktualisieren.
                ;
        // @formatter:on

        RSocketConnector rSocketConnector = MyRSocketClientRemote.createRSocketConnector();

        // @formatter:off
        this.client = LoadbalanceRSocketClient.builder(serverProducer)
                .connector(rSocketConnector)
                .roundRobinLoadbalanceStrategy()
                // .weightedLoadbalanceStrategy()
                .build()
                ;
        // @formatter:on
    }

    /**
     * @see de.freese.jsync.rsocket.client.MyRSocketClient#disconnect()
     */
    @Override
    public void disconnect()
    {
        getClient().dispose();
    }

    /**
     * @see de.freese.jsync.rsocket.client.MyRSocketClient#getClient()
     */
    @Override
    public RSocketClient getClient()
    {
        return this.client;
    }
}
