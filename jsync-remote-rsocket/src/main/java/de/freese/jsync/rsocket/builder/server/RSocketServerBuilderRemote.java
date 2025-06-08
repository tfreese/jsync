// Created: 15 Juni 2024
package de.freese.jsync.rsocket.builder.server;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
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
import io.rsocket.core.RSocketServer;
import io.rsocket.core.Resume;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpServer;
import reactor.netty.tcp.TcpSslContextSpec;
import reactor.util.retry.Retry;
import reactor.util.retry.RetryBackoffSpec;

/**
 * @author Thomas Freese
 */
@SuppressWarnings("java:S6437")
public final class RSocketServerBuilderRemote extends AbstractServerBuilder<RSocketServerBuilderRemote, Mono<CloseableChannel>> {
    private final List<UnaryOperator<TcpServer>> tcpServerCustomizers = new ArrayList<>();

    public RSocketServerBuilderRemote addTcpServerCustomizer(final UnaryOperator<TcpServer> tcpServerCustomizer) {
        tcpServerCustomizers.add(Objects.requireNonNull(tcpServerCustomizer, "tcpServerCustomizer required"));

        return this;
    }

    @Override
    public Mono<CloseableChannel> build() {
        final RSocketServer rSocketServer = configure(RSocketServer.create());
        final TcpServer tcpServer = configure(TcpServer.create());

        final ServerTransport<CloseableChannel> serverTransport = TcpServerTransport.create(tcpServer);

        return rSocketServer.bind(serverTransport)
                .doOnNext(this::startDaemonOnCloseThread)
                ;
    }

    public RSocketServerBuilderRemote logTcpServerBoundStatus() {
        Objects.requireNonNull(getLogger(), "call Method #logger(...) first");

        return addTcpServerCustomizer(tcpServer -> tcpServer
                .doOnBound(server -> getLogger().info("Bound: {}", server.channel()))
                .doOnUnbound(server -> getLogger().info("Unbound: {}", server.channel()))
        );
    }

    public RSocketServerBuilderRemote protocolSslContextSpec(final SslProvider.ProtocolSslContextSpec protocolSslContextSpec) {
        Objects.requireNonNull(protocolSslContextSpec, "protocolSslContextSpec required");

        return addTcpServerCustomizer(tcpServer -> tcpServer.secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec)));
    }

    public RSocketServerBuilderRemote protocolSslContextSpecCertificate() throws Exception {
        final char[] password = "password".toCharArray();

        final KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../spring/spring-thymeleaf/CA/keytool/server_keystore.p12")) {
            keyStore.load(is, password);
        }

        final KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("NewSunX509");
        keyManagerFactory.init(keyStore, password);

        final KeyStore trustStore = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../spring/spring-thymeleaf/CA/keytool/server_truststore.p12")) {
            trustStore.load(is, password);
        }

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(trustStore);

        final Certificate certificate = keyStore.getCertificate("myServer");
        final PrivateKey privateKey = (PrivateKey) keyStore.getKey("myServer", password);
        // KeyStore.Entry entry = keyStore.getEntry("myServer", new KeyStore.PasswordProtection(password));
        // PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();

        final TcpSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forServer(privateKey, (X509Certificate) certificate)
                .configure(builder -> builder
                        .keyManager(keyManagerFactory)
                        .trustManager(trustManagerFactory)
                        .protocols("TLSv1.3")
                        .sslProvider(io.netty.handler.ssl.SslProvider.JDK)
                );

        // final SslProvider.GenericSslContextSpec sslContextSpec

        return protocolSslContextSpec(protocolSslContextSpec);
    }

    public RSocketServerBuilderRemote protocolSslContextSpecCertificateSelfSigned() throws Exception {
        final SelfSignedCertificate cert = new SelfSignedCertificate();

        final SslProvider.ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forServer(cert.certificate(), cert.privateKey())
                .configure(builder -> builder
                        .protocols("TLSv1.3")
                        .sslProvider(io.netty.handler.ssl.SslProvider.JDK)
                );

        return protocolSslContextSpec(protocolSslContextSpec);
    }

    public RSocketServerBuilderRemote resume(final Resume resume) {
        Objects.requireNonNull(resume, "resume required");

        return addRSocketServerCustomizer(rSocketServer -> rSocketServer.resume(resume));
    }

    public RSocketServerBuilderRemote resumeDefault() {
        RetryBackoffSpec retry = Retry.fixedDelay(5, Duration.ofMillis(500));

        if (getLogger() != null) {
            retry = retry.doBeforeRetry(signal -> getLogger().info("Disconnected. Trying to resume..."));
        }

        final Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(retry);

        return resume(resume);
    }

    /**
     * EpollEventLoopGroup only available on Linux -> Use NioEventLoopGroup instead.
     */
    public RSocketServerBuilderRemote runOn(final EventLoopGroup eventLoopGroup) {
        Objects.requireNonNull(eventLoopGroup, "eventLoopGroup required");

        return addTcpServerCustomizer(tcpServer -> tcpServer.runOn(eventLoopGroup));
    }

    public RSocketServerBuilderRemote runOn(final LoopResources loopResources) {
        Objects.requireNonNull(loopResources, "loopResources required");

        return addTcpServerCustomizer(tcpServer -> tcpServer.runOn(loopResources));
    }

    public RSocketServerBuilderRemote socketAddress(final SocketAddress socketAddress) {
        Objects.requireNonNull(socketAddress, "socketAddress required");

        return addTcpServerCustomizer(tcpServer -> tcpServer.bindAddress(() -> socketAddress));
    }

    @Override
    protected RSocketServerBuilderRemote self() {
        return this;
    }

    private TcpServer configure(final TcpServer tcpServer) {
        TcpServer server = tcpServer;

        for (UnaryOperator<TcpServer> serverCustomizer : tcpServerCustomizers) {
            server = serverCustomizer.apply(server);
        }

        return server;
    }

    private void startDaemonOnCloseThread(final CloseableChannel channel) {
        final String name = "daemon-rSocket-" + channel.address().getPort();

        final Thread thread = new Thread(() -> {
            channel.onClose().block();

            if (getLogger() != null) {
                getLogger().info("terminated: {}", name);
            }
        }, name);
        thread.setContextClassLoader(getClass().getClassLoader());
        thread.setDaemon(false);
        thread.start();
    }
}
