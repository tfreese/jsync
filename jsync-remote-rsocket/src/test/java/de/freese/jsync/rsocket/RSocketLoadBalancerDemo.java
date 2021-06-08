// Created: 28.10.2020
package de.freese.jsync.rsocket;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.rsocket.Payload;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.Resume;
import io.rsocket.loadbalance.LoadbalanceRSocketClient;
import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.DefaultPayload;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.netty.tcp.SslProvider.ProtocolSslContextSpec;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpSslContextSpec;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public final class RSocketLoadBalancerDemo
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
        // @formatter:off
        Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(Retry.fixedDelay(10, Duration.ofSeconds(1))
                        .doBeforeRetry(s -> LOGGER.info("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

        // @formatter:off
        ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forClient()
                .configure(builder -> builder
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .sslProvider(SslProvider.JDK))
                ;
        // @formatter:on

        List<LoadbalanceTarget> targets = new ArrayList<>(ports.length);

        for (int port : ports)
        {
            // @formatter:off
            TcpClient tcpClient = TcpClient.create()
                    .host("localhost")
                    .port(port)
                    .secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec))
                    .doOnDisconnected(connection -> LOGGER.info("Disconnected: {}", connection.channel()))
                    //.runOn(LoopResources.create("client-" + port, 2, true), false)
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

        // @formatter:off
        RSocketConnector connector = RSocketConnector.create()
                // .payloadDecoder(PayloadDecoder.ZERO_COPY)
                .reconnect(Retry.fixedDelay(3, Duration.ofSeconds(1)))
                // .reconnect(Retry.backoff(50, Duration.ofMillis(500)))
                .resume(resume)
                ;
        // @formatter:on

        // Mono<RSocket> rSocket = connector.connect(clientTransport);
        // RSocketClient client = RSocketClient.from(rSocket);

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
        // Globale Default-Resourcen.
        // TcpResources.set(LoopResources.create("rsocket"));
        // TcpResources.set(ConnectionProvider.create("rsocket-connectionPool", 16));

        // Fehlermeldung, wenn Client die Verbindung schliesst.
        // Nur einmalig definieren, sonst gibs mehrere Logs-Meldungen !!!
        Hooks.onErrorDropped(th -> LOGGER.warn(th.getMessage()));

        int[] ports =
        {
                7000, 8000, 9000
        };
        // int[] ports =
        // {
        // SocketUtils.findAvailableTcpPort(), SocketUtils.findAvailableTcpPort(), SocketUtils.findAvailableTcpPort()
        // };

        List<Disposable> servers = new ArrayList<>();

        for (int port : ports)
        {
            servers.add(startServer(port));
        }

        RSocketClient client = createClient(ports);

        // Der IntStream blockiert, bis alle parallelen Operationen beendet sind.
        // Der Flux macht dies nicht, sondern im Hintergrund.
        // @formatter:off
        Flux.range(0, 30).parallel().runOn(Schedulers.boundedElastic())
            .map(i ->
                client
                    .requestResponse(Mono.just(DefaultPayload.create("flux-" + i.intValue())))
                    .map(Payload::getDataUtf8)
                    .doOnNext(LOGGER::info)
                    )
            .subscribe(Mono::subscribe);
        // @formatter:on

        // Der IntStream blockiert, bis alle parallelen Operationen beendet sind.
        // Der Flux macht dies nicht, sondern im Hintergrund.
        // @formatter:off
        IntStream.range(0, 30).parallel()
            .mapToObj(i ->
                 client
                    .requestResponse(Mono.just(DefaultPayload.create("intstream-" + i)))
                    .map(Payload::getDataUtf8)
                    .doOnNext(LOGGER::info)
                )
            .forEach(Mono::subscribe);
        // @formatter:on

        for (int i = 0; i < 30; i++)
        {
            // @formatter:off
            client
                .requestResponse(Mono.just(DefaultPayload.create("for-" + i)))
                .map(Payload::getDataUtf8)
                .doOnNext(LOGGER::info)
                .block() // Wartet auf den Response.
                //.subscribe() // Führt alles im Hintergrund aus.
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
