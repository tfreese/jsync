// Created: 15.06.2024
package de.freese.jsync.rsocket.builder.client;

import java.net.SocketAddress;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import javax.net.ssl.TrustManagerFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.rsocket.core.RSocketConnector;
import io.rsocket.core.Resume;
import io.rsocket.frame.decoder.PayloadDecoder;
import org.slf4j.Logger;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpSslContextSpec;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

/**
 * @author Thomas Freese
 */
final class RSocketClientBuilderSupport {
    private final List<UnaryOperator<RSocketConnector>> rSocketConnectorCustomizers = new ArrayList<>();
    private final List<UnaryOperator<TcpClient>> tcpClientCustomizers = new ArrayList<>();

    void addRSocketConnectorCustomizer(final UnaryOperator<RSocketConnector> rSocketConnectorCustomizer) {
        this.rSocketConnectorCustomizers.add(Objects.requireNonNull(rSocketConnectorCustomizer, "rSocketConnectorCustomizer required"));
    }

    void addTcpClientCustomizer(final UnaryOperator<TcpClient> tcpClientCustomizer) {
        this.tcpClientCustomizers.add(Objects.requireNonNull(tcpClientCustomizer, "tcpClientCustomizer required"));
    }

    RSocketConnector configure(final RSocketConnector rSocketConnector) {
        RSocketConnector connector = rSocketConnector;

        for (UnaryOperator<RSocketConnector> connectorCustomizer : this.rSocketConnectorCustomizers) {
            connector = connectorCustomizer.apply(connector);
        }

        return connector;
    }

    TcpClient configure(final TcpClient tcpClient) {
        TcpClient client = tcpClient;

        for (UnaryOperator<TcpClient> clientCustomizer : this.tcpClientCustomizers) {
            client = clientCustomizer.apply(client);
        }

        return client;
    }

    void logTcpClientBoundStatus(final Logger logger) {
        addTcpClientCustomizer(tcpClient -> tcpClient
                .doOnConnected(connection -> logger.info("Connected: {}", connection.channel()))
                .doOnDisconnected(connection -> logger.info("Disconnected: {}", connection.channel()))
        );
    }

    void payloadDecoder(final PayloadDecoder payloadDecoder) {
        Objects.requireNonNull(payloadDecoder, "payloadDecoder required");

        addRSocketConnectorCustomizer(rSocketConnector -> rSocketConnector.payloadDecoder(payloadDecoder));
    }

    void protocolSslContextSpec(final SslProvider.ProtocolSslContextSpec protocolSslContextSpec) {
        Objects.requireNonNull(protocolSslContextSpec, "protocolSslContextSpec required");

        addTcpClientCustomizer(tcpClient -> tcpClient.secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec)));
    }

    void protocolSslContextSpecCertificate(final TrustManagerFactory trustManagerFactory) throws Exception {
        final SslProvider.ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forClient()
                .configure(builder -> builder
                        .trustManager(trustManagerFactory)
                        .protocols("TLSv1.3")
                        .sslProvider(io.netty.handler.ssl.SslProvider.JDK)
                );

        protocolSslContextSpec(protocolSslContextSpec);
    }

    void protocolSslContextSpecTrusted() {
        final SslProvider.ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forClient()
                .configure(builder -> builder
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .protocols("TLSv1.3")
                        .sslProvider(io.netty.handler.ssl.SslProvider.JDK)
                );

        protocolSslContextSpec(protocolSslContextSpec);
    }

    void remoteAddress(final SocketAddress remoteAddress) {
        Objects.requireNonNull(remoteAddress, "remoteAddress required");

        addTcpClientCustomizer(tcpClient -> tcpClient.remoteAddress(() -> remoteAddress));
    }

    void resume(final Resume resume) {
        Objects.requireNonNull(resume, "resume required");

        addRSocketConnectorCustomizer(rSocketConnector -> rSocketConnector.resume(resume));
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

    void retry(final Retry retry) {
        Objects.requireNonNull(retry, "retry required");

        addRSocketConnectorCustomizer(rSocketConnector -> rSocketConnector.reconnect(retry));
    }

    /**
     * @param logger {@link Logger}: optional
     */
    void retryDefault(final Logger logger) {
        RetryBackoffSpec retry = Retry.fixedDelay(5, Duration.ofMillis(500));

        if (logger != null) {
            retry = retry.doBeforeRetry(signal -> logger.info("Trying to retry..."));
        }

        retry(retry);
    }

    /**
     * EpollEventLoopGroup only available on Linux -> Use NioEventLoopGroup instead.
     */
    void runOn(final EventLoopGroup eventLoopGroup) {
        Objects.requireNonNull(eventLoopGroup, "eventLoopGroup required");

        addTcpClientCustomizer(tcpClient -> tcpClient.runOn(eventLoopGroup));
    }

    void runOn(final LoopResources loopResources) {
        Objects.requireNonNull(loopResources, "loopResources required");

        addTcpClientCustomizer(tcpClient -> tcpClient.runOn(loopResources));
    }
}
