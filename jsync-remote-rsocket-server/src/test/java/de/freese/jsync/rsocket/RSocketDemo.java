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
import java.util.stream.Stream;

import io.rsocket.Payload;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketClient;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.util.DefaultPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpResources;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuples;

import de.freese.jsync.rsocket.builder.RSocketBuilders;

/**
 * @author Thomas Freese
 */
public final class RSocketDemo {
    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketDemo.class);

    public static void main(final String[] args) throws Exception {
        System.setProperty("reactor.schedulers.defaultPoolSize", Integer.toString(8));
        System.setProperty("reactor.schedulers.defaultBoundedElasticSize", Integer.toString(8));

        System.setProperty("reactor.netty.ioSelectCount", Integer.toString(4));
        System.setProperty("reactor.netty.ioWorkerCount", Integer.toString(8));

        // Global Default-Resources.
        TcpResources.set(LoopResources.create("rSocket"));
        // TcpResources.set(LoopResources.create("rSocket", 2, 8, true));
        // TcpResources.set(ConnectionProvider.create("connectionPool", 16));

        // Exception, if Client closed the Connection.
        // Create only once, unless multiple Logs are created !!!
        Hooks.onErrorDropped(th -> LOGGER.warn(th.getMessage()));
        // Hooks.onErrorDropped(th -> {
        // });

        // Enable Debug.
        Hooks.onOperatorDebug();

        final Function<Integer, SocketAcceptor> socketAcceptor = port -> SocketAcceptor.forRequestResponse(payload -> {
            final String request = payload.getDataUtf8();
            LOGGER.info("Server {} got request {}", port, request);
            return Mono.just(DefaultPayload.create("Client of Server " + port + " got response " + request));
        });

        // Tuple2<RSocketClient, List<Disposable>> tuple = createSameVm(socketAcceptor);
        final Tuple2<RSocketClient, List<Disposable>> tuple = createRemote(socketAcceptor);
        // Tuple2<RSocketClient, List<Disposable>> tuple = createRemoteWithLoadBalancer(socketAcceptor);
        // Tuple2<RSocketClient, List<Disposable>> tuple = createRemoteWithLoadBalancerAndServiceDiscovery(socketAcceptor);

        final RSocketClient rSocketClient = tuple.getT1();
        final List<Disposable> servers = tuple.getT2();

        // The Flux do not block.
        // @formatter:off
        Flux.range(0, 30).parallel().runOn(Schedulers.boundedElastic())
            .map(i ->
                rSocketClient
                    .requestResponse(Mono.just(DefaultPayload.create("flux-" + i)))
                    .map(Payload::getDataUtf8)
                    .doOnNext(LOGGER::info)
                )
            .subscribe(Mono::subscribe)
            ;
        // @formatter:on

        // The IntStream blocked, until all parallel Operations are finished.
        // @formatter:off
        IntStream.range(0, 30).parallel()
            .mapToObj(i ->
                rSocketClient
                    .requestResponse(Mono.just(DefaultPayload.create("intStream-" + i)))
                    .map(Payload::getDataUtf8)
                    .doOnNext(LOGGER::info)
                )
            .forEach(Mono::subscribe)
            ;
        // @formatter:on

        TimeUnit.SECONDS.sleep(1);

        for (int i = 0; i < 30; i++) {
            TimeUnit.MILLISECONDS.sleep(100);

            // @formatter:off
            rSocketClient
                .requestResponse(Mono.just(DefaultPayload.create("for-" + i)))
                .map(Payload::getDataUtf8)
                .doOnNext(LOGGER::info)
                //.block()
                .subscribe()
                ;
            // @formatter:on
        }

        TimeUnit.SECONDS.sleep(2);
        // System.in.read();

        rSocketClient.dispose();
        servers.forEach(Disposable::dispose);

        Schedulers.shutdownNow();
    }

    static Tuple2<RSocketClient, List<Disposable>> createRemote(final Function<Integer, SocketAcceptor> socketAcceptor) throws Exception {
        final InetSocketAddress serverAddress = new InetSocketAddress("localhost", 6000);

        // @formatter:off
        final CloseableChannel server = RSocketBuilders.serverRemote()
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
        final RSocketClient client = RSocketBuilders.clientRemote()
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

    static Tuple2<RSocketClient, List<Disposable>> createRemoteWithLoadBalancer(final Function<Integer, SocketAcceptor> socketAcceptor) throws Exception {
        final List<InetSocketAddress> serverAddresses = Stream.of(6000, 7000).map(port -> new InetSocketAddress("localhost", port)).toList();

        // @formatter:off
        final List<Disposable> servers = serverAddresses.stream()
                .map(serverAddress -> RSocketBuilders.serverRemote()
                        .socketAddress(serverAddress)
                        .socketAcceptor(socketAcceptor.apply(serverAddress.getPort()))
                        .resumeDefault()
                        .logTcpServerBoundStatus()
                        .logger(LoggerFactory.getLogger("server-" + serverAddress.getPort()))
                        .build())
                .map(Mono::block)
                .map(Disposable.class::cast)
                .toList()
                ;

        final RSocketClient client = RSocketBuilders.clientRemoteLoadBalanced()
                .remoteAddresses(serverAddresses)
                .resumeDefault()
                .retryDefault()
                .logTcpClientBoundStatus()
                .logger(LoggerFactory.getLogger("client"))
                .build()
                ;
        // @formatter:on

        return Tuples.of(client, servers);
    }

    static Tuple2<RSocketClient, List<Disposable>> createRemoteWithLoadBalancerAndServiceDiscovery(final Function<Integer, SocketAcceptor> socketAcceptor) throws Exception {
        final List<InetSocketAddress> serverAddresses = Stream.of(6000, 7000, 8000, 9000).map(port -> new InetSocketAddress("localhost", port)).toList();

        // @formatter:off
        // Simulate Service-Discovery.
        // org.springframework.cloud.client.discovery.DiscoveryClient - org.springframework.cloud:spring-cloud-commons
        final Random random = new Random();
        final Supplier<List<SocketAddress>> serviceDiscovery = () -> serverAddresses.stream()
                .filter(server -> random.nextBoolean()) // Do not use every Server.
                .map(SocketAddress.class::cast)
                .toList()
                ;

        final List<Disposable> servers = serverAddresses.stream()
                .map(serverAddress -> RSocketBuilders.serverRemote()
                        .socketAddress(serverAddress)
                        .socketAcceptor(socketAcceptor.apply(serverAddress.getPort()))
                        .resumeDefault()
                        .logTcpServerBoundStatus()
                        .logger(LoggerFactory.getLogger("server-" + serverAddress.getPort()))
                        .build())
                .map(Mono::block)
                .map(Disposable.class::cast)
                .toList()
                ;

        final RSocketClient client = RSocketBuilders.clientRemoteLoadBalancedWithServiceDiscovery()
                .serviceDiscovery(serviceDiscovery)
                .resumeDefault()
                .retryDefault()
                .logTcpClientBoundStatus()
                .logger(LoggerFactory.getLogger("client"))
                .build()
                ;
        // @formatter:on

        return Tuples.of(client, servers);
    }

    static Tuple2<RSocketClient, List<Disposable>> createSameVm(final Function<Integer, SocketAcceptor> socketAcceptor) throws Exception {
        // @formatter:off
        final Disposable server = RSocketBuilders.serverLocal()
                .name("test1")
                .socketAcceptor(socketAcceptor.apply(0))
                .build()
                .block()
                ;
        // @formatter:on

        final RSocketClient client = RSocketBuilders.clientLocal().name("test1").build();

        return Tuples.of(client, List.of(server));
    }

    private RSocketDemo() {
        super();
    }
}
