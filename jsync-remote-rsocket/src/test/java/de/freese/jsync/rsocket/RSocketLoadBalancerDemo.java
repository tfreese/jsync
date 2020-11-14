// Created: 28.10.2020
package de.freese.jsync.rsocket;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.rsocket.Payload;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.loadbalance.LoadbalanceRSocketClient;
import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.Disposable;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public class RSocketLoadBalancerDemo
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketLoadBalancerDemo.class);

    /**
     * @param ports int[]
     * @return {@link RSocketClient}
     * @throws Exception Falls was schief geht
     */
    private static RSocketClient createClient(final int...ports) throws Exception
    {
        List<LoadbalanceTarget> targets = new ArrayList<>(ports.length);

        for (int port : ports)
        {
            // @formatter:off
            TcpClient tcpClient = TcpClient.create()
                    .host("localhost")
                    .port(port)
                    .runOn(LoopResources.create("client-" + port, 2, true))
                    ;
            // @formatter:on

            ClientTransport clientTransport = TcpClientTransport.create(tcpClient);
            // ClientTransport clientTransport = LocalClientTransport.create("test-local-" + port);

            LoadbalanceTarget target = LoadbalanceTarget.from(Integer.toString(port), clientTransport);

            targets.add(target);
        }

        Mono<List<LoadbalanceTarget>> producer = Mono.just(targets);

        // Flux<List<LoadbalanceTarget>> producer = Flux.interval(Duration.ofSeconds(2)).log().map(i -> {
        // int val = i.intValue();
        // switch (val)
        // {
        // case 0:
        // return Collections.emptyList();
        // case 1:
        // return List.of(targets.get(0));
        // case 2:
        // return List.of(targets.get(0), targets.get(1));
        // case 3:
        // return List.of(targets.get(0), targets.get(2));
        // case 4:
        // return List.of(targets.get(1), targets.get(2));
        // case 5:
        // return List.of(targets.get(0), targets.get(1), targets.get(2));
        // case 6:
        // return Collections.emptyList();
        // case 7:
        // return Collections.emptyList();
        // default:
        // return List.of(targets.get(0), targets.get(1), targets.get(2));
        // }
        // });

        RSocketConnector connector = RSocketConnector.create()
                // .payloadDecoder(PayloadDecoder.ZERO_COPY)
                .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(1)));

        // @formatter:off
        RSocketClient client = LoadbalanceRSocketClient
                .builder(producer)
                .connector(connector)
                .roundRobinLoadbalanceStrategy()
                .build()
                ;
        // @formatter:on

        return client;
    }

    /**
     * @param args String[]
     * @throws Exception Falls was schief geht
     */
    public static void main(final String[] args) throws Exception
    {
        Hooks.onErrorDropped(th -> LOGGER.warn(th.getMessage()));

        List<Disposable> servers = List.of(startServer(7000), startServer(8000), startServer(9000));

        RSocketClient client = createClient(7000, 8000, 9000);

        // Request - Response
        for (int i = 0; i < 30; i++)
        {
            if (i == 10)
            {
                // System.out.println();

                // Funktioniert irgendwie nicht.
                // Disposable server = servers.remove(0);
                // server.dispose();
            }

           // @formatter:off
           client.requestResponse(Mono.just(DefaultPayload.create("test " + i)))
               .map(Payload::getDataUtf8)
               .doOnNext(LOGGER::info)
               .block()
               ;
           // @formatter:on
        }

        client.dispose();
        servers.forEach(Disposable::dispose);
    }

    /**
     * @param port int
     * @return {@link Disposable}
     * @throws Exception Falls was schief geht
     */
    private static final Disposable startServer(final int port) throws Exception
    {
        Server server = new Server();
        server.start(port);

        return server;
    }

    /**
     * Erzeugt ein neues {@link RSocketLoadBalancerDemo} Objekt.
     */
    private RSocketLoadBalancerDemo()
    {
        super();
    }
}
