// Created: 15.06.2024
package de.freese.jsync.rsocket.builder.server;

import java.net.SocketAddress;
import java.security.PrivateKey;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.core.Resume;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.netty.server.CloseableChannel;
import org.slf4j.Logger;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpServer;
import reactor.netty.tcp.TcpSslContextSpec;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

/**
 * @author Thomas Freese
 */
final class RSocketServerBuilderSupport {
    private final List<UnaryOperator<RSocketServer>> rSocketServerCustomizers = new ArrayList<>();
    private final List<UnaryOperator<TcpServer>> tcpServerCustomizers = new ArrayList<>();

    void addRSocketServerCustomizer(final UnaryOperator<RSocketServer> rSocketServerCustomizer) {
        this.rSocketServerCustomizers.add(Objects.requireNonNull(rSocketServerCustomizer, "rSocketServerCustomizer required"));
    }

    void addTcpServerCustomizer(final UnaryOperator<TcpServer> tcpServerCustomizer) {
        this.tcpServerCustomizers.add(Objects.requireNonNull(tcpServerCustomizer, "tcpServerCustomizer required"));
    }

    RSocketServer configure(final RSocketServer rSocketServer) {
        RSocketServer server = rSocketServer;

        for (UnaryOperator<RSocketServer> serverCustomizer : this.rSocketServerCustomizers) {
            server = serverCustomizer.apply(server);
        }

        return server;
    }

    TcpServer configure(final TcpServer tcpServer) {
        TcpServer server = tcpServer;

        for (UnaryOperator<TcpServer> serverCustomizer : this.tcpServerCustomizers) {
            server = serverCustomizer.apply(server);
        }

        return server;
    }

    void logTcpServerBoundStatus(final Logger logger) {
        addTcpServerCustomizer(tcpServer -> tcpServer
                .doOnBound(server -> logger.info("Bound: {}", server.channel()))
                .doOnUnbound(server -> logger.info("Unbound: {}", server.channel()))
        );
    }

    void payloadDecoder(final PayloadDecoder payloadDecoder) {
        Objects.requireNonNull(payloadDecoder, "payloadDecoder required");

        addRSocketServerCustomizer(rSocketServer -> rSocketServer.payloadDecoder(payloadDecoder));
    }

    void protocolSslContextSpec(final SslProvider.ProtocolSslContextSpec protocolSslContextSpec) {
        Objects.requireNonNull(protocolSslContextSpec, "protocolSslContextSpec required");

        addTcpServerCustomizer(tcpServer -> tcpServer.secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec)));
    }

    void protocolSslContextSpecCertificate(final KeyManagerFactory keyManagerFactory, final TrustManagerFactory trustManagerFactory,
                                           final X509Certificate certificate, final PrivateKey privateKey) {
        final SslProvider.ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forServer(privateKey, certificate)
                .configure(builder -> builder
                        .keyManager(keyManagerFactory)
                        .trustManager(trustManagerFactory)
                        .protocols("TLSv1.3")
                        .sslProvider(io.netty.handler.ssl.SslProvider.JDK)
                );

        protocolSslContextSpec(protocolSslContextSpec);
    }

    void protocolSslContextSpecCertificateSelfSigned() throws Exception {
        final SelfSignedCertificate cert = new SelfSignedCertificate();

        final SslProvider.ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forServer(cert.certificate(), cert.privateKey())
                .configure(builder -> builder
                        .protocols("TLSv1.3")
                        .sslProvider(io.netty.handler.ssl.SslProvider.JDK)
                );

        protocolSslContextSpec(protocolSslContextSpec);
    }

    void resume(final Resume resume) {
        Objects.requireNonNull(resume, "resume required");

        addRSocketServerCustomizer(rSocketServer -> rSocketServer.resume(resume));
    }

    /**
     * @param logger {@link Logger}: optional
     */
    void resumeDefault(final Logger logger) {
        RetryBackoffSpec retry = Retry.fixedDelay(5, Duration.ofMillis(500));

        if (logger != null) {
            retry = retry.doBeforeRetry(signal -> logger.info("Disconnected. Trying to resume..."));
        }

        final Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(retry);

        resume(resume);
    }

    /**
     * EpollEventLoopGroup only available on Linux -> Use NioEventLoopGroup instead.
     */
    void runOn(final EventLoopGroup eventLoopGroup) {
        Objects.requireNonNull(eventLoopGroup, "eventLoopGroup required");

        addTcpServerCustomizer(tcpServer -> tcpServer.runOn(eventLoopGroup));
    }

    void runOn(final LoopResources loopResources) {
        Objects.requireNonNull(loopResources, "loopResources required");

        addTcpServerCustomizer(tcpServer -> tcpServer.runOn(loopResources));
    }

    void socketAcceptor(final SocketAcceptor socketAcceptor) {
        Objects.requireNonNull(socketAcceptor, "socketAcceptor required");

        addRSocketServerCustomizer(rSocketServer -> rSocketServer.acceptor(socketAcceptor));
    }

    void socketAddress(final SocketAddress socketAddress) {
        Objects.requireNonNull(socketAddress, "socketAddress required");

        addTcpServerCustomizer(tcpServer -> tcpServer.bindAddress(() -> socketAddress));
    }

    /**
     * @param logger {@link Logger}: optional
     */
    void startDaemonOnCloseThread(final CloseableChannel channel, final Logger logger) {
        final String name = "daemon-rSocket-" + channel.address().getPort();

        final Thread thread = new Thread(() -> {
            channel.onClose().block();

            if (logger != null) {
                logger.info("terminated: {}", name);
            }
        }, name);
        thread.setContextClassLoader(getClass().getClassLoader());
        thread.setDaemon(false);
        thread.start();
    }
}
