// Created: 15 Juni 2024
package de.freese.jsync.rsocket.builder.server;

import java.io.FileInputStream;
import java.io.InputStream;
import java.net.SocketAddress;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.function.UnaryOperator;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;

import io.netty.channel.EventLoopGroup;
import io.rsocket.SocketAcceptor;
import io.rsocket.core.RSocketServer;
import io.rsocket.core.Resume;
import io.rsocket.frame.decoder.PayloadDecoder;
import io.rsocket.transport.ServerTransport;
import io.rsocket.transport.netty.server.CloseableChannel;
import io.rsocket.transport.netty.server.TcpServerTransport;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Mono;
import reactor.netty.resources.LoopResources;
import reactor.netty.tcp.SslProvider;
import reactor.netty.tcp.TcpServer;

/**
 * @author Thomas Freese
 */
public final class RSocketServerBuilderRemote {
    private static final Logger LOGGER = LoggerFactory.getLogger(RSocketServerBuilderRemote.class);

    private final RSocketServerBuilderSupport builderSupport = new RSocketServerBuilderSupport();

    public RSocketServerBuilderRemote addRSocketServerCustomizer(final UnaryOperator<RSocketServer> rSocketServerCustomizer) {
        builderSupport.addRSocketServerCustomizer(rSocketServerCustomizer);

        return this;
    }

    public RSocketServerBuilderRemote addTcpServerCustomizer(final UnaryOperator<TcpServer> tcpServerCustomizer) {
        builderSupport.addTcpServerCustomizer(tcpServerCustomizer);

        return this;
    }

    public Mono<CloseableChannel> build() {
        final RSocketServer rSocketServer = builderSupport.configure(RSocketServer.create());
        final TcpServer tcpServer = builderSupport.configure(TcpServer.create());

        final ServerTransport<CloseableChannel> serverTransport = TcpServerTransport.create(tcpServer);

        return rSocketServer.bind(serverTransport)
                .doOnNext(channel -> builderSupport.startDaemonOnCloseThread(channel, LOGGER))
                ;
    }

    public RSocketServerBuilderRemote logTcpServerBoundStatus() {
        builderSupport.logTcpServerBoundStatus(LOGGER);

        return this;
    }

    public RSocketServerBuilderRemote payloadDecoder(final PayloadDecoder payloadDecoder) {
        builderSupport.payloadDecoder(payloadDecoder);

        return this;
    }

    public RSocketServerBuilderRemote protocolSslContextSpec(final SslProvider.ProtocolSslContextSpec protocolSslContextSpec) {
        builderSupport.protocolSslContextSpec(protocolSslContextSpec);

        return this;
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

        builderSupport.protocolSslContextSpecCertificate(keyManagerFactory, trustManagerFactory, (X509Certificate) certificate, privateKey);

        return this;
    }

    public RSocketServerBuilderRemote protocolSslContextSpecCertificateSelfSigned() throws Exception {
        builderSupport.protocolSslContextSpecCertificateSelfSigned();

        return this;
    }

    public RSocketServerBuilderRemote resume(final Resume resume) {
        builderSupport.resume(resume);

        return this;
    }

    public RSocketServerBuilderRemote resumeDefault() {
        builderSupport.resumeDefault(LOGGER);

        return this;
    }

    /**
     * EpollEventLoopGroup only available on Linux -> Use NioEventLoopGroup instead.
     */
    public RSocketServerBuilderRemote runOn(final EventLoopGroup eventLoopGroup) {
        builderSupport.runOn(eventLoopGroup);

        return this;
    }

    public RSocketServerBuilderRemote runOn(final LoopResources loopResources) {
        builderSupport.runOn(loopResources);

        return this;
    }

    public RSocketServerBuilderRemote socketAcceptor(final SocketAcceptor socketAcceptor) {
        builderSupport.socketAcceptor(socketAcceptor);

        return this;
    }

    public RSocketServerBuilderRemote socketAddress(final SocketAddress socketAddress) {
        builderSupport.socketAddress(socketAddress);

        return this;
    }
}
