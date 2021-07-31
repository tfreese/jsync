// Created: 11.07.2021
package de.freese.jsync.rsocket;

import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.List;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import de.freese.jsync.rsocket.builder.RSocketBuilders;
import io.rsocket.Payload;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketClient;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.util.DefaultPayload;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpResources;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

/**
 * @author Thomas Freese
 */
public class RSocketDemo
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketDemo.class);

    /**
     * Remote
     *
     * @param socketAcceptor {@link Function}
     *
     * @return {@link Tuple2}
     *
     * @throws Exception Falls was schief geht.
     */
    static Tuple2<RSocketClient, List<Disposable>> createRemote(final Function<Integer, SocketAcceptor> socketAcceptor) throws Exception
    {
        InetSocketAddress serverAddress = new InetSocketAddress("localhost", 6000);

        // @formatter:off
        CloseableChannel server = RSocketBuilders.serverRemote()
                .socketAddress(serverAddress)
                .socketAcceptor(socketAcceptor.apply(serverAddress.getPort()))
                .resumeDefault()
                .logTcpServerBoundStatus()
                .logger(LoggerFactory.getLogger("server"))
                .protocolSslContextSpecCertificate()
                .build()
                .block()
                ;
        // @formatter:on

        // @formatter:off
        RSocketClient client = RSocketBuilders.clientRemote()
                .remoteAddress(serverAddress)
                .resumeDefault()
                .retryDefault()
                .logTcpClientBoundStatus()
                .logger(LoggerFactory.getLogger("client"))
                .protocolSslContextSpecTrusted()
                .build()
                ;
        // @formatter:on

        return Tuples.of(client, List.of(server));
    }

    /**
     * Remote mit Client-LoadBalancer.
     *
     * @param socketAcceptor {@link Function}
     *
     * @return {@link Tuple2}
     *
     * @throws Exception Falls was schief geht.
     */
    static Tuple2<RSocketClient, List<Disposable>> createRemoteWithLoadBalancer(final Function<Integer, SocketAcceptor> socketAcceptor) throws Exception
    {
        InetSocketAddress socketAddress1 = new InetSocketAddress("localhost", 6000);
        InetSocketAddress socketAddress2 = new InetSocketAddress("localhost", 7000);

        CloseableChannel server1 = RSocketBuilders.serverRemote().socketAddress(socketAddress1).socketAcceptor(socketAcceptor.apply(socketAddress1.getPort()))
                .resumeDefault().logTcpServerBoundStatus().logger(LoggerFactory.getLogger("server1")).build().block();
        CloseableChannel server2 = RSocketBuilders.serverRemote().socketAddress(socketAddress2).socketAcceptor(socketAcceptor.apply(socketAddress2.getPort()))
                .resumeDefault().logTcpServerBoundStatus().logger(LoggerFactory.getLogger("server2")).build().block();

        RSocketClient client = RSocketBuilders.clientRemoteLoadBalanced().remoteAddresses(List.of(socketAddress1, socketAddress2)).resumeDefault()
                .retryDefault().logTcpClientBoundStatus().logger(LoggerFactory.getLogger("client")).build();

        return Tuples.of(client, List.of(server1, server2));
    }

    /**
     * Remote mit Client-LoadBalancer und Service-Discovery.
     *
     * @param socketAcceptor {@link Function}
     *
     * @return {@link Tuple2}
     *
     * @throws Exception Falls was schief geht.
     */
    static Tuple2<RSocketClient, List<Disposable>> createRemoteWithLoadBalancerAndServiceDiscovery(final Function<Integer, SocketAcceptor> socketAcceptor)
        throws Exception
    {
        InetSocketAddress socketAddress1 = new InetSocketAddress("localhost", 6000);
        InetSocketAddress socketAddress2 = new InetSocketAddress("localhost", 7000);
        InetSocketAddress socketAddress3 = new InetSocketAddress("localhost", 8000);
        InetSocketAddress socketAddress4 = new InetSocketAddress("localhost", 9000);

        CloseableChannel server1 = RSocketBuilders.serverRemote().socketAddress(socketAddress1).socketAcceptor(socketAcceptor.apply(socketAddress1.getPort()))
                .resumeDefault().logTcpServerBoundStatus().logger(LoggerFactory.getLogger("server1")).build().block();
        CloseableChannel server2 = RSocketBuilders.serverRemote().socketAddress(socketAddress2).socketAcceptor(socketAcceptor.apply(socketAddress2.getPort()))
                .resumeDefault().logTcpServerBoundStatus().logger(LoggerFactory.getLogger("server2")).build().block();
        CloseableChannel server3 = RSocketBuilders.serverRemote().socketAddress(socketAddress3).socketAcceptor(socketAcceptor.apply(socketAddress3.getPort()))
                .resumeDefault().logTcpServerBoundStatus().logger(LoggerFactory.getLogger("server3")).build().block();
        CloseableChannel server4 = RSocketBuilders.serverRemote().socketAddress(socketAddress4).socketAcceptor(socketAcceptor.apply(socketAddress4.getPort()))
                .resumeDefault().logTcpServerBoundStatus().logger(LoggerFactory.getLogger("server4")).build().block();

        List<SocketAddress> serverAddresses = List.of(socketAddress1, socketAddress2, socketAddress3, socketAddress4);

        // @formatter:off
        // Wechselnde Service-Discovery Anfrage simulieren.
        // org.springframework.cloud.client.discovery.DiscoveryClient - org.springframework.cloud:spring-cloud-commons
        Random random = new Random();
        Supplier<List<SocketAddress>> serviceDiscovery = () -> serverAddresses.stream()
                .filter(server -> random.nextBoolean()) // Nicht jeden Server verwenden.
                .toList()
                ;
        // @formatter:on

        RSocketClient client = RSocketBuilders.clientRemoteLoadBalancedWithServiceDiscovery().serviceDiscovery(serviceDiscovery).resumeDefault().retryDefault()
                .logTcpClientBoundStatus().logger(LoggerFactory.getLogger("client")).build();

        return Tuples.of(client, List.of(server1, server2, server3, server4));
    }

    /**
     * Client und Server in der gleichen Runtime ohne Network-Stack.
     *
     * @param socketAcceptor {@link Function}
     *
     * @return {@link Tuple2}
     *
     * @throws Exception Falls was schief geht.
     */
    static Tuple2<RSocketClient, List<Disposable>> createSameVm(final Function<Integer, SocketAcceptor> socketAcceptor) throws Exception
    {
        // @formatter:off
        Disposable server = RSocketBuilders.serverLocal()
                .name("test1")
                .socketAcceptor(socketAcceptor.apply(0))
                .build()
                .block()
                ;
        // @formatter:on

        RSocketClient client = RSocketBuilders.clientLocal().name("test1").build();

        return Tuples.of(client, List.of(server));
    }

    /**
     * @param args String[]
     *
     * @throws Exception Falls was schief geht.
     */
    public static void main(final String[] args) throws Exception
    {
        System.setProperty("reactor.schedulers.defaultPoolSize", Integer.toString(8));
        System.setProperty("reactor.schedulers.defaultBoundedElasticSize", Integer.toString(8));

        // Globale Default-Resourcen.
        // TcpResources.set(LoopResources.create("rsocket"));
        TcpResources.set(LoopResources.create("rsocket", 2, 8, true));
        // TcpResources.set(ConnectionProvider.create("rsocket-connectionPool", 16));

        // Fehlermeldung, wenn Client die Verbindung schliesst.
        // Nur einmalig definieren, sonst gibs mehrere Logs-Meldungen !!!
        Hooks.onErrorDropped(th -> LOGGER.warn(th.getMessage()));
        // Hooks.onErrorDropped(th -> {
        // });

        // Debug einschalten.
        Hooks.onOperatorDebug();

        Function<Integer, SocketAcceptor> socketAcceptor = port -> SocketAcceptor.forRequestResponse(payload -> {
            String request = payload.getDataUtf8();
            LOGGER.info("Server {} got request {}", port, request);
            return Mono.just(DefaultPayload.create("Client of Server " + port + " got response " + request));
        });

        // Tuple2<RSocketClient, List<Disposable>> tuple = createSameVm(socketAcceptor);
        Tuple2<RSocketClient, List<Disposable>> tuple = createRemote(socketAcceptor);
        // Tuple2<RSocketClient, List<Disposable>> tuple = createRemoteWithLoadBalancer(socketAcceptor);
        // Tuple2<RSocketClient, List<Disposable>> tuple = createRemoteWithLoadBalancerAndServiceDiscovery(socketAcceptor);

        RSocketClient rSocketClient = tuple.getT1();
        List<Disposable> servers = tuple.getT2();

        // Der IntStream blockiert, bis alle parallelen Operationen beendet sind.
        // Der Flux macht dies nicht, sondern im Hintergrund.
        // @formatter:off
        Flux.range(0, 30).parallel().runOn(Schedulers.boundedElastic())
            .map(i ->
                rSocketClient
                    .requestResponse(Mono.just(DefaultPayload.create("flux-" + i.intValue())))
                    .map(Payload::getDataUtf8)
                    .doOnNext(LOGGER::info)
                )
            .subscribe(Mono::subscribe)
            ;
        // @formatter:on

        // Der IntStream blockiert, bis alle parallelen Operationen beendet sind.
        // Der Flux macht dies nicht, sondern im Hintergrund.
        // @formatter:off
        IntStream.range(0, 30).parallel()
            .mapToObj(i ->
                rSocketClient
                    .requestResponse(Mono.just(DefaultPayload.create("intstream-" + i)))
                    .map(Payload::getDataUtf8)
                    .doOnNext(LOGGER::info)
                )
            .forEach(Mono::subscribe)
            ;
        // @formatter:on

        TimeUnit.SECONDS.sleep(1);

        for (int i = 0; i < 30; i++)
        {
            TimeUnit.MILLISECONDS.sleep(100);

            // @formatter:off
            rSocketClient
                .requestResponse(Mono.just(DefaultPayload.create("for-" + i)))
                .map(Payload::getDataUtf8)
                .doOnNext(LOGGER::info)
                //.block() // Wartet auf den Response, hier knallt es bei ungültigen Connections (ServiceDiscovery).
                .subscribe() // Führt alles im Hintergrund aus.
                ;
            // @formatter:on
        }

        TimeUnit.SECONDS.sleep(2);
        // System.in.read();

        rSocketClient.dispose();
        servers.forEach(Disposable::dispose);
    }
}
