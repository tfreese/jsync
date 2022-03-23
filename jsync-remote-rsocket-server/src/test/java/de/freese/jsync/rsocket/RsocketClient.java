// Created: 12.02.2022
package de.freese.jsync.rsocket;

import java.time.Duration;

import io.rsocket.Payload;
import io.rsocket.RSocket;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.Resume;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.ClientTransport;
import io.rsocket.transport.netty.client.TcpClientTransport;
import io.rsocket.util.ByteBufPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpClient;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public class RsocketClient
{
    /**
     *
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(RsocketClient.class);

    /**
     * @param args String[]
     *
     * @throws Exception Falls was schief geht.
     */
    public static void main(final String[] args) throws Exception
    {
        // Globale Default-Ressourcen.
        // TcpResources.set(LoopResources.create("rsocket"));
        // TcpResources.set(LoopResources.create("rsocket", 2, 8, true));
        // TcpResources.set(ConnectionProvider.create("connectionPool", 16));

        // Fehlermeldung, wenn Client die Verbindung schliesst.
        // Nur einmalig definieren, sonst gib es mehrere Logs-Meldungen !!!
        // Hooks.onErrorDropped(th -> LOGGER.warn(th.getMessage()));
        Hooks.onErrorDropped(th ->
        {
        });

        // Debug einschalten.
        // Hooks.onOperatorDebug();

        // @formatter:off
//        ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forClient()
//                .configure(builder -> builder
//                        //.keyManager(null)
//                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
//                        .protocols("TLSv1.3")
//                        .sslProvider(SslProvider.JDK)
//                        )
//                ;
        // @formatter:on

        // @formatter:off
        TcpClient tcpClient = TcpClient.create()
                .host("localhost")
                .port(8888)
                //.remoteAddress(() -> SocketAddress)
                .runOn(LoopResources.create("client", 4, true))
                //.runOn(EventLoopGroup) // EpollEventLoopGroup geht nur auf Linux -> NioEventLoopGroup verwenden.
                .doOnConnected(connection -> LOGGER.info("Connected: {}", connection.channel()))
                .doOnDisconnected(connection -> LOGGER.info("Disconnected: {}", connection.channel()))
                //.secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec))
                ;
        // @formatter:on

        Retry retry = Retry.fixedDelay(3, Duration.ofSeconds(1));

        // @formatter:off
        Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(Retry.fixedDelay(5, Duration.ofMillis(500))
                        .doBeforeRetry(signal -> LOGGER.info("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

        // @formatter:off
        RSocketConnector connector = RSocketConnector.create()
                .payloadDecoder(PayloadDecoder.DEFAULT)
                .reconnect(retry)
                .resume(resume)
                ;
        // @formatter:on

        ClientTransport clientTransport = TcpClientTransport.create(tcpClient);
        // ClientTransport clientTransport = LocalClientTransport.create("alias");

        Mono<RSocket> rSocket = connector.connect(clientTransport);
        RSocketClient rSocketClient = RSocketClient.from(rSocket);

        // @formatter:off
        rSocketClient
            .requestResponse(Mono.just(ByteBufPayload.create("Hello World")))
            .map(Payload::getDataUtf8)
            .doOnNext(LOGGER::info)
            .block()
            ;
        // @formatter:on

        rSocketClient.dispose();
    }
}
