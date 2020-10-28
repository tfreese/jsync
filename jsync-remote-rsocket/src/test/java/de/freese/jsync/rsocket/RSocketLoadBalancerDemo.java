// Created: 28.10.2020
package de.freese.jsync.rsocket;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.client.LoadBalancedRSocketMono;
import io.rsocket.client.filter.RSocketSupplier;
import io.rsocket.core.RSocketConnector;
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
     * @return {@link Mono}
     * @throws Exception Falls was schief geht
     */
    private static Mono<RSocket> createClient(final int...ports) throws Exception
    {
       // @formatter:off
       List<RSocketSupplier> rSocketSupplierList = IntStream.of(ports)
               .mapToObj(port -> {
                       TcpClient tcpClient = TcpClient.create()
                           .host("localhost")
                           .port(port)
                           .runOn(LoopResources.create("client-" + port, 2, true))
                           ;

                       return RSocketConnector
                           .create()
                           .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(1)))
                           .connect(TcpClientTransport.create(tcpClient))
                           .block()
                           ;
               })
               .map(rsocket -> new RSocketSupplier(() -> Mono.just(rsocket)))
               .collect(Collectors.toList())
               ;
       // @formatter:on

        Publisher<List<RSocketSupplier>> factories = subscriber -> {
            subscriber.onNext(rSocketSupplierList);
            subscriber.onComplete();
        };

        LoadBalancedRSocketMono balancer = LoadBalancedRSocketMono.create(factories);

        return balancer;
    }

    /**
     * @param args String[]
     * @throws Exception Falls was schief geht
     */
    public static void main(final String[] args) throws Exception
    {
        Hooks.onErrorDropped(th -> LOGGER.warn(th.getMessage()));

        List<Disposable> servers = List.of(startServer(7000), startServer(8000), startServer(9000));

        Mono<RSocket> client = createClient(7000, 8000, 9000);

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
           client.block()
               .requestResponse(DefaultPayload.create("test " + i))
               .map(Payload::getDataUtf8)
               .doOnNext(LOGGER::info)
               .block()
               ;
           // @formatter:on
        }

        client.block().dispose();
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
