// Created: 20.06.2021
package de.freese.jsync.rsocket;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.time.Duration;
import java.util.List;
import java.util.Random;
import java.util.function.Supplier;

import javax.net.ssl.TrustManagerFactory;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.handler.ssl.SslProvider;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.Resume;
import io.rsocket.loadbalance.LoadbalanceRSocketClient;
import io.rsocket.loadbalance.LoadbalanceTarget;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.SslProvider.ProtocolSslContextSpec;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpSslContextSpec;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public class Client
{
    /**
    *
    */
    private static final Logger LOGGER = LoggerFactory.getLogger(Client.class);

    /**
     * @param serverAddress {@link SocketAddress}
     * @return {@link RSocketClient}
     */
    static RSocketClient createClient(final SocketAddress serverAddress)
    {
        ProtocolSslContextSpec protocolSslContextSpec = createProtocolSslContextSpec();

        ClientTransport clientTransport = createClientTransport(serverAddress, protocolSslContextSpec);

        RSocketConnector rSocketConnector = createRSocketConnector();

        Mono<RSocket> rSocket = rSocketConnector.connect(clientTransport);
        RSocketClient client = RSocketClient.from(rSocket);

        return client;
    }

    /**
     * @param serverAddresses {@link List}
     * @return {@link RSocketClient}
     */
    static RSocketClient createClientLoadBalanced(final List<SocketAddress> serverAddresses)
    {
        ProtocolSslContextSpec protocolSslContextSpec = createProtocolSslContextSpec();

        // @formatter:off
        Publisher<List<LoadbalanceTarget>> serverProducer = Flux.fromIterable(serverAddresses)
            .map(serverAddress ->  {
                ClientTransport clientTransport = createClientTransport(serverAddress, protocolSslContextSpec);

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

        RSocketConnector rSocketConnector = createRSocketConnector();

        // @formatter:off
        RSocketClient client = LoadbalanceRSocketClient
                .builder(serverProducer)
                .connector(rSocketConnector)
                .roundRobinLoadbalanceStrategy()
                //.weightedLoadbalanceStrategy()
                .build()
                ;
        // @formatter:on

        return client;
    }

    /**
     * @param serverAddresses {@link List}
     * @return {@link RSocketClient}
     */
    static RSocketClient createClientLoadBalancedWithServiceDiscovery(final List<SocketAddress> serverAddresses)
    {
        ProtocolSslContextSpec protocolSslContextSpec = createProtocolSslContextSpec();
        Random random = new Random();

        // Wechselnde Service-Discovery Anfrage simulieren.
        // org.springframework.cloud.client.discovery.DiscoveryClient - org.springframework.cloud:spring-cloud-commons

        // @formatter:off
        Supplier<List<SocketAddress>> serviceRegistry = () -> serverAddresses.stream()
                .filter(server -> random.nextBoolean()) // Nicht jeden Server verwenden.
                .toList()
                ;
        // @formatter:on

        // @formatter:off
        Publisher<List<LoadbalanceTarget>> serverProducer = Mono.fromSupplier(serviceRegistry)
                .map(servers -> {
                    LOGGER.info("Update Server Instances: {}", servers);

                    return servers.stream()
                            .map(server -> {
                                ClientTransport clientTransport = createClientTransport(server, protocolSslContextSpec);

                                return LoadbalanceTarget.from(server.toString(), clientTransport);
                            })
                            .toList()
                            ;
                })
                .repeatWhen(flux -> flux.delayElements(Duration.ofMillis(600))) // Flux regelmäßig aktualisieren.
                ;
        // @formatter:on

        RSocketConnector rSocketConnector = createRSocketConnector();

        // @formatter:off
        RSocketClient client = LoadbalanceRSocketClient
                .builder(serverProducer)
                .connector(rSocketConnector)
                .roundRobinLoadbalanceStrategy()
                //.weightedLoadbalanceStrategy()
                .build()
                ;
        // @formatter:on

        return client;
    }

    /**
     * @param serverAddress {@link SocketAddress}
     * @param protocolSslContextSpec {@link ProtocolSslContextSpec}
     * @return {@link ClientTransport}
     */
    private static ClientTransport createClientTransport(final SocketAddress serverAddress, final ProtocolSslContextSpec protocolSslContextSpec)
    {
        // @formatter:off
        TcpClient tcpClient = TcpClient.create()
                //.host(inetSocketAddress.getHostName())
                //.port(inetSocketAddress.getPort())
                .remoteAddress(() -> serverAddress)
                .secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec))
                .doOnDisconnected(connection -> LOGGER.info("Disconnected: {}", connection.channel())
                //.runOn(LoopResources.create("client-" + port, 2, true), false)
            );
        // @formatter:on

        ClientTransport clientTransport = TcpClientTransport.create(tcpClient);
        // ClientTransport clientTransport = LocalClientTransport.create("test-local-" + port);

        return clientTransport;
    }

    /**
     * @return {@link ProtocolSslContextSpec}
     */
    private static ProtocolSslContextSpec createProtocolSslContextSpec()
    {
        try
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

            return protocolSslContextSpec;
        }
        catch (RuntimeException ex)
        {
            LOGGER.error(null, ex);

            throw ex;
        }
        catch (Exception ex)
        {
            LOGGER.error(null, ex);

            throw new RuntimeException(ex);
        }
    }

    /**
     * @return {@link RSocketConnector}
     */
    private static RSocketConnector createRSocketConnector()
    {
        // @formatter:off
        Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(Retry.fixedDelay(5, Duration.ofMillis(100))
                        .doBeforeRetry(s -> LOGGER.info("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

        // @formatter:off
        RSocketConnector connector = RSocketConnector.create()
                // .payloadDecoder(PayloadDecoder.ZERO_COPY))
                .reconnect(Retry.fixedDelay(5, Duration.ofMillis(100)))
                // .reconnect(Retry.backoff(50, Duration.ofMillis(100)))
                .resume(resume)
                ;
        // @formatter:on

        return connector;
    }
}
