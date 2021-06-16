// Created: 28.10.2020
package de.freese.jsync.rsocket;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import javax.net.ssl.TrustManagerFactory;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.SslProvider;
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
     * @param serverAddresses {@link List}
     * @return {@link RSocketClient}
     * @throws Exception Falls was schief geht
     */
    private static RSocketClient createClient(final List<SocketAddress> serverAddresses) throws Exception
    {
        // KeyStore keyStore = KeyStore.getInstance("PKCS12");
        //
        // try (InputStream is = new FileInputStream("../../spring/spring-thymeleaf/CA/client_keystore.p12"))
        // {
        // keyStore.load(is, "password".toCharArray());
        // }
        //
        // KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("NewSunX509");
        // keyManagerFactory.init(keyStore, "gehaim".toCharArray());

        KeyStore keyStoreTrust = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../../spring/spring-thymeleaf/CA/client_truststore.p12"))
        {
            keyStoreTrust.load(is, "password".toCharArray());
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStoreTrust);

        // @formatter:off
        ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forClient()
                .configure(builder -> builder
                        //.keyManager(keyManagerFactory)
                        //.trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .trustManager(trustManagerFactory)
                        .protocols("TLSv1.3")
                        .sslProvider(SslProvider.JDK))
                ;
        // @formatter:on

        // @formatter:off
        Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(Retry.fixedDelay(10, Duration.ofSeconds(1))
                        .doBeforeRetry(s -> LOGGER.info("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

        // Nachbildung einer Service-Discovery Anfrage.
        // @formatter:off
        Publisher<List<LoadbalanceTarget>> serverProducer = Flux.fromStream(serverAddresses.stream())
            .map(serverAddress ->  {
                TcpClient tcpClient = TcpClient.create()
                    //.host(inetSocketAddress.getHostName())
                    //.port(inetSocketAddress.getPort())
                    .remoteAddress(() -> serverAddress)
                    .secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec))
                    .doOnDisconnected(connection -> LOGGER.info("Disconnected: {}", connection.channel())
                    //.runOn(LoopResources.create("client-" + port, 2, true), false)
                );

                ClientTransport clientTransport = TcpClientTransport.create(tcpClient);
                // ClientTransport clientTransport = LocalClientTransport.create("test-local-" + port);

                return LoadbalanceTarget.from(serverAddress.toString(), clientTransport);
            })
            .collectList()
            //.repeatWhen(f -> f.delayElements(Duration.ofSeconds(1))) // <- continuously retrieve new List of ServiceInstances
            .doOnSubscribe(subcription -> LOGGER.info("Update Server Instances"))
            ;
        // @formatter:on

        // Publisher<List<LoadbalanceTarget>> serverProducer = Flux.interval(Duration.ofSeconds(2)).log().map(i -> {
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
                .builder(serverProducer)
                .connector(connector)
                .roundRobinLoadbalanceStrategy()
                //.weightedLoadbalanceStrategy()
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

        // SocketUtils.findAvailableTcpPort()
        List<SocketAddress> serverAddresses = Stream.of(7000, 8000, 9000).map(port -> new InetSocketAddress("localhost", port)).collect(Collectors.toList());

        List<Disposable> servers = startServer(serverAddresses);
        RSocketClient client = createClient(serverAddresses);

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

        // TimeUnit.MILLISECONDS.sleep(1000);

        client.dispose();
        servers.forEach(Disposable::dispose);
    }

    /**
     * @param serverAddresses {@link List}
     * @return {@link List}
     */
    private static final List<Disposable> startServer(final List<SocketAddress> serverAddresses)
    {
        List<Disposable> servers = new ArrayList<>();

        for (SocketAddress serverAddress : serverAddresses)
        {
            Server server = new Server();

            try
            {
                server.start(serverAddress);

                servers.add(server);
            }
            catch (Exception ex)
            {
                LOGGER.error(null, ex);
            }
        }

        return servers;
    }

    /**
     * Erzeugt ein neues {@link RSocketLoadBalancerDemo} Objekt.
     */
    private RSocketLoadBalancerDemo()
    {
        super();
    }
}
