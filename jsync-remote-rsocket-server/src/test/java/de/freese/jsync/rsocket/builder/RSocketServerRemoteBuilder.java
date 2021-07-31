// Created: 31.07.2021
package de.freese.jsync.rsocket.builder;

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
import java.util.function.Function;

import javax.net.ssl.TrustManagerFactory;

import org.slf4j.Logger;

import io.netty.handler.ssl.SslProvider;
import io.netty.handler.ssl.util.SelfSignedCertificate;
import io.rsocket.core.RSocketServer;
import io.rsocket.core.Resume;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import reactor.core.publisher.Mono;
import reactor.netty.tcp.SslProvider.ProtocolSslContextSpec;
import reactor.netty.tcp.TcpServer;
import reactor.netty.tcp.TcpSslContextSpec;
import reactor.util.retry.Retry;

/**
 * @author Thomas Freese
 */
public class RSocketServerRemoteBuilder extends AbstractRSocketServerBuilder<RSocketServerRemoteBuilder>
{
    /**
     *
     */
    private ProtocolSslContextSpec protocolSslContextSpec;

    /**
     *
     */
    private Resume resume;

    /**
     *
     */
    private SocketAddress socketAddress;

    /**
    *
    */
    private final List<Function<TcpServer, TcpServer>> tcpServerCustomizers = new ArrayList<>();

    /**
     * @param tcpServerCustomizer {@link Function}
     *
     * @return {@link RSocketServerRemoteBuilder}
     */
    public RSocketServerRemoteBuilder addTcpServerCustomizer(final Function<TcpServer, TcpServer> tcpServerCustomizer)
    {
        this.tcpServerCustomizers.add(Objects.requireNonNull(tcpServerCustomizer, "tcpServerCustomizer required"));

        return this;
    }

    /**
     * @see de.freese.jsync.rsocket.builder.AbstractRSocketBuilder#build()
     */
    @Override
    public Mono<CloseableChannel> build()
    {
        RSocketServer rSocketServer = configure(RSocketServer.create());

        if (this.resume != null)
        {
            rSocketServer = rSocketServer.resume(this.resume);
        }

        TcpServer tcpServer = TcpServer.create();
        tcpServer = tcpServer.bindAddress(() -> Objects.requireNonNull(this.socketAddress, "socketAddress required"));

        if (this.protocolSslContextSpec != null)
        {
            tcpServer = tcpServer.secure(sslContextSpec -> sslContextSpec.sslContext(this.protocolSslContextSpec));
        }

        for (Function<TcpServer, TcpServer> serverCustomizer : this.tcpServerCustomizers)
        {
            tcpServer = serverCustomizer.apply(tcpServer);
        }

        ServerTransport<CloseableChannel> serverTransport = TcpServerTransport.create(tcpServer);

        return rSocketServer.bind(serverTransport);
    }

    /**
     * @param logger {@link Logger}
     *
     * @return {@link RSocketServerRemoteBuilder}
     */
    public RSocketServerRemoteBuilder logTcpServerBoundStatus(final Logger logger)
    {
        // @formatter:off
        addTcpServerCustomizer(tcpServer -> tcpServer
            .doOnBound(server -> logger.info("Bound: {}", server.channel()))
            .doOnUnbound(server -> logger.info("Unbound: {}", server.channel()))
            )
        ;
        // @formatter:on

        return this;
    }

    /**
     * @param protocolSslContextSpec {@link ProtocolSslContextSpec}
     *
     * @return {@link RSocketServerRemoteBuilder}
     */
    public RSocketServerRemoteBuilder protocolSslContextSpec(final ProtocolSslContextSpec protocolSslContextSpec)
    {
        this.protocolSslContextSpec = Objects.requireNonNull(protocolSslContextSpec, "protocolSslContextSpec required");

        return this;
    }

    /**
     * @return {@link RSocketServerRemoteBuilder}
     *
     * @throws Exception Falls was schief geht.
     */
    public RSocketServerRemoteBuilder protocolSslContextSpecCertificate() throws Exception
    {
        KeyStore keyStore = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../../spring/spring-thymeleaf/CA/server_keystore.p12"))
        {
            keyStore.load(is, "password".toCharArray());
        }

        // KeyManagerFactory keyManagerFactory = KeyManagerFactory.getInstance("NewSunX509");
        // keyManagerFactory.init(keyStore, "gehaim".toCharArray());

        KeyStore keyStoreTrust = KeyStore.getInstance("PKCS12");

        try (InputStream is = new FileInputStream("../../spring/spring-thymeleaf/CA/server_truststore.p12"))
        {
            keyStoreTrust.load(is, "password".toCharArray());
        }

        TrustManagerFactory trustManagerFactory = TrustManagerFactory.getInstance("SunX509");
        trustManagerFactory.init(keyStoreTrust);

        Certificate certificate = keyStore.getCertificate("server");
        PrivateKey privateKey = (PrivateKey) keyStore.getKey("server", "password".toCharArray());
        // KeyStore.Entry entry = keyStore.getEntry("server", new KeyStore.PasswordProtection("password".toCharArray()));
        // PrivateKey privateKey = ((KeyStore.PrivateKeyEntry) entry).getPrivateKey();

        // @formatter:off
        this.protocolSslContextSpec = TcpSslContextSpec.forServer(privateKey, (X509Certificate) certificate)
                .configure(builder -> builder
                        //.keyManager(keyManagerFactory) // Verursacht Fehler
                        .trustManager(trustManagerFactory)
                        .protocols("TLSv1.3")
                        .sslProvider(SslProvider.JDK)
                        )
                ;
        // @formatter:on

        return this;
    }

    /**
     * @return {@link RSocketServerRemoteBuilder}
     *
     * @throws Exception Falls was schief geht.
     */
    public RSocketServerRemoteBuilder protocolSslContextSpecCertificateSelfSigned() throws Exception
    {
        SelfSignedCertificate cert = new SelfSignedCertificate();

        // @formatter:off
        this.protocolSslContextSpec = TcpSslContextSpec.forServer(cert.certificate(), cert.privateKey())
                .configure(builder -> builder
                        .protocols("TLSv1.3")
                        .sslProvider(SslProvider.JDK)
                        )
                 ;
         // @formatter:on

        return this;
    }

    /**
     * @param resume {@link Resume}
     *
     * @return {@link RSocketServerRemoteBuilder}
     */
    public RSocketServerRemoteBuilder resume(final Resume resume)
    {
        this.resume = Objects.requireNonNull(resume, "resume required");

        return this;
    }

    /**
     * @param logger {@link Logger}
     *
     * @return {@link RSocketServerRemoteBuilder}
     */
    public RSocketServerRemoteBuilder resumeDefault(final Logger logger)
    {
        // @formatter:off
        this.resume = new Resume()
                .sessionDuration(Duration.ofMinutes(5))
                .retry(Retry.fixedDelay(5, Duration.ofMillis(500))
                        .doBeforeRetry(signal -> logger.info("Disconnected. Trying to resume..."))
                )
                ;
        // @formatter:on

        return this;
    }

    /**
     * @param socketAddress {@link SocketAddress}
     *
     * @return {@link RSocketServerRemoteBuilder}
     */
    public RSocketServerRemoteBuilder socketAddress(final SocketAddress socketAddress)
    {
        this.socketAddress = Objects.requireNonNull(socketAddress, "socketAddress required");

        return this;
    }
}
