// Created: 11.07.2021
package de.freese.jsync.rsocket.client;

import java.net.InetSocketAddress;
import java.util.List;

import org.reactivestreams.Publisher;

import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.loadbalance.LoadbalanceRSocketClient;
import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import reactor.core.publisher.Flux;
import reactor.netty.tcp.SslProvider.ProtocolSslContextSpec;
import reactor.netty.tcp.TcpClient;

/**
 * @author Thomas Freese
 */
public class MyRSocketClientRemoteLoadBalanced implements MyRSocketClient<List<InetSocketAddress>>
{
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

        // @formatter:off
        Publisher<List<LoadbalanceTarget>> serverProducer = Flux.fromIterable(serverInfos)
            .map(serverAddress ->  {
                TcpClient tcpClient = MyRSocketClientRemote.createTcpClient(serverAddress, protocolSslContextSpec);
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
