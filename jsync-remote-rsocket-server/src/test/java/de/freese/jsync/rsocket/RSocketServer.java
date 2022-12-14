// Created: 12.02.2022
package de.freese.jsync.rsocket;

import java.time.Duration;

import io.rsocket.SocketAcceptor;
import io.rsocket.core.Resume;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import io.rsocket.util.DefaultPayload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Hooks;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.TcpServer;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public class RSocketServer
{
    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketServer.class);

    public static void main(final String[] args) throws Exception
    {
        // Globale Default-Ressourcen.
        // TcpResources.set(LoopResources.create("rSocket"));
        // TcpResources.set(LoopResources.create("rSocket", 2, 8, true));
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
//        SelfSignedCertificate cert = new SelfSignedCertificate();
//        ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forServer(cert.certificate(), cert.privateKey())
//                .configure(builder -> builder
//                        .protocols("TLSv1.3")
//                        .sslProvider(SslProvider.JDK)
//                        )
//                 ;
         // @formatter:on

        // @formatter:off
        TcpServer tcpServer = TcpServer.create()
                .host("localhost")
                .port(8888)
                //.bindAddress(() -> SocketAddress)
                .runOn(LoopResources.create("server", 4, false))
                //.runOn(EventLoopGroup) // EpollEventLoopGroup geht nur auf Linux -> NioEventLoopGroup verwenden.
                .doOnBound(server -> LOGGER.info("Bound: {}", server.channel()))
                .doOnUnbound(server -> LOGGER.info("Unbound: {}", server.channel()))
                .doOnConnection(connection -> LOGGER.info("Connected client: {}", connection.channel()))
                //.secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec))
                ;
        // @formatter:on

        SocketAcceptor socketAcceptor = SocketAcceptor.forRequestResponse(payload ->
        {
            String request = payload.getDataUtf8();
            LOGGER.info("Server got request: {}", request);
            return Mono.just(DefaultPayload.create("Client got response: " + request));
        });

        // @formatter:off
        Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(Retry.fixedDelay(5, Duration.ofMillis(500))
                        .doBeforeRetry(signal -> LOGGER.info("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

        ServerTransport<CloseableChannel> serverTransport = TcpServerTransport.create(tcpServer);
        // ServerTransport<CloseableChannel> serverTransport = LocalServerTransport.create("alias");

        // @formatter:off
        CloseableChannel rSocketServer = io.rsocket.core.RSocketServer.create()
                .acceptor(socketAcceptor)
                .resume(resume)
                .payloadDecoder(PayloadDecoder.DEFAULT)
                .bindNow(serverTransport)
                ;
        // @formatter:on
    }
}
