// Created: 31.07.2021
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

import javax.net.ssl.TrustManagerFactory;

import io.netty.channel.EventLoopGroup;
import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.rsocket.core.RSocketServer;
import io.rsocket.core.Resume;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.SslProvider.ProtocolSslContextSpec;
import reactor.netty.tcp.TcpServer;
import reactor.netty.tcp.TcpSslContextSpec;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public class RSocketServerRemoteBuilder extends AbstractRSocketServerBuilder<RSocketServerRemoteBuilder> {
    private final List<UnaryOperator<TcpServer>> tcpServerCustomizers = new ArrayList<>();

    public RSocketServerRemoteBuilder addTcpServerCustomizer(final UnaryOperator<TcpServer> tcpServerCustomizer) {
        this.tcpServerCustomizers.add(Objects.requireNonNull(tcpServerCustomizer, "tcpServerCustomizer required"));

        return this;
    }

    @Override
    public Mono<CloseableChannel> build() {
        final RSocketServer rSocketServer = configure(RSocketServer.create());
        final TcpServer tcpServer = configure(TcpServer.create());

        final ServerTransport<CloseableChannel> serverTransport = TcpServerTransport.create(tcpServer);

        // @formatter:off
        return rSocketServer.bind(serverTransport)
                .doOnNext(this::startDaemonOnCloseThread)
                ;
        // @formatter:on
    }

    public RSocketServerRemoteBuilder logTcpServerBoundStatus() {
        // @formatter:off
        addTcpServerCustomizer(tcpServer -> tcpServer
            .doOnBound(server -> getLogger().info("Bound: {}", server.channel()))
            .doOnUnbound(server -> getLogger().info("Unbound: {}", server.channel()))
            )
            ;
        // @formatter:on

        return this;
    }

    public RSocketServerRemoteBuilder protocolSslContextSpec(final ProtocolSslContextSpec protocolSslContextSpec) {
        Objects.requireNonNull(protocolSslContextSpec, "protocolSslContextSpec required");

        addTcpServerCustomizer(tcpServer -> tcpServer.secure(sslContextSpec -> sslContextSpec.sslContext(protocolSslContextSpec)));

        return this;
    }

    public RSocketServerRemoteBuilder protocolSslContextSpecCertificate() throws Exception {
        final KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../../spring/spring-thymeleaf/CA/server_keystore.p12")) {
            keyStore.load(is, "password".toCharArray());
        }

        // KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("NewSunX509");
        // keyManagerFactory.init(keyStore, "gehaim".toCharArray());

        final KeyStore keyStoreTrust = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../../spring/spring-thymeleaf/CA/server_truststore.p12")) {
            keyStoreTrust.load(is, "password".toCharArray());
        }

        final TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStoreTrust);

        final Certificate certificate = keyStore.getCertificate("server");
        final PrivateKey privateKey = (PrivateKey) keyStore.getKey("server", "password".toCharArray());
        // KeyStore.Entry entry = keyStore.getEntry("server", new KeyStore.PasswordProtection("password".toCharArray()));
        // PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();

        // @formatter:off
        final ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forServer(privateKey, (X509Certificate) certificate)
                .configure(builder -> builder
                        //.keyManager(keyManagerFactory) // Verursacht Fehler
                        .trustManager(trustManagerFactory)
                        .protocols("TLSv1.3")
                        .sslProvider(SslProvider.JDK)
                        )
                ;
        // @formatter:on

        protocolSslContextSpec(protocolSslContextSpec);

        return this;
    }

    public RSocketServerRemoteBuilder protocolSslContextSpecCertificateSelfSigned() throws Exception {
        final SelfSignedCertificate cert = new SelfSignedCertificate();

        // @formatter:off
        final ProtocolSslContextSpec protocolSslContextSpec = TcpSslContextSpec.forServer(cert.certificate(), cert.privateKey())
                .configure(builder -> builder
                        .protocols("TLSv1.3")
                        .sslProvider(SslProvider.JDK)
                        )
                 ;
         // @formatter:on

        protocolSslContextSpec(protocolSslContextSpec);

        return this;
    }

    public RSocketServerRemoteBuilder resume(final Resume resume) {
        Objects.requireNonNull(resume, "resume required");

        addRSocketServerCustomizer(rSocketServer -> rSocketServer.resume(resume));

        return this;
    }

    public RSocketServerRemoteBuilder resumeDefault() {
        // @formatter:off
        final Resume resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(Retry.fixedDelay(5, Duration.ofMillis(500))
                        .doBeforeRetry(signal -> getLogger().info("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

        return resume(resume);
    }

    /**
     * EpollEventLoopGroup only available on Linux -> Use NioEventLoopGroup instead.
     */
    public RSocketServerRemoteBuilder runOn(final EventLoopGroup eventLoopGroup) {
        Objects.requireNonNull(eventLoopGroup, "eventLoopGroup required");

        addTcpServerCustomizer(tcpServer -> tcpServer.runOn(eventLoopGroup));

        return this;
    }

    public RSocketServerRemoteBuilder runOn(final LoopResources loopResources) {
        Objects.requireNonNull(loopResources, "loopResources required");

        addTcpServerCustomizer(tcpServer -> tcpServer.runOn(loopResources));

        return this;
    }

    public RSocketServerRemoteBuilder socketAddress(final SocketAddress socketAddress) {
        Objects.requireNonNull(socketAddress, "socketAddress required");

        addTcpServerCustomizer(tcpServer -> tcpServer.bindAddress(() -> socketAddress));

        return this;
    }

    protected TcpServer configure(final TcpServer tcpServer) {
        TcpServer server = tcpServer;

        for (UnaryOperator<TcpServer> serverCustomizer : this.tcpServerCustomizers) {
            server = serverCustomizer.apply(server);
        }

        return server;
    }

    protected void startDaemonOnCloseThread(final CloseableChannel channel) {
        final String name = "daemon-rSocket-" + channel.address().getPort();

        final Thread thread = new Thread(() -> {
            channel.onClose().block();
            getLogger().info("terminated: {}", name);
        }, name);
        thread.setContextClassLoader(getClass().getClassLoader());
        thread.setDaemon(false);
        thread.start();
    }
}
