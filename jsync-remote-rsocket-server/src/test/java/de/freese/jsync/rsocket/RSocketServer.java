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
public final class RSocketServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketServer.class);

    public static void main(final String[] args) {
        // Global Default-Resources.
        // TcpResources.set(LoopResources.create("rSocket"));
        // TcpResources.set(LoopResources.create("rSocket", 2, 8, true));
        // TcpResources.set(ConnectionProvider.create("connectionPool", 16));

        // Exception, if Client closed the Connection.
        // Create only once, unless multiple Logs are created !!!
        // Hooks.onErrorDropped(th -> LOGGER.warn(th.getMessage()));
        Hooks.onErrorDropped(th -> {
        });

        // Enable Debug.
        // Hooks.onOperatorDebug();

        // final SelfSignedCertificate cert = new SelfSignedCertificate();
        // final ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forServer(cert.certificate(), cert.privateKey())
        //         .configure(builder -> builder
        //                 .protocols("TLSv1.3")
        //                 .sslProvider(SslProvider.JDK)
        //         );

        final TcpServer tcpServer = TcpServer.create()
                .host("localhost")
                .port(8888)
                //.bindAddress(() -> SocketAddress)
                .runOn(LoopResources.create("server", 4, false))
                //.runOn(EventLoopGroup) // EpollEventLoopGroup only available on Linux -> Use NioEventLoopGroup instead.
                .doOnBound(server -> LOGGER.info("Bound: {}", server.channel()))
                .doOnUnbound(server -> LOGGER.info("Unbound: {}", server.channel()))
                .doOnConnection(connection -> LOGGER.info("Connected client: {}", connection.channel()))
                //.secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec))
                ;

        final SocketAcceptor socketAcceptor = SocketAcceptor.forRequestResponse(payload -> {
            final String request = payload.getDataUtf8();
            LOGGER.info("Server got request: {}", request);
            return Mono.just(DefaultPayload.create("Client got response: " + request));
        });

        final Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(Retry.fixedDelay(5, Duration.ofMillis(500))
                        .doBeforeRetry(signal -> LOGGER.info("Disconnected. Trying to resume..."))
                );

        final ServerTransport<CloseableChannel> serverTransport = TcpServerTransport.create(tcpServer);
        // ServerTransport<CloseableChannel> serverTransport = LocalServerTransport.create("alias");

        // CloseableChannel rSocketServer =
        io.rsocket.core.RSocketServer.create()
                .acceptor(socketAcceptor)
                .resume(resume)
                .payloadDecoder(PayloadDecoder.DEFAULT)
                .bindNow(serverTransport)
        ;
    }

    private RSocketServer() {
        super();
    }
}
