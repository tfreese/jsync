// Created: 31.07.2021
package de.freese.jsync.rsocket.builder.client;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.UnaryOperator;

import javax.net.ssl.TrustManagerFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.rsocket.core.RSocketClient;
import io.rsocket.core.Resume;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpClient;
import reactor.netty.tcp.TcpSslContextSpec;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

import de.freese.jsync.rsocket.builder.AbstractBuilder;

/**
 * @author Thomas Freese
 */
abstract class AbstractClientBuilderRemote<T extends AbstractBuilder<?, RSocketClient>> extends AbstractClientBuilder<T> {
    private final List<UnaryOperator<TcpClient>> tcpClientCustomizers = new ArrayList<>();

    public T addTcpClientCustomizer(final UnaryOperator<TcpClient> tcpClientCustomizer) {
        tcpClientCustomizers.add(Objects.requireNonNull(tcpClientCustomizer, "tcpClientCustomizer required"));

        return self();
    }

    public T logTcpClientBoundStatus() {
        Objects.requireNonNull(getLogger(), "call Method #logger(...) first");

        return addTcpClientCustomizer(tcpClient -> tcpClient
                .doOnConnected(connection -> getLogger().info("Connected: {}", connection.channel()))
                .doOnDisconnected(connection -> getLogger().info("Disconnected: {}", connection.channel()))
        );
    }

    public T protocolSslContextSpec(final SslProvider.GenericSslContextSpec<SslContextBuilder> protocolSslContextSpec) {
        Objects.requireNonNull(protocolSslContextSpec, "protocolSslContextSpec required");

        return addTcpClientCustomizer(tcpClient -> tcpClient.secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec)));
    }

    public T protocolSslContextSpecCertificate() throws Exception {
        final KeyStore keyStoreTrust = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../spring/spring-thymeleaf/CA/keytool/client_truststore.p12")) {
            keyStoreTrust.load(is, "password".toCharArray());
        }

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStoreTrust);

        final SslProvider.ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forClient()
                .configure(builder -> builder
                        .trustManager(trustManagerFactory)
                        .protocols("TLSv1.3")
                        .sslProvider(io.netty.handler.ssl.SslProvider.JDK)
                );

        return protocolSslContextSpec(protocolSslContextSpec);
    }

    public T protocolSslContextSpecTrusted() {
        final SslProvider.ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forClient()
                .configure(builder -> builder
                        .trustManager(InsecureTrustManagerFactory.INSTANCE)
                        .protocols("TLSv1.3")
                        .sslProvider(io.netty.handler.ssl.SslProvider.JDK)
                );

        return protocolSslContextSpec(protocolSslContextSpec);
    }

    public T resume(final Resume resume) {
        Objects.requireNonNull(resume, "resume required");

        return addRSocketConnectorCustomizer(rSocketConnector -> rSocketConnector.resume(resume));
    }

    public T resumeDefault() {
        RetryBackoffSpec retry = Retry.fixedDelay(5, Duration.ofMillis(500));

        if (getLogger() != null) {
            retry = retry.doBeforeRetry(signal -> getLogger().info("Disconnected. Trying to resume..."));
        }

        final Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(retry);

        return resume(resume);
    }

    public T retry(final Retry retry) {
        Objects.requireNonNull(retry, "retry required");

        return addRSocketConnectorCustomizer(rSocketConnector -> rSocketConnector.reconnect(retry));
    }

    public T retryDefault() {
        RetryBackoffSpec retry = Retry.fixedDelay(5, Duration.ofMillis(500));

        if (getLogger() != null) {
            retry = retry.doBeforeRetry(signal -> getLogger().info("Trying to retry..."));
        }

        return retry(retry);
    }

    public T runOn(final EventLoopGroup eventLoopGroup) {
        Objects.requireNonNull(eventLoopGroup, "eventLoopGroup required");

        return addTcpClientCustomizer(tcpClient -> tcpClient.runOn(eventLoopGroup));
    }

    public T runOn(final LoopResources loopResources) {
        Objects.requireNonNull(loopResources, "loopResources required");

        return addTcpClientCustomizer(tcpClient -> tcpClient.runOn(loopResources));
    }

    protected TcpClient configure(final TcpClient tcpClient) {
        TcpClient client = tcpClient;

        for (UnaryOperator<TcpClient> clientCustomizer : tcpClientCustomizers) {
            client = clientCustomizer.apply(client);
        }

        return client;
    }
}
