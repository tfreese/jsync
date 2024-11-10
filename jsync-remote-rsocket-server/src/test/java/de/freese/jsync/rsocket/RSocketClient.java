// Created: 12.02.2022
package de.freese.jsync.rsocket;

import java.time.Duration;

import io.rsocket.Payload;
import io.rsocket.RSocket;
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
public final class RSocketClient {
    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketClient.class);

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

        // final ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forClient()
        //         .configure(builder -> builder
        //                 //.keyManager(null)
        //                 .trustManager(InsecureTrustManagerFactory.INSTANCE)
        //                 .protocols("TLSv1.3")
        //                 .sslProvider(SslProvider.JDK)
        //         );

        final TcpClient tcpClient = TcpClient.create()
                .host("localhost")
                .port(8888)
                //.remoteAddress(() -> SocketAddress)
                .runOn(LoopResources.create("client", 4, true))
                //.runOn(EventLoopGroup) // EpollEventLoopGroup only available on Linux -> Use NioEventLoopGroup instead.
                .doOnConnected(connection -> LOGGER.info("Connected: {}", connection.channel()))
                .doOnDisconnected(connection -> LOGGER.info("Disconnected: {}", connection.channel()))
                //.secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec))
                ;

        final Retry retry = Retry.fixedDelay(3, Duration.ofSeconds(1));

        final Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(Retry.fixedDelay(5, Duration.ofMillis(500))
                        .doBeforeRetry(signal -> LOGGER.info("Disconnected. Trying to resume..."))
                );

        final RSocketConnector connector = RSocketConnector.create()
                .payloadDecoder(PayloadDecoder.DEFAULT)
                .reconnect(retry)
                .resume(resume);

        final ClientTransport clientTransport = TcpClientTransport.create(tcpClient);
        // ClientTransport clientTransport = LocalClientTransport.create("alias");

        final Mono<RSocket> rSocket = connector.connect(clientTransport);
        final io.rsocket.core.RSocketClient rSocketClient = io.rsocket.core.RSocketClient.from(rSocket);

        rSocketClient
                .requestResponse(Mono.just(ByteBufPayload.create("Hello World")))
                .map(Payload::getDataUtf8)
                .doOnNext(LOGGER::info)
                .block()
        ;

        rSocketClient.dispose();
    }

    private RSocketClient() {
        super();
    }
}
